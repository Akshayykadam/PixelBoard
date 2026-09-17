package com.akshaykadam.pixelboard.patches.gboard.registry

import com.google.gson.JsonParser
import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GboardContributionWiringTest {
    @Test
    fun `typed wiring covers every multi-contribution catalog feature`() {
        val catalog = JsonParser.parseString(
            Files.readString(
                Path.of("src/main/resources/gboard/gboard-port-product-catalog.json"),
            ),
        ).asJsonObject
        val expected = catalog.getAsJsonArray("features")
            .map { feature -> feature.asJsonObject }
            .filter { feature -> feature.getAsJsonArray("contributions").size() > 1 }
            .associate { feature ->
                val featureId = feature.get("feature_id").asString
                featureId to GboardTargetAdmission.admittedContributionIdsByFeature
                    .getValue(featureId)
            }

        assertEquals(expected, GboardContributionWiring.registeredContributionIdsByFeature)
    }

    @Test
    fun `independent execution units can be admitted without sibling mutations`() {
        assertEquals(
            setOf("advanced_voice_typing.flag"),
            GboardContributionWiring.activeContributionIds(
                "advanced_voice_typing",
                setOf("advanced_voice_typing.flag"),
            ),
        )
        assertEquals(
            setOf("advanced_voice_typing.bytecode"),
            GboardContributionWiring.activeContributionIds(
                "advanced_voice_typing",
                setOf("advanced_voice_typing.bytecode"),
            ),
        )
    }

    @Test
    fun `unregistered contribution IDs fail closed`() {
        assertThrows(IllegalStateException::class.java) {
            GboardContributionWiring.activeContributionIds(
                "advanced_voice_typing",
                setOf("advanced_voice_typing.unknown"),
            )
        }
    }
}
