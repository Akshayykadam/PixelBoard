/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Based on upstream Gboard patch framework by Jason Wu (@jasonwu1994)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.registry

import com.akshaykadam.pixelboard.patches.shared.Patch
import com.akshaykadam.pixelboard.patches.shared.ResourcePatchBuilder
import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import com.akshaykadam.pixelboard.patches.shared.stringOption
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoiceFeatureMarkerPatch
import com.akshaykadam.pixelboard.patches.gboard.features.packagerename.applyGboardApplicationDisplayName
import com.akshaykadam.pixelboard.patches.gboard.features.packagerename.gboardPackageRenameResourcePatch
import com.akshaykadam.pixelboard.patches.gboard.features.packagerename.isValidGboardAppDisplayName
import com.akshaykadam.pixelboard.patches.gboard.features.signaturebypass.gboardSignatureBypassBytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingToolsFeatureMarkerPatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingTools1803AutoFixAcceptancePatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingTools1803AutoFixRoutePatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingTools1803GenAiInitPatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingTools1803GenAiRefreshPatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingTools1803SmartEditInitPatch
import com.akshaykadam.pixelboard.patches.gboard.features.writingtools.gboardAiWritingToolsFlagValuePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesSettingsPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

private fun gboardPublicResourcePatch(
    featureId: String,
    name: String,
    description: String,
    default: Boolean,
    block: ResourcePatchBuilder.() -> Unit,
) = GboardTargetAdmission.admittedContributions(featureId).let { admitted ->
    resourcePatch(
        name = name.takeIf { admitted.isNotEmpty() },
        description = description,
        default = default,
    ) {
        if (admitted.isNotEmpty()) {
            block()
            GboardContributionWiring.wireAdmitted(featureId, admitted, this)
        }
    }
}

val gboardAdvancedVoiceTypingPatch = gboardPublicResourcePatch(
    featureId = "advanced_voice_typing",
    name = "Advanced Voice Typing",
    description = "Enable Advanced Voice Typing with automatic punctuation and rambler mode.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardAdvancedVoiceFeatureMarkerPatch,
    )
}

val gboardAiWritingToolsPatch = gboardPublicResourcePatch(
    featureId = "ai_writing_tools",
    name = "AI Writing Tools",
    description = "Enable AI writing tools with support for all languages.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardAiWritingToolsFeatureMarkerPatch,
        gboardAiWritingToolsFlagValuePatch,
        gboardAiWritingTools1803AutoFixRoutePatch,
        gboardAiWritingTools1803AutoFixAcceptancePatch,
        gboardAiWritingTools1803GenAiInitPatch,
        gboardAiWritingTools1803GenAiRefreshPatch,
        gboardAiWritingTools1803SmartEditInitPatch,
    )
}

val gboardSignatureBypassPatch = gboardPublicResourcePatch(
    featureId = "add_gboard_signature_bypass",
    name = "Add Gboard Signature Bypass",
    description = "Bypass Gboard signature whitelist checks and force them to pass.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardSignatureBypassBytecodePatch
    )
}

val gboardPackageRenamePatch = gboardPublicResourcePatch(
    featureId = "package_rename",
    name = "Package Rename",
    description = "Rename the package to com.akshaykadam.pixelboard and allow " +
        "a custom app name so it can be installed alongside the official Gboard.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    val appDisplayName = stringOption(
        key = "app_display_name",
        default = "PixelBoard",
        values = linkedMapOf(
            "PixelBoard" to "PixelBoard",
            "Gboard" to "Gboard",
            "Gboard Dev" to "Gboard Dev",
            "Gboard Patches" to "Gboard Patches",
            "Gboard Mod" to "Gboard Mod",
        ),
        title = "App Name",
        description = "",
        required = true,
    ) { value ->
        isValidGboardAppDisplayName(value)
    }

    dependsOn(
        gboardPackageRenameResourcePatch
    )

    finalize {
        document("AndroidManifest.xml").use { manifestDocument ->
            applyGboardApplicationDisplayName(
                manifestDocument = manifestDocument,
                displayName = checkNotNull(appDisplayName.value),
            )
        }
    }
}

object GboardPublishedPatchCatalog {
    val publishedRegistrations: Set<Patch<*>> = linkedSetOf(
        gboardAdvancedVoiceTypingPatch,
        gboardAiWritingToolsPatch,
        gboardSignatureBypassPatch,
        gboardPackageRenamePatch,
    ).filterTo(linkedSetOf()) { patch -> patch.name != null }

    @Suppress("DEPRECATION")
    fun publishedInventory(version: String): String {
        val publishedPatches = publishedRegistrations.sortedBy { patch -> patch.name }.map { patch ->
            PublishedPatch(
                name = patch.name!!,
                description = patch.description,
                use = patch.use,
                dependencies = patch.dependencies.mapNotNull { dependency -> dependency.name }.sorted(),
                compatiblePackages = patch.compatiblePackages?.associate { (packageName, versions) ->
                    packageName to versions
                },
                options = patch.options.values.map { option ->
                    PublishedPatch.Option(
                        key = option.key,
                        title = option.title,
                        description = option.description,
                        required = option.required,
                        type = option.type.toString(),
                        default = option.default,
                        values = option.values,
                    )
                },
            )
        }
        val gson = GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
        val inventory = JsonObject().apply {
            addProperty("version", version)
            add("patches", gson.toJsonTree(publishedPatches))
        }

        return gson.toJson(inventory) + "\n"
    }

    private class PublishedPatch(
        val name: String,
        val description: String?,
        val use: Boolean,
        val dependencies: List<String>,
        val compatiblePackages: Map<String, Set<String>?>?,
        val options: List<Option>,
    ) {
        class Option(
            val key: String,
            val title: String?,
            val description: String?,
            val required: Boolean,
            val type: String,
            val default: Any?,
            val values: Map<String, Any?>?,
        )
    }
}
