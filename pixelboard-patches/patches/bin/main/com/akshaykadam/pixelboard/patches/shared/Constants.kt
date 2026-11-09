package com.akshaykadam.pixelboard.patches.shared

import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission

internal object Constants {
    const val GBOARD_PACKAGE_NAME = "com.google.android.inputmethod.latin"
    const val GBOARD_PATCHED_PACKAGE_NAME = "com.akshaykadam.pixelboard"
    const val GBOARD_PATCH_AUTHOR = "Akshay Kadam"
    const val GBOARD_PATCH_AUTHOR_URL = "https://github.com/Akshayykadam"
    const val GBOARD_PATCH_REPOSITORY_URL = "https://github.com/Akshayykadam/PixelBoard"
    val GBOARD_PATCH_VERSION = PatchBuildInfo.VERSION

    val COMPATIBILITY_GBOARD = Compatibility(
        name = "Gboard",
        packageName = GboardTargetAdmission.packageName,
        apkFileType = GboardTargetAdmission.apkFileType,
        appIconColor = 0x1A73E8,
        signatures = GboardTargetAdmission.signatures,
        targets = GboardTargetAdmission.versionNames.map { versionName ->
            AppTarget(
                version = versionName,
                isExperimental = false,
            )
        },
    )
}
