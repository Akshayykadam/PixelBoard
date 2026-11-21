package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.invokeRegisters
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.MutableClass
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import java.lang.reflect.Modifier

private const val PERFORM_HAPTIC_FEEDBACK_TARGET =
    "Landroid/view/View;->performHapticFeedback(I)Z"

private val PRESS_EFFECT_PLAYER_CLASSES = listOf(
    "Lcom/google/android/libraries/inputmethod/keypresseffect/module/PressEffectPlayerImpl;",
    "Lpoj;", // 18.6.3 PressEffectPlayerImpl
    "Lpiq;", // text handle / cursor drag haptics
)

internal val gboardKeyboardHapticPatch = bytecodePatch(
    description = "Bypass OEM global haptic restrictions on MagicOS/EMUI by passing FLAG_IGNORE_GLOBAL_SETTING on keypress haptic feedback.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val candidateClasses = mutableSetOf<MutableClass>()

        try {
            val matchingClasses = getAllClassesWithString(
                "com/google/android/libraries/inputmethod/keypresseffect/module/PressEffectPlayerImpl"
            )
            for (classDef in matchingClasses) {
                mutableClassDefByOrNull(classDef.type)?.let { candidateClasses.add(it) }
            }
        } catch (_: Throwable) {
        }

        for (classType in PRESS_EFFECT_PLAYER_CLASSES) {
            mutableClassDefByOrNull(classType)?.let { candidateClasses.add(it) }
        }

        val call = RuntimeCallId.KEYBOARD_RUNTIME_PERFORM_HAPTIC_FEEDBACK
        val runtimeReference = RuntimeAbiCatalog.abi(call).reference

        for (clazz in candidateClasses) {
            for (method in clazz.methods) {
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions
                val replacements = mutableListOf<Pair<Int, String>>()

                for ((index, instruction) in instructions.withIndex()) {
                    if (instruction.isMethodReference(runtimeReference)) {
                        continue
                    }
                    val regs = instruction.invokeRegisters(PERFORM_HAPTIC_FEEDBACK_TARGET)
                    if (regs != null && regs.size == 2) {
                        val regView = registerName(regs[0], method)
                        val regFeedback = registerName(regs[1], method)
                        replacements.add(index to RuntimeCallEmitter.invoke(call, "$regView, $regFeedback"))
                    }
                }

                for ((index, replacement) in replacements.reversed()) {
                    method.replaceInstruction(index, replacement)
                }
            }
        }
    }
}

private fun registerName(registerIndex: Int, method: MutableMethod): String {
    val impl = method.implementation ?: return "v$registerIndex"
    val totalRegisters = impl.registerCount
    val isStatic = Modifier.isStatic(method.accessFlags)
    val paramWords = (if (isStatic) 0 else 1) +
        method.parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 }
    val locals = totalRegisters - paramWords
    return if (locals >= 0 && registerIndex >= locals) {
        "p${registerIndex - locals}"
    } else {
        "v$registerIndex"
    }
}
