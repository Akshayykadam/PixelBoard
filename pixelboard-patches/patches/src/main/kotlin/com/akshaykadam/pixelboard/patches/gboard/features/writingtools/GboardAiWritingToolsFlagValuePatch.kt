package com.akshaykadam.pixelboard.patches.gboard.features.writingtools

import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardFlagFamilyFeature
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrThrow
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardFlagFamilyFeaturePatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isFieldReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardAiWritingToolsFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "Enable Writing Tools and official backend selector with 18.0.3 typed flag tuple.",
    feature = GboardFlagFamilyFeature.AI_WRITING_TOOLS,
)

private val autoFixRouteBuilder = GboardMethodTarget(
    classType = "Lrzb;",
    name = "v",
    parameterTypes = emptyList(),
    returnType = "Lvvw;",
)

private val autoFixAmbiguityGate = GboardMethodTarget(
    classType = "Lgwf;",
    name = "a",
    parameterTypes = listOf("Lgwo;"),
    returnType = "Llyg;",
)

private val genAiVoiceEditInit = GboardMethodTarget(
    classType = "Lifn;",
    name = "c",
    parameterTypes = listOf(
        "Lcom/google/android/apps/inputmethod/libs/nga/impl/input/NgaInputManager;",
        "Lhmi;",
    ),
    returnType = "V",
)

private val smartEditInit = GboardMethodTarget(
    classType = "Lifn;",
    name = "d",
    parameterTypes = emptyList(),
    returnType = "V",
)

private val genAiClientRefresh = GboardMethodTarget(
    classType = "Lcom/google/android/apps/inputmethod/libs/nga/impl/input/NgaInputManager;",
    name = "e",
    parameterTypes = emptyList(),
    returnType = "V",
)

private val genAiInitClientTypeRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_APPLY_GEN_AI_INIT_CLIENT_TYPE
private val smartEditInitClientTypeRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_APPLY_SMART_EDIT_INIT_CLIENT_TYPE
private val rememberGenAiInitRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_REMEMBER_GEN_AI_INIT_CALL
private val beginGenAiRefreshRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_BEGIN_GEN_AI_REFRESH
private val observeGenAiRefreshRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_OBSERVE_GEN_AI_REFRESH_CLIENT_TYPE
private val finishGenAiRefreshRuntime =
    RuntimeCallId.AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_FINISH_GEN_AI_REFRESH

internal val gboardAiWritingTools1803AutoFixRoutePatch = bytecodePatch(
    description = "Add 18.0.3 INTENT_AUTO_FIX to official Writing Tools v2 route set.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        findMutableMethodOrThrow(autoFixRouteBuilder).applyAutoFixRoute()
    }
}

internal val gboardAiWritingTools1803AutoFixAcceptancePatch = bytecodePatch(
    description = "Allow 18.0.3 MobileBERT verified INTENT_AUTO_FIX to pass V2 disambiguation.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        findMutableMethodOrThrow(autoFixAmbiguityGate).applyAutoFixAcceptance()
    }
}

internal val gboardAiWritingTools1803GenAiInitPatch = bytecodePatch(
    description = "Support transient UNKNOWN client type at 18.0.3 GenAI voice-edit initialization point.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrThrow(genAiVoiceEditInit).applyGenAiInitClientTypeCompatibility()
    }
}

internal val gboardAiWritingTools1803SmartEditInitPatch = bytecodePatch(
    description = "Support transient UNKNOWN client type at 18.0.3 SmartEdit initialization point.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrThrow(smartEditInit).applySmartEditInitClientTypeCompatibility()
    }
}

internal val gboardAiWritingTools1803GenAiRefreshPatch = bytecodePatch(
    description = "Retry missing GenAI manager after 18.0.3 Smart Dictation client refresh.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrThrow(genAiClientRefresh).applyGenAiClientRefreshRetry()
    }
}

private fun MutableMethod.applyAutoFixRoute() {
    val instructions = implementation?.instructions
        ?: error("No 18.0.3 Writing Tools v2 route builder implementation")
    check(implementation!!.registerCount == 3) {
        "18.0.3 Writing Tools v2 route builder register drift"
    }

    val existing = instructions.count { it.isFieldReference("Lzxi;->c:Lzxi;") }
    if (existing != 0) {
        check(existing == 1) { "Duplicate INTENT_AUTO_FIX route entries" }
        return
    }

    val builderInitIndices = instructions.indices.filter {
        instructions[it].isMethodReference("Lvvl;-><init>(I)V")
    }
    check(builderInitIndices.size == 1) {
        "Expected one exact Writing Tools v2 route-set builder initialization"
    }
    val builderInit = instructions[builderInitIndices.single()] as? FiveRegisterInstruction
    check(builderInit != null && builderInit.registerCount == 2 &&
        builderInit.registerC == 0 && builderInit.registerD == 1) {
        "18.0.3 Writing Tools v2 route-set builder register drift"
    }

    val freezeCalls = instructions.count { it.isMethodReference("Lvvr;->g()Lvvw;") }
    check(freezeCalls == 1) {
        "Expected one exact Writing Tools v2 route-set freeze call"
    }
    addInstructions(
        builderInitIndices.single() + 1,
        """
            sget-object v1, Lzxi;->c:Lzxi;
            invoke-virtual {v0, v1}, Lvvr;->h(Ljava/lang/Object;)V
        """.trimIndent(),
    )
}

private fun MutableMethod.applyAutoFixAcceptance() {
    val instructions = implementation?.instructions
        ?: error("No 18.0.3 Writing Tools ambiguity implementation")
    check(implementation!!.registerCount == 6) {
        "18.0.3 Writing Tools ambiguity register drift"
    }

    val existing = instructions.count { it.isFieldReference("Lzxi;->c:Lzxi;") }
    if (existing != 0) {
        check(existing == 1) { "Duplicate INTENT_AUTO_FIX ambiguity delegates" }
        return
    }

    val continuation = instructions.first()
    addInstructionsWithLabels(
        0,
        """
            iget-object v0, p1, Lgwo;->a:Lzxi;
            sget-object v1, Lzxi;->c:Lzxi;
            if-ne v0, v1, :jasondev_auto_fix_stock_ambiguity
            sget-object v0, Llyg;->e:Llyg;
            return-object v0
        """.trimIndent(),
        ExternalLabel("jasondev_auto_fix_stock_ambiguity", continuation),
    )
}

private fun MutableMethod.applyGenAiInitClientTypeCompatibility() {
    val instructions = implementation?.instructions
        ?: error("No 18.0.3 GenAI voice-edit init implementation")
    check(implementation!!.registerCount == 7) {
        "18.0.3 GenAI voice-edit init register drift"
    }

    val rememberRuntimeReference = RuntimeAbiCatalog.abi(rememberGenAiInitRuntime).reference
    val rememberCalls = instructions.count { it.isMethodReference(rememberRuntimeReference) }
    if (rememberCalls == 0) {
        addInstructions(
            0,
            RuntimeCallEmitter.invoke(rememberGenAiInitRuntime, "p0, p1, p2"),
        )
    } else {
        check(rememberCalls == 1) { "Duplicate 18.0.3 GenAI init remember delegates" }
    }

    val runtimeReference = RuntimeAbiCatalog.abi(genAiInitClientTypeRuntime).reference
    val existing = instructions.count { it.isMethodReference(runtimeReference) }
    if (existing != 0) {
        check(existing == 1) { "Duplicate 18.0.3 GenAI init compatibility delegates" }
        return
    }

    val predicateIndices = instructions.indices.filter {
        instructions[it].isMethodReference("Lunb;->cH(Lknm;)Z")
    }
    check(predicateIndices.size == 1) {
        "Expected one exact 18.0.3 GenAI client-type predicate"
    }
    val predicateIndex = predicateIndices.single()
    val predicate = instructions[predicateIndex] as? FiveRegisterInstruction
    check(predicate != null && predicate.registerCount == 1 && predicate.registerC == 0) {
        "18.0.3 GenAI client-type predicate register drift"
    }
    val moveResult = instructions.getOrNull(predicateIndex + 1) as? OneRegisterInstruction
    check(moveResult != null && moveResult.opcode == Opcode.MOVE_RESULT &&
        moveResult.registerA == 0) {
        "18.0.3 GenAI client-type predicate result drift"
    }

    val clientProducerIndices = instructions.indices.filter { index ->
        index < predicateIndex &&
            instructions[index].isFieldReference("Lidr;->j:Lknm;") &&
            (instructions[index] as? TwoRegisterInstruction)?.let { producer ->
                producer.opcode == Opcode.IGET_OBJECT && producer.registerA == 0
            } == true
    }
    check(clientProducerIndices.size == 1) {
        "Expected one exact 18.0.3 GenAI client-type producer"
    }

    addInstructions(
        predicateIndex + 2,
        """
            ${RuntimeCallEmitter.invoke(genAiInitClientTypeRuntime, "v1, v0")}
            move-result v0
        """.trimIndent(),
    )
    addInstructions(clientProducerIndices.single() + 1, "move-object v1, v0")
}

private fun MutableMethod.applyGenAiClientRefreshRetry() {
    val instructions = implementation?.instructions
        ?: error("No 18.0.3 GenAI client refresh implementation")
    check(implementation!!.registerCount == 4) {
        "18.0.3 GenAI client refresh register drift"
    }

    val beginReference = RuntimeAbiCatalog.abi(beginGenAiRefreshRuntime).reference
    val observeReference = RuntimeAbiCatalog.abi(observeGenAiRefreshRuntime).reference
    val finishReference = RuntimeAbiCatalog.abi(finishGenAiRefreshRuntime).reference
    val beginCalls = instructions.count { it.isMethodReference(beginReference) }
    val observeCalls = instructions.count { it.isMethodReference(observeReference) }
    val finishCalls = instructions.count { it.isMethodReference(finishReference) }
    if (beginCalls + observeCalls + finishCalls != 0) {
        check(beginCalls == 1 && observeCalls == 1 && finishCalls == 1) {
            "Incomplete 18.0.3 GenAI client refresh delegates"
        }
        return
    }

    val predicateIndices = instructions.indices.filter {
        instructions[it].isMethodReference("Lunb;->cH(Lknm;)Z")
    }
    check(predicateIndices.size == 1) {
        "Expected one exact 18.0.3 refresh client-type predicate"
    }
    val predicateIndex = predicateIndices.single()
    val predicate = instructions[predicateIndex] as? FiveRegisterInstruction
    check(predicate != null && predicate.registerCount == 1 && predicate.registerC == 0) {
        "18.0.3 refresh client-type predicate register drift"
    }
    val clientProducerIndices = instructions.indices.filter { index ->
        index < predicateIndex &&
            instructions[index].isFieldReference("Lidr;->j:Lknm;") &&
            (instructions[index] as? TwoRegisterInstruction)?.let { producer ->
                producer.opcode == Opcode.IGET_OBJECT && producer.registerA == 0
            } == true
    }
    check(clientProducerIndices.size == 1) {
        "Expected one exact 18.0.3 refresh client-type producer"
    }
    val returns = instructions.indices.filter { instructions[it].opcode == Opcode.RETURN_VOID }
    check(returns.size == 1) { "Expected one 18.0.3 refresh RETURN_VOID" }

    addInstructions(
        returns.single(),
        RuntimeCallEmitter.invoke(finishGenAiRefreshRuntime, "p0"),
    )
    addInstructions(
        clientProducerIndices.single() + 1,
        RuntimeCallEmitter.invoke(observeGenAiRefreshRuntime, "p0, v0"),
    )
    addInstructions(0, RuntimeCallEmitter.invoke(beginGenAiRefreshRuntime, "p0"))
}

private fun MutableMethod.applySmartEditInitClientTypeCompatibility() {
    val instructions = implementation?.instructions
        ?: error("No 18.0.3 SmartEdit init implementation")
    check(implementation!!.registerCount == 19) {
        "18.0.3 SmartEdit init register drift"
    }

    val runtimeReference = RuntimeAbiCatalog.abi(smartEditInitClientTypeRuntime).reference
    val existing = instructions.count { it.isMethodReference(runtimeReference) }
    if (existing != 0) {
        check(existing == 1) { "Duplicate 18.0.3 SmartEdit init compatibility delegates" }
        return
    }

    val predicateIndices = instructions.indices.filter {
        instructions[it].isMethodReference("Lunb;->cG(Lknm;)Z")
    }
    check(predicateIndices.size == 1) {
        "Expected one exact 18.0.3 SmartEdit client-type predicate"
    }
    val predicateIndex = predicateIndices.single()
    val predicate = instructions[predicateIndex] as? FiveRegisterInstruction
    check(predicate != null && predicate.registerCount == 1 && predicate.registerC == 10) {
        "18.0.3 SmartEdit client-type predicate register drift"
    }
    val moveResult = instructions.getOrNull(predicateIndex + 1) as? OneRegisterInstruction
    check(moveResult != null && moveResult.opcode == Opcode.MOVE_RESULT &&
        moveResult.registerA == 10) {
        "18.0.3 SmartEdit client-type predicate result drift"
    }

    val clientProducerIndices = instructions.indices.filter { index ->
        index < predicateIndex &&
            instructions[index].isFieldReference("Lidr;->j:Lknm;") &&
            (instructions[index] as? TwoRegisterInstruction)?.let { producer ->
                producer.opcode == Opcode.IGET_OBJECT && producer.registerA == 10
            } == true
    }
    check(clientProducerIndices.size == 1) {
        "Expected one exact 18.0.3 SmartEdit client-type producer"
    }

    addInstructions(
        predicateIndex + 2,
        """
            ${RuntimeCallEmitter.invoke(smartEditInitClientTypeRuntime, "v11, v10")}
            move-result v10
        """.trimIndent(),
    )
    addInstructions(clientProducerIndices.single() + 1, "move-object v11, v10")
}
