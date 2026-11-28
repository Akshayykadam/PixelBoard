package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

private data class PopupGuardTarget(
    val handlerClass: String,
    val callbacksInterface: String,
    val actionDefClass: String,
    val actionField: String,
    val alwaysShowPopupField: String,
    val keyDatasField: String,
    val keyDatasSizeMethod: String,
    val callbacksField: String,
    val popupEnabledMethod: String,
    val actionDefField: String,
    val actionDefIsValidMethod: String,
    val endTraceMethod: String,
    val handleActionMethod: GboardMethodTarget,
    val showPopupMethod: GboardMethodTarget,
)

private val POPUP_GUARD_TARGETS = listOf(
    PopupGuardTarget(
        handlerClass = "Lqck;",
        callbacksInterface = "Lqcl;",
        actionDefClass = "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;",
        actionField = "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->e:Lpuc;",
        alwaysShowPopupField = "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->j:Z",
        keyDatasField = "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->f:Lwhe;",
        keyDatasSizeMethod = "Lwhe;->size()I",
        callbacksField = "Lqck;->r:Lqcl;",
        popupEnabledMethod = "Lqcl;->s()Z",
        actionDefField = "Lqck;->n:Lcom/google/android/libraries/inputmethod/metadata/ActionDef;",
        actionDefIsValidMethod = "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->d()Z",
        endTraceMethod = "Landroid/os/Trace;->endSection()V",
        handleActionMethod = GboardMethodTarget(
            classType = "Lqck;",
            name = "n",
            parameterTypes = listOf(
                "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;",
                "Z",
                "Z",
                "Z",
                "J",
                "I",
            ),
            returnType = "V",
        ),
        showPopupMethod = GboardMethodTarget(
            classType = "Lqck;",
            name = "D",
            parameterTypes = listOf("J"),
            returnType = "V",
        ),
    ),
)

internal val gboardKeyboardPopupGuardPatch = bytecodePatch(
    description = "Ensure normal PRESS key popups never appear when Popup on Keypresses is turned off.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        POPUP_GUARD_TARGETS.forEach { target ->
            findMutableMethodOrNull(target.handleActionMethod)?.guardHandleActionPopup(target)
            findMutableMethodOrNull(target.showPopupMethod)?.guardDirectShowPopup(target)
        }
    }
}

private fun MutableMethod.guardHandleActionPopup(target: PopupGuardTarget) {
    val call = RuntimeCallId.KEYBOARD_RUNTIME_SHOULD_SHOW_KEY_POPUP
    val runtimeRef = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(runtimeRef) }) return

    val showPopupRef = target.showPopupMethod.reference
    val callDIndex = instructions.indexOfFirst { it.isMethodReference(showPopupRef) }
    if (callDIndex < 0 || callDIndex + 1 >= instructions.size) return

    addInstructionsWithLabels(
        callDIndex,
        """
            move/from16 v2, p2
            iget-object v3, v1, ${target.actionField}
            iget-boolean v4, v1, ${target.alwaysShowPopupField}
            iget-object v5, v1, ${target.keyDatasField}
            invoke-virtual {v5}, ${target.keyDatasSizeMethod}
            move-result v5
            ${RuntimeCallEmitter.invoke(call, "v2, v3, v4, v5")}
            move-result v2
            if-eqz v2, :pixelboard_skip_press_popup
        """.trimIndent(),
        ExternalLabel("pixelboard_skip_press_popup", instructions[callDIndex + 1]),
    )
}

private fun MutableMethod.guardDirectShowPopup(target: PopupGuardTarget) {
    val call = RuntimeCallId.KEYBOARD_RUNTIME_IS_PRESS_POPUP_BLOCKED
    val runtimeRef = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(runtimeRef) }) return

    val isValidIndex = instructions.indexOfFirst {
        it.isMethodReference(target.actionDefIsValidMethod)
    }
    if (isValidIndex < 0) return

    val branchIndex = (isValidIndex + 1 until instructions.size)
        .take(3)
        .firstOrNull { idx -> instructions[idx].isOpcode("IF_EQZ") }
        ?: return

    val returnExitIndex = instructions.indices.firstOrNull { idx ->
        instructions[idx].isMethodReference(target.endTraceMethod) &&
            idx + 1 < instructions.size &&
            instructions[idx + 1].isOpcode("RETURN_VOID")
    } ?: return

    addInstructionsWithLabels(
        branchIndex + 1,
        """
            iget-object v3, v0, ${target.callbacksField}
            invoke-interface {v3}, ${target.popupEnabledMethod}
            move-result v3
            iget-object v4, v0, ${target.actionDefField}
            iget-object v4, v4, ${target.actionField}
            ${RuntimeCallEmitter.invoke(call, "v3, v4")}
            move-result v3
            if-nez v3, :pixelboard_block_press_popup
        """.trimIndent(),
        ExternalLabel("pixelboard_block_press_popup", instructions[returnExitIndex]),
    )
}
