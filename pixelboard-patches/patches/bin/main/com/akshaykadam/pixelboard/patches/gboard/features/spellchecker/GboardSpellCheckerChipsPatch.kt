package com.akshaykadam.pixelboard.patches.gboard.features.spellchecker

import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val gboardSpellCheckerChipsPatch = bytecodePatch(
    description = "Enforce modern inline spell checker suggestion chips and add-to-dictionary UI across all Android 12+ devices.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        // Locate the spell checker utility class (e.g. rcq) uniquely by its method signature:
        // (Context, TextServicesManager) -> Boolean
        val spellCheckerClass = mutableClassDefByOrNull { classDef ->
            classDef.methods.any { method ->
                method.parameterTypes == listOf(
                    "Landroid/content/Context;",
                    "Landroid/view/textservice/TextServicesManager;"
                ) && method.returnType == "Z"
            }
        } ?: return@execute

        // Method 1: System spell checker compatibility check (rcq.m)
        // Signature: (Landroid/content/Context;Landroid/view/textservice/TextServicesManager;)Z
        val mMethod = spellCheckerClass.methods.firstOrNull { method ->
            method.parameterTypes == listOf(
                "Landroid/content/Context;",
                "Landroid/view/textservice/TextServicesManager;"
            ) && method.returnType == "Z"
        } ?: return@execute

        val mInstructions = mMethod.implementation?.instructions ?: return@execute
        val mFirst = mInstructions.firstOrNull() ?: return@execute

        // Check if mMethod is already patched (third instruction is our branch)
        val mAlreadyPatched = mInstructions.size >= 4 &&
            mInstructions[2].opcode.name.startsWith("IF_")
        if (!mAlreadyPatched) {
            mMethod.addInstructionsWithLabels(
                0,
                """
                    sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
                    const/16 v1, 0x1f
                    if-lt v0, v1, :pixelboard_sdk_below
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
                ExternalLabel("pixelboard_sdk_below", mFirst),
            )
        }

        // Method 2: Inline chips enablement check (rcq.o)
        // Locate oMethod: the (Context) -> Boolean method in spellCheckerClass that calls another
        // static (Context) -> Boolean method in spellCheckerClass (pMethod)
        val contextMethods = spellCheckerClass.methods.filter { method ->
            method.parameterTypes == listOf("Landroid/content/Context;") && method.returnType == "Z"
        }

        var targetOMethod: MutableMethod? = null
        var targetPMethod: MethodReference? = null

        for (method in contextMethods) {
            val instructions = method.implementation?.instructions ?: continue
            for (instruction in instructions) {
                val methodRef = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (methodRef.definingClass == spellCheckerClass.type &&
                    methodRef.parameterTypes == listOf("Landroid/content/Context;") &&
                    methodRef.returnType == "Z"
                ) {
                    targetOMethod = method
                    targetPMethod = methodRef
                    break
                }
            }
            if (targetOMethod != null) break
        }

        if (targetOMethod != null && targetPMethod != null) {
            val oInstructions = targetOMethod.implementation?.instructions ?: return@execute
            val oAlreadyPatched = oInstructions.size >= 3 &&
                oInstructions[2].opcode.name.startsWith("RETURN")
            if (!oAlreadyPatched) {
                targetOMethod.addInstructions(
                    0,
                    """
                        invoke-static {p0}, ${targetPMethod.definingClass}->${targetPMethod.name}(Landroid/content/Context;)Z
                        move-result v0
                        return v0
                    """.trimIndent(),
                )
            }
        }
    }
}
