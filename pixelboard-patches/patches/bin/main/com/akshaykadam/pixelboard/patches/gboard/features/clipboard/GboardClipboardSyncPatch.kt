package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isFieldReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val clipboardOnStartInputViewTargets = listOf(
    GboardMethodTarget(
        classType = "Lfrc;",
        name = "ft",
        parameterTypes = listOf(
            "Lpev;",
            "Landroid/view/inputmethod/EditorInfo;",
            "Z",
            "Ljava/util/Map;",
            "Lohr;",
        ),
        returnType = "Z",
    ),
    GboardMethodTarget(
        classType = "Lfml;",
        name = "ft",
        parameterTypes = listOf(
            "Loyg;",
            "Landroid/view/inputmethod/EditorInfo;",
            "Z",
            "Ljava/util/Map;",
            "Lobf;",
        ),
        returnType = "Z",
    ),
)

internal val gboardClipboardSyncPatch = bytecodePatch(
    description = "Synchronize clipboard history upon input view start for Samsung and Vivo devices.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = clipboardOnStartInputViewTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) } ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.CLIPBOARD_SYNC_RUNTIME_SYNC_ON_START_INPUT_VIEW
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        val igetFIndex = instructions.indexOfFirst { instruction ->
            instruction.isFieldReference("Lfrc;->f:Lfqx;") ||
                instruction.isFieldReference("Lfml;->f:Lfmg;") ||
                (instruction.opcode == Opcode.IGET_OBJECT &&
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.name == "f")
        }
        if (igetFIndex == -1) return@execute

        val igetInstruction = instructions[igetFIndex]
        val handlerReg = if (igetInstruction is TwoRegisterInstruction) {
            "v${igetInstruction.registerA}"
        } else {
            "p4"
        }

        method.addInstructions(
            igetFIndex + 1,
            RuntimeCallEmitter.invoke(call, handlerReg)
        )
    }
}
