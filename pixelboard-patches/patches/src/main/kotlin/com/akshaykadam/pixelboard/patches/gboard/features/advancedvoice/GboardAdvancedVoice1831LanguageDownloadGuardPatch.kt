package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** LanguageDownloadQueue.enqueue(languageTag, source, durationConsumer) in 18.3.1 and 18.4.1. */
private val languageDownloadEnqueueTargets = listOf(
    // 18.4.1 release
    GboardMethodTarget(
        classType = "Lsqk;",
        name = "c",
        parameterTypes = listOf("Ljava/lang/String;", "Lsrc;", "Ljava/util/function/Consumer;"),
        returnType = "V",
    ),
    // 18.3.1 release
    GboardMethodTarget(
        classType = "Lsnm;",
        name = "c",
        parameterTypes = listOf("Ljava/lang/String;", "Lsoe;", "Ljava/util/function/Consumer;"),
        returnType = "V",
    ),
)

internal val gboardAdvancedVoice1831LanguageDownloadGuardPatch = bytecodePatch(
    description = "Request each language pack download only once per process: the speech " +
        "service refuses silent downloads but completes them, which re-triggers the request.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = languageDownloadEnqueueTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) } ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.LANGUAGE_DOWNLOAD_GUARD_SHOULD_SKIP
        val isQueue = instructions.any {
            ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "languageTag"
        }
        if (!isQueue || instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }
        check(implementation.registerCount > 4) { "No free local register for the guard" }
        method.addInstructionsWithLabels(
            0,
            """
                ${RuntimeCallEmitter.invoke(call, "p1, p2")}
                move-result v0
                if-eqz v0, :pixelboard_language_download_continue
                return-void
            """.trimIndent(),
            ExternalLabel("pixelboard_language_download_continue", instructions.first()),
        )
    }
}
