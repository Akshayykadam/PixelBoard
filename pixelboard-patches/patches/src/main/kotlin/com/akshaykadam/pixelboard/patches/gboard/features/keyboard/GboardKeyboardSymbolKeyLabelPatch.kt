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

private val inputConnectionCommitTargets = listOf(
    // Gboard 18.6.3
    GboardMethodTarget(
        classType = "Lowy;",
        name = "f",
        parameterTypes = listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I"),
        returnType = "V",
    ),
    // Gboard 18.5.3
    GboardMethodTarget(
        classType = "Lpdd;",
        name = "f",
        parameterTypes = listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I"),
        returnType = "V",
    ),
)

private val inputConnectionSetComposingTargets = listOf(
    // Gboard 18.6.3
    GboardMethodTarget(
        classType = "Lowy;",
        name = "h",
        parameterTypes = listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I", "Ljava/lang/Object;"),
        returnType = "V",
    ),
    // Gboard 18.5.3
    GboardMethodTarget(
        classType = "Lpdd;",
        name = "h",
        parameterTypes = listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I", "Ljava/lang/Object;"),
        returnType = "V",
    ),
)

internal val gboardKeyboardSymbolKeyLabelPatch = bytecodePatch(
    description = "Sanitize symbol key and other softkey labels to remove literal escaping artifacts at runtime and intercept InputConnection commits.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val methodsToHook = mutableSetOf<MutableMethod>()

        // Hook on-screen key label rendering in AutoCenterScaleTextView
        findMutableMethodOrNull(autoCenterScaleTextViewSetTextTarget)?.let { methodsToHook.add(it) }

        // Hook InputConnectionUtil commitText and setComposingText to intercept text insertion
        for (target in inputConnectionCommitTargets + inputConnectionSetComposingTargets) {
            findMutableMethodOrNull(target)?.let { methodsToHook.add(it) }
        }

        // Dynamic lookup for InputConnectionUtil across any Gboard version
        try {
            val icuClassDef = getAllClassesWithString("com/google/android/libraries/inputmethod/inputconnection/InputConnectionUtil").firstOrNull()
            if (icuClassDef != null) {
                val mutableIcu = mutableClassDefBy(icuClassDef)
                mutableIcu.methods.firstOrNull {
                    it.returnType == "V" && it.parameterTypes == listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I")
                }?.let { methodsToHook.add(it) }
                mutableIcu.methods.firstOrNull {
                    it.returnType == "V" && it.parameterTypes == listOf("Landroid/view/inputmethod/InputConnection;", "Ljava/lang/CharSequence;", "I", "Ljava/lang/Object;")
                }?.let { methodsToHook.add(it) }
            }
        } catch (_: Throwable) {
            // Dynamic lookup best-effort
        }

        val call = RuntimeCallId.KEYBOARD_RUNTIME_SANITIZE_KEY_LABEL
        for (method in methodsToHook) {
            val implementation = method.implementation ?: continue
            val instructions = implementation.instructions
            if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
                continue
            }

            // In AutoCenterScaleTextView.setText(CharSequence, BufferType), p1 is CharSequence.
            // In InputConnectionUtil.f(InputConnection, CharSequence, int), p1 is CharSequence.
            // In InputConnectionUtil.h(InputConnection, CharSequence, int, Object), p1 is CharSequence.
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
