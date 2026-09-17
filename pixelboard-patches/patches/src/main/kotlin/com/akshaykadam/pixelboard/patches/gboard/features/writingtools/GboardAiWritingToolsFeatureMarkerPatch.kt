/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.writingtools

import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.applyFeatureMarker
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardAiWritingToolsFeatureMarkerPatch = resourcePatch(
    description = "Mark AI Writing Tools feature injected into target APK for settings UI filtering."
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(AI_WRITING_TOOLS_FEATURE_MARKER_NAME)
    }
}
