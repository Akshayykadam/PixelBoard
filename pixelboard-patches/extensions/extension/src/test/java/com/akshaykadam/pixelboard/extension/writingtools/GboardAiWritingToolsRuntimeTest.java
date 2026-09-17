package com.akshaykadam.pixelboard.extension.writingtools;

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
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_ON_SELECTED_TEXT,
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

        String proofread = "202406101250_prod_sd_config";
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
