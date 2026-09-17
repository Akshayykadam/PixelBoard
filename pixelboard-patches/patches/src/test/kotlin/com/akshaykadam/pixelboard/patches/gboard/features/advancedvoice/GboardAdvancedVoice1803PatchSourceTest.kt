package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardAdvancedVoice1803PatchSourceTest {
    @Test
    fun `formal zh tw patch contains only the admitted locale mdd and formatter graph`() {
        val source = Files.readString(
            repositoryRoot().resolve(
                "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                    "features/advancedvoice/GboardAdvancedVoice1803ZhTwPatch.kt",
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("classType = \"Lsdc;\""))
        assertTrue(source.contains("classType = \"Lrtu;\""))
        assertTrue(source.contains("classType = \"Lrwr;\""))
        assertTrue(source.contains("classType = \"Lscn;\""))
        assertTrue(source.contains(
            "ADVANCED_VOICE_RUNTIME_AFTER_1803_NATIVE_SPLIT_READINESS",
        ))
        assertTrue(source.contains(
            "RuntimeCallEmitter.invoke(call, \"v\$register .. v\$register\")",
        ))
        assertTrue(source.contains("ADVANCED_VOICE_RUNTIME_SEED_APPLICATION_CONTEXT"))
        assertEquals(2, Regex("method\\.applyApplicationContextSeed\\(\\)")
            .findAll(source).count())
        assertTrue(source.contains("RuntimeCallEmitter.invoke(seedCall, \"p1 .. p1\")"))
        assertTrue(source.contains("RuntimeCallEmitter.invoke(call, \"p23 .. p23\")"))
        assertTrue(source.contains(
            "ADVANCED_VOICE_RUNTIME_INCLUDE_EXACT_ZH_TW_SUPPORTED_LOCALE",
        ))
        assertTrue(source.contains("ADVANCED_VOICE_RUNTIME_AFTER_MDD_PROVIDER_CONSTRUCTED"))
        assertTrue(source.contains("ADVANCED_VOICE_RUNTIME_BEFORE_FORMATTER_CONSTRUCTED"))
        assertEquals(4, Regex("findMutableMethodOrThrow\\(").findAll(source).count())
        assertFalse(source.contains("Lric;"))
        assertFalse(source.contains("Leqb;"))
        assertFalse(source.contains("Lqzh;"))
        assertFalse(source.contains("Lrcb;"))
    }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }
}
