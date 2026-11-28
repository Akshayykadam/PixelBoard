/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.indexOfFirstMoveResultAfter
import com.akshaykadam.pixelboard.patches.gboard.shared.invokeRegisters
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val gboardRamblerLocaleScriptFixPatch = bytecodePatch(
    description = "Ensure Indic transliteration keyboards (e.g. Marathi mr-Latn-IN) output native script instead of Romanized text during Rambler voice dictation.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findVoiceImeSessionParamsMethod()?.patchVoiceImeSessionParams(this)
        findJetsonLiteHinglishRuleMethods().forEach { it.patchHinglishOverrideRule() }
    }
}

private fun BytecodePatchContext.findVoiceImeSessionParamsMethod(): MutableMethod? {
    val owner = classDefByOrNull { classDef ->
        classDef.methods.any { method ->
            method.parameterTypes == listOf("Landroid/view/inputmethod/EditorInfo;", "Ljava/lang/Object;") &&
                method.containsString("VoiceImeUtils.java") &&
                method.containsString("getSessionParams") &&
                method.containsString("user_preferences")
        }
    } ?: return null
    return mutableClassDefBy(owner).methods.firstOrNull { method ->
        method.parameterTypes == listOf("Landroid/view/inputmethod/EditorInfo;", "Ljava/lang/Object;") &&
            method.containsString("getSessionParams") &&
            method.containsString("user_preferences")
    }
}

private fun BytecodePatchContext.findJetsonLiteHinglishRuleMethods(): List<MutableMethod> {
    val owner = classDefByOrNull { classDef ->
        classDef.methods.any { method ->
            method.containsString("{HINGLISH_OVERRIDE_RULE}") &&
                method.containsStringContaining("Hinglish Override:")
        }
    } ?: return emptyList()
    return mutableClassDefBy(owner).methods.filter { method ->
        method.containsString("{HINGLISH_OVERRIDE_RULE}") &&
            method.containsStringContaining("Hinglish Override:")
    }
}

private fun MutableMethod.patchVoiceImeSessionParams(patchContext: BytecodePatchContext) {
    val normalizeCall = RuntimeCallId.RAMBLER_VOICE_SCRIPT_NORMALIZE_PRIMARY
    val filterSecondaryCall = RuntimeCallId.RAMBLER_VOICE_SCRIPT_FILTER_SECONDARY
    val buildUserPrefsCall = RuntimeCallId.RAMBLER_VOICE_SCRIPT_BUILD_USER_PREFERENCES
    val normalizeRef = RuntimeAbiCatalog.abi(normalizeCall).reference
    val filterRef = RuntimeAbiCatalog.abi(filterSecondaryCall).reference
    val buildUserPrefsRef = RuntimeAbiCatalog.abi(buildUserPrefsCall).reference

    val instructions = implementation?.instructions ?: return
    if (instructions.any {
            it.isMethodReference(normalizeRef) ||
                it.isMethodReference(filterRef) ||
                it.isMethodReference(buildUserPrefsRef)
        }
    ) {
        return
    }

    // 1. Locate `iput-object vSec, vBuilder, Lkmw;->b:Ljava/util/Collection;`
    val secondaryPutIndex = instructions.indexOfFirst { inst ->
        inst.isOpcode("IPUT_OBJECT") &&
            ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.type == "Ljava/util/Collection;"
    }
    if (secondaryPutIndex <= 0) return
    val secondaryPutField = (instructions[secondaryPutIndex] as? ReferenceInstruction)?.reference as? FieldReference
        ?: return
    val sessionBuilderType = secondaryPutField.definingClass
    val secondaryPut = instructions[secondaryPutIndex] as? TwoRegisterInstruction ?: return
    val secReg = secondaryPut.registerA
    if (secReg > 15) return

    // 2. Locate primary LanguageTag lookup: `invoke-static {vCtx, vFilter}, Luya;->aV(Context, Lszw;)Lrzf;`
    //    which precedes `secondaryPutIndex`, returns a non-List class, and takes (Context, <filter>).
    val primaryPut = instructions.subList(0, secondaryPutIndex).lastOrNull { inst ->
        inst.isOpcode("IPUT_OBJECT") &&
            ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.type?.startsWith("L") == true &&
            ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.type != "Ljava/util/Collection;"
    }
    val primaryTagType = ((primaryPut as? ReferenceInstruction)?.reference as? FieldReference)?.type
        ?: return

    val primaryLookupIndex = (0 until secondaryPutIndex).firstOrNull { idx ->
        val inst = instructions[idx]
        if (!inst.isOpcode("INVOKE_STATIC")) return@firstOrNull false
        val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@firstOrNull false
        methodRef.returnType == primaryTagType &&
            methodRef.parameterTypes.size == 2 &&
            methodRef.parameterTypes[0] == "Landroid/content/Context;"
    } ?: return
    val contextReg = (instructions[primaryLookupIndex] as? FiveRegisterInstruction)?.registerC ?: return
    if (contextReg > 15) return

    val moveResultIndex = indexOfFirstMoveResultAfter(primaryLookupIndex)
    if (moveResultIndex <= primaryLookupIndex || moveResultIndex >= secondaryPutIndex) return
    val primaryReg = (instructions[moveResultIndex] as? OneRegisterInstruction)?.registerA ?: return
    if (primaryReg > 15) return

    // 3. Locate `Lkmw;.j:(Lwhe;)V` after secondaryPutIndex and `Lwhe;.o:(Ljava/util/Collection;)Lwhe;`
    var copyOfMethodRef: MethodReference? = null
    val userPrefsSetterIndex = (secondaryPutIndex + 1 until instructions.size).firstOrNull { idx ->
        val inst = instructions[idx]
        if (!inst.isOpcode("INVOKE_VIRTUAL")) return@firstOrNull false
        val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@firstOrNull false
        if (methodRef.definingClass != sessionBuilderType ||
            methodRef.returnType != "V" ||
            methodRef.parameterTypes.size != 1
        ) {
            return@firstOrNull false
        }
        val paramType = methodRef.parameterTypes[0].toString()
        if (!paramType.startsWith("L")) return@firstOrNull false
        val listClass = patchContext.classDefByOrNull(paramType) ?: return@firstOrNull false
        val copyOf = listClass.methods.firstOrNull { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.parameterTypes == listOf("Ljava/util/Collection;") &&
                m.returnType == paramType
        }
        if (copyOf != null) {
            copyOfMethodRef = copyOf
            true
        } else {
            false
        }
    }
    val userPrefsReg = (userPrefsSetterIndex?.let { instructions[it] } as? FiveRegisterInstruction)?.registerD
    val resolvedCopyOf = copyOfMethodRef

    // Insert in descending instruction order: userPreferences enrichment -> secondary filter -> primary normalize.
    if (userPrefsSetterIndex != null && userPrefsReg != null && userPrefsReg <= 15 && resolvedCopyOf != null) {
        addInstructionsWithLabels(
            userPrefsSetterIndex,
            """
                ${RuntimeCallEmitter.invoke(buildUserPrefsCall, "v$contextReg, v$primaryReg, v$userPrefsReg")}
                move-result-object v$secReg
                if-eqz v$secReg, :skip_indic_user_prefs
                invoke-static {v$secReg}, $resolvedCopyOf
                move-result-object v$userPrefsReg
                :skip_indic_user_prefs
                nop
            """.trimIndent(),
        )
    }

    addInstructions(
        secondaryPutIndex,
        """
            ${RuntimeCallEmitter.invoke(filterSecondaryCall, "v$primaryReg, v$secReg")}
            move-result-object v$secReg
        """.trimIndent(),
    )

    // Normalize primary LanguageTag right after Luya.aV move-result-object.
    addInstructions(
        moveResultIndex + 1,
        """
            ${RuntimeCallEmitter.invoke(normalizeCall, "v$primaryReg")}
            move-result-object v$primaryReg
            check-cast v$primaryReg, $primaryTagType
        """.trimIndent(),
    )
}

private fun MutableMethod.patchHinglishOverrideRule() {
    val resolveCall = RuntimeCallId.RAMBLER_VOICE_SCRIPT_RESOLVE_HINGLISH_RULE
    val resolveRef = RuntimeAbiCatalog.abi(resolveCall).reference
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(resolveRef) }) return

    val targetIndex = instructions.indexOfFirst { inst ->
        inst.isOpcode("CONST_STRING") &&
            ((inst as? ReferenceInstruction)?.reference?.toString()?.contains("Hinglish Override:") == true)
    }
    if (targetIndex < 0) return

    val reg = (instructions[targetIndex] as? OneRegisterInstruction)?.registerA ?: return
    if (reg > 15) return

    addInstructions(
        targetIndex + 1,
        """
            ${RuntimeCallEmitter.invoke(resolveCall, "v$reg")}
            move-result-object v$reg
        """.trimIndent(),
    )
}

private fun com.android.tools.smali.dexlib2.iface.Method.containsString(value: String): Boolean =
    implementation?.instructions?.any { inst ->
        inst.isOpcode("CONST_STRING") &&
            (inst as? ReferenceInstruction)?.reference?.toString() == value
    } == true

private fun com.android.tools.smali.dexlib2.iface.Method.containsStringContaining(substring: String): Boolean =
    implementation?.instructions?.any { inst ->
        inst.isOpcode("CONST_STRING") &&
            ((inst as? ReferenceInstruction)?.reference?.toString()?.contains(substring) == true)
    } == true
