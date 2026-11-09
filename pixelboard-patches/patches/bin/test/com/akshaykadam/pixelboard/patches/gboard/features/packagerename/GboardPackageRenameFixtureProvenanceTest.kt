package com.akshaykadam.pixelboard.patches.gboard.features.packagerename

import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class GboardPackageRenameFixtureProvenanceTest {
    @Test
    fun `fixture pins exact apk decoded manifest and normalization hashes`() {
        val provenance = JsonParser.parseString(readText(PROVENANCE_FIXTURE)).asJsonObject

        assertEquals(SOURCE_PACKAGE, provenance.get("targetPackage").asString)
        assertEquals(TARGET_VERSION, provenance.get("targetVersion").asString)
        assertEquals(TARGET_APK_SHA256, provenance.get("targetApkSha256").asString)
        assertEquals(DECODED_MANIFEST_SHA256, provenance.get("decodedManifestSha256").asString)
        assertEquals(MANIFEST_FIXTURE_SHA256, provenance.get("manifestFixtureSha256").asString)
        assertEquals(15, provenance.get("selectedManifestAttributeCount").asInt)
        assertEquals(
            "UTF-8 without BOM; CRLF normalized to LF; XML content otherwise verbatim",
            provenance.get("manifestFixtureNormalization").asString,
        )
        assertEquals(
            MANIFEST_FIXTURE_SHA256,
            sha256(normalizeLineEndings(readBytes(MANIFEST_FIXTURE))),
        )
    }

    @Test
    fun `normalized target manifest independently contains exactly fifteen stock package attributes`() {
        val manifestBytes = readBytes(MANIFEST_FIXTURE)
        assertFalse(
            manifestBytes.size >= 3 &&
                manifestBytes[0] == 0xEF.toByte() &&
                manifestBytes[1] == 0xBB.toByte() &&
                manifestBytes[2] == 0xBF.toByte(),
        )
        val manifest = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(ByteArrayInputStream(manifestBytes))
        val selected = manifest.getElementsByTagName("*").asList()
            .filterIsInstance<Element>()
            .flatMap { element -> element.attributes.asList().map { element to it } }
            .filter { (_, attribute) -> attribute.nodeValue.contains(SOURCE_PACKAGE) }

        assertEquals(15, selected.size)
        assertEquals(SOURCE_PACKAGE, manifest.documentElement.getAttribute("package"))
        assertTrue(selected.any { (element, attribute) ->
            element.tagName == "manifest" &&
                attribute.nodeName == "package" &&
                attribute.nodeValue == SOURCE_PACKAGE
        })
    }

    @Test
    fun `retained literal evidence pins all three decoded resource sources`() {
        val provenance = JsonParser.parseString(readText(PROVENANCE_FIXTURE)).asJsonObject
        val resources = provenance.getAsJsonArray("resourceSources").map { it.asJsonObject }
            .associate { resource ->
                resource.get("path").asString to resource.get("sha256").asString
            }

        assertEquals(
            mapOf(
                "res/values/arrays.xml" to
                    "29564FEA71EF0956B2C0AE78BBC35170BA3768F0E7BF30F1738A50520AC40322",
                "res/values/strings.xml" to
                    "932259E4C1B977407919085735065A48AE336C1E4D01D35F0AAF41C29BE3E22B",
                "res/xml/APKTOOL_RENAMED_0x7f170033.xml" to
                    "6637D8037933B4BBDA35719F72DAF1181F0B5BB5BCD5F2AAD33B670CF3FA0CB0",
            ),
            resources,
        )
        val literals = readText(RETAINED_LITERALS_FIXTURE)
        assertEquals(4, Regex(Regex.escape(SOURCE_PACKAGE)).findAll(literals).count())
        assertTrue(literals.contains("market://details?id=$SOURCE_PACKAGE"))
        assertTrue(literals.contains("http://play.google.com/store/apps/details?id=$SOURCE_PACKAGE"))
        assertTrue(literals.contains("phenotype-configuration-package"))
    }

    @Test
    fun `settings fixtures pin both official 1803 settings resources`() {
        val provenance = JsonParser.parseString(readText(PROVENANCE_FIXTURE)).asJsonObject
        val resources = provenance.getAsJsonArray("settingsSources").map { it.asJsonObject }
            .associate { resource ->
                resource.get("path").asString to resource.get("sha256").asString
            }

        assertEquals(
            mapOf(
                "res/xml/settings.xml" to
                    "4441821A8091B0F14B98E5B1D60FAE3D7A4464F7394F90D3D03BE85C639D8B2E",
                "res/xml/settings_legacy.xml" to
                    "D556A6D2469893FBAB00EB74BDD722921CF4FAC162017A1AC93672CD039015A4",
            ),
            resources,
        )
        assertEquals(
            "38335296A8D56AE8DAD93E0728A33C31666A71476199333FD47914ACDF73467A",
            sha256(normalizeLineEndings(readBytes(SETTINGS_FIXTURE))),
        )
        assertEquals(
            "C4917C166457BE1D8628E8CE6C23FBE13B77507483D9E7BEB726FA6256588929",
            sha256(normalizeLineEndings(readBytes(SETTINGS_LEGACY_FIXTURE))),
        )
    }

    @Test
    fun `all six split manifests stay on the official package and require standalone renamed output`() {
        val inventory = JsonParser.parseString(readText(SPLIT_MANIFEST_INVENTORY)).asJsonObject
        val apks = inventory.getAsJsonArray("apks").map { it.asJsonObject }

        assertEquals(TARGET_VERSION, inventory.get("targetVersion").asString)
        assertEquals(175940518, inventory.get("versionCode").asInt)
        assertEquals(SOURCE_PACKAGE, inventory.get("sourcePackage").asString)
        assertEquals(7, apks.size)
        assertEquals(1, apks.count { it.get("split").isJsonNull })
        assertEquals(6, apks.count { !it.get("split").isJsonNull })
        assertEquals(3, apks.count { it.get("isFeatureSplit").asBoolean })
        assertTrue(apks.all { it.get("package").asString == SOURCE_PACKAGE })
        assertEquals(
            EXPECTED_APK_AND_MANIFEST_HASHES,
            apks.associate { apk ->
                apk.get("name").asString to listOf(
                    apk.get("apkSha256").asString,
                    apk.get("binaryManifestSha256").asString,
                )
            },
        )
        val policy = inventory.get("packageRenameArtifactPolicy").asString
        assertTrue(policy.contains("only the decoded base AndroidManifest.xml"))
        assertTrue(policy.contains("standalone APK"))
        assertTrue(policy.contains("must not be installed with the original-package split APKs"))
    }

    private fun readText(path: String): String =
        String(readBytes(path), StandardCharsets.UTF_8)

    private fun readBytes(path: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream(path)) {
            "Missing fixture $path"
        }.use { it.readBytes() }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02X".format(byte) }

    private fun normalizeLineEndings(bytes: ByteArray): ByteArray =
        String(bytes, StandardCharsets.UTF_8)
            .replace("\r\n", "\n")
            .toByteArray(StandardCharsets.UTF_8)

    private fun org.w3c.dom.NamedNodeMap.asList(): List<org.w3c.dom.Node> =
        (0 until length).map(::item)

    private fun org.w3c.dom.NodeList.asList(): List<org.w3c.dom.Node> =
        (0 until length).map(::item)

    private companion object {
        const val SOURCE_PACKAGE = "com.google.android.inputmethod.latin"
        const val TARGET_VERSION = "18.0.3.954559732-release-arm64-v8a"
        const val TARGET_APK_SHA256 =
            "66F93FDD6CA2E53675C725AF38E7F47C9474CBB45E80AFCEFE9782817CEC7179"
        const val DECODED_MANIFEST_SHA256 =
            "A8D58F88C6DC39E6280A5311D196ACD0F5DC5D9B536645F3AE20511F49D3438E"
        const val MANIFEST_FIXTURE_SHA256 =
            "7AD94356577526170AB5F53A335FBC8BA1E55E5FC40DD6B5DD620645FD6A6B74"
        const val PROVENANCE_FIXTURE =
            "/gboard/18.0.3/package-rename/provenance.json"
        const val MANIFEST_FIXTURE =
            "/gboard/18.0.3/package-rename/AndroidManifest.normalized.xml"
        const val RETAINED_LITERALS_FIXTURE =
            "/gboard/18.0.3/package-rename/retained-resource-literals.xml"
        const val SETTINGS_FIXTURE =
            "/gboard/18.0.3/package-rename/settings.xml"
        const val SETTINGS_LEGACY_FIXTURE =
            "/gboard/18.0.3/package-rename/settings_legacy.xml"
        const val SPLIT_MANIFEST_INVENTORY =
            "/gboard/18.0.3/package-rename/split-manifest-inventory.json"
        val EXPECTED_APK_AND_MANIFEST_HASHES = mapOf(
            "base.apk" to listOf(
                "66F93FDD6CA2E53675C725AF38E7F47C9474CBB45E80AFCEFE9782817CEC7179",
                "67D154F16CDE5FA6DE34D3AA0614ED98F10A39680373D952F92BC41B2848E216",
            ),
            "split_brella_feature_split.apk" to listOf(
                "EC0EF128B009834DF9FE570AF47E4A15EFCA48080BFCE2A69CC5A7E11397128B",
                "D4032329CBFDE5A32B88AB966E47FB269C2FD2CE99769601EDB321976E3C388A",
            ),
            "split_config.mdpi.apk" to listOf(
                "82142C101A29E28E35AE40C672964EC7E967677A0A547A16F5EDC31A69F78B10",
                "2C08522FAA37789995780262700E68A1D30E81E14BBBCD49BED5B7C72D909BB5",
            ),
            "split_config.tvdpi.apk" to listOf(
                "00250F9C17BC672AB51DD4530A2D0CC54CAEB897A2BCC9A2141705D651E3C21E",
                "930F55BFD0CF188135E4FD8D638B760B51F9137F2483702BA9ADB0816DA65425",
            ),
            "split_config.xxhdpi.apk" to listOf(
                "03C26F98EDED9B7CBCE4AAF57440FEF67675BC7724061847153EBBEB280DF3BC",
                "BA6FDFC8B7AB8A2A5FA0E6ABE36F309E96257B3F1894E4E141A84B8B7FDBB156",
            ),
            "split_dictation_feature_split.apk" to listOf(
                "86697CC0575EFB7355FF63B2E0141474D0400AF6CD4518530A20EC8AAFD51F80",
                "5A3D27F7BB5548B8D2D010F65D77CB0C458D60F4A3208ED3BF18B65576A94801",
            ),
            "split_tenoranimation_feature_split.apk" to listOf(
                "F56FBE3C7E5E81D319F7961A61AEC7F7DC4D78B9C8C738E9AD6C9DC47B634415",
                "8B82F7ACCAE457295E2F58C700D5341F3F1F478A170803FC6B00892D4C643B96",
            ),
        )
    }
}
