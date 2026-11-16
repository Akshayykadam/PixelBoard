/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrThrow
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
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
    val micActivation: GboardMethodTarget? = null,
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

private val RAMBLER_1841_TARGETS = RamblerTargets(
    layout = method(VOICE_SETTINGS, "aC", emptyList(), "I"),
    setup = method(VOICE_SETTINGS, "ac", emptyList(), "V"),
    create = method(VOICE_SETTINGS, "f", listOf("Landroid/os/Bundle;"), "V"),
    selectionWrite = method(
        VOICE_SETTINGS,
        "aE",
        listOf(
            "Z",
            "Lqou;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
        ),
        "V",
    ),
    selectionRead = method("Lmvq;", "a", listOf("Landroid/content/Context;"), "Z"),
    defaultSelection = method("Lfde;", "hT", emptyList(), "V"),
    micActivation = method("Lmjz;", "B", listOf("Landroid/content/Context;"), "Z"),
)

private val RAMBLER_1853_TARGETS = RamblerTargets(
    layout = method(VOICE_SETTINGS, "aC", emptyList(), "I"),
    setup = method(VOICE_SETTINGS, "ac", emptyList(), "V"),
    create = method(VOICE_SETTINGS, "f", listOf("Landroid/os/Bundle;"), "V"),
    selectionWrite = method(
        VOICE_SETTINGS,
        "aE",
        listOf(
            "Z",
            "Lqvt;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
            "Lcom/google/android/libraries/inputmethod/preferencewidgets/" +
                "CustomSelectorWithWidgetPreference;",
        ),
        "V",
    ),
    selectionRead = method("Lnca;", "a", listOf("Landroid/content/Context;"), "Z"),
    defaultSelection = method("Lfhv;", "hU", emptyList(), "V"),
    micActivation = method("Lmql;", "B", listOf("Landroid/content/Context;"), "Z"),
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
            findMutableMethodOrNull(RAMBLER_1853_TARGETS.selectionRead) != null -> RAMBLER_1853_TARGETS
            findMutableMethodOrNull(RAMBLER_1841_TARGETS.selectionRead) != null -> RAMBLER_1841_TARGETS
            findMutableMethodOrNull(RAMBLER_1831_TARGETS.selectionRead) != null -> RAMBLER_1831_TARGETS
            findMutableMethodOrNull(RAMBLER_1803_TARGETS.selectionRead) != null -> RAMBLER_1803_TARGETS
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
        targets.micActivation?.let { target ->
            findMutableMethodOrNull(target)?.applyToolbarMicActivation()
        }
        suppressRamblerFeedbackAccessPoint()
    }
}

private val ramblerFeedbackTargets = listOf(
    method("Lfhm;", "a", listOf("Landroid/content/Context;"), "Z"),
    method("Lfcx;", "a", listOf("Landroid/content/Context;"), "Z"),
)

private fun BytecodePatchContext.suppressRamblerFeedbackAccessPoint() {
    val method = ramblerFeedbackTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) }
        ?: run {
            val providerDef = classDefByOrNull { classDef ->
                classDef.directMethods.any { method ->
                    method.implementation?.instructions?.any { inst ->
                        inst.isOpcode("CONST_STRING") && (inst as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference?.toString() ==
                            "com/google/android/apps/inputmethod/libs/agenticdictation/compliance/AgenticDictationFeedbackAccessPointProvider"
                    } == true
                }
            } ?: return@run null
            val providerClass = mutableClassDefBy(providerDef)
            val itemType = providerClass.methods
                .flatMap { it.implementation?.instructions.orEmpty() }
                .filter { it.isOpcode("NEW_INSTANCE") }
                .mapNotNull { (it as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference?.toString() }
                .firstOrNull() ?: return@run null
            val itemClass = mutableClassDefByOrNull(itemType) ?: return@run null
            itemClass.methods.firstOrNull {
                it.name == "a" && it.returnType == "Z" && it.parameterTypes == listOf("Landroid/content/Context;")
            }
        } ?: return

    val instructions = method.implementation?.instructions ?: return
    if (instructions.size >= 2 && instructions[0].isOpcode("CONST_4") && instructions[1].isOpcode("RETURN")) {
        return
    }
    method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
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

private fun MutableMethod.applyToolbarMicActivation() {
    val call = RuntimeCallId.RAMBLER_RUNTIME_SHOULD_ENABLE_AGENTIC_DICTATION
    val reference = RuntimeAbiCatalog.abi(call).reference
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(reference) }) {
        return
    }
    addInstructions(
        0,
        """
        ${RuntimeCallEmitter.invoke(call, "")}
        move-result v0
        if-eqz v0, :cond_stock_mic
        const/4 v0, 0x1
        return v0
        :cond_stock_mic
        """.trimIndent(),
    )
}
