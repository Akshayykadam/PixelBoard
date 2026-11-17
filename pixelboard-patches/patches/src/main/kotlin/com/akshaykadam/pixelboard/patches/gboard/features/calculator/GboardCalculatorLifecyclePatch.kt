/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Simple Calculator ported from Gboard-patches by Jason Wu (@jasonwu1994)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.calculator

import com.android.tools.smali.dexlib2.AccessFlags
import com.akshaykadam.pixelboard.patches.gboard.shared.addHelperMethodIfMissing
import com.akshaykadam.pixelboard.patches.gboard.shared.applyVoidExitLifecycleDelegate
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

/**
 * Hooks the IME service lifecycle so the calculator runtime (merged by the
 * shared extension carrier) can watch the text before the cursor and show the
 * result of a complete arithmetic expression in a suggestion-style chip.
 *
 * Unlike the upstream Gboard-patches build, which hard-pins Gboard 18.5.4 and
 * fails the whole patch run on any other version, this port resolves its
 * targets defensively: a verified fast path first, then a signature scan, and
 * a silent skip if a future Gboard build moves the lifecycle class. Skipping
 * must never break Rambler or the other PixelBoard features sharing the run.
 */
internal val gboardCalculatorLifecyclePatch = bytecodePatch(
    description = "Add Simple Calculator delegates to the Gboard input session lifecycle.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val targets = resolveCalculatorTargets() ?: return@execute

        check(
            isCalculatorHostCompatible(targets.inputView.type) { type ->
                runCatching { mutableClassDefByOrNull(type)?.superclass }.getOrNull()
            },
        ) {
            "Calculator host ${targets.inputView.type} must inherit from $FRAME_LAYOUT_TYPE"
        }

        val onStartInputView = findMutableMethodOrNull(targets.onStartInputView)
            ?: return@execute
        onStartInputView.applyVoidExitLifecycleDelegate(
            RuntimeCallId.CALCULATOR_RUNTIME_ON_INPUT_VIEW_STARTED,
            "p0 .. p0",
        )

        val onUpdateSelection = findMutableMethodOrNull(targets.onUpdateSelection)
            ?: return@execute
        targets.onUpdateSelection.ownerClass(this).addHelperMethodIfMissing(
            name = SELECTION_HELPER_NAME,
            parameterTypes = listOf("I", "I"),
            returnType = "V",
            accessFlags = AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
            registerCount = SELECTION_HELPER_REGISTER_COUNT,
            body = selectionHelperBody(targets),
        )
        onUpdateSelection.applyCalculatorSelectionEntryDelegate(targets)

        val onWindowHidden = findMutableMethodOrNull(targets.onWindowHidden)
            ?: return@execute
        onWindowHidden.applyVoidExitLifecycleDelegate(
            RuntimeCallId.CALCULATOR_RUNTIME_ON_INPUT_WINDOW_HIDDEN,
            "",
        )
    }
}

private fun BytecodePatchContext.resolveCalculatorTargets(): GboardCalculatorTargets? {
    // Fast path: class and field names verified against Gboard 18.5.4, whose
    // obfuscation map matches 18.5.3 for the IME service (Lphr;).
    val known = GboardCalculatorTargets(
        GboardCalculatorTargets.KNOWN_IME_CLASS_TYPE,
        GboardCalculatorTargets.KNOWN_INPUT_VIEW_FIELD_NAME,
    )
    if (findMutableMethodOrNull(known.onStartInputView) != null &&
        findMutableMethodOrNull(known.onUpdateSelection) != null &&
        findMutableMethodOrNull(known.onWindowHidden) != null &&
        known.inputView.resolveOrNull(this) != null
    ) {
        return known
    }

    // Fallback: find the IME service class by its framework lifecycle
    // signatures. The method names are Android API overrides and are never
    // obfuscated; requiring all three plus an InputView field makes the match
    // unique in practice.
    val imeClass = mutableClassDefByOrNull { classDef ->
        classDef.methods.any { method ->
            method.name == "onStartInputView" &&
                method.parameterTypes == listOf("Landroid/view/inputmethod/EditorInfo;", "Z") &&
                method.returnType == "V"
        } &&
            classDef.methods.any { method ->
                method.name == "onUpdateSelection" &&
                    method.parameterTypes == List(6) { "I" } &&
                    method.returnType == "V"
            } &&
            classDef.methods.any { method ->
                method.name == "onWindowHidden" &&
                    method.parameterTypes.isEmpty() &&
                    method.returnType == "V"
            } &&
            classDef.fields.any { field ->
                field.type == GboardCalculatorTargets.INPUT_VIEW_TYPE
            }
    } ?: return null

    val inputViewFieldName = imeClass.fields.firstOrNull { field ->
        field.type == GboardCalculatorTargets.INPUT_VIEW_TYPE
    }?.name ?: return null

    return GboardCalculatorTargets(imeClass.type, inputViewFieldName)
}

internal fun isCalculatorHostCompatible(
    startType: String,
    superclassOf: (String) -> String?,
): Boolean {
    var current: String? = startType
    val visited = mutableSetOf<String>()
    while (current != null && visited.add(current)) {
        if (current == FRAME_LAYOUT_TYPE) {
            return true
        }
        current = superclassOf(current)
    }
    return false
}

private fun MutableMethod.applyCalculatorSelectionEntryDelegate(
    targets: GboardCalculatorTargets,
) {
    val helperReference =
        "${targets.onUpdateSelection.ownerDescriptor}->$SELECTION_HELPER_NAME(II)V"
    val instructions = implementation?.instructions
        ?: error("No instructions in $definingClass->$name")
    val existing = instructions.count { it.isMethodReference(helperReference) }
    if (existing > 0) {
        check(existing == 1 && instructions[0].isMethodReference(helperReference)) {
            "Malformed calculator selection delegate in $definingClass->$name"
        }
        return
    }
    // p3/p4 are newSelStart/newSelEnd in the framework onUpdateSelection
    // signature (p0 = this, p1/p2 = old selection, p5/p6 = candidates), so
    // this is stable across Gboard versions and register counts.
    addInstructions(
        0,
        "invoke-direct {p0, p3, p4}, $helperReference",
    )
}

private fun selectionHelperBody(targets: GboardCalculatorTargets): String = """
    iget-object v0, p0, ${targets.inputView.reference}

    ${RuntimeCallEmitter.invoke(
        RuntimeCallId.CALCULATOR_RUNTIME_ON_SELECTION_UPDATED,
        "p0, v0, p1, p2",
    )}

    return-void
""".trimIndent()

private const val SELECTION_HELPER_REGISTER_COUNT = 4
private const val SELECTION_HELPER_NAME = "pixelboardCalculatorSelection"
private const val FRAME_LAYOUT_TYPE = "Landroid/widget/FrameLayout;"
