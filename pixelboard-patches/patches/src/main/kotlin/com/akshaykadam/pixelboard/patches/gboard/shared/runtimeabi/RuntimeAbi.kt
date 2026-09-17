package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

internal enum class RuntimeCallId(internal val encodedAbi: String) {

    ADVANCED_VOICE_RUNTIME_AFTER_FLAG_VALUE("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->afterFlagValue(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
    ADVANCED_VOICE_RUNTIME_AFTER_1803_NATIVE_SPLIT_READINESS("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->after1803NativeSplitReadiness(Z)Z"),
    ADVANCED_VOICE_RUNTIME_AFTER_MDD_PROVIDER_CONSTRUCTED("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->afterMddProviderConstructed(Ljava/lang/Object;)V"),
    ADVANCED_VOICE_RUNTIME_BEFORE_FORMATTER_CONSTRUCTED("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->beforeFormatterConstructed(Ljava/util/Locale;Ljava/lang/Object;Z)Z"),
    ADVANCED_VOICE_RUNTIME_INCLUDE_EXACT_ZH_TW_SUPPORTED_LOCALE("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->includeExactZhTwSupportedLocale(Ljava/lang/Object;)Ljava/lang/Object;"),
    ADVANCED_VOICE_RUNTIME_SEED_APPLICATION_CONTEXT("Lcom/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime;->seedApplicationContext(Landroid/content/Context;)V"),
    RAMBLER_RUNTIME_ENTER_DEFAULT_SELECTION_SUPPRESSION("Lcom/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime;->enterDefaultSelectionSuppression()V"),
    RAMBLER_RUNTIME_ENTER_VOICE_SETTINGS_SCOPE("Lcom/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime;->enterVoiceSettingsScope()V"),
    RAMBLER_RUNTIME_EXIT_DEFAULT_SELECTION_SUPPRESSION("Lcom/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime;->exitDefaultSelectionSuppression()V"),
    RAMBLER_RUNTIME_EXIT_VOICE_SETTINGS_SCOPE("Lcom/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime;->exitVoiceSettingsScope()V"),
    RAMBLER_RUNTIME_UPDATE_OFFICIAL_SELECTION("Lcom/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime;->updateOfficialSelection(Z)V"),
    AI_WRITING_TOOLS_RUNTIME_APPLY_OVERRIDDEN_FLAG_VALUE("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsRuntime;->applyOverriddenFlagValue(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_APPLY_GEN_AI_INIT_CLIENT_TYPE("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->applyGenAiInitClientType(Ljava/lang/Object;Z)Z"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_APPLY_SMART_EDIT_INIT_CLIENT_TYPE("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->applySmartEditInitClientType(Ljava/lang/Object;Z)Z"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_BEGIN_GEN_AI_REFRESH("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->beginGenAiRefresh(Ljava/lang/Object;)V"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_FINISH_GEN_AI_REFRESH("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->finishGenAiRefresh(Ljava/lang/Object;)V"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_OBSERVE_GEN_AI_REFRESH_CLIENT_TYPE("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->observeGenAiRefreshClientType(Ljava/lang/Object;Ljava/lang/Object;)V"),
    AI_WRITING_TOOLS_VOICE_COMMAND_RUNTIME_REMEMBER_GEN_AI_INIT_CALL("Lcom/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsVoiceCommandRuntime;->rememberGenAiInitCall(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"),
}

internal data class RuntimeAbi(
    val owner: String,
    val name: String,
    val parameters: List<String>,
    val returnType: String,
    val isStatic: Boolean,
) {
    val descriptor: String
        get() = parameters.joinToString(prefix = "(", postfix = ")$returnType", separator = "")

    val reference: String
        get() = "$owner->$name$descriptor"

    companion object {
        fun decode(encoded: String): RuntimeAbi {
            val owner = encoded.substringBefore("->")
            val method = encoded.substringAfter("->")
            val name = method.substringBefore('(')
            val parametersDescriptor = method.substringAfter('(').substringBefore(')')
            return RuntimeAbi(
                owner = owner,
                name = name,
                parameters = decodeTypes(parametersDescriptor),
                returnType = method.substringAfter(')'),
                isStatic = true,
            )
        }

        private fun decodeTypes(descriptor: String): List<String> = buildList {
            var index = 0
            while (index < descriptor.length) {
                val start = index
                while (descriptor[index] == '[') index++
                if (descriptor[index] == 'L') {
                    index = descriptor.indexOf(';', index) + 1
                    require(index > 0) { "Malformed object type in runtime ABI: $descriptor" }
                } else {
                    index++
                }
                add(descriptor.substring(start, index))
            }
        }
    }
}

internal object RuntimeAbiCatalog {
    private val abiByCall = RuntimeCallId.entries.associateWith { call ->
        RuntimeAbi.decode(call.encodedAbi)
    }

    fun abi(call: RuntimeCallId): RuntimeAbi = abiByCall.getValue(call)
}
