/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Simple Calculator ported from Gboard-patches by Jason Wu (@jasonwu1994)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.calculator

import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.applyFeatureMarker
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardCalculatorFeatureMarkerPatch = resourcePatch(
    description = "Mark Simple Calculator feature injected into target APK",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(CALCULATOR_FEATURE_MARKER)
    }
}

internal const val CALCULATOR_FEATURE_MARKER =
    "com.akshaykadam.pixelboard.feature.simple_calculator"
