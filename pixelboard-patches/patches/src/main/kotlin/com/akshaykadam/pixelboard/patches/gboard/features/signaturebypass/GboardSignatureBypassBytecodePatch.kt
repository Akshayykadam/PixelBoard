/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.signaturebypass

import com.akshaykadam.pixelboard.patches.shared.addInstruction
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.mutableClass
import com.akshaykadam.pixelboard.patches.gboard.shared.VerifiedTransformationPlan
import com.akshaykadam.pixelboard.patches.gboard.shared.VerifiedTransformationState
import com.akshaykadam.pixelboard.patches.gboard.shared.applyVerified
import com.akshaykadam.pixelboard.patches.gboard.shared.isFieldReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isLiteralWrite
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

private const val SIGNATURE_UTILS_CLASS = "Lrpv;"
private const val SIGNATURE_UTILS_CLASS_1831 = "Lajpz;"
private const val SIGNATURE_UTILS_CLASS_1831_RELEASE = "Lrvl;"

internal val gboardSignatureBypassBytecodePatch = bytecodePatch(
    description = "Force bypass Gboard signature whitelist checks."
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        injectSignatureBypass()
    }
}

context(context: BytecodePatchContext)
private fun injectSignatureBypass() = with(context) {
    val targetClass = when {
        mutableClassDefByOrNull(SIGNATURE_UTILS_CLASS_1831_RELEASE) != null -> SIGNATURE_UTILS_CLASS_1831_RELEASE
        mutableClassDefByOrNull(SIGNATURE_UTILS_CLASS_1831) != null -> SIGNATURE_UTILS_CLASS_1831
        else -> SIGNATURE_UTILS_CLASS
    }
    mutableClass(targetClass)
        .methods
        .findGboardSignatureBypassTargetOrThrow()
        .applyGboardSignatureBypass()
}

internal fun Iterable<MutableMethod>.findGboardSignatureBypassTargetOrThrow(): MutableMethod {
    val targets = filter(MutableMethod::isExactGboardSignatureBypassTarget)
    check(targets.size == 1) {
        "Expected exactly one signature bypass target, found ${targets.size}"
    }
    return targets.single()
}

internal fun MutableMethod.applyGboardSignatureBypass() {
    applyVerified(
        VerifiedTransformationPlan(
            targetName = "${definingClass}->$SIGNATURE_CHECK_METHOD_NAME",
            classify = MutableMethod::classifyGboardSignatureBypass,
            mutate = { method ->
                method.forceSignatureBypassReturns()
                method
            },
        ),
    )
}

private fun MutableMethod.classifyGboardSignatureBypass(): VerifiedTransformationState {
    check(isExactGboardSignatureBypassTarget()) {
        "Refusing non-target signature bypass method $definingClass->$name"
    }
    val instructions = implementation?.instructions
        ?: error("No instructions available in $definingClass->$name")
    check(implementation!!.registerCount == TARGET_REGISTER_COUNT) {
        "Unexpected register count in $definingClass->$name"
    }

    val returnIndices = instructions.indices.filter { index ->
        instructions[index].isOpcode("RETURN")
    }
    check(returnIndices.size == TARGET_RETURN_REGISTERS.size) {
        "Expected three normal returns in $definingClass->$name"
    }
    val returnRegisters = returnIndices.map { returnIndex ->
        (instructions[returnIndex] as? OneRegisterInstruction)?.registerA
            ?: error("RETURN at $returnIndex has no register")
    }
    check(returnRegisters == TARGET_RETURN_REGISTERS) {
        "Unexpected normal return registers in $definingClass->$name"
    }
    val digestMethod = when (definingClass) {
        SIGNATURE_UTILS_CLASS_1831_RELEASE -> DIGEST_METHOD_DESCRIPTOR_1831_RELEASE
        SIGNATURE_UTILS_CLASS_1831 -> DIGEST_METHOD_DESCRIPTOR_1831
        else -> DIGEST_METHOD_DESCRIPTOR
    }
    val targetFields = when (definingClass) {
        SIGNATURE_UTILS_CLASS_1831_RELEASE -> TARGET_FIELD_DESCRIPTORS_1831_RELEASE
        SIGNATURE_UTILS_CLASS_1831 -> TARGET_FIELD_DESCRIPTORS_1831
        else -> TARGET_FIELD_DESCRIPTORS
    }
    check(instructions.count { it.isMethodReference(digestMethod) } == 1) {
        "Expected exact digest call in $definingClass->$name"
    }
    check(instructions.count { it.isMethodReference(ARRAYS_EQUALS_DESCRIPTOR) } == 1) {
        "Expected exact digest comparison in $definingClass->$name"
    }
    targetFields.forEach { descriptor ->
        check(instructions.count { it.isFieldReference(descriptor) } == 1) {
            "Expected exact field $descriptor in $definingClass->$name"
        }
    }
    TARGET_BASELINE_LITERALS.forEach { expected ->
        check(instructions.any { instruction -> instruction.matchesLiteral(expected) }) {
            "Expected exact literal shape $expected in $definingClass->$name; " +
                "actual=${instructions.mapNotNull { instruction -> instruction.literalShape() }}"
        }
    }

    val completedReturns = returnIndices.count { returnIndex ->
        val returned = instructions[returnIndex] as OneRegisterInstruction
        instructions.getOrNull(returnIndex - 1).isForcedTrueFor(returned.registerA)
    }
    val oneLiteralCount = instructions.count { instruction ->
        instruction.normalizedOpcode().startsWith("CONST") &&
            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 1
    }
    return when {
        completedReturns == returnIndices.size &&
            oneLiteralCount == TARGET_COMPLETED_ONE_LITERAL_COUNT ->
            VerifiedTransformationState.PATCHED
        completedReturns == 0 &&
            oneLiteralCount == TARGET_STOCK_ONE_LITERAL_COUNT ->
            VerifiedTransformationState.STOCK
        else -> VerifiedTransformationState.MALFORMED
    }
}

private fun MutableMethod.forceSignatureBypassReturns() {
    val instructions = implementation?.instructions
        ?: error("No instructions available in $definingClass->$name")
    val returnIndices = instructions.indices.filter { index ->
        instructions[index].isOpcode("RETURN")
    }
    returnIndices.asReversed().forEach { returnIndex ->
        val resultRegister = (instructions[returnIndex] as OneRegisterInstruction).registerA
        check(resultRegister <= MAX_CONST_4_REGISTER) {
            "RETURN register v$resultRegister cannot be forced with const/4"
        }
        replaceInstruction(returnIndex, "const/4 v$resultRegister, 0x1")
        addInstruction(returnIndex + 1, "return v$resultRegister")
    }
}

private fun MutableMethod.isExactGboardSignatureBypassTarget(): Boolean =
    (definingClass == SIGNATURE_UTILS_CLASS ||
        definingClass == SIGNATURE_UTILS_CLASS_1831 ||
        definingClass == SIGNATURE_UTILS_CLASS_1831_RELEASE) &&
        name == SIGNATURE_CHECK_METHOD_NAME &&
        returnType == "Z" &&
        parameterTypes == SIGNATURE_CHECK_PARAMETERS &&
        accessFlags == TARGET_ACCESS_FLAGS

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction?.isForcedTrueFor(
    register: Int,
): Boolean = this?.isOpcode("CONST_4") == true && isLiteralWrite(register, 1)

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.matchesLiteral(
    expected: LiteralShape,
): Boolean = literalShape() == expected

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.literalShape(): LiteralShape? {
    if (!isOpcode("CONST_4")) return null
    val register = (this as? OneRegisterInstruction)?.registerA ?: return null
    val literal = (this as? NarrowLiteralInstruction)?.narrowLiteral ?: return null
    return LiteralShape(register, literal)
}

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction
    .normalizedOpcode(): String = opcode.name.uppercase().replace('-', '_').replace('/', '_')

private data class LiteralShape(
    val register: Int,
    val literal: Int,
)

private const val SIGNATURE_CHECK_METHOD_NAME = "a"
private const val SIGNATURE_CHECK_DESCRIPTOR =
    "Lrpv;->a(Landroid/content/Context;Ljava/lang/String;)Z"
private val SIGNATURE_CHECK_PARAMETERS =
    listOf("Landroid/content/Context;", "Ljava/lang/String;")
private const val DIGEST_METHOD_DESCRIPTOR =
    "Lrpv;->c(Landroid/content/Context;Ljava/lang/String;)[B"
private const val DIGEST_METHOD_DESCRIPTOR_1831 =
    "Lajpz;->c(Landroid/content/Context;Ljava/lang/String;)[B"
private const val DIGEST_METHOD_DESCRIPTOR_1831_RELEASE =
    "Lrvl;->c(Landroid/content/Context;Ljava/lang/String;)[B"
private const val ARRAYS_EQUALS_DESCRIPTOR = "Ljava/util/Arrays;->equals([B[B)Z"
private val TARGET_FIELD_DESCRIPTORS = listOf(
    "Lrpv;->e:[B",
    "Lrpv;->d:[B",
    "Lrpv;->c:[B",
    "Lrox;->b:Z",
)
private val TARGET_FIELD_DESCRIPTORS_1831 = listOf(
    "Lajpz;->e:[B",
    "Lajpz;->d:[B",
    "Lajpz;->c:[B",
    "Lajom;->b:Z",
)
private val TARGET_FIELD_DESCRIPTORS_1831_RELEASE = listOf(
    "Lrvl;->e:[B",
    "Lrvl;->d:[B",
    "Lrvl;->c:[B",
    "Lrum;->b:Z",
)
private val TARGET_BASELINE_LITERALS = listOf(
    LiteralShape(0, 3),
    LiteralShape(3, 0),
    LiteralShape(4, 1),
    LiteralShape(5, 2),
)
private val TARGET_RETURN_REGISTERS = listOf(6, 4, 3)
private const val TARGET_REGISTER_COUNT = 8
private const val TARGET_STOCK_ONE_LITERAL_COUNT = 1
private const val TARGET_COMPLETED_ONE_LITERAL_COUNT = 4
private const val MAX_CONST_4_REGISTER = 15
private val TARGET_ACCESS_FLAGS = AccessFlags.PUBLIC.value or AccessFlags.STATIC.value
