package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardAdvancedVoiceFeatureMarkerSourceTest {
    @Test
    fun `marker adds exact speech package visibility without query all packages`() {
        val source = Files.readString(
            repositoryRoot().resolve(
                "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/"
                    + "advancedvoice/GboardAdvancedVoiceFeatureMarkerPatch.kt",
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("ensurePackageQueries()"))
        assertTrue(source.contains("\"com.google.android.tts\""))
        assertTrue(source.contains(
            "\"com.google.audio.hearing.visualization.accessibility.scribe\"",
        ))
        assertFalse(source.contains("QUERY_ALL_PACKAGES"))
    }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }
}
