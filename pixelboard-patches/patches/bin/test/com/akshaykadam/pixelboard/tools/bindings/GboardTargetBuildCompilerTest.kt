package com.akshaykadam.pixelboard.tools.bindings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardTargetBuildCompilerTest {
    @Test
    fun `pending developer-only bundle admits zero-binding build`() {
        val catalog = catalog(
            bindingContracts = mapOf(
                "flag_bool_getter" to "method",
                "flag_name_field" to "field",
            ),
            features = listOf(
                feature(
                    id = "developer_options",
                    patchName = "Developer options",
                    contributions = listOf(contribution("developer_options.resources")),
                ),
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution(
                            "advanced_voice_typing.flag",
                            listOf("flag_bool_getter", "flag_name_field"),
                        ),
                    ),
                ),
            ),
        )
        val bundle = bundle(
            catalog,
            status = "pending",
            admittedByFeature = mapOf(
                "developer_options" to setOf("developer_options.resources"),
            ),
        )
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"a".repeat(64)}","bindings":{}}"""
            , bundle)

        val compiled = GboardTargetBuildCompiler.compile(catalog, bundle, bindings)

        assertEquals(emptySet<String>(), compiled.requiredBindingKeys)
        assertEquals(setOf("developer_options"), compiled.admittedFeatureIds)
        assertTrue(compiled.admissionKotlinSource.contains("\"developer_options\""))
        assertTrue(compiled.bindingsKotlinSource.contains("Binding flag_bool_getter is unavailable"))
        assertTrue(compiled.bindingsKotlinSource.contains("Binding flag_name_field is unavailable"))
    }

    @Test
    fun `advanced voice flag-only requires only shared flag bindings`() {
        val catalog = catalog(
            bindingContracts = mapOf(
                "advanced_voice_asr_session_reset" to "method",
                "flag_bool_getter" to "method",
                "flag_name_field" to "field",
            ),
            features = listOf(
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution(
                            "advanced_voice_typing.flag",
                            listOf("flag_bool_getter", "flag_name_field"),
                        ),
                        contribution(
                            "advanced_voice_typing.bytecode",
                            listOf("advanced_voice_asr_session_reset"),
                        ),
                    ),
                ),
            ),
        )
        val bundle = bundle(
            catalog,
            status = "ready",
            admittedByFeature = mapOf(
                "advanced_voice_typing" to setOf("advanced_voice_typing.flag"),
            ),
        )
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"b".repeat(64)}","bindings":{"flag_bool_getter":{"kind":"method","class_type":"Lflag/Owner;","method_name":"read","parameter_types":[],"return_type":"Z","confidence":1},"flag_name_field":{"kind":"field","class_type":"Lflag/Owner;","field_name":"name","field_type":"Ljava/lang/String;","confidence":1}}}"""
            , bundle)

        val compiled = GboardTargetBuildCompiler.compile(catalog, bundle, bindings)

        assertEquals(
            setOf("flag_bool_getter", "flag_name_field"),
            compiled.requiredBindingKeys,
        )
        assertTrue(
            compiled.bindingsKotlinSource.contains(
                "Binding advanced_voice_asr_session_reset is unavailable",
            ),
        )
    }

    @Test
    fun `canonical admitted contribution accepts catalog set ordering differences`() {
        val catalog = catalog(
            bindingContracts = mapOf(
                "alpha_binding" to "method",
                "zeta_binding" to "method",
            ),
            features = listOf(
                feature(
                    id = "ordered_feature",
                    patchName = "Ordered feature",
                    contributions = listOf(
                        contribution(
                            "ordered_feature.bytecode",
                            listOf("zeta_binding", "alpha_binding"),
                        ),
                    ),
                ),
            ),
        )
        val rawBundle = JsonParser.parseString(
            bundle(
                catalog,
                status = "ready",
                admittedByFeature = mapOf(
                    "ordered_feature" to setOf("ordered_feature.bytecode"),
                ),
            ),
        ).asJsonObject
        rawBundle.getAsJsonArray("features")[0].asJsonObject
            .getAsJsonArray("admitted_contributions")[0].asJsonObject
            .add("required_bindings", Gson().toJsonTree(listOf("alpha_binding", "zeta_binding")))
        val canonicalBundle = withRecomputedBundleSha(rawBundle)
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"e".repeat(64)}","bindings":{"alpha_binding":{"kind":"method","class_type":"Ltest/Owner;","method_name":"alpha","parameter_types":[],"return_type":"V","confidence":1},"zeta_binding":{"kind":"method","class_type":"Ltest/Owner;","method_name":"zeta","parameter_types":[],"return_type":"V","confidence":1}}}""",
            canonicalBundle,
        )

        val compiled = GboardTargetBuildCompiler.compile(catalog, canonicalBundle, bindings)

        assertEquals(setOf("alpha_binding", "zeta_binding"), compiled.requiredBindingKeys)
    }

    @Test
    fun `bundled mode admits the complete catalog without external linkage`() {
        val catalog = catalog(
            bindingContracts = mapOf("sample_binding" to "method"),
            features = listOf(
                feature(
                    id = "sample_feature",
                    patchName = "Sample feature",
                    contributions = listOf(
                        contribution("sample_feature.bytecode", listOf("sample_binding")),
                    ),
                ),
            ),
        )
        val bindings =
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"1.0.0","bindings":{"sample_binding":{"kind":"method","class_type":"Lsample/Owner;","method_name":"run","parameter_types":[],"return_type":"V","confidence":1}}}"""

        val compiled = GboardTargetBuildCompiler.compile(catalog, null, bindings)

        assertEquals(setOf("sample_feature"), compiled.admittedFeatureIds)
        assertEquals(setOf("sample_binding"), compiled.requiredBindingKeys)
        assertTrue(compiled.bindingsKotlinSource.contains("Lsample/Owner;"))
        assertEquals(null, compiled.bundleSha256)
    }

    @Test
    fun `bundle digest interoperates with a compact external export`() {
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"c".repeat(64)}","bindings":{}}""",
            EXTERNAL_EXPORT_PORT_BUNDLE,
        )

        val compiled = GboardTargetBuildCompiler.compile(
            EXTERNAL_EXPORT_CATALOG,
            EXTERNAL_EXPORT_PORT_BUNDLE,
            bindings,
        )

        assertTrue(EXTERNAL_EXPORT_PORT_BUNDLE.contains("Emojis \\u0026 Symbols \\u003c test"))
        assertEquals(EXTERNAL_EXPORT_BUNDLE_SHA256, compiled.bundleSha256)
        assertEquals(setOf("developer_options"), compiled.admittedFeatureIds)
    }

    @Test
    fun `invalid digest catalog and target identity fail closed at the compiler seam`() {
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"d".repeat(64)}","bindings":{}}""",
            EXTERNAL_EXPORT_PORT_BUNDLE,
        )

        assertContractError("PORT_BUNDLE_DIGEST_MISMATCH") {
            GboardTargetBuildCompiler.compile(
                EXTERNAL_EXPORT_CATALOG,
                EXTERNAL_EXPORT_PORT_BUNDLE.replace(EXTERNAL_EXPORT_BUNDLE_SHA256, "0".repeat(64)),
                bindings,
            )
        }
        val invalid = JsonParser.parseString(EXTERNAL_EXPORT_PORT_BUNDLE).asJsonObject
        invalid.getAsJsonObject("validation").addProperty("status", "invalid")
        assertContractError("PORT_BUNDLE_INVALID") {
            GboardTargetBuildCompiler.compile(
                EXTERNAL_EXPORT_CATALOG,
                withRecomputedBundleSha(invalid),
                bindings,
            )
        }
        assertContractError("PRODUCT_CATALOG_DIGEST_MISMATCH") {
            GboardTargetBuildCompiler.compile(
                EXTERNAL_EXPORT_CATALOG + "\n",
                EXTERNAL_EXPORT_PORT_BUNDLE,
                bindings,
            )
        }
        assertContractError("TARGET_VERSION_MISMATCH") {
            GboardTargetBuildCompiler.compile(
                EXTERNAL_EXPORT_CATALOG,
                EXTERNAL_EXPORT_PORT_BUNDLE,
                bindings.replace("18.0.3", "18.0.4"),
            )
        }
        val wrongIdentity = JsonParser.parseString(EXTERNAL_EXPORT_PORT_BUNDLE).asJsonObject
        wrongIdentity.getAsJsonObject("target_contract")
            .getAsJsonObject("target_app")
            .addProperty("package_name", "com.example.not.gboard")
        assertContractError("TARGET_IDENTITY_MISMATCH") {
            GboardTargetBuildCompiler.compile(
                EXTERNAL_EXPORT_CATALOG,
                withRecomputedBundleSha(wrongIdentity),
                bindings,
            )
        }
    }

    @Test
    fun `external artifact identity renders exact compatibility primitives`() {
        val catalog = catalog(
            bindingContracts = mapOf("flag_bool_getter" to "method"),
            features = listOf(
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution("advanced_voice_typing.flag", listOf("flag_bool_getter")),
                    ),
                ),
            ),
        )
        val bundle = bundle(
            catalog,
            status = "ready",
            admittedByFeature = mapOf(
                "advanced_voice_typing" to setOf("advanced_voice_typing.flag"),
            ),
        )
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"1".repeat(64)}","bindings":{"flag_bool_getter":{"kind":"method","class_type":"Lflag/Owner;","method_name":"read","parameter_types":[],"return_type":"Z","confidence":1}}}""",
            bundle,
        )

        val compiled = GboardTargetBuildCompiler.compile(catalog, bundle, bindings)

        assertEquals("com.google.android.inputmethod.latin", compiled.artifactIdentity.packageName)
        assertEquals("APKM", compiled.artifactIdentity.apkFileType)
        assertEquals(setOf(TEST_SIGNATURE), compiled.artifactIdentity.signatures)
        assertEquals(listOf(TEST_VERSION_NAME), compiled.artifactIdentity.versionNames)
        assertTrue(compiled.admissionKotlinSource.contains("ApkFileType.APKM"))
        assertTrue(compiled.admissionKotlinSource.contains(TEST_VERSION_NAME))
    }

    @Test
    fun `external artifact identity rejects more than one version name`() {
        val catalog = catalog(
            bindingContracts = mapOf("flag_bool_getter" to "method"),
            features = listOf(
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution("advanced_voice_typing.flag", listOf("flag_bool_getter")),
                    ),
                ),
            ),
        )
        val validBundle = bundle(
            catalog,
            status = "ready",
            admittedByFeature = mapOf(
                "advanced_voice_typing" to setOf("advanced_voice_typing.flag"),
            ),
        )
        val invalidBundle = JsonParser.parseString(validBundle).asJsonObject
        val targetContract = invalidBundle.getAsJsonObject("target_contract")
        targetContract.addProperty("target_version", "18.0.3")
        targetContract.getAsJsonObject("artifact_identity").add(
            "version_names",
            Gson().toJsonTree(
                listOf(
                    "18.0.3.954559732-release-arm64-v8a",
                    "second-version-name",
                ),
            ),
        )
        val invalidBundleText = withRecomputedBundleSha(invalidBundle)
        val bindings = linkedBindings(
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"4".repeat(64)}","bindings":{"flag_bool_getter":{"kind":"method","class_type":"Lflag/Owner;","method_name":"read","parameter_types":[],"return_type":"Z","confidence":1}}}""",
            invalidBundleText,
        )

        assertContractError("PORT_BUNDLE_INVALID") {
            GboardTargetBuildCompiler.compile(catalog, invalidBundleText, bindings)
        }
    }

    @Test
    fun `reviewed bindings must cryptographically link the exact profile and PortBundle`() {
        val catalog = catalog(
            bindingContracts = mapOf("flag_bool_getter" to "method"),
            features = listOf(
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution("advanced_voice_typing.flag", listOf("flag_bool_getter")),
                    ),
                ),
            ),
        )
        val bundle = bundle(
            catalog,
            status = "ready",
            admittedByFeature = mapOf(
                "advanced_voice_typing" to setOf("advanced_voice_typing.flag"),
            ),
        )
        val rawBindings =
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"2".repeat(64)}","bindings":{"flag_bool_getter":{"kind":"method","class_type":"Lflag/Owner;","method_name":"read","parameter_types":[],"return_type":"Z","confidence":1}}}"""
        assertContractError("REVIEWED_BINDINGS_INVALID") {
            GboardTargetBuildCompiler.compile(catalog, bundle, rawBindings)
        }
        val linked = JsonParser.parseString(linkedBindings(rawBindings, bundle)).asJsonObject

        linked.addProperty("projection_sha256", "0".repeat(64))
        assertContractError("REVIEWED_BINDINGS_PROJECTION_MISMATCH") {
            GboardTargetBuildCompiler.compile(catalog, bundle, Gson().toJson(linked))
        }
        linked.addProperty("projection_sha256", PROJECTION_SHA256)
        linked.addProperty("profile_sha256", "0".repeat(64))
        assertContractError("REVIEWED_BINDINGS_LINKAGE_MISMATCH") {
            GboardTargetBuildCompiler.compile(catalog, bundle, Gson().toJson(linked))
        }
        linked.addProperty("profile_sha256", PROFILE_SHA256)
        linked.addProperty("port_bundle_sha256", "0".repeat(64))
        assertContractError("REVIEWED_BINDINGS_LINKAGE_MISMATCH") {
            GboardTargetBuildCompiler.compile(catalog, bundle, Gson().toJson(linked))
        }
    }

    @Test
    fun `complete PortBundle schema rejects unknown missing and wrong-shape fields`() {
        val catalog = catalog(
            bindingContracts = mapOf("flag_bool_getter" to "method"),
            features = listOf(
                feature(
                    id = "advanced_voice_typing",
                    patchName = "Advanced Voice Typing",
                    contributions = listOf(
                        contribution("advanced_voice_typing.flag", listOf("flag_bool_getter")),
                    ),
                ),
            ),
        )
        val bundleText = bundle(
            catalog,
            status = "ready",
            admittedByFeature = mapOf(
                "advanced_voice_typing" to setOf("advanced_voice_typing.flag"),
            ),
        )
        val rawBindings =
            """{"format":"gboard-version-bindings.v1","target_version":"18.0.3","profile_version":"2.0.0","projection_sha256":"${"3".repeat(64)}","bindings":{"flag_bool_getter":{"kind":"method","class_type":"Lflag/Owner;","method_name":"read","parameter_types":[],"return_type":"Z","confidence":1}}}"""
        val bindings = linkedBindings(rawBindings, bundleText)
        val mutations = listOf<(JsonObject) -> Unit>(
            { bundle -> bundle.addProperty("unknown", true) },
            { bundle -> bundle.getAsJsonObject("target_contract").remove("profile_sha256") },
            { bundle -> bundle.getAsJsonObject("target_contract").remove("projection_sha256") },
            { bundle ->
                bundle.getAsJsonObject("target_contract").addProperty("projection_sha256", true)
            },
            { bundle -> bundle.add("features", JsonObject()) },
            { bundle ->
                bundle.getAsJsonArray("features")[0].asJsonObject.addProperty("unknown", true)
            },
        )

        mutations.forEach { mutate ->
            val invalid = JsonParser.parseString(bundleText).asJsonObject
            mutate(invalid)
            assertContractError("PORT_BUNDLE_INVALID") {
                GboardTargetBuildCompiler.compile(catalog, withRecomputedBundleSha(invalid), bindings)
            }
        }
    }

    private fun catalog(
        bindingContracts: Map<String, String>,
        features: List<JsonObject>,
    ): String = Gson().toJson(JsonObject().apply {
        addProperty("format", "gboard-port-product-catalog.v1")
        addProperty("catalog_version", "test")
        add("binding_contracts", Gson().toJsonTree(bindingContracts))
        add("features", Gson().toJsonTree(features))
    })

    private fun feature(
        id: String,
        patchName: String,
        contributions: List<JsonObject>,
    ): JsonObject = JsonObject().apply {
        addProperty("feature_id", id)
        addProperty("public_patch_name", patchName)
        addProperty("feature_marker", "$id.marker")
        add("depends_on_feature_ids", Gson().toJsonTree(emptyList<String>()))
        add(
            "target_family_ids",
            Gson().toJsonTree(contributions.map { it.get("anchor_family_id").asString }.distinct()),
        )
        add("contributions", Gson().toJsonTree(contributions))
    }

    private fun contribution(
        id: String,
        requiredBindings: List<String> = emptyList(),
    ): JsonObject = JsonObject().apply {
        addProperty("contribution_id", id)
        addProperty("anchor_family_id", if (requiredBindings.isEmpty()) "resource_only" else "flag_bool_getter")
        addProperty("phase", if (requiredBindings.isEmpty()) "resource" else "after_stock_result")
        addProperty("order", 100)
        add("required_bindings", Gson().toJsonTree(requiredBindings))
        add("runtime_calls", Gson().toJsonTree(emptyList<String>()))
    }

    private fun bundle(
        catalogText: String,
        status: String,
        admittedByFeature: Map<String, Set<String>>,
    ): String {
        val catalog = JsonParser.parseString(catalogText).asJsonObject
        val features = catalog.getAsJsonArray("features").map { value ->
            value.asJsonObject.deepCopy().apply {
                val admittedIds = admittedByFeature[get("feature_id").asString].orEmpty()
                add(
                    "admitted_contributions",
                    Gson().toJsonTree(
                        getAsJsonArray("contributions")
                            .filter { it.asJsonObject.get("contribution_id").asString in admittedIds },
                    ),
                )
            }
        }
        val requiredKeys = features.flatMap { feature ->
            feature.getAsJsonArray("admitted_contributions").flatMap { contribution ->
                contribution.asJsonObject.getAsJsonArray("required_bindings").map { it.asString }
            }
        }.toSortedSet()
        val body = JsonObject().apply {
            addProperty("format", "gboard-port-bundle.v1")
            addProperty("contract_version", "1.0.0")
            add("product_catalog", JsonObject().apply {
                addProperty("format", "gboard-port-product-catalog.v1")
                addProperty("catalog_version", catalog.get("catalog_version").asString)
                addProperty("sha256", sha256(catalogText))
            })
            add("target_contract", JsonObject().apply {
                addProperty("profile_version", "2.0.0")
                addProperty("profile_sha256", PROFILE_SHA256)
                addProperty("projection_sha256", PROJECTION_SHA256)
                addProperty("target_version", "18.0.3")
                add("target_app", JsonObject().apply {
                    addProperty("id", "gboard")
                    addProperty("package_name", "com.google.android.inputmethod.latin")
                    add("patched_package_names", Gson().toJsonTree(emptyList<String>()))
                })
                add("artifact_identity", JsonObject().apply {
                    addProperty("apk_file_type", "APKM")
                    add("package_names", Gson().toJsonTree(listOf("com.google.android.inputmethod.latin")))
                    add("signatures", Gson().toJsonTree(listOf(TEST_SIGNATURE)))
                    add("version_names", Gson().toJsonTree(listOf(TEST_VERSION_NAME)))
                })
                add(
                    "binding_exports",
                    Gson().toJsonTree(requiredKeys.map { key ->
                        mapOf(
                            "key" to key,
                            "family_id" to "test-family",
                            "touchpoint_id" to "test-touchpoint",
                            "required_shape_checks" to emptyList<String>(),
                        )
                    }),
                )
            })
            add("features", Gson().toJsonTree(features))
            add("validation", JsonObject().apply {
                addProperty("status", status)
                add("errors", Gson().toJsonTree(emptyList<String>()))
            })
        }
        val result = body.deepCopy()
        result.addProperty("bundle_sha256", sha256(Gson().toJson(body)))
        return Gson().toJson(result)
    }

    private fun linkedBindings(bindingsText: String, bundleText: String): String {
        val bindings = JsonParser.parseString(bindingsText).asJsonObject
        val bundle = JsonParser.parseString(bundleText).asJsonObject
        bindings.addProperty(
            "projection_sha256",
            bundle.getAsJsonObject("target_contract").get("projection_sha256").asString,
        )
        bindings.addProperty(
            "profile_sha256",
            bundle.getAsJsonObject("target_contract").get("profile_sha256").asString,
        )
        bindings.addProperty("port_bundle_sha256", bundle.get("bundle_sha256").asString)
        return Gson().toJson(bindings)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun withRecomputedBundleSha(bundle: JsonObject): String {
        val body = bundle.deepCopy().apply { remove("bundle_sha256") }
        val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
        val result = body.deepCopy().apply { addProperty("bundle_sha256", sha256(gson.toJson(body))) }
        return gson.toJson(result)
    }

    private fun assertContractError(code: String, block: () -> Unit) {
        val failure = assertThrows(GboardTargetBuildContractException::class.java, block)
        assertEquals(code, failure.code)
    }

    private companion object {
        const val PROFILE_SHA256 =
            "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
        const val PROJECTION_SHA256 =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val TEST_SIGNATURE =
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        const val TEST_VERSION_NAME = "18.0.3.123456789-release-arm64-v8a"
        // Canonical compact JSON fixture produced by an external exporter.
        const val EXTERNAL_EXPORT_BUNDLE_SHA256 =
            "f4087fe13199c157f3ce975a7fac04c038d8679a54bf7018158c6b1535a2f43c"
        const val EXTERNAL_EXPORT_CATALOG =
            """{"format":"gboard-port-product-catalog.v1","catalog_version":"ls-interop","binding_contracts":{"flag_bool_getter":"method","flag_name_field":"field"},"features":[{"feature_id":"developer_options","public_patch_name":"Developer options","depends_on_feature_ids":[],"contributions":[{"contribution_id":"developer_options.resources","anchor_family_id":"resource_only","phase":"resource","order":100,"required_bindings":[],"runtime_calls":[]}]},{"feature_id":"advanced_voice_typing","public_patch_name":"Advanced Voice Typing","depends_on_feature_ids":[],"contributions":[{"contribution_id":"advanced_voice_typing.flag","anchor_family_id":"flag_bool_getter","phase":"after_stock_result","order":100,"required_bindings":["flag_bool_getter","flag_name_field"],"runtime_calls":[]}]}]}"""
        const val EXTERNAL_EXPORT_PORT_BUNDLE =
            """{"format":"gboard-port-bundle.v1","contract_version":"1.0.0","product_catalog":{"format":"gboard-port-product-catalog.v1","catalog_version":"ls-interop","sha256":"5802663d4ac7c19816048897edeff6c0b1cdab1454f531921474f1dc3f48743f"},"target_contract":{"profile_version":"2.0.0","profile_sha256":"eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee","projection_sha256":"cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc","target_version":"18.0.3","target_app":{"id":"gboard","package_name":"com.google.android.inputmethod.latin","patched_package_names":[]},"artifact_identity":{"apk_file_type":"APKM","package_names":["com.google.android.inputmethod.latin"],"signatures":["ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"],"version_names":["18.0.3.123456789-release-arm64-v8a"]},"binding_exports":[]},"features":[{"feature_id":"developer_options","public_patch_name":"Developer options","feature_marker":"Emojis \u0026 Symbols \u003c test","depends_on_feature_ids":[],"target_family_ids":["resource_only"],"contributions":[{"contribution_id":"developer_options.resources","anchor_family_id":"resource_only","phase":"resource","order":100,"required_bindings":[],"runtime_calls":[]}],"admitted_contributions":[{"contribution_id":"developer_options.resources","anchor_family_id":"resource_only","phase":"resource","order":100,"required_bindings":[],"runtime_calls":[]}]},{"feature_id":"advanced_voice_typing","public_patch_name":"Advanced Voice Typing","feature_marker":null,"depends_on_feature_ids":[],"target_family_ids":["flag_bool_getter"],"contributions":[{"contribution_id":"advanced_voice_typing.flag","anchor_family_id":"flag_bool_getter","phase":"after_stock_result","order":100,"required_bindings":["flag_bool_getter","flag_name_field"],"runtime_calls":[]}],"admitted_contributions":[]}],"validation":{"status":"pending","errors":[]},"bundle_sha256":"f4087fe13199c157f3ce975a7fac04c038d8679a54bf7018158c6b1535a2f43c"}"""
    }
}
