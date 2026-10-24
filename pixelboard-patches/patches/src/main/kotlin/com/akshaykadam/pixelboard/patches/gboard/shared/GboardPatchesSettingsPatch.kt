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
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element

import com.akshaykadam.pixelboard.patches.gboard.features.emoji.gboardCandidateEmojiVibrancyPatch
import com.akshaykadam.pixelboard.patches.gboard.features.clipboard.gboardClipboardSyncPatch
import com.akshaykadam.pixelboard.patches.gboard.features.clipboard.gboardClipboardHistorySliderPatch
import com.akshaykadam.pixelboard.patches.gboard.features.spellchecker.gboardSpellCheckerChipsPatch
import com.akshaykadam.pixelboard.patches.gboard.features.accessibility.gboardFeaturesMenuAccessibilityPatch
import com.akshaykadam.pixelboard.patches.gboard.features.rambler.gboardRamblerDictationPromptFixPatch

internal val gboardPatchesSettingsPatch = resourcePatch(
    description = "Add Patches entry to the bottom of Gboard settings."
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardCandidateEmojiVibrancyPatch,
        gboardClipboardSyncPatch,
        gboardClipboardHistorySliderPatch,
        gboardSpellCheckerChipsPatch,
        gboardFeaturesMenuAccessibilityPatch,
        gboardRamblerDictationPromptFixPatch,
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
    applySpellCheckerXmlOverride()
    applyEmojiSuggestionXmlOverride()
    applyClipboardXmlOverride()
    applyPixelboardStringsOverride()
}

internal fun applyGboardPatchesSettings(
    manifestDocument: Document,
    settingsDocuments: List<Document>,
    iconOverride: String? = null,
) {
    require(settingsDocuments.size == GBOARD_SETTINGS_XML_PATHS.size) {
        "Expected both Gboard settings documents"
    }

    applyPixelboardBranding(manifestDocument, settingsDocuments)

    val manifest = manifestDocument.documentElement
    val application = manifest.childElements("application").firstOrNull()
        ?: error("Could not find application element in AndroidManifest.xml")
    val packageName = manifest.getAttribute("package")

    ensureManifestUsesPermission(manifestDocument, manifest, "android.permission.CAMERA")
    ensureManifestUsesPermission(manifestDocument, manifest, "android.permission.READ_MEDIA_IMAGES")
    ensureManifestUsesPermission(manifestDocument, manifest, "android.permission.READ_EXTERNAL_STORAGE")

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
        it.getAttributeNS(ANDROID_NS, "name") == GBOARD_SETTINGS_ACTIVITY_CLASS ||
            it.getAttribute("android:name") == GBOARD_SETTINGS_ACTIVITY_CLASS
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

        // Remove "Rate Gboard" and "Help & feedback" from settings
        var foundAbout = false
        candidateEntries.forEach { entry ->
            val tag = entry.tagName
            val key = entry.androidAttribute("key") ?: ""
            val title = entry.androidAttribute("title") ?: ""
            val fragment = entry.androidAttribute("fragment") ?: ""
            if (fragment.contains("AboutSettingsFragment")) {
                foundAbout = true
            }
            val isRateGboard = tag == RATE_US_PREFERENCE_TAG ||
                tag.endsWith("RateUsPreference") ||
                key.contains("0x7f140b59") ||
                key.contains("0x7f140a77") ||
                key.contains("rate", ignoreCase = true) ||
                title.contains("0x7f140d4e") ||
                title.contains("0x7f140d4f") ||
                title.contains("0x7f140c34") ||
                title.contains("Rate Gboard", ignoreCase = true)
            val isHelpFeedback = entry.tagName == HEADER_PREFERENCE_TAG && (
                key.contains("0x7f140b50") ||
                key.contains("0x7f140a6e") ||
                key.contains("feedback", ignoreCase = true) ||
                title.contains("0x7f140cd3") ||
                title.contains("0x7f140bd7") ||
                title.contains("Help & feedback", ignoreCase = true) ||
                title.contains("Help and feedback", ignoreCase = true) ||
                (foundAbout && fragment.isBlank() && key != PATCHES_SETTINGS_ENTRY_KEY)
            )
            if (isRateGboard || isHelpFeedback) {
                entry.parentNode?.removeChild(entry)
            }
        }
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

internal fun applyPixelboardBranding(
    manifestDocument: Document,
    settingsDocuments: List<Document>,
) {
    val manifest = manifestDocument.documentElement
    val application = manifest.childElements("application").firstOrNull() ?: return

    val settingsActivity = application.childElements("activity").firstOrNull {
        it.androidAttribute("name") == GBOARD_SETTINGS_ACTIVITY_CLASS
    }
    settingsActivity?.setAndroidAttribute("label", PIXELBOARD_SETTINGS_TITLE)

    val spellCheckerService = application.childElements("service").firstOrNull {
        it.androidAttribute("name") == GBOARD_SPELL_CHECKER_SERVICE_CLASS
    }
    spellCheckerService?.setAndroidAttribute("label", PIXELBOARD_SPELL_CHECKER_LABEL)

    val spellCheckerActivity = application.childElements("activity").firstOrNull {
        it.androidAttribute("name") == GBOARD_SPELL_CHECKER_SETTINGS_ACTIVITY_CLASS
    }
    spellCheckerActivity?.setAndroidAttribute("label", PIXELBOARD_SPELL_CHECKER_SETTINGS_LABEL)

    settingsDocuments.forEach { document ->
        document.documentElement.setAndroidAttribute("title", PIXELBOARD_SETTINGS_TITLE)
    }
}

context(context: ResourcePatchContext)
internal fun applySpellCheckerXmlOverride() = with(context) {
    try {
        document(GBOARD_SPELL_CHECKER_XML_PATH).use { document ->
            val root = document.documentElement ?: return@use
            val currentLabel = root.androidAttribute("label")
            // Android's SpellCheckerInfo requires android:label in spell_checker.xml to be a resource ID reference (@string/...).
            // If it is a string literal (or missing), ensure it points to @string/string_0x7f140d61.
            // If it is already a resource reference, keep it so TypedArray.getResourceId() succeeds.
            if (currentLabel == null || !currentLabel.startsWith("@string/")) {
                root.setAndroidAttribute("label", "@string/string_0x7f140d61")
            }
        }
    } catch (_: Throwable) {
    }
}

context(context: ResourcePatchContext)
internal fun applyEmojiSuggestionXmlOverride() = with(context) {
    try {
        document("res/xml/setting_expression.xml").use { doc ->
            val switches = doc.getElementsByTagName("SwitchPreferenceCompat")
            for (i in 0 until switches.length) {
                val element = switches.item(i) as? Element ?: continue
                val key = element.androidAttribute("key")
                if (key == "@string/string_0x7f14093e" || key == "pref_key_enable_emoji_suggestion") {
                    element.setAndroidAttribute("defaultValue", "true")
                }
            }
        }
    } catch (_: Throwable) {
    }
}

context(context: ResourcePatchContext)
internal fun applyClipboardXmlOverride() = with(context) {
    try {
        document("res/xml/setting_clipboard.xml").use { doc ->
            val switches = doc.getElementsByTagName("SwitchPreferenceCompat")
            for (i in 0 until switches.length) {
                val element = switches.item(i) as? Element ?: continue
                val key = element.androidAttribute("key")
                if (key == "@string/string_0x7f140963" || key == "enable_screenshot_in_clipboard" ||
                    key == "@string/string_0x7f140945" || key == "pref_key_enable_clipboard"
                ) {
                    element.setAndroidAttribute("defaultValue", "true")
                }
            }
        }
    } catch (_: Throwable) {
    }
}

context(context: ResourcePatchContext)
internal fun applyPixelboardStringsOverride() = with(context) {
    val resDir = try {
        this.get("res")
    } catch (_: Throwable) {
        return@with
    }
    if (!resDir.exists() || !resDir.isDirectory) return@with

    val stringFiles = resDir.walkTopDown()
        .filter { it.isFile && it.name == "strings.xml" }
        .toList()

    val spellCheckerStringNames = setOf("string_0x7f140d61", "string_0x7f140d5e")
    val spellCheckerSettingsStringNames = setOf("string_0x7f140d6b", "string_0x7f140d68")
    val settingsStringNames = setOf("string_0x7f140c9a")

    for (file in stringFiles) {
        var text = file.readText()
        var modified = false

        for (name in spellCheckerStringNames) {
            val regex = Regex("""<string name="$name">.*?</string>""")
            if (regex.containsMatchIn(text)) {
                text = regex.replace(text, """<string name="$name">$PIXELBOARD_SPELL_CHECKER_LABEL</string>""")
                modified = true
            }
        }
        for (name in spellCheckerSettingsStringNames) {
            val regex = Regex("""<string name="$name">.*?</string>""")
            if (regex.containsMatchIn(text)) {
                text = regex.replace(text, """<string name="$name">$PIXELBOARD_SPELL_CHECKER_SETTINGS_LABEL</string>""")
                modified = true
            }
        }
        for (name in settingsStringNames) {
            val regex = Regex("""<string name="$name">.*?</string>""")
            if (regex.containsMatchIn(text)) {
                text = regex.replace(text, """<string name="$name">$PIXELBOARD_SETTINGS_TITLE</string>""")
                modified = true
            }
        }

        if (text.contains(">Gboard settings<")) {
            text = text.replace(">Gboard settings<", ">$PIXELBOARD_SETTINGS_TITLE<")
            modified = true
        }
        if (text.contains(">Gboard spell checker<")) {
            text = text.replace(">Gboard spell checker<", ">$PIXELBOARD_SPELL_CHECKER_LABEL<")
            modified = true
        }
        if (text.contains(">Gboard spellchecker<")) {
            text = text.replace(">Gboard spellchecker<", ">$PIXELBOARD_SPELL_CHECKER_LABEL<")
            modified = true
        }
        if (text.contains(">Gboard spell checker settings<")) {
            text = text.replace(">Gboard spell checker settings<", ">$PIXELBOARD_SPELL_CHECKER_SETTINGS_LABEL<")
            modified = true
        }
        if (text.contains(">Gboard spellchecker settings<")) {
            text = text.replace(">Gboard spellchecker settings<", ">$PIXELBOARD_SPELL_CHECKER_SETTINGS_LABEL<")
            modified = true
        }

        val parentDirName = file.parentFile?.name ?: ""
        val lang = if (parentDirName == "values") "en" else {
            parentDirName.removePrefix("values-").split("-", "_").firstOrNull()?.lowercase() ?: "en"
        }
        val localizedTitle = SETTINGS_TITLE_TRANSLATIONS[lang] ?: "Advanced settings"
        if (!text.contains("name=\"pixelboard_patches_settings_title\"")) {
            val endResourcesIndex = text.lastIndexOf("</resources>")
            if (endResourcesIndex >= 0) {
                text = text.substring(0, endResourcesIndex) +
                    "    <string name=\"pixelboard_patches_settings_title\">$localizedTitle</string>\n" +
                    text.substring(endResourcesIndex)
                modified = true
            }
        }

        if (modified) {
            file.writeText(text)
        }
    }
}

private fun Element.findAndroidAttr(localName: String): Attr? =
    attributes?.asSequence()?.filterIsInstance<Attr>()?.firstOrNull { attribute ->
        (
            attribute.namespaceURI == ANDROID_NS &&
                attribute.localName == localName
            ) ||
            attribute.nodeName == "android:$localName" ||
            attribute.name == "android:$localName" ||
            attribute.localName == localName
    }

private fun Element.androidAttribute(localName: String): String? {
    val attr = findAndroidAttr(localName)
    if (attr != null && attr.value.isNotBlank()) {
        return attr.value
    }
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

internal fun Element.setAndroidAttribute(localName: String, value: String) {
    val attr = findAndroidAttr(localName)
    if (attr != null) {
        attr.value = value
    } else {
        try {
            setAttributeNS(ANDROID_NS, "android:$localName", value)
        } catch (_: Throwable) {
            setAttribute("android:$localName", value)
        }
    }
}

internal const val PIXELBOARD_SETTINGS_TITLE = "PixelBoard Settings"
internal const val PIXELBOARD_SPELL_CHECKER_LABEL = "PixelBoard Spell Checker"
internal const val PIXELBOARD_SPELL_CHECKER_SETTINGS_LABEL = "PixelBoard Spell Checker Settings"
internal const val GBOARD_SETTINGS_ACTIVITY_CLASS =
    "com.google.android.apps.inputmethod.latin.preference.SettingsActivity"
internal const val GBOARD_SPELL_CHECKER_SERVICE_CLASS =
    "com.android.inputmethod.latin.spellcheck.AndroidSpellCheckerService"
internal const val GBOARD_SPELL_CHECKER_SETTINGS_ACTIVITY_CLASS =
    "com.google.android.apps.inputmethod.latin.spelling.LatinSpellCheckerSettingsActivity"
internal const val GBOARD_SPELL_CHECKER_XML_PATH = "res/xml/spell_checker.xml"

internal const val GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS =
    "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity"
internal const val GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS =
    "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsProvider"
internal const val GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX = ".gboard_patches"
private const val PATCHES_SETTINGS_ENTRY_KEY = "gboard_patches_entry"
private const val PATCHES_SETTINGS_ENTRY_TITLE = "@string/pixelboard_patches_settings_title"
private const val HEADER_PREFERENCE_TAG =
    "com.google.android.libraries.inputmethod.settings.widget.HeaderPreference"
private const val RATE_US_PREFERENCE_TAG =
    "com.google.android.libraries.inputmethod.rateus.RateUsPreference"
internal val GBOARD_SETTINGS_XML_PATHS = listOf(
    "res/xml/settings.xml",
    "res/xml/settings_legacy.xml",
)

private val SETTINGS_TITLE_TRANSLATIONS = mapOf(
    "en" to "Advanced settings",
    "es" to "Ajustes avanzados",
    "fr" to "Paramètres avancés",
    "de" to "Erweiterte Einstellungen",
    "it" to "Impostazioni avanzate",
    "pt" to "Configurações avançadas",
    "ru" to "Расширенные настройки",
    "hi" to "उन्नत सेटिंग्स",
    "ja" to "詳細設定",
    "ko" to "고급 설정",
    "zh" to "高级设置",
    "ar" to "الإعدادات المتقدمة",
    "in" to "Setelan lanjutan",
    "id" to "Setelan lanjutan",
    "tr" to "Gelişmiş ayarlar",
    "vi" to "Cài đặt nâng cao",
    "pl" to "Ustawienia zaawansowane",
    "nl" to "Geavanceerde instellingen",
    "th" to "การตั้งค่าขั้นสูง",
    "uk" to "Розширені налаштування",
)
