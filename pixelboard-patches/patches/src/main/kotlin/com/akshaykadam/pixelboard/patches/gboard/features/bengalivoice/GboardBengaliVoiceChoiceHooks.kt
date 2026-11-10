package com.akshaykadam.pixelboard.patches.gboard.features.bengalivoice

import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrThrow
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.returnInstructionIndices
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/** Verified against the Gboard 18.5.3 base; called only by that selector target. */
context(context: BytecodePatchContext)
internal fun installBengaliVoiceChoiceHooks() = with(context) {
    // Validate all native anchors before modifying any method. Other bases do not
    // call this installer and retain their existing settings/routing behavior.
    val languageSettings = findMutableMethodOrThrow(
        GboardMethodTarget(
            "Lcom/google/android/apps/inputmethod/libs/languageselection/preference/LanguageSpecificSettingFragment;",
            "aU", emptyList(), "V",
        ),
    )
    val nativeSelection = findMutableMethodOrThrow(
        GboardMethodTarget("Lnca;", "a", listOf("Landroid/content/Context;"), "Z"),
    )
    val toolbarMic = findMutableMethodOrThrow(
        GboardMethodTarget("Lmql;", "B", listOf("Landroid/content/Context;"), "Z"),
    )
    findMutableMethodOrThrow(GboardMethodTarget("Lpem;", "e", emptyList(), "Ljava/util/Locale;"))

    languageSettings.installBengaliLanguageSettingsRow()
    nativeSelection.installBengaliLanguageSelectionOverride()
    toolbarMic.installBengaliLanguageMicOverride()
}

internal fun MutableMethod.installBengaliLanguageSettingsRow() {
    val call = RuntimeCallId.BENGALI_VOICE_AFTER_LANGUAGE_SETTINGS_REBUILT
    val instructions = implementation?.instructions ?: error("Missing language settings body")
    val returns = returnInstructionIndices()
    check(returns.isNotEmpty() && returns.all { instructions[it].isOpcode("RETURN_VOID") })
    val existing = instructions.count { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }
    if (existing != 0) {
        check(existing == returns.size) { "Partial Bengali language-settings hook" }
        return
    }
    // aU has 19 registers in 18.5.3: p0 is v18 and requires invoke-range.
    returns.asReversed().forEach { index ->
        addInstructions(index, RuntimeCallEmitter.invoke(call, "p0 .. p0"))
    }
}

internal fun MutableMethod.installBengaliLanguageSelectionOverride() {
    val call = RuntimeCallId.BENGALI_VOICE_AFTER_NATIVE_SELECTION
    val instructions = implementation?.instructions ?: error("Missing dictation selector body")
    val returns = returnInstructionIndices()
    val existing = instructions.count { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }
    if (existing != 0) {
        check(existing == returns.size) { "Partial Bengali dictation-selector hook" }
        return
    }
    val globalObserver = RuntimeAbiCatalog.abi(RuntimeCallId.RAMBLER_RUNTIME_UPDATE_OFFICIAL_SELECTION).reference
    check(returns.isNotEmpty() && instructions.count { it.isMethodReference(globalObserver) } == returns.size) {
        "Install Bengali routing after the global-selection observer"
    }
    returns.asReversed().forEach { index ->
        check(instructions[index].isOpcode("RETURN"))
        val register = (instructions[index] as OneRegisterInstruction).registerA
        // nca.a reuses p0/v2 for its boolean result; it is no longer a Context.
        // Preserve that register and resolve application context inside the runtime.
        addInstructions(index, "${RuntimeCallEmitter.invoke(call, "v$register")}\nmove-result v$register")
    }
}

internal fun MutableMethod.installBengaliLanguageMicOverride() {
    val call = RuntimeCallId.BENGALI_VOICE_ACTIVE_ROUTE_OVERRIDE
    val instructions = implementation?.instructions ?: error("Missing toolbar microphone body")
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) return
    check(implementation!!.registerCount >= 2) { "Toolbar microphone needs its existing local v0" }
    addInstructions(
        0,
        """
        ${RuntimeCallEmitter.invoke(call, "")}
        move-result v0
        if-eqz v0, :pixelboard_bengali_inherit
        add-int/lit8 v0, v0, -0x1
        return v0
        :pixelboard_bengali_inherit
        """.trimIndent(),
    )
}
