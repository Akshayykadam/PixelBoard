package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstruction
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

// Stock Gboard only shows unpinned clips from the 5 most recent copies, however long the
// retention is, so older clips vanish while they are still stored. Raise that to the
// clipboard's existing 100-item cap.
private const val STOCK_RECENT_COPY_LIMIT = 5
private const val RECENT_COPY_LIMIT = 100

// Gboard 18.5.3: fqr.k builds the clip list, fpn.call is the older list builder used when
// enable_clipboard_query_refactoring is off, and fqn.F trims the open clipboard after a copy.
private val CLIPBOARD_LIST_BUILDER = GboardMethodTarget(
    "Lfqr;",
    "k",
    listOf("Landroid/content/Context;", "Z"),
    "Lwns;",
)
private val CLIPBOARD_LEGACY_LIST_BUILDER = GboardMethodTarget(
    "Lfpn;",
    "call",
    emptyList(),
    "Ljava/lang/Object;",
)
private val CLIPBOARD_LIVE_TRIM = GboardMethodTarget("Lfqn;", "F", emptyList(), "V")

// Gboard 18.6.3: fof.k builds the clip list, fnb.call is the older list builder,
// and fob.F trims the open clipboard after a copy.
private val CLIPBOARD_LIST_BUILDER_1863 = GboardMethodTarget(
    "Lfof;",
    "k",
    listOf("Landroid/content/Context;", "Z"),
    "Lwhe;",
)
private val CLIPBOARD_LEGACY_LIST_BUILDER_1863 = GboardMethodTarget(
    "Lfnb;",
    "call",
    emptyList(),
    "Ljava/lang/Object;",
)
private val CLIPBOARD_LIVE_TRIM_1863 = GboardMethodTarget("Lfob;", "F", emptyList(), "V")

internal val gboardClipboardRecentLimitPatch = bytecodePatch(
    description = "Show unpinned clips from the last 100 copies instead of the last 5.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        findMutableMethodOrNull(CLIPBOARD_LIST_BUILDER)?.raiseLimitReusedByArray()
        findMutableMethodOrNull(CLIPBOARD_LEGACY_LIST_BUILDER)?.raiseLimitBeforeCompare("IF_GT")
        findMutableMethodOrNull(CLIPBOARD_LIVE_TRIM)?.raiseLimitBeforeCompare("IF_GT")

        findMutableMethodOrNull(CLIPBOARD_LIST_BUILDER_1863)?.raiseLimitReusedByArray()
        findMutableMethodOrNull(CLIPBOARD_LEGACY_LIST_BUILDER_1863)?.raiseLimitBeforeCompare("IF_GT")
        findMutableMethodOrNull(CLIPBOARD_LIVE_TRIM_1863)?.raiseLimitBeforeCompare("IF_GT")
    }
}

// fqr.k loads 5 into a register, sizes a query-argument array with it, then compares the
// copy count against it. Reload the register with 100 once the array is built.
private fun MutableMethod.raiseLimitReusedByArray() {
    val instructions = implementation?.instructions ?: return
    val index = instructions.indices.firstOrNull { index ->
        val register = instructions[index].stockLimitRegister() ?: return@firstOrNull false
        val next = instructions.getOrNull(index + 1) ?: return@firstOrNull false
        next.isOpcode("NEW_ARRAY") && (next as TwoRegisterInstruction).registerB == register
    } ?: return
    val register = instructions[index].stockLimitRegister()!!
    val usesLimitLater = instructions.drop(index + 2).any {
        it.isOpcode("IF_GE") && (it as TwoRegisterInstruction).registerB == register
    }
    if (!usesLimitLater) return
    addInstruction(index + 2, "const/16 v$register, $RECENT_COPY_LIMIT")
}

// fpn.call and fqn.F load 5 right before comparing the number of distinct copy times to it.
private fun MutableMethod.raiseLimitBeforeCompare(compareOpcode: String) {
    val instructions = implementation?.instructions ?: return
    val index = instructions.indices.firstOrNull { index ->
        val register = instructions[index].stockLimitRegister() ?: return@firstOrNull false
        val next = instructions.getOrNull(index + 1) ?: return@firstOrNull false
        next.isOpcode(compareOpcode) && (next as TwoRegisterInstruction).registerB == register
    } ?: return
    val register = instructions[index].stockLimitRegister()!!
    replaceInstruction(index, "const/16 v$register, $RECENT_COPY_LIMIT")
}

private fun Instruction.stockLimitRegister(): Int? {
    if (!isOpcode("CONST_4")) return null
    if ((this as NarrowLiteralInstruction).narrowLiteral != STOCK_RECENT_COPY_LIMIT) return null
    return (this as OneRegisterInstruction).registerA
}
