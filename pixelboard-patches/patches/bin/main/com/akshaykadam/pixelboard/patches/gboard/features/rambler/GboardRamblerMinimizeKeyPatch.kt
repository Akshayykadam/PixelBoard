/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.returnInstructionIndices
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private data class RamblerMinimizeKeyTargets(
    val headerCloseKey: GboardMethodTarget,
    val headerTeardown: GboardMethodTarget,
    val headerHideSlots: GboardMethodTarget,
    val widgetCloseKey: GboardMethodTarget,
    val softKeyFactory: String,
    val softKeyBuilder: String,
    val setContentDescription: String,
    val buildSoftKey: String,
    val minimizeKeyDescription: String,
    val showKeyboardKeyDescription: String,
    val closeIcon: String,
    val closeKey: String,
    val addAccessPoint: String,
    val removeAccessPoint: String,
)

// Gboard 18.5.3: flz is the Rambler keyboard header, flw the floating Rambler widget, flb
// their shared base, mtr a soft key definition built by mtm, mub/mue the add/remove access
// point notifications.
private val RAMBLER_MINIMIZE_KEY_1853_TARGETS = RamblerMinimizeKeyTargets(
    headerCloseKey = GboardMethodTarget("Lflz;", "D", emptyList(), "Lmtr;"),
    headerTeardown = GboardMethodTarget(
        "Lflb;",
        "I",
        listOf("Lfkw;", "Landroid/view/View;", "Landroid/view/View;"),
        "V",
    ),
    headerHideSlots = GboardMethodTarget(
        "Lflz;",
        "W",
        listOf("Landroid/view/ViewGroup;", "Z"),
        "V",
    ),
    widgetCloseKey = GboardMethodTarget("Lflw;", "D", emptyList(), "Lmtr;"),
    softKeyFactory = "Lflb;->F(ILjava/lang/Runnable;)Lmtr;",
    softKeyBuilder = "Lflb;->C(ILjava/lang/Runnable;)Lmtm;",
    setContentDescription = "Lmtm;->h(I)V",
    buildSoftKey = "Lmtm;->a()Lmtr;",
    // "Minimize keyboard", the label stock voice typing gives its minimize key.
    minimizeKeyDescription = "0x7f14167c",
    // "Back to keyboard".
    showKeyboardKeyDescription = "0x7f1401ab",
    closeIcon = "Lflb;->u()I",
    closeKey = "Lflb;->E(I)Lmtr;",
    addAccessPoint = "Lmub;->a(ILmtr;)V",
    removeAccessPoint = "Lmue;->a(ILjava/lang/String;)V",
)

internal val gboardRamblerMinimizeKeyPatch = bytecodePatch(
    description = "Keep the Rambler close key and show Gboard's minimize key next to the finish key.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val targets = RAMBLER_MINIMIZE_KEY_1853_TARGETS
        val closeKeyMethod = findMutableMethodOrNull(targets.headerCloseKey) ?: return@execute
        val teardownMethod = findMutableMethodOrNull(targets.headerTeardown) ?: return@execute
        val hideSlotsMethod = findMutableMethodOrNull(targets.headerHideSlots) ?: return@execute
        val widgetCloseKeyMethod = findMutableMethodOrNull(targets.widgetCloseKey) ?: return@execute

        closeKeyMethod.moveMinimizeKeyToEndSlot(targets)
        teardownMethod.removeMinimizeKeyOnTeardown(targets)
        hideSlotsMethod.revealMinimizeKeyAfterHiding()
        widgetCloseKeyMethod.labelShowKeyboardKey(targets)
    }
}

private fun MutableMethod.moveMinimizeKeyToEndSlot(targets: RamblerMinimizeKeyTargets) {
    val call = RuntimeCallId.RAMBLER_MINIMIZE_KEY_RUNTIME_PLACE_MINIMIZE_KEY
    val instructions = implementation?.instructions ?: error("No Rambler header key implementation")
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        return
    }
    // Stock: when the AL toolbar is off, D() returns F(minimize icon, enter Rambler widget)
    // for the close slot. Keep that key, but publish it in the end extra key slot and return
    // the stock close key for the close slot instead.
    val factoryIndex = instructions.indexOfFirst { it.isMethodReference(targets.softKeyFactory) }
    check(factoryIndex >= 0) { "Rambler header minimize key factory not found" }
    check(
        instructions[factoryIndex + 1].isOpcode("MOVE_RESULT_OBJECT") &&
            instructions[factoryIndex + 2].isOpcode("RETURN_OBJECT"),
    ) { "Unexpected Rambler header minimize key shape" }
    // p0 still holds the header after the factory call, so keep the key in v0.
    replaceInstruction(factoryIndex + 1, "move-result-object v0")
    replaceInstruction(factoryIndex + 2, "return-object v0")
    labelSoftKey(factoryIndex, targets.minimizeKeyDescription, targets)
    // labelSoftKey() added four instructions before the return.
    val returnIndex = factoryIndex + 6
    addInstructionsWithLabels(
        returnIndex,
        """
        ${RuntimeCallEmitter.invoke(call, "")}
        move-result v1
        if-eqz v1, :pixelboard_stock_minimize_key
        invoke-static {v1, v0}, ${targets.addAccessPoint}
        invoke-virtual {p0}, ${targets.closeIcon}
        move-result v0
        invoke-virtual {p0, v0}, ${targets.closeKey}
        move-result-object v0
        """.trimIndent(),
        ExternalLabel("pixelboard_stock_minimize_key", implementation!!.instructions[returnIndex]),
    )
}

private fun MutableMethod.labelShowKeyboardKey(targets: RamblerMinimizeKeyTargets) {
    val instructions = implementation?.instructions ?: error("No Rambler widget key implementation")
    if (instructions.any { it.isMethodReference(targets.setContentDescription) }) {
        return
    }
    // Stock: the floating widget's D() returns F(keyboard icon, back to the keyboard).
    val factoryIndex = instructions.indexOfFirst { it.isMethodReference(targets.softKeyFactory) }
    check(factoryIndex >= 0) { "Rambler widget keyboard key factory not found" }
    labelSoftKey(factoryIndex, targets.showKeyboardKeyDescription, targets)
}

// F(icon, action) labels every key "Close Rambler". Build the key through F()'s own builder
// instead and give it the label for what it does.
private fun MutableMethod.labelSoftKey(
    factoryIndex: Int,
    description: String,
    targets: RamblerMinimizeKeyTargets,
) {
    val instructions = implementation!!.instructions
    val factory = instructions[factoryIndex] as? FiveRegisterInstruction
    val result = instructions[factoryIndex + 1] as? OneRegisterInstruction
    check(
        factory?.registerCount == 3 &&
            instructions[factoryIndex + 1].isOpcode("MOVE_RESULT_OBJECT") &&
            result != null &&
            result.registerA != factory.registerD,
    ) { "Unexpected Rambler key factory shape" }
    // The icon register is free once the key is built.
    val key = "v${result!!.registerA}"
    val label = "v${factory!!.registerD}"
    replaceInstruction(
        factoryIndex,
        "invoke-virtual {v${factory.registerC}, $label, v${factory.registerE}}, ${targets.softKeyBuilder}",
    )
    addInstructions(
        factoryIndex + 2,
        """
        const $label, $description
        invoke-virtual {$key, $label}, ${targets.setContentDescription}
        invoke-virtual {$key}, ${targets.buildSoftKey}
        move-result-object $key
        """.trimIndent(),
    )
}

private fun MutableMethod.removeMinimizeKeyOnTeardown(targets: RamblerMinimizeKeyTargets) {
    val call = RuntimeCallId.RAMBLER_MINIMIZE_KEY_RUNTIME_CLEAR_MINIMIZE_KEY
    val instructions = implementation?.instructions ?: error("No Rambler header teardown implementation")
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        return
    }
    // p1 (the session state) is overwritten by the first instruction of the stock method.
    addInstructions(
        0,
        """
        ${RuntimeCallEmitter.invoke(call, "")}
        move-result p1
        if-eqz p1, :no_minimize_key
        const-string v0, "ramble_close"
        invoke-static {p1, v0}, ${targets.removeAccessPoint}
        :no_minimize_key
        """.trimIndent(),
    )
}

private fun MutableMethod.revealMinimizeKeyAfterHiding() {
    val call = RuntimeCallId.RAMBLER_MINIMIZE_KEY_RUNTIME_REVEAL_MINIMIZE_KEY
    val instructions = implementation?.instructions ?: error("No Rambler header slot hiding implementation")
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        return
    }
    // Branches target the stock return-void, so the call replaces it in place to keep them.
    returnInstructionIndices().asReversed().forEach {
        check(instructions[it].isOpcode("RETURN_VOID")) { "Unexpected Rambler header slot hiding return" }
        replaceInstruction(it, RuntimeCallEmitter.invoke(call, "p1"))
        addInstructions(it + 1, "return-void")
    }
}
