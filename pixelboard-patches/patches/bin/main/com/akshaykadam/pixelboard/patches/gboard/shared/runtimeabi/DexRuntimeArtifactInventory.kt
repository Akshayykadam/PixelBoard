package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import java.io.File

internal class DexRuntimeArtifactInventory private constructor(
    methods: List<RuntimeArtifactMethod>,
) : RuntimeArtifactInventory {
    private val methodsByOwnerAndName = methods.groupBy { method -> method.owner to method.name }

    override fun methods(owner: String, name: String): List<RuntimeArtifactMethod> =
        methodsByOwnerAndName[owner to name].orEmpty()

    companion object {
        fun read(artifact: File): DexRuntimeArtifactInventory {
            require(artifact.isFile) { "Runtime extension artifact not found: $artifact" }
            val dex = artifact.inputStream().buffered().use { input ->
                DexBackedDexFile.fromInputStream(Opcodes.getDefault(), input)
            }
            return DexRuntimeArtifactInventory(
                dex.classes.flatMap { classDef ->
                    classDef.methods.map { method ->
                        RuntimeArtifactMethod(
                            owner = classDef.type,
                            name = method.name,
                            descriptor = method.parameterTypes.joinToString(
                                prefix = "(",
                                postfix = ")${method.returnType}",
                                separator = "",
                            ),
                            isStatic = AccessFlags.STATIC.isSet(method.accessFlags),
                        )
                    }
                }
            )
        }
    }
}
