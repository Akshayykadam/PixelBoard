/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrThrow
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.returnInstructionIndices
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

private const val VOICE_SETTINGS =
    "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;"

private data class RamblerTargets(
    val layout: GboardMethodTarget,
    val setup: GboardMethodTarget,
    val create: GboardMethodTarget,
    val selectionWrite: GboardMethodTarget,
    val selectionRead: GboardMethodTarget,
    val defaultSelection: GboardMethodTarget,
)

private val RAMBLER_1803_TARGETS = RamblerTargets(
    layout = method(VOICE_SETTINGS, "aB", emptyList(), "I"),
    setup = method(VOICE_SETTINGS, "ac", emptyList(), "V"),
    create = method(VOICE_SETTINGS, "f", listOf("Landroid/os/Bundle;"), "V"),
    selectionWrite = method(
        VOICE_SETTINGS,
        "aD",
        listOf(
            "Z",
            "Lqhy;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
        ),
        "V",
    ),
    selectionRead = method("Lmqk;", "a", listOf("Landroid/content/Context;"), "Z"),
    defaultSelection = method("Lfbl;", "hN", emptyList(), "V"),
)

private val RAMBLER_1831_TARGETS = RamblerTargets(
    layout = method(VOICE_SETTINGS, "aD", emptyList(), "I"),
    setup = method(VOICE_SETTINGS, "ac", emptyList(), "V"),
    create = method(VOICE_SETTINGS, "f", listOf("Landroid/os/Bundle;"), "V"),
    selectionWrite = method(
        VOICE_SETTINGS,
        "aF",
        listOf(
            "Z",
            "Lahbz;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
        ),
        "V",
    ),
    selectionRead = method("Laaeo;", "a", listOf("Landroid/content/Context;"), "Z"),
    defaultSelection = method("Lkcm;", "ge", emptyList(), "V"),
)

private fun method(
    owner: String,
    name: String,
    parameters: List<String>,
    returnType: String,
) = GboardMethodTarget(owner, name, parameters, returnType)

internal val gboardRambler1803OfficialSelectorPatch = bytecodePatch(
    description = "Allow Rambler capability only in official Voice typing selector scope.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val targets = when {
            findMutableMethodOrNull(RAMBLER_1803_TARGETS.layout) != null -> RAMBLER_1803_TARGETS
            findMutableMethodOrNull(RAMBLER_1831_TARGETS.layout) != null -> RAMBLER_1831_TARGETS
            else -> return@execute
        }
        findMutableMethodOrThrow(targets.layout)
            .applyScope(
                RuntimeCallId.RAMBLER_RUNTIME_ENTER_VOICE_SETTINGS_SCOPE,
                RuntimeCallId.RAMBLER_RUNTIME_EXIT_VOICE_SETTINGS_SCOPE,
            )
        findMutableMethodOrThrow(targets.setup)
            .applyScope(
                RuntimeCallId.RAMBLER_RUNTIME_ENTER_VOICE_SETTINGS_SCOPE,
                RuntimeCallId.RAMBLER_RUNTIME_EXIT_VOICE_SETTINGS_SCOPE,
            )
        findMutableMethodOrThrow(targets.create)
            .applyScope(
                RuntimeCallId.RAMBLER_RUNTIME_ENTER_VOICE_SETTINGS_SCOPE,
                RuntimeCallId.RAMBLER_RUNTIME_EXIT_VOICE_SETTINGS_SCOPE,
            )
        findMutableMethodOrThrow(targets.selectionWrite)
            .applySelectionWriteObserver()
        findMutableMethodOrThrow(targets.selectionRead)
            .applySelectionReadObserver()
        findMutableMethodOrThrow(targets.defaultSelection)
            .applyScope(
                RuntimeCallId.RAMBLER_RUNTIME_ENTER_DEFAULT_SELECTION_SUPPRESSION,
                RuntimeCallId.RAMBLER_RUNTIME_EXIT_DEFAULT_SELECTION_SUPPRESSION,
            )
    }
}

private fun MutableMethod.applyScope(enter: RuntimeCallId, exit: RuntimeCallId) {
    val enterReference = RuntimeAbiCatalog.abi(enter).reference
    val exitReference = RuntimeAbiCatalog.abi(exit).reference
    val instructions = implementation?.instructions
        ?: error("No implementation in $definingClass->$name")
    val enterCount = instructions.count { it.isMethodReference(enterReference) }
    val exitCount = instructions.count { it.isMethodReference(exitReference) }
    val returns = returnInstructionIndices()
    check(returns.isNotEmpty()) { "No returns in $definingClass->$name" }
    if (enterCount + exitCount != 0) {
        check(enterCount == 1 && exitCount == returns.size) {
            "Malformed Rambler selector scope in $definingClass->$name"
        }
        return
    }
    returns.asReversed().forEach { addInstructions(it, RuntimeCallEmitter.invoke(exit, "")) }
    addInstructions(0, RuntimeCallEmitter.invoke(enter, ""))
}

private fun MutableMethod.applySelectionWriteObserver() {
    val call = RuntimeCallId.RAMBLER_RUNTIME_UPDATE_OFFICIAL_SELECTION
    val reference = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: error("No selection writer implementation")
    val returns = returnInstructionIndices()
    val existing = instructions.count { it.isMethodReference(reference) }
    if (existing != 0) {
        check(existing == returns.size) { "Malformed Rambler selection writer observer" }
        return
    }
    returns.asReversed().forEach {
        addInstructions(it, RuntimeCallEmitter.invoke(call, "p0"))
    }
}

private fun MutableMethod.applySelectionReadObserver() {
    val call = RuntimeCallId.RAMBLER_RUNTIME_UPDATE_OFFICIAL_SELECTION
    val reference = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: error("No selection reader implementation")
    val returns = returnInstructionIndices()
    val existing = instructions.count { it.isMethodReference(reference) }
    if (existing != 0) {
        check(existing == returns.size) { "Malformed Rambler selection reader observer" }
        return
    }
    returns.asReversed().forEach { returnIndex ->
        val resultRegister = (instructions[returnIndex] as? OneRegisterInstruction)?.registerA
            ?: error("Rambler selection return does not expose registerA")
        check(resultRegister <= 15) { "Rambler selection result register is not invoke-35c safe" }
        addInstructions(returnIndex, RuntimeCallEmitter.invoke(call, "v$resultRegister"))
    }
}
