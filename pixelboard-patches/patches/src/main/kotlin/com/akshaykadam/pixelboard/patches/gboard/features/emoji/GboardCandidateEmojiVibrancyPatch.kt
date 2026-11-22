package com.akshaykadam.pixelboard.patches.gboard.features.emoji

import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private val autoCenterScaleTextViewOnDrawTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;",
    name = "onDraw",
    parameterTypes = listOf("Landroid/graphics/Canvas;"),
    returnType = "V",
)

private const val LATIN_FIXED_COUNT_CANDIDATES_HOLDER_VIEW_TYPE =
    "Lcom/google/android/apps/inputmethod/latin/keyboard/widget/LatinFixedCountCandidatesHolderView;"

internal val gboardCandidateEmojiVibrancyPatch = bytecodePatch(
    description = "Ensure candidate strip emojis render with full opacity and vibrant colors, and position emoji suggestions on the right side of the candidate strip.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        // 1. TextPaint vibrancy hook
        findMutableMethodOrNull(autoCenterScaleTextViewOnDrawTarget)?.let { method ->
            val implementation = method.implementation ?: return@let
            val instructions = implementation.instructions
            val call = RuntimeCallId.CANDIDATE_EMOJI_RUNTIME_ADJUST_CANDIDATE_PAINT
            if (!instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
                val setColorIndex = instructions.indexOfFirst { instruction ->
                    instruction.isMethodReference("Landroid/text/TextPaint;->setColor(I)V") ||
                        instruction.isMethodReference("Landroid/graphics/Paint;->setColor(I)V")
                }
                if (setColorIndex != -1) {
                    val setColorInstruction = instructions[setColorIndex]
                    val paintReg = if (setColorInstruction is FiveRegisterInstruction) {
                        "v${setColorInstruction.registerC}"
                    } else {
                        "v0"
                    }
                    method.addInstructions(
                        setColorIndex + 1,
                        RuntimeCallEmitter.invoke(call, "p0, $paintReg")
                    )
                }
            }
        }

        // 2. Candidate strip layout hook: position suggested emojis on the right side (slot 2)
        val holderClass = mutableClassDefByOrNull(LATIN_FIXED_COUNT_CANDIDATES_HOLDER_VIEW_TYPE)
        val appendMethod = holderClass?.methods?.firstOrNull { method ->
            method.parameterTypes == listOf("Ljava/util/List;", "Ljava/util/ArrayList;") &&
                method.returnType == "V"
        }
        if (appendMethod != null) {
            val call = RuntimeCallId.CANDIDATE_EMOJI_RUNTIME_REORDER_CANDIDATES
            val instructions = appendMethod.implementation?.instructions
            if (instructions != null && !instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
                appendMethod.addInstructions(
                    0,
                    """
                        ${RuntimeCallEmitter.invoke(call, "p0 .. p1")}
                        move-result-object p1
                    """.trimIndent()
                )
            }
        }
    }
}
