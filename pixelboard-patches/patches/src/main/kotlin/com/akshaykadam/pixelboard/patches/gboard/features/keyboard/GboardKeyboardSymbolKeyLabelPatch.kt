package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
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
        val methodsToHook = mutableSetOf<MutableMethod>()

        // Hook on-screen key label rendering in AutoCenterScaleTextView
        findMutableMethodOrNull(autoCenterScaleTextViewSetTextTarget)?.let { methodsToHook.add(it) }

        val call = RuntimeCallId.KEYBOARD_RUNTIME_SANITIZE_KEY_LABEL
        for (method in methodsToHook) {
            val implementation = method.implementation ?: continue
            val instructions = implementation.instructions
            if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
                continue
            }

            // In AutoCenterScaleTextView.setText(CharSequence, BufferType), p1 is CharSequence.
            method.addInstructions(
                0,
                """
                    ${RuntimeCallEmitter.invoke(call, "p1")}
                    move-result-object p1
                """.trimIndent()
            )
        }
    }
}
