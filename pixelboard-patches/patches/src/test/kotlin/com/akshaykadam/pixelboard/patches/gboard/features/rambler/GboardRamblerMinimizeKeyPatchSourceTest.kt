package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardRamblerMinimizeKeyPatchSourceTest {
    @Test
    fun `minimize key patch targets the 18_5_3 Rambler header and is wired into the bundle`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/rambler/GboardRamblerMinimizeKeyPatch.kt",
        )
        assertTrue(source.contains("GboardMethodTarget(\"Lflz;\", \"D\", emptyList(), \"Lmtr;\")"))
        assertTrue(source.contains("\"Lflb;\",\n        \"I\","))
        assertTrue(source.contains("\"Lflz;\",\n        \"W\","))
        assertTrue(source.contains("RAMBLER_MINIMIZE_KEY_RUNTIME_PLACE_MINIMIZE_KEY"))
        assertTrue(source.contains("RAMBLER_MINIMIZE_KEY_RUNTIME_CLEAR_MINIMIZE_KEY"))
        assertTrue(source.contains("RAMBLER_MINIMIZE_KEY_RUNTIME_REVEAL_MINIMIZE_KEY"))
        assertTrue(source.contains("ExternalLabel(\"pixelboard_stock_minimize_key\""))
        assertTrue(source.contains("softKeyBuilder = \"Lflb;->C(ILjava/lang/Runnable;)Lmtm;\""))
        assertTrue(source.contains("minimizeKeyDescription = \"0x7f14167c\""))
        assertTrue(source.contains("widgetCloseKey = GboardMethodTarget(\"Lflw;\", \"D\", emptyList(), \"Lmtr;\")"))
        assertTrue(source.contains("showKeyboardKeyDescription = \"0x7f1401ab\""))

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("        gboardRamblerMinimizeKeyPatch,\n"))
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
