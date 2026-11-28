/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.returnInstructionIndices
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private val ramblerOnVoiceTextTargets = listOf(
    GboardMethodTarget(
        classType = "Lfhv;",
        name = "j",
        parameterTypes = listOf(
            "Laaru;",
            "Lsgm;",
            "Ljava/lang/String;",
        ),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lfde;",
        name = "j",
        parameterTypes = listOf(
            "Laajc;",
            "Lrzc;",
            "Ljava/lang/String;",
        ),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lffh;",
        name = "j",
        parameterTypes = listOf(
            "Laaks;",
            "Lsao;",
            "Ljava/lang/String;",
        ),
        returnType = "V",
    ),
)

internal val gboardRamblerDictationPromptFixPatch = bytecodePatch(
    description = "Ensure Rambler dictation honors verbatim mode, custom user_preferences, and commits verbatim spoken transcript instead of AI summaries or errant diffs.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        ramblerOnVoiceTextTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) }?.let { method ->
            val implementation = method.implementation
            val instructions = implementation?.instructions
            val call = RuntimeCallId.RAMBLER_DICTATION_RUNTIME_FILTER_VOICE_TEXT
            if (instructions != null &&
                instructions.none { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }
            ) {
                // p0 is this (AgenticDictationExtension), p1 is the voice text proto parameter.
                // Method j has 30 locals (registers 30-33), so range invocation is required for p0 .. p1.
                method.addInstructions(
                    0,
                    RuntimeCallEmitter.invoke(call, "p0 .. p1"),
                )
            }
        }

        findSessionBundleBuilderMethod()?.applySessionBundlePreferencesHook()
    }
}

private fun com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext.findSessionBundleBuilderMethod(): MutableMethod? {
    val candidates = ramblerOnVoiceTextTargets.map { target -> target.ownerDescriptor }
    for (classType in candidates) {
        val classDef = mutableClassDefByOrNull(classType) ?: continue
        val method = classDef.methods.firstOrNull { method ->
            method.returnType == "Landroid/os/Bundle;" &&
                method.hasStringConstant("user_preferences") &&
                method.hasStringConstant("session_type")
        }
        if (method != null) return method
    }
    val matchedClass = classDefByOrNull { classDef ->
        classDef.methods.any { method ->
            method.returnType == "Landroid/os/Bundle;" &&
                method.implementation?.instructions?.any { inst ->
                    inst.isOpcode("CONST_STRING") &&
                        (inst as? ReferenceInstruction)?.reference?.toString() == "user_preferences"
                } == true &&
                method.implementation?.instructions?.any { inst ->
                    inst.isOpcode("CONST_STRING") &&
                        (inst as? ReferenceInstruction)?.reference?.toString() == "session_type"
                } == true
        }
    } ?: return null
    return mutableClassDefBy(matchedClass).methods.firstOrNull { method ->
        method.returnType == "Landroid/os/Bundle;" &&
            method.hasStringConstant("user_preferences") &&
            method.hasStringConstant("session_type")
    }
}

private fun MutableMethod.hasStringConstant(value: String): Boolean =
    implementation?.instructions?.any { inst ->
        inst.isOpcode("CONST_STRING") &&
            (inst as? ReferenceInstruction)?.reference?.toString() == value
    } == true

private fun MutableMethod.applySessionBundlePreferencesHook() {
    val call = RuntimeCallId.RAMBLER_DICTATION_RUNTIME_APPLY_PREFERENCES
    val callRef = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(callRef) }) return

    val returns = returnInstructionIndices().filter { index ->
        implementation!!.instructions[index].isOpcode("RETURN_OBJECT")
    }
    if (returns.isEmpty()) return

    returns.asReversed().forEach { returnIndex ->
        val retReg = (implementation!!.instructions[returnIndex] as? OneRegisterInstruction)?.registerA
            ?: return@forEach
        if (retReg > 15) return@forEach
        addInstructions(
            returnIndex,
            """
                ${RuntimeCallEmitter.invoke(call, "p0, v$retReg")}
                move-result-object v$retReg
            """.trimIndent(),
        )
    }
}
