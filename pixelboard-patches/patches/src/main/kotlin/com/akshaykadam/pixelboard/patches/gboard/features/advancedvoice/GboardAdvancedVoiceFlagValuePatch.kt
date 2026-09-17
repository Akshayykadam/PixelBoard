package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardFlagFamilyFeature
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardFlagFamilyFeaturePatch

internal val gboardAdvancedVoiceFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "Apply Advanced Voice and Rambler typed policy after 18.0.3 stock flag getter.",
    feature = GboardFlagFamilyFeature.ADVANCED_VOICE_TYPING,
)
