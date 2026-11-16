package com.akshaykadam.pixelboard.patches.gboard.shared.generated

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardFieldTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardTypeTarget

internal object GboardVersionBindings {
    const val targetVersion = "18.0.3"
    const val profileVersion = "1.3.0"
    const val projectionSha256 = "ac6c1060e1eeac68c95715ade860f265680e94749650af661095811bd2bf74d2"
    const val sourceSha256 = "0748c01bb41db95a4c9a48097a8e362106bf010c72985d5d396ce74af3a5c0cd"

    val `flagBoolGetter` = GboardMethodTarget(
        classType = "Lnxw;",
        name = "g",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/Object;",
    )

    val `flagNameField` = GboardFieldTarget(
        classType = "Lnxw;",
        name = "a",
        type = "Ljava/lang/String;",
    )
}
