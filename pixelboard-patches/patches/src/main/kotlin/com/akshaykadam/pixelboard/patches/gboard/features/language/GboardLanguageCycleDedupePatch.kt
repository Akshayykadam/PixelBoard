package com.akshaykadam.pixelboard.patches.gboard.features.language

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.indexOfFirstMoveResultAfter
import com.akshaykadam.pixelboard.patches.gboard.shared.invokeRegisters
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstruction
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// With multilingual typing, Gboard creates one input method entry per primary language, so
// English+German and German+English both appear in the globe-key rotation although they type
// identically. This patch makes the globe key skip an entry whose language set and layout are
// already covered by an earlier entry in the Languages list. The long-press language picker and
// the stored entries are left untouched.
//
// Gboard 18.6.3: ozt is InputMethodEntryManager, ozt.t is switchToNextInputMethodEntry, which asks
// the DynamicRotationList (qyf) for the next entry via qyf.g(entry, wrap) twice: once without
// wrapping and once with wrapping when the end of the list is reached.
private data class LanguageCycleTarget(
    val method: GboardMethodTarget,
    val nextEntryCall: String,
    val entryType: String,
)

private val LANGUAGE_CYCLE_TARGETS = listOf(
    LanguageCycleTarget(
        method = GboardMethodTarget(
            classType = "Lozt;",
            name = "t",
            parameterTypes = listOf("Z", "Lozd;"),
            returnType = "Z",
        ),
        nextEntryCall = "Lqyf;->g(Loyq;Z)Loyq;",
        entryType = "Loyq;",
    ),
)

internal val gboardLanguageCycleDedupePatch = bytecodePatch(
    description = "Skip duplicate multilingual entries (e.g. English+German vs German+English) " +
        "when switching languages with the globe key.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        LANGUAGE_CYCLE_TARGETS.forEach { target ->
            findMutableMethodOrNull(target.method)?.routeNextEntryCalls(target)
        }
    }
}

private fun MutableMethod.routeNextEntryCalls(target: LanguageCycleTarget) {
    val call = RuntimeCallId.LANGUAGE_CYCLE_RUNTIME_NEXT_ENTRY
    val instructions = implementation?.instructions ?: return
    // Idempotence: already routed through the runtime.
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) return

    val callIndices = instructions.indices.filter { index ->
        instructions[index].invokeRegisters(target.nextEntryCall)?.size == 3
    }
    // Stock 18.6.3 has exactly two lookups (no-wrap, then wrap). Anything else means the method
    // changed shape: leave it alone rather than guess.
    if (callIndices.size != 2) return

    // Patch from the end so earlier indices stay valid while instructions are inserted.
    callIndices.asReversed().forEach { index ->
        val registers = implementation!!.instructions[index].invokeRegisters(target.nextEntryCall)!!
        val moveResultIndex = indexOfFirstMoveResultAfter(index)
        if (moveResultIndex != index + 1) return@forEach
        val moveResult = implementation!!.instructions[moveResultIndex]
        if (!moveResult.isOpcode("MOVE_RESULT_OBJECT")) return@forEach
        val resultRegister = (moveResult as OneRegisterInstruction).registerA

        // qyf.g(list, entry, wrap) -> GboardLanguageCycleRuntime.nextEntry(list, entry, wrap)
        replaceInstruction(
            index,
            RuntimeCallEmitter.invoke(call, registers.joinToString(", ") { "v$it" }),
        )
        // The runtime returns Object; restore the entry type for the verifier.
        addInstruction(moveResultIndex + 1, "check-cast v$resultRegister, ${target.entryType}")
    }
}
