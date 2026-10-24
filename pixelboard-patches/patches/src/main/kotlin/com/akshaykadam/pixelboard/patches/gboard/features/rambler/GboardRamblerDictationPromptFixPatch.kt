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
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

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
)

internal val gboardRamblerDictationPromptFixPatch = bytecodePatch(
    description = "Ensure Rambler dictation commits verbatim spoken transcript instead of AI prompt responses.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = ramblerOnVoiceTextTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) } ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.RAMBLER_DICTATION_RUNTIME_FILTER_VOICE_TEXT
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        // p1 is the Laajc voice text proto parameter.
        // Method j has 30 locals (registers 30-33), so range invocation is required for p1.
        method.addInstructions(
            0,
            RuntimeCallEmitter.invoke(call, "p1 .. p1")
        )
    }
}
