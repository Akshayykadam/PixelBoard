package com.akshaykadam.pixelboard.extension.writingtools;

import com.akshaykadam.pixelboard.extension.settings.GboardGoogleSansSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardUndoSettings;

import org.junit.Assert;
import org.junit.Test;

public final class GboardAiWritingToolsRuntimeTest {
    @Test
    public void masterOffPreservesEveryStockResultIdentity() {
        Object original = Long.valueOf(3L);

        Assert.assertSame(original, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                original,
                settings(false, true,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void targetBooleanFlagsUseTheFormalStockCapabilityTuple() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(false);
        GboardAiWritingToolsOfficialPreferences.Snapshot proofreadOff = official(false, true);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                Boolean.TRUE,
                settings,
                proofreadOff));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                Boolean.FALSE,
                settings,
                proofreadOff));

        GboardAiWritingToolsOfficialPreferences.Snapshot writingToolsOff = official(true, false);
        String[] editingFlags = new String[]{
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_HELPER_ENABLE_TEXT_STYLIZATION_INTERNAL
        };
        for (String flag : editingFlags) {
            Assert.assertSame(flag, Boolean.TRUE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            flag,
                            Boolean.TRUE,
                            settings,
                            writingToolsOff));
        }
        Assert.assertSame(Boolean.TRUE,
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE,
                        Boolean.FALSE,
                        settings,
                        writingToolsOff));
        Assert.assertSame(Boolean.TRUE,
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_ON_SELECTED_TEXT,
                        Boolean.FALSE,
                        settings,
                        writingToolsOff));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER,
                Boolean.FALSE,
                settings,
                writingToolsOff));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_FOR_MINORS,
                Boolean.FALSE,
                settings,
                writingToolsOff));
    }

    @Test
    public void voiceCommandRolloutFlagIsEnabledWithoutChangingWrongTypes() {
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS,
                Boolean.FALSE,
                serverSettings(false),
                official(false, false)));

        Object wrongType = "false";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS,
                wrongType,
                serverSettings(false),
                official(true, true)));
    }

    @Test
    public void unknownOfficialValuesStayEnabledForReachability() {
        GboardAiWritingToolsOfficialPreferences.Snapshot unknown = official(null, null);

        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                Boolean.FALSE,
                serverSettings(false),
                unknown));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                Boolean.FALSE,
                serverSettings(false),
                unknown));
    }

    @Test
    public void allKeyboardsChangesOnlyTwoStringAllowlists() {
        GboardAiWritingToolsSettings.Snapshot enabled = serverSettings(true);
        Assert.assertEquals("*", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS,
                "en-US",
                enabled,
                official(true, true)));
        Assert.assertEquals("*", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_LLM_PC_SUPPORTED_LANGUAGE_TAGS,
                "en-US",
                enabled,
                official(true, true)));

        Object wrongType = Integer.valueOf(7);
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS,
                wrongType,
                enabled,
                official(true, true)));
    }

    @Test
    public void renamedServerPackageUsesModelConfigs() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(false);

        String proofread = "202408051448_prod_sd_config";
        Assert.assertEquals("202408051448_prod_sd_config",
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                        proofread,
                        settings,
                        official(true, true),
                        "com.akshaykadam.pixelboard"));
        String stylization = "composer_stylization_base";
        Assert.assertEquals("202504090000_writing_tools_config",
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime
                                .FLAG_WRITING_HELPER_TEXT_STYLIZATION_MODEL_VERSION,
                        stylization,
                        settings,
                        official(true, true),
                        "com.akshaykadam.pixelboard"));
    }

    @Test
    public void officialServerPackagePreservesItsRemoteModelConfigs() {
        String stockModel = "official-remote-model";
        Assert.assertSame(stockModel, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                stockModel,
                serverSettings(false),
                official(true, true),
                "com.google.android.inputmethod.latin"));
    }

    @Test
    public void serverModelConfigsPreserveWrongTypeIdentity() {
        Object wrongType = Integer.valueOf(7);

        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                wrongType,
                serverSettings(false),
                official(true, true),
                "com.akshaykadam.pixelboard"));
    }

    @Test
    public void privateBackendsPreserveTheirStockModelConfigs() {
        String stockModel = "private-stock-model";

        Assert.assertSame(stockModel, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                stockModel,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_AICORE),
                official(true, true)));
    }

    @Test
    public void unrelatedAndWrongTypesStayUnchanged() {
        Object unrelated = new Object();
        Assert.assertSame(unrelated, GboardAiWritingToolsRuntime.computeOverrideValue(
                "unrelated_flag",
                unrelated,
                serverSettings(false),
                official(true, true)));

        Object wrongType = "false";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                wrongType,
                serverSettings(false),
                official(false, false)));

    }

    @Test
    public void distinctFalseOfficialBooleanIsOverridden() throws Exception {
        Boolean distinctFalse = distinctBoolean(false);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                distinctFalse,
                serverSettings(false),
                official(true, true)));
    }

    @Test
    public void distinctTrueOfficialBooleanIsOverridden() throws Exception {
        Boolean distinctTrue = distinctBoolean(true);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                distinctTrue,
                serverSettings(false),
                official(true, false)));
    }

    @Test
    public void modelessRegexPromotesOnlyAnEmptyString() {
        Assert.assertEquals("v3", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_MODELESS_SMART_EDIT_REGEX_VERSION,
                "",
                serverSettings(false),
                official(false, false)));
        String stock = "v4";
        Assert.assertSame(stock, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_MODELESS_SMART_EDIT_REGEX_VERSION,
                stock,
                serverSettings(false),
                official(false, false)));
    }

    @Test
    public void backendDecisionReturnsOneAtomicThreeValueTuple() {
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER,
                1L,
                false,
                false);
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_AICORE,
                2L,
                true,
                true);
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA,
                3L,
                true,
                false);
        assertBackend("INVALID", 1L, false, false);
    }

    @Test
    public void backendFlagsPreserveWrongTypeIdentity() {
        GboardAiWritingToolsSettings.Snapshot astrea = settings(
                true,
                false,
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA);
        Assert.assertEquals(Long.valueOf(3L), GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                Long.valueOf(1L),
                astrea,
                official(true, true)));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_HYBRID,
                Boolean.FALSE,
                astrea,
                official(true, true)));
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ON_DEVICE_PROOFREAD,
                Boolean.FALSE,
                astrea,
                official(true, true)));

        Integer wrongLongType = Integer.valueOf(1);
        Assert.assertSame(wrongLongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                wrongLongType,
                astrea,
                official(true, true)));
    }

    @Test
    public void distinctFalseBackendBooleanIsOverridden() throws Exception {
        Boolean distinctFalse = distinctBoolean(false);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_HYBRID,
                distinctFalse,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void distinctTrueBackendBooleanIsOverridden() throws Exception {
        Boolean distinctTrue = distinctBoolean(true);
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ON_DEVICE_PROOFREAD,
                distinctTrue,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void writingToolsV2EnabledOnSupportedDeviceWhenToggleActive() {
        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(true);
        try {
            GboardAiWritingToolsSettings.Snapshot settings = new GboardAiWritingToolsSettings.Snapshot(
                    true, true, GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER, true);

            String[] v2Flags = new String[]{
                    GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2,
                    GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR,
                    GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_ZERO_STATE,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ALLOW_EMPTY_USER_INSTRUCTION,
                    GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS,
                    GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_MY_STYLE,
                    GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS
            };

            for (String flag : v2Flags) {
                Assert.assertSame(flag, Boolean.TRUE,
                        GboardAiWritingToolsRuntime.computeOverrideValue(
                                flag, Boolean.FALSE, settings, official(true, true)));
            }
        } finally {
            GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(null);
        }
    }

    @Test
    public void writingToolsV2DisabledOnUnsupportedDeviceEvenWhenToggleActive() {
        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(false);
        try {
            GboardAiWritingToolsSettings.Snapshot settings = new GboardAiWritingToolsSettings.Snapshot(
                    true, true, GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER, true);

            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2,
                            Boolean.TRUE, settings, official(true, true)));
            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE,
                            Boolean.TRUE, settings, official(true, true)));
        } finally {
            GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(null);
        }
    }

    @Test
    public void writingToolsV2DisabledOnSupportedDeviceWhenToggleInactive() {
        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(true);
        try {
            GboardAiWritingToolsSettings.Snapshot settings = new GboardAiWritingToolsSettings.Snapshot(
                    true, true, GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER, false);

            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2,
                            Boolean.TRUE, settings, official(true, true)));
            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE,
                            Boolean.TRUE, settings, official(true, true)));
        } finally {
            GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(null);
        }
    }

    static class TestFlagReceiver {
        private final String a;

        TestFlagReceiver(String name) {
            this.a = name;
        }
    }

    @Test
    public void autoCorrectionAndDecoderFlagsAreEnforced() {
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_DECODER_RESPONSE_WAIT_WITHOUT_LOCK));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_TRY_INITIALIZE_DECODER_AFTER_LM_LOADS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_PK_AUTO_CAPITALIZATION_USE_AUTO_CORRECTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_AUTO_CORRECTION_STATS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_CORRECTION_COMMIT_ANIMATION));

        Object delayResult = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_DELAY_TO_WAIT_FOR_SUGGESTIONS_BEFORE_COMMIT_MS),
                Long.valueOf(40L));
        Assert.assertEquals(Long.valueOf(150L), delayResult);

        Object webTextResult = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_WEB_TEXT),
                "*");
        Assert.assertEquals("", webTextResult);

        Object appsResult = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION),
                "com.example.app");
        Assert.assertEquals("", appsResult);

        Object specialTypesResult = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_SPECIAL_TYPES),
                "com.example.special");
        Assert.assertEquals("", specialTypesResult);
    }

    @Test
    public void quickEmojiAndMagicEmojiFlagsAreEnforcedAndConfigured() {
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_FAST_ACCESS_BAR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY_SETTINGS_OPTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY_SHORTCUT_WHEN_FOLLOWED_BY_EMOJI));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY_WHEN_TALKBACK_ON));

        Object fastAccessWhitelist = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_FAST_ACCESS_BAR_APP_WHITELIST),
                "com.whatsapp");
        Assert.assertEquals("*", fastAccessWhitelist);

        Object emojifyLanguages = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY_LANGUAGE_TAGS),
                "-");
        Assert.assertEquals("*", emojifyLanguages);

        Object emojifyApps = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_EMOJIFY_APP_ALLOWLIST),
                "-");
        Assert.assertEquals("*", emojifyApps);

        Object emojifyPatterns = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_EMOJIFY_ENABLED_PATTERNS_IN_ORDER),
                "1");
        Assert.assertEquals("1,2,3", emojifyPatterns);

        Object emojifyModel = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(
                        GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJIFY_MODEL),
                Boolean.TRUE);
        Assert.assertEquals(Boolean.FALSE, emojifyModel);
    }

    @Test
    public void googleSansAndUndoFlagsAreEnforcedHeadlessByDefault() {
        Object googleSansFlex = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardGoogleSansSettings.FLAG_GOOGLE_SANS_FLEX),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, googleSansFlex);

        Object platformGoogleSans = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardGoogleSansSettings.FLAG_PLATFORM_GOOGLE_SANS_FLEX),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, platformGoogleSans);

        Object gm3Expressive = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardGoogleSansSettings.FLAG_GM3_EXPRESSIVE),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, gm3Expressive);

        Object undoPostCorrection = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardUndoSettings.FLAG_UNDO_PROACTIVE_POST_CORRECTION),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, undoPostCorrection);

        Object undoAutoCorrection = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardUndoSettings.FLAG_UNDO_PROACTIVE_AUTO_CORRECTION_COOPERATIVE),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, undoAutoCorrection);

        Object undoChunkFusion = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardUndoSettings.FLAG_UNDO_ENABLE_CHUNK_FUSION),
                Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, undoChunkFusion);

        Object undoThreshold = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardUndoSettings.FLAG_UNDO_PROMOTION_BANNER_THRESHOLD),
                Long.valueOf(3L));
        Assert.assertEquals(Long.valueOf(1L), undoThreshold);

        Object undoStopDeferred = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new TestFlagReceiver(GboardUndoSettings.FLAG_UNDO_STOP_DEFERRED_RANGE_NOTIFICATION),
                Boolean.TRUE);
        Assert.assertEquals(Boolean.FALSE, undoStopDeferred);
    }

    private static Boolean distinctBoolean(boolean value) throws Exception {
        return Boolean.class.getDeclaredConstructor(boolean.class).newInstance(value);
    }

    private static void assertBackend(String backend, long type, boolean hybrid,
            boolean onDeviceProofread) {
        GboardAiWritingToolsRuntime.BackendDecision decision =
                GboardAiWritingToolsRuntime.backendDecision(backend);
        Assert.assertEquals(Long.valueOf(type), decision.backendType);
        Assert.assertEquals(Boolean.valueOf(hybrid), decision.hybridEnabled);
        Assert.assertEquals(Boolean.valueOf(onDeviceProofread),
                decision.onDeviceProofreadEnabled);
    }

    @Test
    public void textSelectionCooperativeAndHelperFlagsAreEnabledForInlineProofreadSuggestions() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(false);
        GboardAiWritingToolsOfficialPreferences.Snapshot preferences = official(true, true);

        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE,
                Boolean.FALSE,
                settings,
                preferences));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_ON_SELECTED_TEXT,
                Boolean.FALSE,
                settings,
                preferences));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_ENABLE_STABLE_ENTRANCE,
                Boolean.FALSE,
                settings,
                preferences));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_REPLACE_BUTTON,
                Boolean.FALSE,
                settings,
                preferences));
    }

    private static GboardAiWritingToolsSettings.Snapshot serverSettings(
            boolean allKeyboardsEnabled) {
        return settings(
                true,
                allKeyboardsEnabled,
                GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER);
    }

    private static GboardAiWritingToolsSettings.Snapshot settings(boolean featureEnabled,
            boolean allKeyboardsEnabled, String backendType) {
        return new GboardAiWritingToolsSettings.Snapshot(
                featureEnabled,
                allKeyboardsEnabled,
                backendType);
    }

    private static GboardAiWritingToolsOfficialPreferences.Snapshot official(
            Boolean proofreadEnabled, Boolean writingToolsEnabled) {
        return new GboardAiWritingToolsOfficialPreferences.Snapshot(
                proofreadEnabled,
                writingToolsEnabled);
    }
}
