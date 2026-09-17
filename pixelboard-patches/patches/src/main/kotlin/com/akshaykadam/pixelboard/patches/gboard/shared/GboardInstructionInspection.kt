package com.akshaykadam.pixelboard.patches.gboard.shared

import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal fun Instruction.isOpcode(expected: String): Boolean =
    opcode.name.normalizedGboardOpcode() == expected.normalizedGboardOpcode()

internal fun Instruction.isMethodReference(descriptor: String): Boolean =
    ((this as? ReferenceInstruction)?.reference as? MethodReference)?.toString() == descriptor

internal fun Instruction.isMethodReferenceInClass(classDescriptor: String): Boolean =
    ((this as? ReferenceInstruction)?.reference as? MethodReference)?.definingClass == classDescriptor

internal fun Instruction.isFieldReference(descriptor: String): Boolean =
    ((this as? ReferenceInstruction)?.reference as? FieldReference)?.toString() == descriptor

internal fun Instruction.isReference(descriptor: String): Boolean =
    (this as? ReferenceInstruction)?.reference?.toString() == descriptor

internal fun Instruction.invokeRegisters(descriptor: String): List<Int>? {
    if (!isMethodReference(descriptor) || !opcode.name.normalizedGboardOpcode().startsWith("INVOKE_")) {
        return null
    }
    return when (this) {
        is RegisterRangeInstruction ->
            (startRegister until startRegister + registerCount).toList()
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
            .take(registerCount)
        else -> null
    }
}

internal fun Instruction.isInvoke(
    opcode: String,
    descriptor: String,
    vararg registers: Int,
): Boolean = isOpcode(opcode) && invokeRegisters(descriptor) == registers.toList()

internal fun Instruction.isRegisterOperation(opcode: String, vararg registers: Int): Boolean =
    isOpcode(opcode) && registerShape() == registers.toList()

internal fun Instruction.isLiteralWrite(register: Int, literal: Long): Boolean =
    (this as? OneRegisterInstruction)?.registerA == register &&
        when (this) {
            is WideLiteralInstruction -> wideLiteral == literal
            is NarrowLiteralInstruction -> narrowLiteral.toLong() == literal
            else -> false
        }

internal fun Instruction.semanticShape(): String = buildString {
    append(opcode.name.normalizedGboardOpcode())
    when (this@semanticShape) {
        is RegisterRangeInstruction -> append("|range=")
            .append(startRegister).append(',').append(registerCount)
        is FiveRegisterInstruction -> append("|5=")
            .append(registerCount).append(',').append(registerC).append(',')
            .append(registerD).append(',').append(registerE).append(',')
            .append(registerF).append(',').append(registerG)
        is ThreeRegisterInstruction -> append("|3=")
            .append(registerA).append(',').append(registerB).append(',').append(registerC)
        is TwoRegisterInstruction -> append("|2=")
            .append(registerA).append(',').append(registerB)
        is OneRegisterInstruction -> append("|1=").append(registerA)
    }
    if (this@semanticShape is ReferenceInstruction) {
        append("|ref=").append(reference)
    }
    when (this@semanticShape) {
        is WideLiteralInstruction -> append("|wide=").append(wideLiteral)
        is NarrowLiteralInstruction -> append("|narrow=").append(narrowLiteral)
    }
    if (this@semanticShape is OffsetInstruction) {
        append("|offset=").append(codeOffset)
    }
}

internal fun List<Instruction>.indexOfLiteralGuard(
    literal: Long,
    branchOpcode: String,
): Int = (0 until lastIndex).indexOfFirst { index ->
    val instruction = this[index]
    (instruction is NarrowLiteralInstruction || instruction is WideLiteralInstruction) &&
        instruction.literalValue() == literal &&
        this[index + 1].isOpcode(branchOpcode)
}

private fun Instruction.literalValue(): Long? = when (this) {
    is WideLiteralInstruction -> wideLiteral
    is NarrowLiteralInstruction -> narrowLiteral.toLong()
    else -> null
}

private fun Instruction.registerShape(): List<Int>? = when (this) {
    is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
    is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
        .take(registerCount)
    is ThreeRegisterInstruction -> listOf(registerA, registerB, registerC)
    is TwoRegisterInstruction -> listOf(registerA, registerB)
    is OneRegisterInstruction -> listOf(registerA)
    else -> null
}

private fun String.normalizedGboardOpcode(): String =
    uppercase().replace('-', '_').replace('/', '_')
