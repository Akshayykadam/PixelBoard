package com.akshaykadam.pixelboard.patches.gboard.features.language

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardLanguageCycleDedupePatchSourceTest {
    @Test
    fun `language cycle patch targets the 18_6_3 globe-key rotation and is wired into the bundle`() {
        val source = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "features/language/GboardLanguageCycleDedupePatch.kt",
        )
        // switchToNextInputMethodEntry and its DynamicRotationList next-entry lookup.
        assertTrue(source.contains("classType = \"Lozt;\""))
        assertTrue(source.contains("name = \"t\""))
        assertTrue(source.contains("parameterTypes = listOf(\"Z\", \"Lozd;\")"))
        assertTrue(source.contains("nextEntryCall = \"Lqyf;->g(Loyq;Z)Loyq;\""))
        assertTrue(source.contains("entryType = \"Loyq;\""))
        // Both lookups (no-wrap and wrap) are routed, or the method is left alone.
        assertTrue(source.contains("if (callIndices.size != 2) return"))
        assertTrue(source.contains("LANGUAGE_CYCLE_RUNTIME_NEXT_ENTRY"))
        assertTrue(source.contains("check-cast v\$resultRegister, \${target.entryType}"))

        val abi = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/runtimeabi/RuntimeAbi.kt",
        )
        assertTrue(
            abi.contains(
                "LANGUAGE_CYCLE_RUNTIME_NEXT_ENTRY(\"Lcom/akshaykadam/pixelboard/extension/language/" +
                    "GboardLanguageCycleRuntime;->nextEntry(Ljava/lang/Object;Ljava/lang/Object;Z)" +
                    "Ljava/lang/Object;\")",
            ),
        )

        val settings = read(
            "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                "shared/GboardPatchesSettingsPatch.kt",
        )
        assertTrue(settings.contains("        gboardLanguageCycleDedupePatch,\n"))
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
