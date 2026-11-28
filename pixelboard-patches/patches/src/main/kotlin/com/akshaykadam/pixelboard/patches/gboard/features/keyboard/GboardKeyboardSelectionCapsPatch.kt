package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.addHelperMethodIfMissing
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isFieldReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import java.lang.reflect.Modifier

private data class SelectionCapsTarget(
    val latinImeClass: String,
    val abstractImeClass: String,
    val eventClass: String,
    val delegateInterface: String,
    val replaceInterface: String,
    val surroundingType: String,
    val inputStateClass: String,
    val handleMethod: GboardMethodTarget,
    val decoderRecapMethod: String,
    val eventCodeMethod: String,
    val eventActionField: String,
    val eventSourceField: String,
    val delegateField: String,
    val getSurroundingMethod: String,
    val beginBatchMethod: String,
    val finishComposingMethod: String,
    val offsetSelectionMethod: String,
    val replaceTextMethod: String,
    val endBatchMethod: String,
    val surroundingTextField: String,
    val surroundingSelStartField: String,
    val surroundingSelEndField: String,
    val inputStateBlockField: String,
    val handledMetricField: String,
)

private val SELECTION_CAPS_TARGETS = listOf(
    SelectionCapsTarget(
        latinImeClass = "Lcom/google/android/apps/inputmethod/libs/latin5/LatinIme;",
        abstractImeClass = "Lcom/google/android/libraries/inputmethod/ime/AbstractIme;",
        eventClass = "Loak;",
        delegateInterface = "Lord;",
        replaceInterface = "Lora;",
        surroundingType = "Loxs;",
        inputStateClass = "Lfxy;",
        handleMethod = GboardMethodTarget(
            classType = "Lcom/google/android/apps/inputmethod/libs/latin5/LatinIme;",
            name = "p",
            parameterTypes = listOf("Loak;"),
            returnType = "Z",
        ),
        decoderRecapMethod = "Lfwo;->w(J)Z",
        eventCodeMethod = "Loak;->a()I",
        eventActionField = "Loak;->a:Lpuc;",
        eventSourceField = "Loak;->w:I",
        delegateField = "Lcom/google/android/libraries/inputmethod/ime/AbstractIme;->E:Lord;",
        getSurroundingMethod = "Lord;->gf(III)Loxs;",
        beginBatchMethod = "Lord;->b()V",
        finishComposingMethod = "Lord;->j()V",
        offsetSelectionMethod = "Lord;->n(II)V",
        replaceTextMethod = "Lora;->r(IILjava/lang/CharSequence;ZZ)V",
        endBatchMethod = "Lord;->i()V",
        surroundingTextField = "Loxs;->b:Ljava/lang/CharSequence;",
        surroundingSelStartField = "Loxs;->c:I",
        surroundingSelEndField = "Loxs;->d:I",
        inputStateBlockField = "Lfxy;->e:Z",
        handledMetricField = "Lcom/google/android/apps/inputmethod/libs/latin5/LatinIme;->d:Loso;",
    ),
)

private const val RECAP_HELPER_NAME = "pixelboardRecapitalizeSelection"

internal val gboardKeyboardSelectionCapsPatch = bytecodePatch(
    description = "Fix Shift/Enter Caps on selected text to reliably cycle UPPERCASE, lowercase, and Title Case across arbitrary selections.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val call = RuntimeCallId.KEYBOARD_RUNTIME_NEXT_SELECTION_CASE_FOR_KEY_EVENT
        SELECTION_CAPS_TARGETS.forEach { target ->
            val handleMethod = findMutableMethodOrNull(target.handleMethod) ?: return@forEach
            val helperRef = "${target.latinImeClass}->$RECAP_HELPER_NAME(${target.eventClass})Z"
            val instructions = handleMethod.implementation?.instructions ?: return@forEach
            if (instructions.any { it.isMethodReference(helperRef) }) {
                return@forEach
            }

            addHelperMethodIfMissing(
                classType = target.latinImeClass,
                name = RECAP_HELPER_NAME,
                parameterTypes = listOf(target.eventClass),
                returnType = "Z",
                accessFlags = Modifier.PRIVATE or Modifier.FINAL,
                registerCount = 11,
                body = buildHelperBody(target, call),
            )
            handleMethod.patchSelectionRecapFallback(target, helperRef)
        }
    }
}

private fun buildHelperBody(target: SelectionCapsTarget, call: RuntimeCallId): String {
    return """
        iget-object v2, p0, ${target.delegateField}
        if-eqz v2, :ret_false
        const/16 v0, 4096
        const/4 v1, 0
        invoke-interface {v2, v0, v0, v1}, ${target.getSurroundingMethod}
        move-result-object v0
        if-eqz v0, :ret_false
        iget-object v1, v0, ${target.surroundingTextField}
        if-eqz v1, :ret_false
        iget v3, v0, ${target.surroundingSelStartField}
        iget v4, v0, ${target.surroundingSelEndField}
        if-ge v3, v4, :ret_false
        if-ltz v3, :ret_false
        invoke-interface {v1}, Ljava/lang/CharSequence;->length()I
        move-result v5
        if-gt v4, v5, :ret_false
        invoke-interface {v1, v3, v4}, Ljava/lang/CharSequence;->subSequence(II)Ljava/lang/CharSequence;
        move-result-object v8
        invoke-virtual {p1}, ${target.eventCodeMethod}
        move-result v5
        iget-object v6, p1, ${target.eventActionField}
        iget v7, p1, ${target.eventSourceField}
        ${RuntimeCallEmitter.invoke(call, "v5, v6, v7, v8")}
        move-result-object v5
        if-eqz v5, :ret_false
        sub-int v3, v4, v3
        if-lez v3, :ret_false
        invoke-virtual {v5}, Ljava/lang/String;->length()I
        move-result v0
        const/4 v4, 0
        const/4 v6, 1
        const/4 v7, 0
        invoke-interface {v2}, ${target.beginBatchMethod}
        invoke-interface {v2}, ${target.finishComposingMethod}
        invoke-interface {v2, v3, v4}, ${target.offsetSelectionMethod}
        invoke-interface/range {v2 .. v7}, ${target.replaceTextMethod}
        neg-int v0, v0
        invoke-interface {v2, v0, v4}, ${target.offsetSelectionMethod}
        invoke-interface {v2}, ${target.endBatchMethod}
        const/4 v0, 1
        return v0
        :ret_false
        const/4 v0, 0
        return v0
    """.trimIndent()
}

private fun MutableMethod.patchSelectionRecapFallback(
    target: SelectionCapsTarget,
    helperRef: String,
) {
    val instructions = implementation?.instructions ?: return
    val decoderRecapIndex = instructions.indexOfFirst { it.isMethodReference(target.decoderRecapMethod) }
    if (decoderRecapIndex < 3) return

    val entryIndex = ((decoderRecapIndex - 30).coerceAtLeast(0) until decoderRecapIndex)
        .lastOrNull { idx -> instructions[idx].isFieldReference(target.inputStateBlockField) }
        ?: return

    // After fwo.w(J)Z -> move-result -> if-eqz, stock lands on iget-object LatinIme.d:Loso.
    val doneIndex = (decoderRecapIndex + 1 until instructions.size)
        .take(5)
        .firstOrNull { idx -> instructions[idx].isFieldReference(target.handledMetricField) }
        ?: return

    // Confirm entryIndex+1 is the conditional branch right after reading fxy.e so entryIndex itself
    // remains the branch target of earlier code while entryIndex+1 sees all live state intact.
    if (!instructions[entryIndex + 1].isOpcode("IF_NEZ")) return

    val doneInstruction = instructions[doneIndex]
    addInstructionsWithLabels(
        entryIndex + 1,
        """
            invoke-direct/range {p0 .. p1}, $helperRef
            move-result v0
            if-nez v0, :pixelboard_recap_done
        """.trimIndent(),
        ExternalLabel("pixelboard_recap_done", doneInstruction),
    )
}
