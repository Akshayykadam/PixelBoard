package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

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

private val autoCenterScaleTextViewSetTextTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;",
    name = "setText",
    parameterTypes = listOf("Ljava/lang/CharSequence;", "Landroid/widget/TextView\$BufferType;"),
    returnType = "V",
)

internal val gboardKeyboardSymbolKeyLabelPatch = bytecodePatch(
    description = "Sanitize symbol key and other softkey labels to remove literal escaping artifacts at runtime.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = findMutableMethodOrNull(autoCenterScaleTextViewSetTextTarget) ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.KEYBOARD_RUNTIME_SANITIZE_KEY_LABEL
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        // p1 is the CharSequence parameter in setText(CharSequence, BufferType)
        method.addInstructions(
            0,
            """
                ${RuntimeCallEmitter.invoke(call, "p1")}
                move-result-object p1
            """.trimIndent()
        )
    }
}
