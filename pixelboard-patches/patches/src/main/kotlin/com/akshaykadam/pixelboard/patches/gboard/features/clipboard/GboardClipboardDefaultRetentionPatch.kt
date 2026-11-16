package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// The retention slider shows 24 hours until one is chosen, but Gboard falls back to "1" hour
// when it reads the unset clipboard_history_retention preference. Make that fallback the
// 24 hours PixelBoard shows and advertises.
private const val STOCK_DEFAULT_RETENTION_HOURS = "1"
private const val DEFAULT_RETENTION_HOURS = "24"
private const val CLIPBOARD_HISTORY_RETENTION_KEY = 0x7f140914 // "clipboard_history_retention" (18.5.3)
private const val CLIPBOARD_HISTORY_RETENTION_KEY_1863 = 0x7f1408fb // "clipboard_history_retention" (18.6.3)
private val CLIPBOARD_HISTORY_RETENTION_KEYS = setOf(
    CLIPBOARD_HISTORY_RETENTION_KEY,
    CLIPBOARD_HISTORY_RETENTION_KEY_1863,
)

// Gboard 18.5.3: fqr.a returns how long unpinned clips are kept, in milliseconds.
private val CLIPBOARD_RETENTION_GETTER = GboardMethodTarget(
    "Lfqr;",
    "a",
    listOf("Landroid/content/Context;"),
    "J",
)
// Gboard 18.6.3: fof.a returns how long unpinned clips are kept, in milliseconds.
private val CLIPBOARD_RETENTION_GETTER_1863 = GboardMethodTarget(
    "Lfof;",
    "a",
    listOf("Landroid/content/Context;"),
    "J",
)
private val CLIPBOARD_RETENTION_GETTERS = listOf(
    CLIPBOARD_RETENTION_GETTER,
    CLIPBOARD_RETENTION_GETTER_1863,
)

internal val gboardClipboardDefaultRetentionPatch = bytecodePatch(
    description = "Keep unpinned clipboard clips for 24 hours until another retention is chosen.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        findMutableMethodOrNull(CLIPBOARD_RETENTION_GETTER)?.useDefaultRetention()
        findMutableMethodOrNull(CLIPBOARD_RETENTION_GETTER_1863)?.useDefaultRetention()
    }
}

private fun MutableMethod.useDefaultRetention() {
    val instructions = implementation?.instructions ?: return
    val keyIndex = instructions.indexOfFirst {
        it.isOpcode("CONST") &&
            (it as NarrowLiteralInstruction).narrowLiteral in CLIPBOARD_HISTORY_RETENTION_KEYS
    }
    if (keyIndex == -1) return
    val fallback = instructions.getOrNull(keyIndex + 1) ?: return
    if (!fallback.isOpcode("CONST_STRING")) return
    val value = ((fallback as ReferenceInstruction).reference as StringReference).string
    if (value != STOCK_DEFAULT_RETENTION_HOURS) return
    val register = (fallback as OneRegisterInstruction).registerA
    replaceInstruction(keyIndex + 1, "const-string v$register, \"$DEFAULT_RETENTION_HOURS\"")
}
