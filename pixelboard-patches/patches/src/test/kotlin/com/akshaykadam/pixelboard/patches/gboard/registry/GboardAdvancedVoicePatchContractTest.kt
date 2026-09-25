package com.akshaykadam.pixelboard.patches.gboard.registry

import com.google.gson.JsonParser
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoice1803ZhTwPatch
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoiceFeatureMarkerPatch
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoiceFlagValuePatch
import com.akshaykadam.pixelboard.patches.gboard.features.rambler.gboardRambler1803OfficialSelectorPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesSettingsPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardAdvancedVoicePatchContractTest {
    @Test
    fun releasePublishesExactlyOneTargetOnlyAdvancedVoiceTypingPatch() {
        val rows = generatedPublishedPatches()
            .filter { it.get("name").asString == "Advanced Voice Typing" }

        assertEquals(1, rows.size)
        val row = rows.single()
        assertTrue(row.get("use").asBoolean)
        val compatiblePackages = row.getAsJsonObject("compatiblePackages")
        assertEquals(setOf(GBOARD_PACKAGE), compatiblePackages.keySet())
        assertEquals(
            listOf(GBOARD_VERSION, GBOARD_VERSION_1831, GBOARD_VERSION_1831_RELEASE),
            compatiblePackages.getAsJsonArray(GBOARD_PACKAGE).map { it.asString },
        )
    }

    @Test
    fun formal1803AdmissionUsesTypedFlagsAndOfficialVoiceSelectionOnly() {
        assertEquals(
            setOf(
                "advanced_voice_typing.flag",
                "advanced_voice_typing.bytecode",
            ),
            GboardTargetAdmission.admittedContributionIdsByFeature
                .getValue("advanced_voice_typing"),
        )
        assertEquals(
            listOf(
                gboardPatchesSettingsPatch,
                gboardAdvancedVoiceFeatureMarkerPatch,
                gboardAdvancedVoiceFlagValuePatch,
                gboardAdvancedVoice1803ZhTwPatch,
                gboardRambler1803OfficialSelectorPatch,
            ),
            gboardAdvancedVoiceTypingPatch.dependencies.toList(),
        )

        val bindings = JsonParser.parseString(
            Files.readString(
                repositoryRoot().resolve(
                    "patches/src/main/resources/gboard/gboard-version-bindings.json",
                ),
                StandardCharsets.UTF_8,
            ),
        ).asJsonObject.getAsJsonObject("bindings")
        assertTrue(bindings.keySet().none { key -> key.startsWith("advanced_voice_") })
        assertTrue(bindings.keySet().none { key -> key.startsWith("rambler_") })
    }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }

    private companion object {
        const val GBOARD_PACKAGE = "com.google.android.inputmethod.latin"
        const val GBOARD_VERSION = "18.0.3.954559732-release-arm64-v8a"
        const val GBOARD_VERSION_1831 = "18.3.1.977415014-beta-arm64-v8a"
        const val GBOARD_VERSION_1831_RELEASE = "18.3.1.977415014-release-arm64-v8a"
    }
}
