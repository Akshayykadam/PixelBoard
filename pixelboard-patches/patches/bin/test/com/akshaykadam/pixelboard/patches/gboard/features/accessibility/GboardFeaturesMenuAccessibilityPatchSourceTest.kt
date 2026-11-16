package com.akshaykadam.pixelboard.patches.gboard.features.accessibility

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardFeaturesMenuAccessibilityPatchSourceTest {
    @Test
    fun `features menu accessibility patch targets AccessPointsPanel classes and emits runtime calls`() {
        val source = Files.readString(
            repositoryRoot().resolve(
                "patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/" +
                    "features/accessibility/GboardFeaturesMenuAccessibilityPatch.kt",
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("AccessPointsPanelV2"))
        assertTrue(source.contains("AccessPointsPanel"))
        assertTrue(source.contains("AccessPointsPanelViewPage"))
        assertTrue(source.contains("onFinishInflate"))
        assertTrue(source.contains("onVisibilityChanged"))
        assertTrue(source.contains("FEATURES_MENU_ACCESSIBILITY_ON_INFLATE"))
        assertTrue(source.contains("FEATURES_MENU_ACCESSIBILITY_ON_VISIBILITY_CHANGED"))
        assertTrue(source.contains("RuntimeCallEmitter.invoke(inflateCall, \"p0\")"))
        assertTrue(source.contains("RuntimeCallEmitter.invoke(visibilityCall, \"p0\")"))
    }

    private fun repositoryRoot(): Path {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        return generateSequence(workingDirectory) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            ?: error("Could not locate repository root from $workingDirectory")
    }
}
