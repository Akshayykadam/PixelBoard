package com.akshaykadam.pixelboard.patches.gboard.features.stickers

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

private const val ACCESS_POINTS_BAR =
    "Lcom/google/android/libraries/inputmethod/accesspoint/widget/AccessPointsBar;"

// The bar holding the Sticker / GIF / Clipboard access points in the keyboard header.
private val accessPointsBarTargets = listOf(
    GboardMethodTarget(
        classType = ACCESS_POINTS_BAR,
        name = "onFinishInflate",
        parameterTypes = emptyList(),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = ACCESS_POINTS_BAR,
        name = "onVisibilityChanged",
        parameterTypes = listOf("Landroid/view/View;", "I"),
        returnType = "V",
    ),
)

// The only place Gboard sends GIFs and stickers: a static outline wrapping commitContent.
private val commitContentTarget = GboardMethodTarget(
    classType = "Lgu\$\$ExternalSyntheticApiModelOutline0;",
    name = "m",
    parameterTypes = listOf(
        "Landroid/view/inputmethod/InputConnection;",
        "Landroid/view/inputmethod/InputContentInfo;",
        "I",
        "Landroid/os/Bundle;",
    ),
    returnType = "Z",
)

internal val gboardCustomStickersCapturePatch = bytecodePatch(
    description = "Let the next GIF or sticker picked in Gboard be saved as a custom sticker.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = findMutableMethodOrNull(commitContentTarget) ?: return@execute
        val instructions = method.implementation?.instructions ?: return@execute
        val call = RuntimeCallId.CUSTOM_STICKERS_INTERCEPT_COMMIT_CONTENT
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }
        // No spare registers: the runtime hands the content back (send it) or null (it was
        // captured), and p1 is free to hold the "handled" result once it is null.
        method.addInstructionsWithLabels(
            0,
            """
                ${RuntimeCallEmitter.invoke(call, "p1")}
                move-result-object p1
                if-nez p1, :pixelboard_commit_content
                const/4 p1, 0x1
                return p1
            """.trimIndent(),
            ExternalLabel("pixelboard_commit_content", instructions.first()),
        )
    }
}

internal val gboardCustomStickersBarPatch = bytecodePatch(
    description = "Add a Custom Stickers button next to the keyboard header access points.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val call = RuntimeCallId.CUSTOM_STICKERS_ON_ACCESS_POINTS_BAR_CHANGED
        val reference = RuntimeAbiCatalog.abi(call).reference
        accessPointsBarTargets.forEach { target ->
            val method = findMutableMethodOrNull(target) ?: return@forEach
            val instructions = method.implementation?.instructions ?: return@forEach
            if (instructions.any { it.isMethodReference(reference) }) {
                return@forEach
            }
            val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            if (returnIndex == -1) return@forEach
            method.addInstructions(returnIndex, RuntimeCallEmitter.invoke(call, "p0"))
        }
    }
}
