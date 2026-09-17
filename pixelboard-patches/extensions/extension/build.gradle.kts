import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

private data class LocalizedSettingsText(
    val type: String,
    val name: String,
    val formatted: Boolean,
    val translations: Map<String, String>,
    val quantities: List<Pair<String, Map<String, String>>>,
)

private fun Element.childElements(name: String? = null): List<Element> =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filter { node ->
            node.nodeType == Node.ELEMENT_NODE && (name == null || node.nodeName == name)
        }
        .map { it as Element }

private fun Element.requiredTranslations(
    entryName: String,
    requiredLocales: Set<String>,
): Map<String, String> {
    val translations = childElements("translation").associate { translation ->
        translation.getAttribute("locale") to translation.textContent
    }
    require(translations.keys == requiredLocales) {
        "$entryName must define exactly ${requiredLocales.joinToString()}"
    }
    return translations
}

private fun escapeXmlText(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun decodeAndroidEscapes(value: String): String = value
    .replace("\\n", "\n")
    .replace("\\'", "'")
    .replace("\\?", "?")

private fun escapeJavaString(value: String): String = decodeAndroidEscapes(value)
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\r", "\\r")
    .replace("\n", "\\n")

extension {
    name = "extensions/pixelboard-patches.rve"
}

val settingsTextAuthority = layout.projectDirectory.file(
    "src/main/settings-text/gboard_settings_text.xml"
)
val generatedSettingsTextResDir = layout.buildDirectory.dir(
    "generated/settingsText/res"
)
val generatedSettingsTextJavaDir = layout.buildDirectory.dir(
    "generated/source/settingsText/java"
)

android {
    namespace = "com.akshaykadam.pixelboard.extension"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "PATCH_AUTHOR", "\"Akshay Kadam\"")
        buildConfigField("String", "PATCH_AUTHOR_URL", "\"https://github.com/Akshayykadam\"")
        buildConfigField(
            "String",
            "PATCH_REPOSITORY_URL",
            "\"https://github.com/Akshayykadam/PixelBoard\""
        )
        buildConfigField("String", "PATCH_VERSION", "\"${project.version}\"")
    }

    sourceSets.named("main") {
        java.directories.add(generatedSettingsTextJavaDir.get().asFile.absolutePath)
        res.directories.add(generatedSettingsTextResDir.get().asFile.absolutePath)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

val generateSettingsText = tasks.register("generateSettingsText") {
    inputs.file(settingsTextAuthority)
    outputs.dir(generatedSettingsTextResDir)
    outputs.dir(generatedSettingsTextJavaDir)

    doLast {
        val requiredLocales = linkedSetOf("en")
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
        }
        val document = documentBuilderFactory.newDocumentBuilder()
            .parse(settingsTextAuthority.asFile)
        val root = document.documentElement
        require(root.nodeName == "settings-text") {
            "Settings text authority must use a <settings-text> root"
        }
        require(root.getAttribute("locales").split(Regex("\\s+")).toSet() == requiredLocales) {
            "Settings text authority must declare exactly ${requiredLocales.joinToString()}"
        }

        val seenNames = linkedSetOf<String>()
        val entries = root.childElements().map { entry ->
            val type = entry.nodeName
            require(type == "string" || type == "plurals") {
                "Unsupported settings text type: $type"
            }
            val name = entry.getAttribute("name")
            require(name.isNotBlank() && seenNames.add(name)) {
                "Settings text names must be non-blank and unique: $name"
            }
            if (type == "string") {
                LocalizedSettingsText(
                    type = type,
                    name = name,
                    formatted = entry.getAttribute("formatted") != "false",
                    translations = entry.requiredTranslations(name, requiredLocales),
                    quantities = emptyList(),
                )
            } else {
                val quantities = entry.childElements("item").map { item ->
                    val quantity = item.getAttribute("quantity")
                    require(quantity.isNotBlank()) { "$name has a plural item without quantity" }
                    quantity to item.requiredTranslations("$name[$quantity]", requiredLocales)
                }
                require(quantities.isNotEmpty() && quantities.map { it.first }.distinct().size == quantities.size) {
                    "$name must define unique plural quantities"
                }
                LocalizedSettingsText(
                    type = type,
                    name = name,
                    formatted = true,
                    translations = emptyMap(),
                    quantities = quantities,
                )
            }
        }
        require(entries.isNotEmpty()) { "Settings text authority must not be empty" }

        val resRoot = generatedSettingsTextResDir.get().asFile
        mapOf("en" to "values").forEach { (locale, qualifier) ->
            val output = resRoot.resolve("$qualifier/gboard_settings_strings.xml")
            output.parentFile.mkdirs()
            output.writeText(buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<resources>")
                entries.forEach { entry ->
                    if (entry.type == "string") {
                        append("    <string name=\"")
                        append(entry.name)
                        if (!entry.formatted) {
                            append("\" formatted=\"false")
                        }
                        append("\">")
                        append(escapeXmlText(entry.translations.getValue(locale)))
                        appendLine("</string>")
                    } else {
                        append("    <plurals name=\"")
                        append(entry.name)
                        appendLine("\">")
                        entry.quantities.forEach { (quantity, translations) ->
                            append("        <item quantity=\"")
                            append(quantity)
                            append("\">")
                            append(escapeXmlText(translations.getValue(locale)))
                            appendLine("</item>")
                        }
                        appendLine("    </plurals>")
                    }
                }
                appendLine("</resources>")
            })
        }

        val stringEntries = entries.filter { it.type == "string" }
        val pluralEntries = entries.filter { it.type == "plurals" }
        val javaOutput = generatedSettingsTextJavaDir.get().asFile.resolve(
            "com/akshaykadam/pixelboard/extension/settings/GboardSettingsTextCatalog.java"
        )
        javaOutput.parentFile.mkdirs()
        javaOutput.writeText(buildString {
            appendLine("package com.akshaykadam.pixelboard.extension.settings;")
            appendLine()
            appendLine("import java.util.HashMap;")
            appendLine("import java.util.Map;")
            appendLine()
            appendLine("import com.akshaykadam.pixelboard.extension.R;")
            appendLine()
            appendLine("final class GboardSettingsTextCatalog {")
            appendLine("    private static final Map<Integer, String> ENGLISH = createEnglish();")
            appendLine()
            appendLine("    private GboardSettingsTextCatalog() {")
            appendLine("    }")
            appendLine()
            appendLine("    static String template(int resId, String languageTag) {")
            appendLine("        return ENGLISH.get(resId);")
            appendLine("    }")
            appendLine()
            appendLine("    static String quantityTemplate(int resId, String languageTag, int quantity) {")
            pluralEntries.forEach { entry ->
                val one = entry.quantities.firstOrNull { it.first == "one" }?.second
                    ?: entry.quantities.first().second
                val other = entry.quantities.firstOrNull { it.first == "other" }?.second
                    ?: entry.quantities.last().second
                append("        if (resId == R.plurals.")
                append(entry.name)
                appendLine(") {")
                append("            return quantity == 1 ? \"")
                append(escapeJavaString(one.getValue("en")))
                append("\" : \"")
                append(escapeJavaString(other.getValue("en")))
                appendLine("\";")
                appendLine("        }")
            }
            appendLine("        return null;")
            appendLine("    }")
            appendLine()
            appendLine("    private static Map<Integer, String> createEnglish() {")
            appendLine("        Map<Integer, String> templates = new HashMap<Integer, String>();")
            stringEntries.forEach { entry ->
                append("        templates.put(R.string.")
                append(entry.name)
                append(", \"")
                append(escapeJavaString(entry.translations.getValue("en")))
                appendLine("\");")
            }
            appendLine("        return templates;")
            appendLine("    }")
            appendLine("}")
        })
    }
}

tasks.named("preBuild") {
    dependsOn(generateSettingsText)
}

tasks.named("syncExtension") {
    doLast {
        val extensionDex = layout.buildDirectory.asFileTree.matching {
            include("**/extensions/pixelboard-patches.rve")
        }.singleOrNull() ?: throw GradleException("PixelBoard extension dex was not generated")
        val dexText = String(extensionDex.readBytes(), StandardCharsets.ISO_8859_1)
        check(!dexText.contains("Lcom/android/tools/r8/RecordTag;")) {
            "PixelBoard extension dex contains record classes without the required D8 global synthetic"
        }
    }
}
