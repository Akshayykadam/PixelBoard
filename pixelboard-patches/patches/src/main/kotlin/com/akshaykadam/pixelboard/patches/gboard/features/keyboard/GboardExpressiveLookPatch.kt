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

// TopRoundedKeyboard module (module id 392): applyOutline(KeyboardViewType, View).
// It runs for every keyboard view once the top_rounded_keyboard flag is on, which
// the Expressive look setting forces through the flag override hook.
private val topRoundedKeyboardApplyOutlineTargets = listOf(
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
}
