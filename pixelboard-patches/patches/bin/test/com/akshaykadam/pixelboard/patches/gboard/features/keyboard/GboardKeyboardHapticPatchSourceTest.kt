package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardKeyboardHapticPatchSourceTest {
    @Test
    fun `keyboard haptic patch targets PressEffectPlayerImpl and is wired into settings`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/keyboard/GboardKeyboardHapticPatch.kt",
        )
        assertTrue(source.contains("PressEffectPlayerImpl"))
        assertTrue(source.contains("Landroid/view/View;->performHapticFeedback(I)Z"))
        assertTrue(source.contains("KEYBOARD_RUNTIME_PERFORM_HAPTIC_FEEDBACK"))
        assertTrue(source.contains("FLAG_IGNORE_GLOBAL_SETTING"))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("gboardKeyboardHapticPatch"))
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
