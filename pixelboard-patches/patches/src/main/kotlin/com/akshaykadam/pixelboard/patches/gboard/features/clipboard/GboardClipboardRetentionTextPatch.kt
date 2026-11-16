package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Gboard 18.5.3 resource ids of the texts that say clips last 1 hour.
private const val PIN_TIP_STRING = 0x7f1401a6 // "...Unpinned clips will be deleted after 1 hour."
private const val EMPTY_SUBTITLE_STRING = 0x7f14019e // "No items copied within the last hour"
private const val INTRO_CARD_LAYOUT = 0x7f0e0061 // card with "...keeping them for 1 hour..."

// Gboard 18.6.3 resource ids of the texts that say clips last 1 hour.
private const val PIN_TIP_STRING_1863 = 0x7f1401a5
private const val EMPTY_SUBTITLE_STRING_1863 = 0x7f14019d

// fpk.f returns the string id a clipboard tip shows, fss.apply builds the empty clipboard
// search result, and ClipboardKeyboard.Z inflates the "Copy more for faster pasting" card.
private val CLIPBOARD_TIP_TEXT = GboardMethodTarget("Lfpk;", "f", emptyList(), "I")
private val CLIPBOARD_EMPTY_STATE = GboardMethodTarget(
    "Lfss;",
    "apply",
    listOf("Ljava/lang/Object;"),
    "Ljava/lang/Object;",
)
private val CLIPBOARD_INTRO_CARD = GboardMethodTarget(
    "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
    "Z",
    emptyList(),
    "V",
)

// Gboard 18.6.3 targets.
private val CLIPBOARD_TIP_TEXT_1863_ADAPTER = GboardMethodTarget(
    "Lfob;",
    "d",
    listOf("Landroid/view/ViewGroup;", "I"),
    "Lks;",
)
private val CLIPBOARD_TIP_TEXT_1863_TRIM = GboardMethodTarget(
    "Lfob;",
    "N",
    emptyList(),
    "V",
)
private val CLIPBOARD_EMPTY_STATE_1863 = GboardMethodTarget(
    "Lfqg;",
    "d",
    listOf("Ljvb;", "Ljux;", "Landroid/view/ViewGroup;", "Z"),
    "Lxkb;",
)
private const val INFLATE =
    "Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;"

internal val gboardClipboardRetentionTextPatch = bytecodePatch(
    description = "Only say clipboard clips last 1 hour while the retention is Gboard's default 1 hour.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrNull(CLIPBOARD_TIP_TEXT)?.resolveStringAfterLoad(PIN_TIP_STRING)
        findMutableMethodOrNull(CLIPBOARD_EMPTY_STATE)?.resolveStringAfterLoad(EMPTY_SUBTITLE_STRING)
        findMutableMethodOrNull(CLIPBOARD_INTRO_CARD)?.applyIntroCardTextAfterInflate()

        findMutableMethodOrNull(CLIPBOARD_TIP_TEXT_1863_ADAPTER)?.resolveStringAfterLoad(PIN_TIP_STRING_1863)
        findMutableMethodOrNull(CLIPBOARD_TIP_TEXT_1863_TRIM)?.resolveStringAfterLoad(PIN_TIP_STRING_1863)
        findMutableMethodOrNull(CLIPBOARD_EMPTY_STATE_1863)?.resolveStringAfterLoad(EMPTY_SUBTITLE_STRING_1863)
    }
}

// Pass the loaded string id through the runtime, which swaps it when the retention changed.
private fun MutableMethod.resolveStringAfterLoad(stringId: Int) {
    val call = RuntimeCallId.CLIPBOARD_RETENTION_TEXT_RUNTIME_RESOLVE_TEXT
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) return
    val index = instructions.indexOfFirst {
        it.isOpcode("CONST") && (it as NarrowLiteralInstruction).narrowLiteral == stringId
    }
    if (index == -1) return
    val register = (instructions[index] as OneRegisterInstruction).registerA
    addInstructions(
        index + 1,
        """
            ${RuntimeCallEmitter.invoke(call, "v$register")}
            move-result v$register
        """.trimIndent(),
    )
}

// Hand the freshly inflated card to the runtime so it can replace the "1 hour" text.
private fun MutableMethod.applyIntroCardTextAfterInflate() {
    val call = RuntimeCallId.CLIPBOARD_RETENTION_TEXT_RUNTIME_APPLY_INTRO_CARD_TEXT
    val instructions = implementation?.instructions ?: return
    if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) return
    val layoutIndex = instructions.indexOfFirst {
        it.isOpcode("CONST") && (it as NarrowLiteralInstruction).narrowLiteral == INTRO_CARD_LAYOUT
    }
    if (layoutIndex == -1) return
    val inflateIndex = (layoutIndex until instructions.size).firstOrNull {
        instructions[it].isMethodReference(INFLATE)
    } ?: return
    val result = instructions.getOrNull(inflateIndex + 1) ?: return
    if (!result.isOpcode("MOVE_RESULT_OBJECT")) return
    val register = (result as OneRegisterInstruction).registerA
    addInstructions(inflateIndex + 2, RuntimeCallEmitter.invoke(call, "v$register"))
}
