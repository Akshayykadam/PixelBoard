package com.akshaykadam.pixelboard.patches.gboard.features.launchericon

import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isFieldReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.isReference
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

internal val gboardLauncherIconVisibilityPatch = bytecodePatch(
    description = "Ensure 'Show in app list' toggle is enabled and configurable in Gboard Settings -> Preferences across all devices.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        // Step 1: Locate the Environment class containing Gboard runtime environment helpers
        // (Must contain the Environment logger tag and the launcher icon restriction check)
        val environmentClass = mutableClassDefByOrNull { classDef ->
            val hasEnvTag = classDef.methods.any { method ->
                method.implementation?.instructions?.any { instruction ->
                    instruction.isReference("com/google/android/libraries/inputmethod/utils/Environment")
                } == true
            }
            val hasIconFixedMethod = classDef.methods.any { method ->
                method.parameterTypes == listOf("Landroid/content/Context;") &&
                    method.returnType == "Z" &&
                    method.implementation?.instructions?.any { inst ->
                        inst.isFieldReference("Landroid/os/Build\$VERSION;->SDK_INT:I")
                    } == true &&
                    method.implementation?.instructions?.any { inst ->
                        (inst as? NarrowLiteralInstruction)?.narrowLiteral == 29
                    } == true
            }
            hasEnvTag && hasIconFixedMethod
        } ?: return@execute

        // Step 2: Locate the method checking whether launcher icon visibility is fixed / unconfigurable (Environment.u)
        val iconFixedMethod = environmentClass.methods.firstOrNull { method ->
            method.parameterTypes == listOf("Landroid/content/Context;") &&
                method.returnType == "Z" &&
                method.implementation?.instructions?.any { inst ->
                    inst.isFieldReference("Landroid/os/Build\$VERSION;->SDK_INT:I")
                } == true &&
                method.implementation?.instructions?.any { inst ->
                    (inst as? NarrowLiteralInstruction)?.narrowLiteral == 29
                } == true
        } ?: return@execute

        // Patch Environment.u to always return false (0), indicating launcher icon is configurable
        val iconFixedInstructions = iconFixedMethod.implementation?.instructions ?: return@execute
        val iconFixedAlreadyPatched = iconFixedInstructions.size >= 2 &&
            iconFixedInstructions[0].isOpcode("CONST_4") &&
            iconFixedInstructions[1].isOpcode("RETURN")

        if (!iconFixedAlreadyPatched) {
            iconFixedMethod.addInstructions(
                0,
                """
                    const/4 p0, 0x0
                    return p0
                """.trimIndent(),
            )
        }

        // Step 3: Locate and patch any static preference filter method that calls iconFixedMethod (e.g. ppr.d)
        // to ensure it also unconditionally returns false (do not hide preference)
        val targetMethodDescriptor = "${environmentClass.type}->${iconFixedMethod.name}(Landroid/content/Context;)Z"
        val filterClass = mutableClassDefByOrNull { classDef ->
            classDef.methods.any { method ->
                method.parameterTypes == listOf("Landroid/content/Context;") &&
                    method.returnType == "Z" &&
                    method.implementation?.instructions?.any { inst ->
                        inst.isMethodReference(targetMethodDescriptor)
                    } == true
            }
        }

        filterClass?.let { cls ->
            cls.methods.filter { method ->
                method.parameterTypes == listOf("Landroid/content/Context;") &&
                    method.returnType == "Z" &&
                    method.implementation?.instructions?.any { inst ->
                        inst.isMethodReference(targetMethodDescriptor)
                    } == true
            }.forEach { method ->
                val instructions = method.implementation?.instructions ?: return@forEach
                val isAlreadyPatched = instructions.size >= 2 &&
                    instructions[0].isOpcode("CONST_4") &&
                    instructions[1].isOpcode("RETURN")
                if (!isAlreadyPatched) {
                    method.addInstructions(
                        0,
                        """
                            const/4 p0, 0x0
                            return p0
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}
