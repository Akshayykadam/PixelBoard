/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.packagerename

import com.akshaykadam.pixelboard.patches.shared.ResourcePatchContext
import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.ANDROID_NS
import com.akshaykadam.pixelboard.patches.gboard.shared.GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS
import com.akshaykadam.pixelboard.patches.gboard.shared.GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX
import com.akshaykadam.pixelboard.patches.gboard.shared.GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS
import com.akshaykadam.pixelboard.patches.gboard.shared.GBOARD_SETTINGS_XML_PATHS
import com.akshaykadam.pixelboard.patches.gboard.shared.childElements
import com.akshaykadam.pixelboard.patches.gboard.shared.elements
import com.akshaykadam.pixelboard.patches.shared.Constants.GBOARD_PACKAGE_NAME
import com.akshaykadam.pixelboard.patches.shared.Constants.GBOARD_PATCHED_PACKAGE_NAME
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element

internal val gboardPackageRenameResourcePatch = resourcePatch(
    description =
        "Rename package to a custom value for coexistence; supports single APK only."
) {
    finalize {
        applyManifestPackageOverride()
    }
}

context(context: ResourcePatchContext)
private fun applyManifestPackageOverride() = with(context) {
    document("AndroidManifest.xml").use { manifestDocument ->
        document(GBOARD_SETTINGS_XML_PATHS[0]).use { settingsDocument ->
            document(GBOARD_SETTINGS_XML_PATHS[1]).use { legacySettingsDocument ->
                applyGboardPackageRename(
                    manifestDocument = manifestDocument,
                    settingsDocuments = listOf(settingsDocument, legacySettingsDocument),
                )
            }
        }
    }
    sanitizeMissingSplitResources()
}

context(context: ResourcePatchContext)
private fun sanitizeMissingSplitResources() = with(context) {
    val resDir = try {
        this.get("res")
    } catch (_: Throwable) {
        return@with
    }
    if (!resDir.exists() || !resDir.isDirectory) return@with

    val xmlFiles = resDir.walkTopDown()
        .filter { it.isFile && it.extension == "xml" && it.name != "public.xml" }
        .toList()

    val hexPattern = Regex("""@0x7f08[0-9a-fA-F]{4}""")
    val missingHexRefs = mutableSetOf<String>()
    for (file in xmlFiles) {
        val text = file.readText()
        hexPattern.findAll(text).forEach { match ->
            missingHexRefs.add(match.value)
        }
    }
    if (missingHexRefs.isEmpty()) return@with

    val publicFile = this.get("res/values/public.xml")
    val drawablesFile = this.get("res/values/drawables.xml")
    if (!drawablesFile.exists()) {
        drawablesFile.parentFile?.mkdirs()
        drawablesFile.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n")
    }

    var publicText = publicFile.readText()
    var drawablesText = drawablesFile.readText()
    val newPublic = mutableListOf<String>()
    val newDrawables = mutableListOf<String>()

    val fallbackDrawable = when {
        publicText.contains("name=\"drawable_0x7f0804a5\"") -> "@drawable/drawable_0x7f0804a5"
        publicText.contains("name=\"drawable_0x7f080000\"") -> "@drawable/drawable_0x7f080000"
        else -> "@null"
    }

    for (ref in missingHexRefs.sorted()) {
        val hexId = ref.substring(1).lowercase()
        val drawableName = "drawable_$hexId"
        if (!publicText.contains("id=\"$hexId\"")) {
            newPublic.add("  <public id=\"$hexId\" type=\"drawable\" name=\"$drawableName\" />")
        }
        if (!drawablesText.contains("name=\"$drawableName\"")) {
            newDrawables.add("  <drawable name=\"$drawableName\">$fallbackDrawable</drawable>")
        }
    }

    if (newPublic.isNotEmpty()) {
        val closeTag = "</resources>"
        val index = publicText.lastIndexOf(closeTag)
        if (index != -1) {
            publicText = publicText.substring(0, index) + newPublic.joinToString("\n") + "\n" + closeTag + publicText.substring(index + closeTag.length)
            publicFile.writeText(publicText)
        }
    }
    if (newDrawables.isNotEmpty()) {
        val closeTag = "</resources>"
        val index = drawablesText.lastIndexOf(closeTag)
        if (index != -1) {
            drawablesText = drawablesText.substring(0, index) + newDrawables.joinToString("\n") + "\n" + closeTag + drawablesText.substring(index + closeTag.length)
            drawablesFile.writeText(drawablesText)
        }
    }

    for (file in xmlFiles) {
        var content = file.readText()
        var modified = false
        for (ref in missingHexRefs) {
            if (content.contains(ref)) {
                val hexId = ref.substring(1).lowercase()
                content = content.replace(ref, "@drawable/drawable_$hexId")
                modified = true
            }
        }
        if (modified) {
            file.writeText(content)
        }
    }
}

internal data class GboardPackageRenameMapping(
    val elementName: String,
    val namespaceUri: String?,
    val attributeName: String,
    val originalValue: String,
    val renamedValue: String,
) {
    val qualifiedAttributeName: String
        get() = if (namespaceUri == null) attributeName else "android:$attributeName"
}

internal enum class GboardPackageRenameResult {
    RENAMED,
    ALREADY_RENAMED,
}

internal const val MAX_GBOARD_APP_DISPLAY_NAME_CODE_POINTS = 40

internal fun isValidGboardAppDisplayName(value: String?): Boolean {
    if (value.isNullOrEmpty() || value != value.trim()) return false
    if (value.codePointCount(0, value.length) > MAX_GBOARD_APP_DISPLAY_NAME_CODE_POINTS) return false
    if (value.first() == '@' || value.first() == '?') return false
    return value.none { character ->
        character.isISOControl() || character == '\u2028' || character == '\u2029'
    }
}

internal fun applyGboardApplicationDisplayName(
    manifestDocument: Document,
    displayName: String,
) {
    require(isValidGboardAppDisplayName(displayName)) {
        "Invalid Gboard app display name"
    }
    val applications = manifestDocument.documentElement.childElements("application").toList()
    check(applications.size == 1) {
        "Expected exactly one application element, found ${applications.size}"
    }
    val label = applications.single().androidAttribute("label")
        ?: error("Gboard application is missing android:label")
    label.value = displayName
}

internal val GBOARD_PACKAGE_RENAME_MAPPINGS = listOf(
    mapping("manifest", null, "package", GBOARD_PACKAGE_NAME, GBOARD_PATCHED_PACKAGE_NAME),
    androidMapping(
        "permission",
        "name",
        "$GBOARD_PACKAGE_NAME.pixelbundle.RECEIVER",
        "$GBOARD_PATCHED_PACKAGE_NAME.pixelbundle.RECEIVER",
    ),
    androidMapping(
        "permission",
        "name",
        "$GBOARD_PACKAGE_NAME.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        "$GBOARD_PATCHED_PACKAGE_NAME.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    ),
    androidMapping(
        "meta-data",
        "name",
        "com.google.android.gms.phenotype.registration.binarypb:$GBOARD_PACKAGE_NAME",
        "com.google.android.gms.phenotype.registration.binarypb:$GBOARD_PATCHED_PACKAGE_NAME",
    ),
    androidMapping(
        "meta-data",
        "name",
        "com.google.android.gms.phenotype.registration.xml:$GBOARD_PACKAGE_NAME",
        "com.google.android.gms.phenotype.registration.xml:$GBOARD_PATCHED_PACKAGE_NAME",
    ),
    androidMapping(
        "provider",
        "authorities",
        GBOARD_PACKAGE_NAME,
        GBOARD_PATCHED_PACKAGE_NAME,
    ),
    androidMapping(
        "data",
        "host",
        "deeplink.$GBOARD_PACKAGE_NAME",
        "deeplink.$GBOARD_PATCHED_PACKAGE_NAME",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.clipboard_content",
        "$GBOARD_PATCHED_PACKAGE_NAME.clipboard_content",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.inputactionprovider",
        "$GBOARD_PATCHED_PACKAGE_NAME.inputactionprovider",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.swissarmyknifefileprovider",
        "$GBOARD_PATCHED_PACKAGE_NAME.swissarmyknifefileprovider",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.fileprovider",
        "$GBOARD_PATCHED_PACKAGE_NAME.fileprovider",
    ),
    androidMapping(
        "receiver",
        "permission",
        "$GBOARD_PACKAGE_NAME.pixelbundle.RECEIVER",
        "$GBOARD_PATCHED_PACKAGE_NAME.pixelbundle.RECEIVER",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.tracing",
        "$GBOARD_PATCHED_PACKAGE_NAME.tracing",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.wdb",
        "$GBOARD_PATCHED_PACKAGE_NAME.wdb",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.mlkitinitprovider",
        "$GBOARD_PATCHED_PACKAGE_NAME.mlkitinitprovider",
    ),
)

internal val GBOARD_OPTIONAL_PACKAGE_RENAME_MAPPINGS = listOf(
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.androidx-startup",
        "$GBOARD_PATCHED_PACKAGE_NAME.androidx-startup",
    ),
    androidMapping(
        "provider",
        "authorities",
        "$GBOARD_PACKAGE_NAME.train.androidx-startup",
        "$GBOARD_PATCHED_PACKAGE_NAME.train.androidx-startup",
    ),
)

internal fun applyGboardPackageRename(
    manifestDocument: Document,
    settingsDocuments: List<Document>,
): GboardPackageRenameResult {
    val rootPackage = manifestDocument.documentElement.getAttribute("package")
    val state = when (rootPackage) {
        GBOARD_PACKAGE_NAME -> PackageState.ORIGINAL
        GBOARD_PATCHED_PACKAGE_NAME -> PackageState.RENAMED
        else -> error(
            "Unexpected Gboard manifest package '$rootPackage'; expected " +
                "'$GBOARD_PACKAGE_NAME' or '$GBOARD_PATCHED_PACKAGE_NAME'",
        )
    }

    require(settingsDocuments.size == GBOARD_SETTINGS_XML_PATHS.size) {
        "Expected both Gboard settings documents"
    }

    val versionCodeAttr = manifestDocument.documentElement.androidAttribute("versionCode")
    if (versionCodeAttr != null) {
        val currentCode = versionCodeAttr.value.toLongOrNull() ?: 0L
        if (currentCode in 176004200..176004237) {
            versionCodeAttr.value = "176004240"
        }
    }

    val allManifestAttributes = manifestDocument.getElementsByTagName("*")
        .elements()
        .flatMap { element -> element.attributes.asSequence().map { attribute -> element to attribute } }
        .toList()
    val doublePrefixedValue = allManifestAttributes.firstOrNull { (_, attribute) ->
        attribute.nodeValue.contains(DOUBLE_PREFIX)
    }
    check(doublePrefixedValue == null) {
        val (element, attribute) = checkNotNull(doublePrefixedValue)
        "Double-prefixed package value at ${element.localElementName()} " +
            "${attribute.nodeName}: ${attribute.nodeValue}"
    }

    val selectedAttributes = GBOARD_PACKAGE_RENAME_MAPPINGS.map { mapping ->
        val matchingElements = manifestDocument.getElementsByTagName("*")
            .elements()
            .filter { element -> element.localElementName() == mapping.elementName }
        val originalMatches = matchingElements.mapNotNull { element ->
            element.attribute(mapping)?.takeIf { attribute ->
                attribute.value == mapping.originalValue
            }
        }.toList()
        val renamedMatches = matchingElements.mapNotNull { element ->
            element.attribute(mapping)?.takeIf { attribute ->
                attribute.value == mapping.renamedValue
            }
        }.toList()

        val expectedMatches = when (state) {
            PackageState.ORIGINAL -> originalMatches
            PackageState.RENAMED -> renamedMatches
        }
        val oppositeMatches = when (state) {
            PackageState.ORIGINAL -> renamedMatches
            PackageState.RENAMED -> originalMatches
        }
        check(expectedMatches.size == 1 && oppositeMatches.isEmpty()) {
            "Expected exactly one ${state.label} ${mapping.elementName} " +
                "${mapping.qualifiedAttributeName} mapping from '${mapping.originalValue}' " +
                "to '${mapping.renamedValue}', found original=${originalMatches.size}, " +
                "renamed=${renamedMatches.size}"
        }
        expectedMatches.single()
    }

    val selectedOptionalAttributes = GBOARD_OPTIONAL_PACKAGE_RENAME_MAPPINGS.mapNotNull { mapping ->
        val matchingElements = manifestDocument.getElementsByTagName("*")
            .elements()
            .filter { element -> element.localElementName() == mapping.elementName }
        val originalMatches = matchingElements.mapNotNull { element ->
            element.attribute(mapping)?.takeIf { attribute ->
                attribute.value == mapping.originalValue
            }
        }.toList()
        val renamedMatches = matchingElements.mapNotNull { element ->
            element.attribute(mapping)?.takeIf { attribute ->
                attribute.value == mapping.renamedValue
            }
        }.toList()

        val expectedMatches = when (state) {
            PackageState.ORIGINAL -> originalMatches
            PackageState.RENAMED -> renamedMatches
        }
        val oppositeMatches = when (state) {
            PackageState.ORIGINAL -> renamedMatches
            PackageState.RENAMED -> originalMatches
        }
        check(expectedMatches.size <= 1 && oppositeMatches.isEmpty()) {
            "Expected at most one ${state.label} ${mapping.elementName} " +
                "${mapping.qualifiedAttributeName} mapping from '${mapping.originalValue}' " +
                "to '${mapping.renamedValue}', found original=${originalMatches.size}, " +
                "renamed=${renamedMatches.size}"
        }
        expectedMatches.singleOrNull()?.let { attribute -> attribute to mapping }
    }

    val settingsIdentity = validateSettingsIdentity(
        manifestDocument = manifestDocument,
        settingsDocuments = settingsDocuments,
        state = state,
    )
    val allowedPackageAttributes = selectedAttributes +
        selectedOptionalAttributes.map { it.first } +
        listOfNotNull(settingsIdentity.providerAuthority)
    val unexpectedPackageAttribute = allManifestAttributes.firstOrNull { (_, attribute) ->
        attribute.nodeValue.contains(GBOARD_PACKAGE_NAME) &&
            allowedPackageAttributes.none { allowed -> allowed === attribute }
    }
    check(unexpectedPackageAttribute == null) {
        val (element, attribute) = checkNotNull(unexpectedPackageAttribute)
        "Unexpected package-derived manifest value at ${element.localElementName()} " +
            "${attribute.nodeName}: ${attribute.nodeValue}"
    }

    if (state == PackageState.RENAMED) {
        return GboardPackageRenameResult.ALREADY_RENAMED
    }

    sanitizeStandaloneSplitManifest(manifestDocument)
    selectedAttributes.zip(GBOARD_PACKAGE_RENAME_MAPPINGS).forEach { (attribute, mapping) ->
        attribute.value = mapping.renamedValue
    }
    selectedOptionalAttributes.forEach { (attribute, mapping) ->
        attribute.value = mapping.renamedValue
    }
    settingsIdentity.providerAuthority?.value =
        GBOARD_PATCHED_PACKAGE_NAME + GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX
    settingsIdentity.targetPackages.forEach { attribute ->
        attribute.value = GBOARD_PATCHED_PACKAGE_NAME
    }
    return GboardPackageRenameResult.RENAMED
}

private fun sanitizeStandaloneSplitManifest(manifestDocument: Document) {
    val manifest = manifestDocument.documentElement
    listOf("requiredSplitTypes", "splitTypes").forEach { attributeName ->
        manifest.androidAttribute(attributeName)?.let(manifest::removeAttributeNode)
    }
    manifestDocument.getElementsByTagName("*")
        .elements()
        .filter { element ->
            element.localElementName() == "meta-data" &&
                element.androidAttribute("name")?.value == REQUIRED_SPLITS_METADATA
        }
        .toList()
        .forEach { element -> element.parentNode.removeChild(element) }
}

private fun validateSettingsIdentity(
    manifestDocument: Document,
    settingsDocuments: List<Document>,
    state: PackageState,
): SettingsIdentity {
    val settingsProviders = manifestDocument.getElementsByTagName("*")
        .elements()
        .filter { element ->
            element.localElementName() == "provider" &&
                element.androidAttribute("name")?.value == GBOARD_PATCHES_SETTINGS_PROVIDER_CLASS
        }
        .toList()
    check(settingsProviders.size <= 1) {
        "Expected at most one Gboard patches settings provider, found ${settingsProviders.size}"
    }
    val providerAuthority = settingsProviders.singleOrNull()?.androidAttribute("authorities")
    check(settingsProviders.isEmpty() || providerAuthority != null) {
        "Gboard patches settings provider is missing android:authorities"
    }

    val expectedPackage = state.packageName
    if (providerAuthority != null) {
        val expectedAuthority =
            expectedPackage + GBOARD_PATCHES_SETTINGS_PROVIDER_AUTHORITY_SUFFIX
        check(providerAuthority.value == expectedAuthority) {
            "Unexpected Gboard patches settings provider authority " +
                "'${providerAuthority.value}'; expected '$expectedAuthority'"
        }
    }

    val targetPackagesByDocument = settingsDocuments.map { document ->
        val matchingIntents = document.getElementsByTagName("*")
            .elements()
            .filter { element ->
                element.localElementName() == "intent" &&
                    element.androidAttribute("targetClass")?.value ==
                    GBOARD_PATCHES_SETTINGS_ACTIVITY_CLASS
            }
            .toList()
        check(matchingIntents.size <= 1) {
            "Expected at most one Gboard patches settings intent, found ${matchingIntents.size}"
        }
        matchingIntents.singleOrNull()?.let { intent ->
            val targetPackage = intent.androidAttribute("targetPackage")
                ?: error("Gboard patches settings intent is missing android:targetPackage")
            check(targetPackage.value == expectedPackage) {
                "Unexpected Gboard patches settings targetPackage '${targetPackage.value}'; " +
                    "expected '$expectedPackage'"
            }
            targetPackage
        }
    }

    val hasProvider = providerAuthority != null
    val hasAllSettingsIntents =
        settingsDocuments.isNotEmpty() && targetPackagesByDocument.all { it != null }
    val hasAnySettingsIntent = targetPackagesByDocument.any { it != null }
    check(
        (!hasProvider && !hasAnySettingsIntent) ||
            (hasProvider && hasAllSettingsIntents),
    ) {
        "Partial Gboard patches settings identity: provider=$hasProvider, " +
            "settingsIntents=${targetPackagesByDocument.count { it != null }}/" +
            settingsDocuments.size
    }

    return SettingsIdentity(
        providerAuthority = providerAuthority,
        targetPackages = targetPackagesByDocument.filterNotNull(),
    )
}

private fun mapping(
    elementName: String,
    namespaceUri: String?,
    attributeName: String,
    originalValue: String,
    renamedValue: String,
) = GboardPackageRenameMapping(
    elementName = elementName,
    namespaceUri = namespaceUri,
    attributeName = attributeName,
    originalValue = originalValue,
    renamedValue = renamedValue,
)

private fun androidMapping(
    elementName: String,
    attributeName: String,
    originalValue: String,
    renamedValue: String,
) = mapping(
    elementName = elementName,
    namespaceUri = ANDROID_NS,
    attributeName = attributeName,
    originalValue = originalValue,
    renamedValue = renamedValue,
)

private fun Element.attribute(mapping: GboardPackageRenameMapping): Attr? =
    attributes.asSequence().filterIsInstance<Attr>().firstOrNull { attribute ->
        if (mapping.namespaceUri == null) {
            attribute.namespaceURI == null && attribute.nodeName == mapping.attributeName
        } else {
            (
                attribute.namespaceURI == mapping.namespaceUri &&
                    attribute.localName == mapping.attributeName
                ) ||
                attribute.nodeName == "android:${mapping.attributeName}"
        }
    }

private fun Element.androidAttribute(localName: String): Attr? =
    attributes.asSequence().filterIsInstance<Attr>().firstOrNull { attribute ->
        (
            attribute.namespaceURI == ANDROID_NS &&
                attribute.localName == localName
            ) ||
            attribute.nodeName == "android:$localName"
    }

private fun Element.localElementName(): String = localName ?: tagName.substringAfter(':')

private fun org.w3c.dom.NamedNodeMap.asSequence(): Sequence<org.w3c.dom.Node> =
    (0 until length).asSequence().map(::item)

private enum class PackageState(
    val packageName: String,
    val label: String,
) {
    ORIGINAL(GBOARD_PACKAGE_NAME, "original"),
    RENAMED(GBOARD_PATCHED_PACKAGE_NAME, "renamed"),
}

private data class SettingsIdentity(
    val providerAuthority: Attr?,
    val targetPackages: List<Attr>,
)

private const val DOUBLE_PREFIX =
    "com.akshaykadam.com.akshaykadam.pixelboard"

private const val REQUIRED_SPLITS_METADATA = "com.android.vending.splits.required"
