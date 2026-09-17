/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.ResourcePatchContext
import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import org.w3c.dom.Document
import org.w3c.dom.Element

internal val gboardPatchesSettingsPatch = resourcePatch(
    description = "Add Patches entry to the bottom of Gboard settings."
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesExtensionCarrierPatch
    )

    finalize {
        applyPatchesSettingsPatch()
    }
}

context(context: ResourcePatchContext)
private fun applyPatchesSettingsPatch() = with(context) {
    try {
        val sparkleFile = get("res/drawable/ic_gemini_sparkle.xml", false)
        sparkleFile.parentFile?.mkdirs()
        sparkleFile.writeText(
            """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?android:attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C12,7.52 16.48,12 22,12C16.48,12 12,16.48 12,22C12,16.48 7.52,12 2,12C7.52,12 12,7.52 12,2Z" />
</vector>
"""
        )
    } catch (_: Throwable) {
    }

    document("AndroidManifest.xml").use { manifestDocument ->
        document(GBOARD_SETTINGS_XML_PATHS[0]).use { settingsDocument ->
            document(GBOARD_SETTINGS_XML_PATHS[1]).use { legacySettingsDocument ->
                applyGboardPatchesSettings(
                    manifestDocument = manifestDocument,
                    settingsDocuments = listOf(settingsDocument, legacySettingsDocument),
                    iconOverride = "@drawable/ic_gemini_sparkle",
                )
            }
        }
    }
}

internal fun applyGboardPatchesSettings(
    manifestDocument: Document,
    settingsDocuments: List<Document>,
    iconOverride: String? = null,
) {
    require(settingsDocuments.size == GBOARD_SETTINGS_XML_PATHS.size) {
        "Expected both Gboard settings documents"
    }

    val manifest = manifestDocument.documentElement
    val application = manifest.childElements("application").firstOrNull()
        ?: error("Could not find application element in AndroidManifest.xml")
    val packageName = manifest.getAttribute("package")

    val activity = application.childElements("activity").firstOrNull {
        it.getAttributeNS(ANDROID_NS, "name") == GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS ||
            it.getAttribute("android:name") == GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS
    } ?: manifestDocument.createElement("activity").also { createdActivity ->
        application.appendChild(createdActivity)
    }

    activity.setAndroidAttribute("name", GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS)
    if (activity.androidAttribute("exported").isNullOrBlank()) {
        activity.setAndroidAttribute("exported", "false")
    }
    val settingsActivity = application.childElements("activity").firstOrNull {
        it.getAttributeNS(ANDROID_NS, "name") == "com.google.android.apps.inputmethod.latin.preference.SettingsActivity" ||
            it.getAttribute("android:name") == "com.google.android.apps.inputmethod.latin.preference.SettingsActivity"
    }
    val settingsTheme = settingsActivity?.let {
        it.getAttributeNS(ANDROID_NS, "theme").takeIf { t -> t.isNotBlank() }
            ?: it.getAttribute("android:theme").takeIf { t -> t.isNotBlank() }
    }
    if (!settingsTheme.isNullOrBlank()) {
        activity.setAndroidAttribute("theme", settingsTheme)
    }

    val provider = application.childElements("provider").firstOrNull {
        it.getAttributeNS(ANDROID_NS, "name") == GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS ||
            it.getAttribute("android:name") == GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS
    } ?: manifestDocument.createElement("provider").also { createdProvider ->
        application.appendChild(createdProvider)
    }

    provider.setAndroidAttribute("name", GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS)
    provider.setAndroidAttribute(
        "authorities",
        packageName + GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX,
    )
    provider.setAndroidAttribute("exported", "false")

    settingsDocuments.forEach { document ->
        val root = document.documentElement
        val candidateEntries = root.childElements().flatMap { parent ->
            if (parent.tagName == "androidx.preference.PreferenceCategory") {
                parent.childElements()
            } else {
                sequenceOf(parent)
            }
        }.toList()
        val footerPreference = root.childElements().firstOrNull {
            it.tagName == "com.android.settingslib.widget.FooterPreference"
        }
        val targetCategory = root.childElements("androidx.preference.PreferenceCategory").lastOrNull()
        val rateUsPreference = candidateEntries.firstOrNull {
            it.tagName == RATE_US_PREFERENCE_TAG
        }
        val iconValue = iconOverride ?: rateUsPreference?.androidAttribute("icon")
        val existingEntry = candidateEntries.firstOrNull {
            it.androidAttribute("key") == PATCHES_SETTINGS_ENTRY_KEY
        }
        val patchesEntry = existingEntry?.takeIf { it.tagName == HEADER_PREFERENCE_TAG }
            ?: document.createElement(HEADER_PREFERENCE_TAG).also { createdPreference ->
                if (existingEntry != null) {
                    existingEntry.parentNode.replaceChild(createdPreference, existingEntry)
                } else if (targetCategory != null) {
                    targetCategory.appendChild(createdPreference)
                } else if (footerPreference != null) {
                    root.insertBefore(createdPreference, footerPreference)
                } else {
                    root.appendChild(createdPreference)
                }
            }

        patchesEntry.setAndroidAttribute("persistent", "false")
        patchesEntry.setAndroidAttribute("title", PATCHES_SETTINGS_ENTRY_TITLE)
        patchesEntry.setAndroidAttribute("selectable", "true")
        patchesEntry.setAndroidAttribute("key", PATCHES_SETTINGS_ENTRY_KEY)
        patchesEntry.removeAndroidAttribute("summary")
        if (!iconValue.isNullOrBlank()) {
            patchesEntry.setAndroidAttribute("icon", iconValue)
        }

        val intent = patchesEntry.childElements("intent").firstOrNull()
            ?: document.createElement("intent").also { createdIntent ->
                patchesEntry.appendChild(createdIntent)
            }
        intent.setAndroidAttribute("action", "android.intent.action.MAIN")
        intent.setAndroidAttribute("targetPackage", packageName)
        intent.setAndroidAttribute("targetClass", GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS)

        if (targetCategory != null && patchesEntry.parentNode != targetCategory) {
            patchesEntry.parentNode?.removeChild(patchesEntry)
            targetCategory.appendChild(patchesEntry)
        } else if (targetCategory == null && footerPreference != null &&
            patchesEntry.parentNode != root
        ) {
            patchesEntry.parentNode?.removeChild(patchesEntry)
            root.insertBefore(patchesEntry, footerPreference)
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

private fun Element.removeAndroidAttribute(localName: String) {
    removeAttributeNS(ANDROID_NS, localName)
    removeAttribute("android:$localName")
}

private fun Element.setAndroidAttribute(localName: String, value: String) {
    setAttributeNS(ANDROID_NS, "android:$localName", value)
}

internal const val GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS =
    "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity"
internal const val GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS =
    "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsProvider"
internal const val GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX = ".gboard_patches"
private const val PATCHES_SETTINGS_ENTRY_KEY = "gboard_patches_entry"
private const val PATCHES_SETTINGS_ENTRY_TITLE = "Advanced settings"
private const val HEADER_PREFERENCE_TAG =
    "com.google.android.libraries.inputmethod.settings.widget.HeaderPreference"
private const val RATE_US_PREFERENCE_TAG =
    "com.google.android.libraries.inputmethod.rateus.RateUsPreference"
internal val GBOARD_SETTINGS_XML_PATHS = listOf(
    "res/xml/settings.xml",
    "res/xml/settings_legacy.xml",
)
