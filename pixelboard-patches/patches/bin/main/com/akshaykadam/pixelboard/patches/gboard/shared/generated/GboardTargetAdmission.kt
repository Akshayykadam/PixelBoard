package com.akshaykadam.pixelboard.patches.gboard.shared.generated

import app.morphe.patcher.patch.ApkFileType

internal object GboardTargetAdmission {
    const val targetVersion = "18.0.3"
    const val profileVersion = "1.3.0"
    const val projectionSha256 = "ac6c1060e1eeac68c95715ade860f265680e94749650af661095811bd2bf74d2"
    const val packageName = "com.google.android.inputmethod.latin"
    val apkFileType = ApkFileType.APK
    val signatures = setOf("7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053", "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83")
    val versionNames = listOf("18.0.3.954559732-release-arm64-v8a", "18.3.1.977415014-beta-arm64-v8a", "18.3.1.977415014-release-arm64-v8a", "18.4.1.985164140-release-arm64-v8a", "18.5.3.993608717-release-arm64-v8a", "18.6.3.1002251336-release-arm64-v8a")

    val admittedContributionIdsByFeature: Map<String, Set<String>> = mapOf(
        "add_gboard_signature_bypass" to setOf("add_gboard_signature_bypass.bytecode"),
        "advanced_voice_typing" to setOf("advanced_voice_typing.bytecode", "advanced_voice_typing.flag"),
        "ai_writing_tools" to setOf("ai_writing_tools.flag"),
        "package_rename" to setOf("package_rename.resources"),
    )

    fun admittedContributions(featureId: String): Set<String> =
        admittedContributionIdsByFeature[featureId].orEmpty()
}
