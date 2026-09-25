package com.akshaykadam.pixelboard.patches.gboard.registry

import com.google.gson.JsonParser
import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardPublishedPatchCatalogContractTest {
    @Test
    fun catalogExposesEachPublishedRegistrationExactlyOnce() {
        val registrations = GboardPublishedPatchCatalog.publishedRegistrations

        assertTrue(registrations.isNotEmpty())
        assertEquals(4, registrations.size)
        assertEquals(registrations.size, registrations.map { patch -> patch.name }.distinct().size)
        assertSame(
            gboardAdvancedVoiceTypingPatch,
            registrations.single { patch -> patch.name == "Advanced Voice Typing" },
        )
        assertSame(
            gboardAiWritingToolsPatch,
            registrations.single { patch -> patch.name == "AI Writing Tools" },
        )
        assertSame(
            gboardSignatureBypassPatch,
            registrations.single { patch -> patch.name == "Add Gboard Signature Bypass" },
        )
        assertSame(
            gboardPackageRenamePatch,
            registrations.single { patch -> patch.name == "Package Rename" },
        )
    }

    @Test
    fun catalogRendersThePublishedInventoryFromItsRegistrations() {
        val inventory = JsonParser.parseString(
            GboardPublishedPatchCatalog.publishedInventory("test-version"),
        ).asJsonObject
        val patches = inventory.getAsJsonArray("patches").map { element -> element.asJsonObject }

        assertEquals("test-version", inventory.get("version").asString)
        assertEquals(
            patches.map { patch -> patch.get("name").asString }.sorted(),
            patches.map { patch -> patch.get("name").asString },
        )

        val aiWriting = patches.single { patch ->
            patch.get("name").asString == "AI Writing Tools"
        }
        assertTrue(aiWriting.get("use").asBoolean)
        assertEquals(
            listOf(
                "18.0.3.954559732-release-arm64-v8a",
                "18.3.1.977415014-beta-arm64-v8a",
                "18.3.1.977415014-release-arm64-v8a",
            ),
            aiWriting.getAsJsonObject("compatiblePackages")
                .getAsJsonArray("com.google.android.inputmethod.latin")
                .map { version -> version.asString },
        )
    }

    @Test
    fun catalogUsesOnlyTheGeneratedTargetAdmission() {
        val patches = generatedPublishedPatches()

        assertMatchesPublishedRegistrationCount(patches.size)
        assertEquals(
            setOf(GboardTargetAdmission.packageName),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages").keySet()
            }.toSet(),
        )
        assertEquals(
            GboardTargetAdmission.versionNames.toSet(),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages")
                    .getAsJsonArray(GboardTargetAdmission.packageName)
                    .map { version -> version.asString }
            }.toSet(),
        )
    }
}
