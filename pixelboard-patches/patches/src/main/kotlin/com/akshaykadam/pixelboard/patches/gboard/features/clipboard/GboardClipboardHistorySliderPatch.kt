package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.invokeRegisters
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private val clipboardConfigTargets = listOf(
    GboardMethodTarget(
        classType = "Lfbr;",
        name = "b",
        parameterTypes = listOf(
            "Landroid/content/Context;",
            "Lrbq;",
        ),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lexc;",
        name = "b",
        parameterTypes = listOf(
            "Landroid/content/Context;",
            "Lquo;",
        ),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Leyw;",
        name = "b",
        parameterTypes = listOf(
            "Landroid/content/Context;",
            "Lqvk;",
        ),
        returnType = "V",
    ),
)

internal val gboardClipboardHistorySliderPatch = bytecodePatch(
    description = "Configure Gboard clipboard history retention slider with discrete duration steps (1h, 24h, 7d, 30d, Never).",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = clipboardConfigTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) } ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.CLIPBOARD_HISTORY_RUNTIME_CONFIGURE_SLIDER
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        // Find the LabelSummarySliderPreference configuration section
        val sliderCheckCastIndex = instructions.indexOfFirst { instruction ->
            instruction is ReferenceInstruction &&
                (instruction.reference as? TypeReference)?.type ==
                    "Lcom/google/android/libraries/inputmethod/preferencewidgets/LabelSummarySliderPreference;"
        }
        if (sliderCheckCastIndex == -1) return@execute

        // Find the Preference.d() call right after slider configuration
        val notifyChangedIndex = (sliderCheckCastIndex until instructions.size).firstOrNull { index ->
            instructions[index].isMethodReference("Landroidx/preference/Preference;->d()V")
        } ?: return@execute

        val prefRegister = instructions[notifyChangedIndex].invokeRegisters("Landroidx/preference/Preference;->d()V")?.firstOrNull() ?: return@execute
        val contextRegister = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
        method.addInstructions(
            notifyChangedIndex + 1,
            RuntimeCallEmitter.invoke(call, "v$prefRegister, $contextRegister")
        )
    }
}
