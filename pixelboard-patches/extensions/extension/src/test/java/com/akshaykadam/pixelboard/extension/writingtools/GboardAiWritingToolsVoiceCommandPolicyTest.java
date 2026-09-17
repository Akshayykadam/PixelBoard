package com.akshaykadam.pixelboard.extension.writingtools;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GboardAiWritingToolsVoiceCommandPolicyTest {
    @Test
    public void requiredLanguagesAreMergedWithoutDeletingOrReorderingStockValues() {
        List<String> stock = new ArrayList<String>(Arrays.asList("en", "fr", "pt"));

        List<String> merged = GboardAiWritingToolsVoiceCommandPolicy
                .mergeRequiredLanguages(stock);

        Assert.assertEquals(
                Arrays.asList("en", "fr", "pt", "de", "es", "it", "ja"),
                merged);
        Assert.assertEquals(Arrays.asList("en", "fr", "pt"), stock);
    }

    @Test
    public void completeAndNullLanguageListsPreserveIdentity() {
        List<String> complete = new ArrayList<String>(
                Arrays.asList("en", "de", "es", "fr", "it", "ja"));

        Assert.assertSame(complete, GboardAiWritingToolsVoiceCommandPolicy
                .mergeRequiredLanguages(complete));
        Assert.assertNull(GboardAiWritingToolsVoiceCommandPolicy
                .mergeRequiredLanguages(null));
    }

    @Test
    public void transientClientTypePromotionIsExactAndScopedByFeature() {
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteUnknownClientType(true, false, "CLIENT_TYPE_UNKNOWN"));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteUnknownClientType(false, false, "CLIENT_TYPE_UNKNOWN"));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteUnknownClientType(true, true, "CLIENT_TYPE_UNKNOWN"));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteUnknownClientType(true, false, "CLIENT_TYPE_REGULAR_DICTATION"));
    }

    @Test
    public void genAiRetryRequiresAnExactSmartDictationRefresh() {
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandPolicy
                .isSmartDictationClientType("CLIENT_TYPE_SMART_DICTATION"));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .isSmartDictationClientType("CLIENT_TYPE_REGULAR_DICTATION"));
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandPolicy.shouldRetryGenAiManager(
                true, true, true, true, false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy.shouldRetryGenAiManager(
                true, false, true, true, false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy.shouldRetryGenAiManager(
                true, true, false, true, false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy.shouldRetryGenAiManager(
                true, true, true, true, true));
    }

    @Test
    public void smartEditDependenciesChangeOnlyTheExactMissingValues() {
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteModulePredicate(
                        true,
                        "enable_nga_lab_modeless_smartedit",
                        false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteModulePredicate(true, "unrelated", false));
        Assert.assertEquals(Boolean.TRUE, GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideClassifierModelessFlag(true, Boolean.FALSE));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideClassifierModelessFlag(true, Boolean.TRUE));
        Assert.assertEquals("v3", GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideRegexVersion(true, ""));
        Assert.assertEquals("v4", GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideRegexVersion(true, "v4"));
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideRegexVersion(true, Boolean.FALSE));
    }

    @Test
    public void commandAmbiguityCapabilityOnlyPromotesFalseWhenEnabled() {
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteCommandAmbiguityCapability(true, false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteCommandAmbiguityCapability(false, false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteCommandAmbiguityCapability(true, true));
    }
}
