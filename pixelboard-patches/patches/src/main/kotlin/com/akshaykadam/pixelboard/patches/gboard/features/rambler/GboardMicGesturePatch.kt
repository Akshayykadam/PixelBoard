// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrThrow
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.AccessFlags

private const val IME = "Lpbb;"
private const val LATIN_IME = "Lcom/android/inputmethod/latin/LatinIME;"
private const val VIEW = "Landroid/view/View;"
private const val EDITOR = "Landroid/view/inputmethod/EditorInfo;"

internal val gboardMicGesturePatch = bytecodePatch(
    description = "Add an opt-in single/double tap microphone gesture for Gboard 18.4.1.",
) {
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        // This adapter is pinned to 18.4.1. Do not add these hooks on other supported versions.
        if (findMutableMethodOrNull(GboardMethodTarget("Lmvq;", "a", listOf("Landroid/content/Context;"), "Z")) == null) return@execute
        val setInputView = GboardMethodTarget(LATIN_IME, "setInputView", listOf(VIEW), "V")
        val owner = setInputView.ownerClass(this)
        val wrap = RuntimeCallId.MIC_GESTURE_WRAP
        val existing = findMutableMethodOrNull(setInputView)
        check(existing == null || existing.implementation?.instructions?.any {
            it.isMethodReference(RuntimeAbiCatalog.abi(wrap).reference)
        } == true) { "Unexpected existing LatinIME.setInputView implementation" }
        setInputView.installHelper(
            this, "setInputView", AccessFlags.PUBLIC.value, 2,
            """
            ${RuntimeCallEmitter.invoke(wrap, "p0, p1")}
            move-result-object p1
            invoke-super {p0, p1}, ${owner.superclass}->setInputView(Landroid/view/View;)V
            return-void
            """.trimIndent(),
        )
        fun hook(name: String, parameters: List<String>, call: RuntimeCallId) {
            findMutableMethodOrThrow(GboardMethodTarget(IME, name, parameters, "V"))
                .applyMicGestureEntry(call, "p0 .. p0")
        }
        hook("onStartInput", listOf(EDITOR, "Z"), RuntimeCallId.MIC_GESTURE_CHECK_SESSION)
        hook("onStartInputView", listOf(EDITOR, "Z"), RuntimeCallId.MIC_GESTURE_CHECK_SESSION)
        hook("onFinishInput", emptyList(), RuntimeCallId.MIC_GESTURE_RESET_SESSION)
        hook("onFinishInputView", listOf("Z"), RuntimeCallId.MIC_GESTURE_RESET_SESSION)
        hook("onWindowHidden", emptyList(), RuntimeCallId.MIC_GESTURE_RESET_SESSION)
        hook("onUnbindInput", emptyList(), RuntimeCallId.MIC_GESTURE_RESET_SESSION)
        hook("onDestroy", emptyList(), RuntimeCallId.MIC_GESTURE_DESTROY)
        findMutableMethodOrThrow(GboardMethodTarget(IME, "aw", listOf("Loas;"), "Z"))
            .applyMicGestureEntry(RuntimeCallId.MIC_GESTURE_BEFORE_EVENT, "p0 .. p1")
    }
}

internal fun MutableMethod.applyMicGestureEntry(call: RuntimeCallId, registers: String) {
    val instructions = implementation?.instructions ?: error("Missing mic hook implementation")
    val reference = RuntimeAbiCatalog.abi(call).reference
    val existing = instructions.count { it.isMethodReference(reference) }
    if (existing > 0) {
        check(existing == 1 && instructions.first().isMethodReference(reference)) {
            "Malformed mic lifecycle hook in $definingClass->$name"
        }
        return
    }
    // Native lifecycle methods can place p0 above v15; always preserve the caller's /range.
    addInstructions(0, RuntimeCallEmitter.invoke(call, registers))
}
