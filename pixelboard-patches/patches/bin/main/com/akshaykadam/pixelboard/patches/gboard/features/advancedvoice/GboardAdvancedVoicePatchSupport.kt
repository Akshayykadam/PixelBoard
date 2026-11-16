package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.akshaykadam.pixelboard.patches.gboard.shared.VerifiedTransformationPlan
import com.akshaykadam.pixelboard.patches.gboard.shared.VerifiedTransformationState
import com.akshaykadam.pixelboard.patches.gboard.shared.applyVerified
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardStructuralFingerprint
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.returnInstructionIndices

internal const val ADVANCED_VOICE_RESULT_TOKEN = "__RESULT__"

internal fun MutableMethod.applyAdvancedVoiceFingerprintGuard(
    descriptor: String,
    runtimeDescriptor: String,
    stockFingerprint: String,
    patchedFingerprint: String,
    inject: () -> Unit,
) {
    applyVerified(
        VerifiedTransformationPlan(
            targetName = descriptor,
            classify = { method ->
                val delegateCount = method.implementation!!.instructions.count {
                    it.isMethodReference(runtimeDescriptor)
                }
                val actual = method.gboardStructuralFingerprint()
                when {
                    delegateCount == 0 -> {
                        check(actual == stockFingerprint) {
                            "Stock body drift in $descriptor: $actual"
                        }
                        VerifiedTransformationState.STOCK
                    }
                    delegateCount > 0 -> {
                        check(actual == patchedFingerprint) {
                            "Patched body drift in $descriptor: $actual"
                        }
                        VerifiedTransformationState.PATCHED
                    }
                    else -> VerifiedTransformationState.MALFORMED
                }
            },
            mutate = { method ->
                inject()
                method
            },
        ),
    )
}

internal fun MutableMethod.injectAdvancedVoiceResultDelegate(
    descriptor: String,
    template: String,
) {
    val instructions = implementation?.instructions
        ?: error("Advanced Voice target has no implementation")
    val returns = returnInstructionIndices()
    check(returns.isNotEmpty()) { "Advanced Voice target has no return instructions" }
    val existing = instructions.count { it.isMethodReference(descriptor) }
    check(existing == 0 || existing == returns.size) {
        "Partial or duplicate Advanced Voice result delegates for $descriptor"
    }
    if (existing != 0) {
        return
    }
    returns.asReversed().forEach { returnIndex ->
        val register = (implementation!!.instructions[returnIndex] as? OneRegisterInstruction)
            ?.registerA
            ?: error("Advanced Voice result return has no register")
        addInstructions(
            returnIndex,
            template.replace(ADVANCED_VOICE_RESULT_TOKEN, register.toString()),
        )
    }
}

internal fun MutableMethod.injectAdvancedVoiceBeforeReturns(
    descriptor: String,
    delegate: String,
) {
    val instructions = implementation?.instructions
        ?: error("Advanced Voice target has no implementation")
    val returns = returnInstructionIndices()
    check(returns.isNotEmpty()) { "Advanced Voice target has no return instructions" }
    val existing = instructions.count { it.isMethodReference(descriptor) }
    check(existing == 0 || existing == returns.size) {
        "Partial or duplicate Advanced Voice return delegates for $descriptor"
    }
    if (existing != 0) {
        return
    }
    returns.asReversed().forEach { returnIndex ->
        addInstructions(returnIndex, delegate)
    }
}
