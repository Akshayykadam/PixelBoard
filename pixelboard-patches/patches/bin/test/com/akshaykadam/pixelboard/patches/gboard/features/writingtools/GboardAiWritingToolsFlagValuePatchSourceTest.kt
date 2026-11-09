package com.akshaykadam.pixelboard.patches.gboard.features.writingtools

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardAiWritingToolsFlagValuePatchSourceTest {
    @Test
    fun aiWritingToolsRegistersItsFlagFamilyAndCurrent1803Routes() {
        val patchSource = readSource("GboardAiWritingToolsFlagValuePatch.kt")
        val constantsSource = readSource("GboardAiWritingToolsPatchConstants.kt")

        assertTrue(patchSource.contains("gboardFlagFamilyFeaturePatch("))
        assertTrue(patchSource.contains("GboardFlagFamilyFeature.AI_WRITING_TOOLS"))
        assertTrue(patchSource.contains("RuntimeCallId."))
        assertTrue(patchSource.contains("gboardAiWritingTools1803AutoFixRoutePatch"))
        assertTrue(patchSource.contains("gboardAiWritingTools1803GenAiRefreshPatch"))
        assertFalse(patchSource.contains("GboardFlagFamilyArgumentShape"))
        assertFalse(patchSource.contains("GboardVersionBindings"))
        assertTrue(patchSource.contains("findMutableMethodOrThrow"))
        assertFalse(patchSource.contains("FLAG_VALUE_CLASS"))
        assertFalse(constantsSource.contains("FLAG_VALUE_CLASS"))
        assertFalse(constantsSource.contains("\"Lmky;\""))
    }

    @Test
    fun directFlagGetterMutationRemainsComposerOwned() {
        val source = String(
            Files.readAllBytes(
                Path.of(
                    "src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/" +
                        "writingtools/GboardAiWritingToolsFlagValuePatch.kt"
                )
            ),
            StandardCharsets.UTF_8
        )

        assertTrue(source.contains("gboardFlagFamilyFeaturePatch("))
        assertFalse(source.contains("injectFeatureFlagReturnOverrides("))
        assertFalse(source.contains("::buildFlagValueDelegate"))
        assertTrue(source.contains("GboardFlagFamilyFeature.AI_WRITING_TOOLS"))
        assertTrue(source.contains("description = \"Enable Writing Tools"))
    }

    private fun readSource(fileName: String): String = String(
        Files.readAllBytes(
            Path.of(
                "src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/" +
                    "writingtools/$fileName",
            ),
        ),
        StandardCharsets.UTF_8,
    )
}
