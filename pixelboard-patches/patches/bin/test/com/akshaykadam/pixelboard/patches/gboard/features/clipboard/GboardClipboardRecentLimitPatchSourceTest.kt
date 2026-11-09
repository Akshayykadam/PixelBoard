package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardClipboardRecentLimitPatchSourceTest {
    @Test
    fun `recent limit patch raises all three 18_5_3 copy caps and is wired into the bundle`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/clipboard/GboardClipboardRecentLimitPatch.kt",
        )
        assertTrue(source.contains("private const val STOCK_RECENT_COPY_LIMIT = 5"))
        assertTrue(source.contains("private const val RECENT_COPY_LIMIT = 100"))
        assertTrue(source.contains("\"Lfqr;\",\n    \"k\",\n    listOf(\"Landroid/content/Context;\", \"Z\"),\n    \"Lwns;\","))
        assertTrue(source.contains("\"Lfpn;\",\n    \"call\","))
        assertTrue(source.contains("GboardMethodTarget(\"Lfqn;\", \"F\", emptyList(), \"V\")"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LIST_BUILDER)?.raiseLimitReusedByArray()"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LEGACY_LIST_BUILDER)?.raiseLimitBeforeCompare(\"IF_GT\")"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LIVE_TRIM)?.raiseLimitBeforeCompare(\"IF_GT\")"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LIST_BUILDER_1863)?.raiseLimitReusedByArray()"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LEGACY_LIST_BUILDER_1863)?.raiseLimitBeforeCompare(\"IF_GT\")"))
        assertTrue(source.contains("findMutableMethodOrNull(CLIPBOARD_LIVE_TRIM_1863)?.raiseLimitBeforeCompare(\"IF_GT\")"))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("        gboardClipboardRecentLimitPatch,\n"))
    }

    @Test
    fun `retention text patch hooks the three 18_5_3 one-hour texts and is wired into the bundle`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/clipboard/GboardClipboardRetentionTextPatch.kt",
        )
        assertTrue(source.contains("GboardMethodTarget(\"Lfpk;\", \"f\", emptyList(), \"I\")"))
        assertTrue(source.contains("\"Lfss;\",\n    \"apply\","))
        assertTrue(source.contains("libs/clipboard/ClipboardKeyboard;\",\n    \"Z\","))
        assertTrue(source.contains("private const val PIN_TIP_STRING = 0x7f1401a6"))
        assertTrue(source.contains("private const val EMPTY_SUBTITLE_STRING = 0x7f14019e"))
        assertTrue(source.contains("private const val PIN_TIP_STRING_1863 = 0x7f1401a5"))
        assertTrue(source.contains("private const val EMPTY_SUBTITLE_STRING_1863 = 0x7f14019d"))
        assertTrue(source.contains("CLIPBOARD_TIP_TEXT_1863_ADAPTER"))
        assertTrue(source.contains("CLIPBOARD_TIP_TEXT_1863_TRIM"))
        assertTrue(source.contains("CLIPBOARD_EMPTY_STATE_1863"))
        assertTrue(source.contains("private const val INTRO_CARD_LAYOUT = 0x7f0e0061"))
        assertTrue(source.contains("CLIPBOARD_RETENTION_TEXT_RUNTIME_RESOLVE_TEXT"))
        assertTrue(source.contains("CLIPBOARD_RETENTION_TEXT_RUNTIME_APPLY_INTRO_CARD_TEXT"))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("        gboardClipboardRetentionTextPatch,\n"))
        assertTrue(settings.contains("val withPinTips = addPinTips(text, file.parentFile.name)"))
    }

    @Test
    fun `default retention patch makes Gboard's unset retention 24 hours`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/clipboard/GboardClipboardDefaultRetentionPatch.kt",
        )
        assertTrue(source.contains("private const val STOCK_DEFAULT_RETENTION_HOURS = \"1\""))
        assertTrue(source.contains("private const val DEFAULT_RETENTION_HOURS = \"24\""))
        assertTrue(source.contains("private const val CLIPBOARD_HISTORY_RETENTION_KEY = 0x7f140914"))
        assertTrue(source.contains("private const val CLIPBOARD_HISTORY_RETENTION_KEY_1863 = 0x7f1408fb"))
        assertTrue(source.contains("CLIPBOARD_RETENTION_GETTER_1863"))
        assertTrue(source.contains("\"Lfqr;\",\n    \"a\",\n    listOf(\"Landroid/content/Context;\"),\n    \"J\","))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("        gboardClipboardDefaultRetentionPatch,\n"))

        // The extension's default must match, so the settings page and keyboard agree.
        val extensionSettings = read(
            "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/" +
                "clipboard/GboardClipboardHistorySettings.java",
        )
        assertTrue(extensionSettings.contains("DEFAULT_DURATION = DURATION_24_HOURS;"))
    }

    private fun read(relativePath: String): String =
        Files.readString(repositoryRoot().resolve(relativePath), StandardCharsets.UTF_8)
            .replace("\r\n", "\n")

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }
}
