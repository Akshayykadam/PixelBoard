package com.akshaykadam.pixelboard.extension.writingtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GboardAiWritingToolsVoiceCommandPolicy {
    public static final String VOICE_COMMAND_LANGUAGES_FLAG =
            "writing_tools_voice_commands_i18n_languages";
    public static final String MODELESS_SMART_EDIT_FLAG =
            "enable_nga_lab_modeless_smartedit";
    public static final String MODELESS_SMART_EDIT_REGEX_VERSION_FLAG =
            "nga_lab_modeless_smartedit_regex_version";
    public static final String UNKNOWN_CLIENT_TYPE = "CLIENT_TYPE_UNKNOWN";
    public static final String SMART_DICTATION_CLIENT_TYPE =
            "CLIENT_TYPE_SMART_DICTATION";
    public static final String DEFAULT_REGEX_VERSION = "v3";

    private static final List<String> REQUIRED_LANGUAGES =
            Collections.unmodifiableList(Arrays.asList("de", "es", "fr", "it", "ja"));

    private GboardAiWritingToolsVoiceCommandPolicy() {
    }

    public static List<String> mergeRequiredLanguages(List<String> originalLanguages) {
        if (originalLanguages == null || originalLanguages.containsAll(REQUIRED_LANGUAGES)) {
            return originalLanguages;
        }
        List<String> merged = new ArrayList<String>(originalLanguages);
        for (String language : REQUIRED_LANGUAGES) {
            if (!merged.contains(language)) {
                merged.add(language);
            }
        }
        return merged;
    }

    public static boolean shouldPromoteUnknownClientType(
            boolean featureEnabled,
            boolean stockResult,
            String clientTypeName) {
        return featureEnabled
                && !stockResult
                && UNKNOWN_CLIENT_TYPE.equals(clientTypeName);
    }

    public static boolean isSmartDictationClientType(String clientTypeName) {
        return SMART_DICTATION_CLIENT_TYPE.equals(clientTypeName);
    }

    public static boolean shouldRetryGenAiManager(
            boolean featureEnabled,
            boolean sawSmartClientType,
            boolean managerMissing,
            boolean initCallAvailable,
            boolean alreadyAttempted) {
        return featureEnabled
                && sawSmartClientType
                && managerMissing
                && initCallAvailable
                && !alreadyAttempted;
    }

    public static boolean shouldPromoteModulePredicate(
            boolean featureEnabled,
            String flagName,
            boolean stockResult) {
        return featureEnabled
                && !stockResult
                && MODELESS_SMART_EDIT_FLAG.equals(flagName);
    }

    public static boolean shouldPromoteCommandAmbiguityCapability(
            boolean featureEnabled,
            boolean stockValue) {
        return featureEnabled && !stockValue;
    }

    public static Object maybeOverrideClassifierModelessFlag(
            boolean featureEnabled,
            Object stockResult) {
        return featureEnabled && Boolean.FALSE.equals(stockResult)
                ? Boolean.TRUE
                : stockResult;
    }

    public static Object maybeOverrideRegexVersion(
            boolean featureEnabled,
            Object stockResult) {
        return featureEnabled
                && stockResult instanceof String
                && ((String) stockResult).isEmpty()
                ? DEFAULT_REGEX_VERSION
                : stockResult;
    }
}
