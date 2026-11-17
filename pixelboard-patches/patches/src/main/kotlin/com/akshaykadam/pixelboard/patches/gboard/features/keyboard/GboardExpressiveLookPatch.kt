package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.addHelperMethodIfMissing
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags

private const val SOFT_KEY_VIEW = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;"
private const val BASIC_POPUP_VIEW = "Lcom/google/android/libraries/inputmethod/popup/BasicPopupView;"

private val basicPopupViewShowParameterTypes = listOf(
    "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;",
    "Landroid/view/View;",
    "F",
    "F",
    "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;",
    "[I",
    "Z",
)

// TopRoundedKeyboard module (module id 392): applyOutline(KeyboardViewType, View).
// It runs for every keyboard view once the top_rounded_keyboard flag is on, which
// the Expressive look setting forces through the flag override hook.
private val topRoundedKeyboardApplyOutlineTargets = listOf(
    // Gboard 18.6.3
    GboardMethodTarget(
        classType = "Lqvg;",
        name = "t",
        parameterTypes = listOf("Lpwj;", "Landroid/view/View;"),
        returnType = "V",
    ),
    // Gboard 18.5.3
    GboardMethodTarget(
        classType = "Lrbm;",
        name = "s",
        parameterTypes = listOf("Lqcl;", "Landroid/view/View;"),
        returnType = "V",
    ),
)

internal val gboardExpressiveLookPatch = bytecodePatch(
    description = "Apply the Material 3 Expressive look (sunburst toolbar buttons, no top hairline) to keyboard views.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        hookTopRoundedKeyboardOutline()
        hookSoftKeyViewPressed()
        hookBasicPopupViewShow()
    }
}

private fun BytecodePatchContext.hookTopRoundedKeyboardOutline() {
    val call = RuntimeCallId.EXPRESSIVE_LOOK_RUNTIME_ON_KEYBOARD_VIEW
    for (target in topRoundedKeyboardApplyOutlineTargets) {
        val method = findMutableMethodOrNull(target) ?: continue
        val implementation = method.implementation ?: continue
        if (implementation.instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            continue
        }
        // p0 = module, p1 = KeyboardViewType, p2 = View
        method.addInstructions(0, RuntimeCallEmitter.invoke(call, "p2"))
    }
}

private fun BytecodePatchContext.hookSoftKeyViewPressed() {
    val softKeyClass = mutableClassDefByOrNull(SOFT_KEY_VIEW) ?: return
    val call = RuntimeCallId.EXPRESSIVE_LOOK_RUNTIME_ON_SOFT_KEY_PRESSED
    val superclass = softKeyClass.superclass ?: "Landroid/widget/FrameLayout;"
    softKeyClass.addHelperMethodIfMissing(
        name = "setPressed",
        parameterTypes = listOf("Z"),
        returnType = "V",
        accessFlags = AccessFlags.PUBLIC.value,
        registerCount = 2,
        body = """
            invoke-super {p0, p1}, $superclass->setPressed(Z)V
            ${RuntimeCallEmitter.invoke(call, "p0, p1")}
            return-void
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.hookBasicPopupViewShow() {
    val popupClass = mutableClassDefByOrNull(BASIC_POPUP_VIEW) ?: return
    val showMethod = popupClass.methods.firstOrNull {
        it.parameterTypes == basicPopupViewShowParameterTypes
    } ?: return
    val implementation = showMethod.implementation ?: return
    val call = RuntimeCallId.EXPRESSIVE_LOOK_RUNTIME_ON_POPUP_VIEW
    if (implementation.instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        return
    }
    showMethod.addInstructions(0, RuntimeCallEmitter.invoke(call, "p0"))
}
