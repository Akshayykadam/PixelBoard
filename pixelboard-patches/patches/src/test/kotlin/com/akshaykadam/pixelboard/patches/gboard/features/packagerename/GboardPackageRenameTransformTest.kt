package com.akshaykadam.pixelboard.patches.gboard.features.packagerename

import com.akshaykadam.pixelboard.patches.gboard.shared.ANDROID_NS
import com.akshaykadam.pixelboard.patches.gboard.shared.applyGboardPatchesSettings
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element

class GboardPackageRenameTransformTest {
    @Test
    fun `application display name changes one attribute and preserves component labels`() {
        val manifest = loadManifest()
        val application = elements(manifest, "application").single()
        val componentLabelsBefore = componentLabels(manifest)
        val latinIme = elements(manifest, "service").single { element ->
            androidAttribute(element, "name") == "com.android.inputmethod.latin.LatinIME"
        }
        val launcher = elements(manifest, "activity").single { element ->
            androidAttribute(element, "name") ==
                "com.google.android.libraries.inputmethod.launcher.LauncherActivity"
        }

        assertEquals("@string/ime_name", androidAttribute(application, "label"))
        assertEquals(null, androidAttribute(latinIme, "label"))
        assertEquals(null, androidAttribute(launcher, "label"))

        applyGboardApplicationDisplayName(manifest, "Gboard Dev")

        assertEquals("Gboard Dev", androidAttribute(application, "label"))
        assertEquals(componentLabelsBefore, componentLabels(manifest))
    }

    @Test
    fun `application display name replaces label in namespace unaware cli documents`() {
        val manifest = parse(readFixtureBytes(MANIFEST_FIXTURE), namespaceAware = false)
        val application = elements(manifest, "application").single()

        applyGboardApplicationDisplayName(manifest, "Gboard Dev")

        assertEquals("Gboard Dev", androidAttribute(application, "label"))
        assertEquals(
            1,
            application.attributes.asList().count { attribute ->
                attribute.nodeName == "android:label" ||
                    (attribute.namespaceURI == ANDROID_NS && attribute.localName == "label")
            },
        )
        assertFalse(serialize(manifest).contains("android:label=\"@string/ime_name\""))
    }

    @Test
    fun `application display name accepts safe unicode and rejects unsafe values`() {
        listOf(
            "Gboard Dev",
            "PixelBoard Keyboard",
            "Keyboard 🚀",
            "x".repeat(MAX_GBOARD_APP_DISPLAY_NAME_CODE_POINTS),
        ).forEach { value ->
            assertTrue(value, isValidGboardAppDisplayName(value))
        }

        listOf(
            null,
            "",
            " ",
            " Gboard Dev",
            "Gboard Dev ",
            "@string/ime_name",
            "?attr/appName",
            "Gboard\nDev",
            "Gboard\u0000Dev",
            "Gboard\u2028Dev",
            "x".repeat(MAX_GBOARD_APP_DISPLAY_NAME_CODE_POINTS + 1),
        ).forEach { value ->
            assertFalse(value, isValidGboardAppDisplayName(value))
        }
    }

    @Test
    fun `application display name requires exactly one application element`() {
        val manifest = loadManifest()
        manifest.documentElement.appendChild(manifest.createElement("application"))
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardApplicationDisplayName(manifest, "Gboard Dev")
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `mapping inventory is the exact fifteen target attributes`() {
        val actual = GBOARD_PACKAGE_RENAME_MAPPINGS.map { mapping ->
            listOf(
                mapping.elementName,
                mapping.qualifiedAttributeName,
                mapping.originalValue,
                mapping.renamedValue,
            )
        }

        assertEquals(EXPECTED_MAPPINGS, actual)
        assertEquals(15, actual.size)
    }

    @Test
    fun `stock target transforms every selected attribute exactly once`() {
        val manifest = loadManifest()
        val originalComponents = componentNames(manifest)

        val result = applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertEquals(GboardPackageRenameResult.RENAMED, result)
        assertEquals(FINAL_PACKAGE, manifest.documentElement.getAttribute("package"))
        GBOARD_PACKAGE_RENAME_MAPPINGS.forEach { mapping ->
            assertEquals(0, mappingAttributes(manifest, mapping, mapping.originalValue).size)
            assertEquals(1, mappingAttributes(manifest, mapping, mapping.renamedValue).size)
        }
        assertFalse(serialize(manifest).contains(DOUBLE_PREFIX))
        assertEquals(originalComponents, componentNames(manifest))
        assertEquals("com.android.inputmethod.latin", originalPackageName(manifest))
    }

    @Test
    fun `standalone package rename removes required split markers but retains split inventory`() {
        val manifest = loadManifest()
        val root = manifest.documentElement
        val requiredSplitMetadata = elements(manifest, "meta-data").single { element ->
            androidAttribute(element, "name") == "com.android.vending.splits.required"
        }
        val splitInventoryMetadata = elements(manifest, "meta-data").single { element ->
            androidAttribute(element, "name") == "com.android.vending.splits"
        }

        assertTrue(root.hasAttributeNS(ANDROID_NS, "requiredSplitTypes"))
        assertTrue(root.hasAttributeNS(ANDROID_NS, "splitTypes"))
        assertEquals("true", androidAttribute(requiredSplitMetadata, "value"))
        assertEquals("@xml/splits0", androidAttribute(splitInventoryMetadata, "resource"))

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertFalse(root.hasAttributeNS(ANDROID_NS, "requiredSplitTypes"))
        assertFalse(root.hasAttributeNS(ANDROID_NS, "splitTypes"))
        assertTrue(elements(manifest, "meta-data").none { element ->
            androidAttribute(element, "name") == "com.android.vending.splits.required"
        })
        assertEquals(
            "@xml/splits0",
            elements(manifest, "meta-data").single { element ->
                androidAttribute(element, "name") == "com.android.vending.splits"
            }.let { element -> androidAttribute(element, "resource") },
        )
    }

    @Test
    fun `stock target component inventory is pinned and preserved`() {
        val manifest = loadManifest()
        val before = componentNames(manifest)

        assertEquals(30, before.getValue("activity").size)
        assertEquals(22, before.getValue("service").size)
        assertEquals(12, before.getValue("receiver").size)
        assertEquals(8, before.getValue("provider").size)
        assertEquals(0, manifest.getElementsByTagName("activity-alias").length)
        assertTrue(before.values.flatten().none { name -> name.startsWith(".") })
        assertTrue(before.values.flatten().none { name -> name.contains(SOURCE_PACKAGE) })

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertEquals(before, componentNames(manifest))
        assertEquals(0, manifest.getElementsByTagName("activity-alias").length)
    }

    @Test
    fun `declared permissions receiver permission and eight authorities stay paired and exact`() {
        val manifest = loadManifest()

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        val declaredPermissions = androidAttributeValues(manifest, "permission", "name")
        assertEquals(
            setOf(
                "$FINAL_PACKAGE.pixelbundle.RECEIVER",
                "$FINAL_PACKAGE.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            declaredPermissions.toSet(),
        )
        val receiverPermissions = androidAttributeValues(manifest, "receiver", "permission")
        val renamedReceiverPermission = "$FINAL_PACKAGE.pixelbundle.RECEIVER"
        assertEquals(1, receiverPermissions.count { it == renamedReceiverPermission })
        assertFalse(receiverPermissions.contains("$SOURCE_PACKAGE.pixelbundle.RECEIVER"))
        assertTrue(renamedReceiverPermission in declaredPermissions)
        assertEquals(
            setOf(
                FINAL_PACKAGE,
                "$FINAL_PACKAGE.clipboard_content",
                "$FINAL_PACKAGE.inputactionprovider",
                "$FINAL_PACKAGE.swissarmyknifefileprovider",
                "$FINAL_PACKAGE.fileprovider",
                "$FINAL_PACKAGE.tracing",
                "$FINAL_PACKAGE.wdb",
                "$FINAL_PACKAGE.mlkitinitprovider",
            ),
            androidAttributeValues(manifest, "provider", "authorities").toSet(),
        )
    }

    @Test
    fun `second application is an exact semantic no-op`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()
        applyGboardPackageRename(manifest, settings)
        val before = listOf(manifest, *settings.toTypedArray()).map(::serialize)

        val result = applyGboardPackageRename(manifest, settings)

        assertEquals(GboardPackageRenameResult.ALREADY_RENAMED, result)
        assertEquals(before, listOf(manifest, *settings.toTypedArray()).map(::serialize))
        assertFalse(before.any { xml -> xml.contains(DOUBLE_PREFIX) })
    }

    @Test
    fun `mismatched root fails before any manifest or settings mutation`() {
        val manifest = loadManifest().also {
            it.documentElement.setAttribute("package", "example.unexpected")
        }
        val settings = loadSettingsDocuments()
        val before = listOf(manifest, *settings.toTypedArray()).map(::serialize)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, settings)
        }

        assertEquals(before, listOf(manifest, *settings.toTypedArray()).map(::serialize))
    }

    @Test
    fun `transform requires both production settings documents`() {
        val manifest = loadManifest()
        val before = serialize(manifest)

        assertFails<IllegalArgumentException> {
            applyGboardPackageRename(manifest, emptyList())
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `mixed source and renamed mappings fail before mutation`() {
        val manifest = loadManifest()
        val mixedMapping = GBOARD_PACKAGE_RENAME_MAPPINGS.single {
            it.originalValue == "$SOURCE_PACKAGE.fileprovider"
        }
        mappingAttributes(manifest, mixedMapping, mixedMapping.originalValue)
            .single()
            .value = mixedMapping.renamedValue
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, loadSettingsDocuments())
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `double prefixed mapping fails before mutation`() {
        val manifest = loadManifest()
        val mapping = GBOARD_PACKAGE_RENAME_MAPPINGS.single {
            it.originalValue == "$SOURCE_PACKAGE.tracing"
        }
        mappingAttributes(manifest, mapping, mapping.originalValue)
            .single()
            .value = "$DOUBLE_PREFIX.tracing"
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, loadSettingsDocuments())
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `missing selected mapping fails before mutation`() {
        val manifest = loadManifest()
        val mapping = GBOARD_PACKAGE_RENAME_MAPPINGS.single {
            it.originalValue == "$SOURCE_PACKAGE.wdb"
        }
        val attribute = mappingAttributes(manifest, mapping, mapping.originalValue).single()
        attribute.ownerElement.removeAttributeNode(attribute)
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, loadSettingsDocuments())
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `duplicate selected mapping fails before mutation`() {
        val manifest = loadManifest()
        val mapping = GBOARD_PACKAGE_RENAME_MAPPINGS.single {
            it.originalValue == "$SOURCE_PACKAGE.mlkitinitprovider"
        }
        val provider = mappingAttributes(manifest, mapping, mapping.originalValue)
            .single()
            .ownerElement
        provider.parentNode.appendChild(provider.cloneNode(true))
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, loadSettingsDocuments())
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `already renamed root rejects partial official derived values`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()
        applyGboardPackageRename(manifest, settings)
        val mapping = GBOARD_PACKAGE_RENAME_MAPPINGS.single {
            it.renamedValue == "$FINAL_PACKAGE.clipboard_content"
        }
        mappingAttributes(manifest, mapping, mapping.renamedValue)
            .single()
            .value = mapping.originalValue
        val before = serialize(manifest)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, settings)
        }

        assertEquals(before, serialize(manifest))
    }

    @Test
    fun `android attributes work with namespace aware alternate prefix`() {
        val source = readFixtureText(MANIFEST_FIXTURE)
            .replace("xmlns:android=", "xmlns:a=")
            .replace("android:", "a:")
        val manifest = parse(source, namespaceAware = true)

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertEquals(FINAL_PACKAGE, manifest.documentElement.getAttribute("package"))
        GBOARD_PACKAGE_RENAME_MAPPINGS.forEach { mapping ->
            assertEquals(1, mappingAttributes(manifest, mapping, mapping.renamedValue).size)
        }
    }

    @Test
    fun `literal android prefixed attributes work without namespace awareness`() {
        val manifest = parse(readFixtureText(MANIFEST_FIXTURE), namespaceAware = false)

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertEquals(FINAL_PACKAGE, manifest.documentElement.getAttribute("package"))
        GBOARD_PACKAGE_RENAME_MAPPINGS.forEach { mapping ->
            assertEquals(1, mappingAttributes(manifest, mapping, mapping.renamedValue).size)
        }
    }

    @Test
    fun `resource package literals are intentionally retained`() {
        val retainedBefore = readFixtureBytes(RETAINED_LITERALS_FIXTURE)
        val manifest = loadManifest()

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        val retainedAfter = readFixtureBytes(RETAINED_LITERALS_FIXTURE)
        assertTrue(retainedBefore.contentEquals(retainedAfter))
        val retained = parse(retainedAfter, namespaceAware = true)
        val values = elements(retained, "literal").map { it.textContent.trim() }
        assertEquals(
            listOf(
                SOURCE_PACKAGE,
                "market://details?id=$SOURCE_PACKAGE",
                "http://play.google.com/store/apps/details?id=$SOURCE_PACKAGE",
                SOURCE_PACKAGE,
            ),
            values,
        )
    }

    @Test
    fun `package rename alone does not inject settings components or entries`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()
        val componentsBefore = componentNames(manifest)
        val settingsBefore = settings.map(::serialize)

        applyGboardPackageRename(manifest, settings)

        assertEquals(componentsBefore, componentNames(manifest))
        assertEquals(settingsBefore, settings.map(::serialize))
        assertTrue(elements(manifest, "provider").none { provider ->
            androidAttribute(provider, "name") == SETTINGS_PROVIDER_CLASS
        })
    }

    @Test
    fun `partial injected settings identity fails before mutation`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()
        applyGboardPatchesSettings(manifest, settings)
        val legacyIntent = elements(settings.last(), "intent").single { intent ->
            androidAttribute(intent, "targetClass") == SETTINGS_ACTIVITY_CLASS
        }
        legacyIntent.parentNode.removeChild(legacyIntent)
        val before = listOf(manifest, *settings.toTypedArray()).map(::serialize)

        assertFails<IllegalStateException> {
            applyGboardPackageRename(manifest, settings)
        }

        assertEquals(before, listOf(manifest, *settings.toTypedArray()).map(::serialize))
    }

    @Test
    fun `package rename then settings finalize uses only final identity`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()

        applyGboardPackageRename(manifest, settings)
        applyGboardPatchesSettings(manifest, settings)
        val beforeSecondRename = listOf(manifest, *settings.toTypedArray()).map(::serialize)
        val secondResult = applyGboardPackageRename(manifest, settings)

        assertEquals(GboardPackageRenameResult.ALREADY_RENAMED, secondResult)
        assertEquals(
            beforeSecondRename,
            listOf(manifest, *settings.toTypedArray()).map(::serialize),
        )
        assertFinalSettingsIdentity(manifest, settings)
    }

    @Test
    fun `settings finalize then package rename uses only final identity`() {
        val manifest = loadManifest()
        val settings = loadSettingsDocuments()

        applyGboardPatchesSettings(manifest, settings)
        applyGboardPackageRename(manifest, settings)

        assertFinalSettingsIdentity(manifest, settings)
    }

    @Test
    fun `package rename never rewrites extension component class names`() {
        val manifest = loadManifest()
        val application = elements(manifest, "application").single()
        val service = manifest.createElement("service")
        service.setAttributeNS(
            ANDROID_NS,
            "android:name",
            "com.akshaykadam.pixelboard.extension.webclipboard.ClipboardSyncService",
        )
        application.appendChild(service)

        applyGboardPackageRename(manifest, loadSettingsDocuments())

        assertEquals(
            "com.akshaykadam.pixelboard.extension.webclipboard.ClipboardSyncService",
            androidAttribute(service, "name"),
        )
    }

    private fun assertFinalSettingsIdentity(
        manifest: Document,
        settings: List<Document>,
    ) {
        assertEquals(FINAL_PACKAGE, manifest.documentElement.getAttribute("package"))
        assertFalse(serialize(manifest).contains(DOUBLE_PREFIX))

        val settingsProviders = elements(manifest, "provider").filter { provider ->
            androidAttribute(provider, "name") == SETTINGS_PROVIDER_CLASS
        }
        assertEquals(1, settingsProviders.size)
        assertEquals(
            "$FINAL_PACKAGE.gboard_patches",
            androidAttribute(settingsProviders.single(), "authorities"),
        )

        settings.forEach { document ->
            val matchingIntents = elements(document, "intent").filter { intent ->
                androidAttribute(intent, "targetClass") == SETTINGS_ACTIVITY_CLASS
            }
            assertEquals(1, matchingIntents.size)
            assertEquals(FINAL_PACKAGE, androidAttribute(matchingIntents.single(), "targetPackage"))
            val xml = serialize(document)
            assertFalse(xml.contains("targetPackage=\"$SOURCE_PACKAGE\""))
            assertFalse(xml.contains(DOUBLE_PREFIX))
        }

        val components = componentNames(manifest)
        assertEquals(31, components.getValue("activity").size)
        assertEquals(22, components.getValue("service").size)
        assertEquals(12, components.getValue("receiver").size)
        assertEquals(9, components.getValue("provider").size)
        assertTrue(components.values.flatten().contains(SETTINGS_ACTIVITY_CLASS))
        assertTrue(components.values.flatten().contains(SETTINGS_PROVIDER_CLASS))
    }

    private fun loadManifest(): Document = parse(
        readFixtureBytes(MANIFEST_FIXTURE),
        namespaceAware = true,
    )

    private fun loadSettingsDocuments(): List<Document> = listOf(
        parse(readFixtureBytes(SETTINGS_FIXTURE), namespaceAware = true),
        parse(readFixtureBytes(SETTINGS_LEGACY_FIXTURE), namespaceAware = true),
    )

    private fun readFixtureText(path: String): String =
        String(readFixtureBytes(path), StandardCharsets.UTF_8)

    private fun readFixtureBytes(path: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream(path)) {
            "Missing fixture $path"
        }.use { it.readBytes() }

    private fun parse(source: String, namespaceAware: Boolean): Document =
        parse(source.toByteArray(StandardCharsets.UTF_8), namespaceAware)

    private fun parse(source: ByteArray, namespaceAware: Boolean): Document =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = namespaceAware
        }.newDocumentBuilder().parse(ByteArrayInputStream(source))

    private fun serialize(document: Document): String {
        val writer = StringWriter()
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty(OutputKeys.INDENT, "no")
        }.transform(DOMSource(document), StreamResult(writer))
        return writer.toString()
    }

    private fun componentNames(document: Document): Map<String, List<String>> =
        COMPONENT_TAGS.associateWith { tag ->
            elements(document, tag).map { element ->
                checkNotNull(androidAttribute(element, "name")) {
                    "Missing android:name on $tag"
                }
            }
        }

    private fun componentLabels(document: Document): Map<Pair<String, String>, String?> =
        COMPONENT_TAGS.flatMap { tag ->
            elements(document, tag).map { element ->
                (tag to checkNotNull(androidAttribute(element, "name"))) to
                    androidAttribute(element, "label")
            }
        }.toMap()

    private fun originalPackageName(document: Document): String =
        checkNotNull(
            elements(document, "original-package").single().getAttribute("android:name")
                .takeIf { it.isNotBlank() }
                ?: elements(document, "original-package").single()
                    .getAttributeNS(ANDROID_NS, "name")
                    .takeIf { it.isNotBlank() },
        )

    private fun androidAttributeValues(
        document: Document,
        elementName: String,
        attributeName: String,
    ): List<String> = elements(document, elementName).mapNotNull { element ->
        androidAttribute(element, attributeName)
    }

    private fun androidAttribute(element: Element, localName: String): String? =
        element.getAttributeNS(ANDROID_NS, localName).takeIf { it.isNotBlank() }
            ?: element.attributes.asList().firstOrNull { attribute ->
                attribute.localName == localName &&
                    attribute.namespaceURI == ANDROID_NS
            }?.nodeValue
            ?: element.getAttribute("android:$localName").takeIf { it.isNotBlank() }

    private fun mappingAttributes(
        document: Document,
        mapping: GboardPackageRenameMapping,
        value: String,
    ): List<Attr> = elements(document, mapping.elementName).mapNotNull { element ->
        element.attributes.asList().firstOrNull { attribute ->
            val nameMatches = if (mapping.namespaceUri == null) {
                attribute.nodeName == mapping.attributeName
            } else {
                (
                    attribute.namespaceURI == mapping.namespaceUri &&
                        attribute.localName == mapping.attributeName
                    ) ||
                    attribute.nodeName == "android:" + mapping.attributeName
            }
            nameMatches && attribute.nodeValue == value
        } as? Attr
    }

    private fun elements(document: Document, tagName: String): List<Element> =
        document.getElementsByTagName("*").asList().filterIsInstance<Element>().filter {
            (it.localName ?: it.tagName.substringAfter(':')) == tagName
        }

    private fun org.w3c.dom.NamedNodeMap.asList(): List<org.w3c.dom.Node> =
        (0 until length).map(::item)

    private fun org.w3c.dom.NodeList.asList(): List<org.w3c.dom.Node> =
        (0 until length).map(::item)

    private inline fun <reified T : Throwable> assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected " + T::class.java.simpleName)
        } catch (throwable: Throwable) {
            if (throwable !is T) {
                throw throwable
            }
        }
    }

    private companion object {
        const val SOURCE_PACKAGE = "com.google.android.inputmethod.latin"
        const val FINAL_PACKAGE = "com.akshaykadam.pixelboard"
        const val DOUBLE_PREFIX = "com.akshaykadam.com.akshaykadam.pixelboard"
        const val SETTINGS_ACTIVITY_CLASS =
            "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity"
        const val SETTINGS_PROVIDER_CLASS =
            "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsProvider"
        const val MANIFEST_FIXTURE =
            "/gboard/18.0.3/package-rename/AndroidManifest.normalized.xml"
        const val SETTINGS_FIXTURE = "/gboard/18.0.3/package-rename/settings.xml"
        const val SETTINGS_LEGACY_FIXTURE =
            "/gboard/18.0.3/package-rename/settings_legacy.xml"
        const val RETAINED_LITERALS_FIXTURE =
            "/gboard/18.0.3/package-rename/retained-resource-literals.xml"
        val COMPONENT_TAGS = listOf("activity", "service", "receiver", "provider")
        val EXPECTED_MAPPINGS = listOf(
            listOf("manifest", "package", SOURCE_PACKAGE, FINAL_PACKAGE),
            listOf(
                "permission",
                "android:name",
                "$SOURCE_PACKAGE.pixelbundle.RECEIVER",
                "$FINAL_PACKAGE.pixelbundle.RECEIVER",
            ),
            listOf(
                "permission",
                "android:name",
                "$SOURCE_PACKAGE.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                "$FINAL_PACKAGE.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            listOf(
                "meta-data",
                "android:name",
                "com.google.android.gms.phenotype.registration.binarypb:$SOURCE_PACKAGE",
                "com.google.android.gms.phenotype.registration.binarypb:$FINAL_PACKAGE",
            ),
            listOf(
                "meta-data",
                "android:name",
                "com.google.android.gms.phenotype.registration.xml:$SOURCE_PACKAGE",
                "com.google.android.gms.phenotype.registration.xml:$FINAL_PACKAGE",
            ),
            listOf("provider", "android:authorities", SOURCE_PACKAGE, FINAL_PACKAGE),
            listOf(
                "data",
                "android:host",
                "deeplink.$SOURCE_PACKAGE",
                "deeplink.$FINAL_PACKAGE",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.clipboard_content",
                "$FINAL_PACKAGE.clipboard_content",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.inputactionprovider",
                "$FINAL_PACKAGE.inputactionprovider",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.swissarmyknifefileprovider",
                "$FINAL_PACKAGE.swissarmyknifefileprovider",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.fileprovider",
                "$FINAL_PACKAGE.fileprovider",
            ),
            listOf(
                "receiver",
                "android:permission",
                "$SOURCE_PACKAGE.pixelbundle.RECEIVER",
                "$FINAL_PACKAGE.pixelbundle.RECEIVER",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.tracing",
                "$FINAL_PACKAGE.tracing",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.wdb",
                "$FINAL_PACKAGE.wdb",
            ),
            listOf(
                "provider",
                "android:authorities",
                "$SOURCE_PACKAGE.mlkitinitprovider",
                "$FINAL_PACKAGE.mlkitinitprovider",
            ),
        )
    }
}
