/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import com.akshaykadam.pixelboard.patches.shared.ResourcePatchContext
import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.applyFeatureMarker
import com.akshaykadam.pixelboard.patches.gboard.shared.ANDROID_NS
import com.akshaykadam.pixelboard.patches.gboard.shared.childElements
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import org.w3c.dom.Element

internal val gboardAdvancedVoiceFeatureMarkerPatch = resourcePatch(
    description = "Mark Advanced Voice Typing feature injected into target APK",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(ADVANCED_VOICE_FEATURE_MARKER)
        ensurePackageQueries()
    }
}

context(context: ResourcePatchContext)
private fun ensurePackageQueries() = with(context) {
    document("AndroidManifest.xml").use { document ->
        val manifest = document.documentElement
        val queries = manifest.childElements("queries").firstOrNull()
            ?: document.createElement("queries").also { createdQueries ->
                val application = manifest.childElements("application").firstOrNull()
                if (application == null) {
                    manifest.appendChild(createdQueries)
                } else {
                    manifest.insertBefore(createdQueries, application)
                }
            }

        ADVANCED_VOICE_PACKAGE_QUERIES.forEach { packageName ->
            val packageQuery = queries.childElements("package").firstOrNull {
                it.androidAttribute("name") == packageName
            } ?: document.createElement("package").also(queries::appendChild)
            packageQuery.setAndroidAttribute("name", packageName)
        }
    }
}

private fun Element.androidAttribute(localName: String): String? {
    val namespaced = getAttributeNS(ANDROID_NS, localName)
    if (namespaced.isNotBlank()) {
        return namespaced
    }
    return getAttribute("android:$localName").takeIf { it.isNotBlank() }
}

private fun Element.setAndroidAttribute(localName: String, value: String) {
    setAttributeNS(ANDROID_NS, "android:$localName", value)
}

internal const val ADVANCED_VOICE_FEATURE_MARKER =
    "com.akshaykadam.pixelboard.feature.advanced_voice_typing"

private val ADVANCED_VOICE_PACKAGE_QUERIES = listOf(
    "com.google.android.tts",
    "com.google.audio.hearing.visualization.accessibility.scribe",
)
