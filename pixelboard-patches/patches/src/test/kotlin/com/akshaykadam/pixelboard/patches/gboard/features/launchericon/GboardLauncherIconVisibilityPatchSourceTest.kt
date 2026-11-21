package com.akshaykadam.pixelboard.patches.gboard.features.launchericon

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardLauncherIconVisibilityPatchSourceTest {
    @Test
    fun `launcher icon visibility patch targets Environment and preference filter methods`() {
        val source = Files.readString(
            repositoryRoot().resolve(
                "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                    "features/launchericon/GboardLauncherIconVisibilityPatch.kt",
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("com/google/android/libraries/inputmethod/utils/Environment"))
        assertTrue(source.contains("SDK_INT"))
        assertTrue(source.contains("narrowLiteral == 29"))
        assertTrue(source.contains("const/4 p0, 0x0"))
        assertTrue(source.contains("return p0"))
    }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }
}
