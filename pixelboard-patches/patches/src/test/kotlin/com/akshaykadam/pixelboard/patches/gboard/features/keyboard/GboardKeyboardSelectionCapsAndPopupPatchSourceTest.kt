package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardKeyboardSelectionCapsAndPopupPatchSourceTest {
    @Test
    fun `selection caps patch targets LatinIme handle and is wired into settings`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/keyboard/GboardKeyboardSelectionCapsPatch.kt",
        )
        assertTrue(source.contains("LatinIme;"))
        assertTrue(source.contains("KEYBOARD_RUNTIME_NEXT_SELECTION_CASE_FOR_KEY_EVENT"))
        assertTrue(source.contains("Lord;->gf(III)Loxs;"))
        assertTrue(source.contains("Lora;->r(IILjava/lang/CharSequence;ZZ)V"))

        val abi = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/runtimeabi/RuntimeAbi.kt",
        )
        assertTrue(abi.contains("KEYBOARD_RUNTIME_NEXT_SELECTION_CASE_FOR_KEY_EVENT"))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("gboardKeyboardSelectionCapsPatch"))
    }

    @Test
    fun `popup guard patch targets TouchActionBundle and is wired into settings`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/keyboard/GboardKeyboardPopupGuardPatch.kt",
        )
        assertTrue(source.contains("Lqck;"))
        assertTrue(source.contains("KEYBOARD_RUNTIME_SHOULD_SHOW_KEY_POPUP"))
        assertTrue(source.contains("KEYBOARD_RUNTIME_IS_PRESS_POPUP_BLOCKED"))

        val abi = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/runtimeabi/RuntimeAbi.kt",
        )
        assertTrue(abi.contains("KEYBOARD_RUNTIME_SHOULD_SHOW_KEY_POPUP"))
        assertTrue(abi.contains("KEYBOARD_RUNTIME_IS_PRESS_POPUP_BLOCKED"))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("gboardKeyboardPopupGuardPatch"))
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
