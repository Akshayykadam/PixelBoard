package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.akshaykadam.pixelboard.extension.settings.GboardEmojiSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardGoogleSansSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardRoundedKeysSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardUndoSettings;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public final class GboardAiWritingToolsRuntimeLoggingTest {
    private static final String TAG = "GboardPatches";

    @Before
    @After
    public void resetRuntimeState() throws Exception {
        while (GboardAiWritingToolsRuntime.currentSettingsControllerScopeDepth() > 0) {
            GboardAiWritingToolsRuntime.exitSettingsControllerScope();
        }
        staticField("applicationContext").set(null, null);
        GboardAiWritingToolsOfficialPreferences.clearForTest();
        for (Field field : GboardAiWritingToolsRuntime.class.getDeclaredFields()) {
            if (field.getType() == AtomicInteger.class) {
                field.setAccessible(true);
                ((AtomicInteger) field.get(null)).set(0);
            } else if (field.getType() == java.util.concurrent.atomic.AtomicBoolean.class) {
                field.setAccessible(true);
                ((java.util.concurrent.atomic.AtomicBoolean) field.get(null)).set(false);
            }
        }
        ShadowLog.clear();
    }

    @Test
    public void persistentFailureLoggingIsBoundedPerSite() throws Exception {
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());
        GboardAiWritingToolsRuntime.enterSettingsControllerScope();

        for (int index = 0; index < 85; index++) {
            Assert.assertSame(Boolean.FALSE,
                    GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                            new Object(), Boolean.FALSE));
            Assert.assertFalse(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                    GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY));
            Assert.assertFalse(GboardAiWritingToolsRuntime.applySignalResult(
                    new Object(), false));
        }

        Assert.assertEquals(80L, warningCount("failed to apply flag override"));
        Assert.assertEquals(80L, warningCount("failed to evaluate settings removal"));
        Assert.assertEquals(80L, warningCount("failed to evaluate signal"));
    }

    @Test
    public void successfulFlagLogsDoNotConsumeTheFailureBudget() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = context.getSharedPreferences(
                GboardAiWritingToolsSettings.PREF_FILE, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        staticField("applicationContext").set(null, context);

        for (int index = 0; index < 80; index++) {
            Assert.assertSame(Boolean.TRUE,
                    GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                            new FlagReceiver(), Boolean.FALSE));
        }
        ShadowLog.clear();
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());

        Assert.assertSame(Boolean.FALSE,
                GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                        new Object(), Boolean.FALSE));
        Assert.assertEquals(1L, warningCount("failed to apply flag override"));
    }

    @Test
    public void successfulSignalLogsDoNotConsumeTheFailureBudget() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = context.getSharedPreferences(
                GboardAiWritingToolsSettings.PREF_FILE, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        staticField("applicationContext").set(null, context);
        Object forcedSignal = Class.forName("gvu").getField("a").get(null);

        for (int index = 0; index < 80; index++) {
            Assert.assertTrue(GboardAiWritingToolsRuntime.applySignalResult(
                    forcedSignal, false));
        }
        ShadowLog.clear();
        staticField("applicationContext").set(null, new ThrowingSharedPreferencesContext());

        Assert.assertFalse(GboardAiWritingToolsRuntime.applySignalResult(
                new Object(), false));
        Assert.assertEquals(1L, warningCount("failed to evaluate signal"));
    }

    @Test
    public void failureSitesUseTheBoundedLogger() throws Exception {
        String source = new String(Files.readAllBytes(Path.of(
                "src/main/java/com/akshaykadam/pixelboard/extension/writingtools/"
                        + "GboardAiWritingToolsRuntime.java")), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        Assert.assertTrue(source.contains(
                "logLimited(FLAG_FAILURE_LOG_COUNT,\n"
                        + "                    \"failed to apply flag override\", throwable);"));
        Assert.assertTrue(source.contains(
                "logLimited(SETTINGS_REMOVAL_LOG_COUNT,\n"
                        + "                    \"failed to evaluate settings removal\", throwable);"));
        Assert.assertTrue(source.contains(
                "logLimited(SIGNAL_FAILURE_LOG_COUNT,\n"
                        + "                    \"failed to evaluate signal\", throwable);"));
        Assert.assertFalse(source.contains(
                "Log.w(TAG, LOG_PREFIX + \"failed to apply flag override\", throwable)"));
        Assert.assertFalse(source.contains(
                "Log.w(TAG, LOG_PREFIX + \"failed to evaluate settings removal\", throwable)"));
        Assert.assertFalse(source.contains(
                "safeLog(\"failed to evaluate signal\", throwable)"));
    }

    @Test
    public void deviceIntelligenceFlagEnforcesTrueAndLogsDiagnostics() throws Exception {
        DeviceIntelligenceFlagReceiver receiver = new DeviceIntelligenceFlagReceiver();

        // 1. Overriding Boolean.FALSE -> returns Boolean.TRUE and logs to GboardPatches tag
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));

        // 2. Already Boolean.TRUE -> returns Boolean.TRUE without redundant logging
        Object unchanged = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, unchanged);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));

        // 3. Non-boolean values are preserved
        Object wrongType = "disabled";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, wrongType));

        // 4. Logging is bounded (at most 80 warnings)
        for (int index = 0; index < 85; index++) {
            GboardAiWritingToolsRuntime.applyOverriddenFlagValue(receiver, Boolean.FALSE);
        }
        Assert.assertEquals(80L, warningCount(
                "flag=enable_device_intelligence, original=Boolean(false), enforced=Boolean(true)"));
    }

    @Test
    public void roundedKeysFlagsEnforceTrueWhenEnabledAndLogDiagnostics() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        staticField("applicationContext").set(null, context);
        SharedPreferences preferences = GboardRoundedKeysSettings.preferences(context);
        GboardRoundedKeysSettings.writeEnabled(preferences, false);

        RoundedKeysFlagReceiver receiver = new RoundedKeysFlagReceiver();

        // 1. When disabled, original value is preserved
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE));
        Assert.assertEquals(0L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 2. When enabled, Boolean.FALSE is overridden to Boolean.TRUE with diagnostics log
        GboardRoundedKeysSettings.writeEnabled(preferences, true);
        ShadowLog.clear();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 3. When already Boolean.TRUE, returns Boolean.TRUE without diagnostic warning
        ShadowLog.clear();
        Object unchanged = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, unchanged);
        Assert.assertEquals(0L, warningCount(
                "flag=belka_rounded_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // 4. Non-boolean values are preserved
        Object wrongType = "invalid";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, wrongType));
    }

    @Test
    public void googleSansFlagsEnforceTrueWhenEnabledAndFalseWhenDisabled() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        staticField("applicationContext").set(null, context);
        SharedPreferences preferences = GboardGoogleSansSettings.preferences(context);
        GboardGoogleSansSettings.writeEnabled(preferences, false);

        GoogleSansFlexFlagReceiver receiver = new GoogleSansFlexFlagReceiver();

        // 1. When disabled, returns Boolean.FALSE
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE));

        // 2. When enabled, Boolean.FALSE is overridden to Boolean.TRUE with diagnostics log
        GboardGoogleSansSettings.writeEnabled(preferences, true);
        ShadowLog.clear();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=google_sans_flex, original=Boolean(false), enforced=Boolean(true)"));
    }

    @Test
    public void undoFlagsEnforceActiveWhenEnabledAndDisabledWhenOff() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        staticField("applicationContext").set(null, context);
        SharedPreferences preferences = GboardUndoSettings.preferences(context);
        GboardUndoSettings.writeEnabled(preferences, false);

        UndoProactivePostCorrectionFlagReceiver postCorrReceiver =
                new UndoProactivePostCorrectionFlagReceiver();
        UndoPromotionBannerThresholdFlagReceiver thresholdReceiver =
                new UndoPromotionBannerThresholdFlagReceiver();

        // 1. When disabled, proactive post correction returns Boolean.FALSE
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                postCorrReceiver, Boolean.TRUE));

        // 2. When enabled, proactive post correction returns Boolean.TRUE with diagnostics log
        GboardUndoSettings.writeEnabled(preferences, true);
        ShadowLog.clear();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                postCorrReceiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=undo_proactive_post_correction, original=Boolean(false), enforced=Boolean(true)"));

        // 3. Banner threshold is overridden to 1L
        Object threshold = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                thresholdReceiver, 3L);
        Assert.assertEquals(1L, threshold);
    }

    @Test
    public void emojiScaleFlagEnforcesTrue() throws Exception {
        Object receiver = new EmojiScaleFlagReceiver();

        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);

        Object alreadyTrue = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE);
        Assert.assertSame(Boolean.TRUE, alreadyTrue);
    }

    @Test
    public void accessPointsFlagsEnforceTrueAndLogDiagnostics() throws Exception {
        AccessPointsMenuRedesignFlagReceiver redesignReceiver =
                new AccessPointsMenuRedesignFlagReceiver();
        AccessPointKeyboardFlagReceiver keyboardReceiver =
                new AccessPointKeyboardFlagReceiver();

        ShadowLog.clear();
        Object enforcedRedesign = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                redesignReceiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforcedRedesign);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_access_points_menu_redesign, original=Boolean(false), enforced=Boolean(true)"));

        ShadowLog.clear();
        Object enforcedKeyboard = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                keyboardReceiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforcedKeyboard);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_access_point_keyboard, original=Boolean(false), enforced=Boolean(true)"));

        // Already true
        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                redesignReceiver, Boolean.TRUE));
        Assert.assertEquals(0L, warningCount(
                "flag=enable_access_points_menu_redesign, original=Boolean(false), enforced=Boolean(true)"));

        // Non-boolean preserved
        String nonBool = "custom";
        Assert.assertSame(nonBool, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                redesignReceiver, nonBool));
    }

    @Test
    public void autofillImeIntegrationFlagEnforcesTrueAndLogsDiagnostics() throws Exception {
        AutofillImeIntegrationFlagReceiver receiver =
                new AutofillImeIntegrationFlagReceiver();

        ShadowLog.clear();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertSame(Boolean.TRUE, enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=enable_autofill_ime_integration, original=Boolean(false), enforced=Boolean(true)"));

        // Already true
        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.TRUE));
        Assert.assertEquals(0L, warningCount(
                "flag=enable_autofill_ime_integration, original=Boolean(false), enforced=Boolean(true)"));

        // Non-boolean preserved
        Long nonBool = 42L;
        Assert.assertSame(nonBool, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, nonBool));
    }

    @Test
    public void spellcheckerChipsUiFlagsEnforceTrueAndLogDiagnostics() throws Exception {
        SpellcheckerChipsUiFlagReceiver chipsUiReceiver =
                new SpellcheckerChipsUiFlagReceiver();
        SpellcheckerChipsUiWebViewFlagReceiver webViewReceiver =
                new SpellcheckerChipsUiWebViewFlagReceiver();
        SpellCheckerExtensionFlagReceiver extensionReceiver =
                new SpellCheckerExtensionFlagReceiver();

        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                chipsUiReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=enable_spellchecker_chips_ui, original=Boolean(false), enforced=Boolean(true)"));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                chipsUiReceiver, Boolean.TRUE));

        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                webViewReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=enable_spellchecker_chips_ui_web_view, original=Boolean(false), enforced=Boolean(true)"));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                webViewReceiver, Boolean.TRUE));

        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                extensionReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=enable_spell_checker_extension, original=Boolean(false), enforced=Boolean(true)"));

        WritingHelperChipInSpellcheckerFlagReceiver writingHelperReceiver =
                new WritingHelperChipInSpellcheckerFlagReceiver();
        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                writingHelperReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=writing_helper_chip_in_spellchecker, original=Boolean(false), enforced=Boolean(true)"));

        SpellCheckerHighlightOnWebInputFlagReceiver webInputReceiver =
                new SpellCheckerHighlightOnWebInputFlagReceiver();
        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                webInputReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=enable_spell_checker_highlight_on_web_input, original=Boolean(false), enforced=Boolean(true)"));

        SpellCheckerTrainingCacheFlagReceiver trainingCacheReceiver =
                new SpellCheckerTrainingCacheFlagReceiver();
        ShadowLog.clear();
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                trainingCacheReceiver, Boolean.FALSE));
        Assert.assertEquals(1L, warningCount(
                "flag=enable_spell_checker_training_cache, original=Boolean(false), enforced=Boolean(true)"));

        // Non-boolean preserved
        Integer nonBool = 123;
        Assert.assertSame(nonBool, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                chipsUiReceiver, nonBool));
    }

    @Test
    public void superInsertPersonalContextFlagsGuardAgainstMissingBundleHint() throws Exception {
        SuperInsertBrowsingHistoryFlagReceiver browsingHistoryReceiver =
                new SuperInsertBrowsingHistoryFlagReceiver();
        SuperInsertNestaFlagReceiver nestaReceiver =
                new SuperInsertNestaFlagReceiver();

        try {
            // When PersonalContext BundleHint is not supported on the platform (e.g. Android 16 and lower)
            GboardAiWritingToolsRuntime.setPersonalContextHintSupportedForTesting(Boolean.FALSE);
            Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    browsingHistoryReceiver, Boolean.FALSE));
            Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    browsingHistoryReceiver, Boolean.TRUE));
            Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    nestaReceiver, Boolean.FALSE));
            Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    nestaReceiver, Boolean.TRUE));

            // When PersonalContext BundleHint is supported (e.g. future Android 17+)
            GboardAiWritingToolsRuntime.setPersonalContextHintSupportedForTesting(Boolean.TRUE);
            Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    browsingHistoryReceiver, Boolean.FALSE));
            Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                    nestaReceiver, Boolean.FALSE));
        } finally {
            GboardAiWritingToolsRuntime.setPersonalContextHintSupportedForTesting(null);
        }
    }

    @Test
    public void isStockPhenotypeEnforcedFlagMatchesOnlyTargetFlags() {
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_DEVICE_INTELLIGENCE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_EMOJI_SCALE_SUPPORTED));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_ACCESS_POINTS_MENU_REDESIGN));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_ACCESS_POINT_KEYBOARD));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_AUTOFILL_IME_INTEGRATION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELLCHECKER_CHIPS_UI));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELLCHECKER_CHIPS_UI_WEB_VIEW));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_EXTENSION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_CHIP_IN_SPELLCHECKER));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_HIGHLIGHT_ON_WEB_INPUT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_TRAINING_CACHE));

        // Delight5 LM User History and Learning flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_PC_USER_HISTORY_NGRAM_COUNT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_POPULATE_NGRAM_COUNT_WITH_CONTEXT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_POPULATE_NGRAM_TIMESTAMP_IN_USER_HISTORY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_DYNAMIC_LM_UNIGRAM_CONTINUATION_SCORING));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_PRESERVE_CONTEXT_IN_DYNAMIC_LM_COMPUTE_HISTORY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_MULTIWORD_PREDICTIONS_FROM_USER_HISTORY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_MULTIWORD_SUGGESTIONS_FROM_USER_HISTORY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_DYNAMIC_LM_NOISE_FILTER));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_OOV_DEDUPLICATION));

        // Concept prediction and emoji suggestions flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_EXPRESSION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_CONCEPT_PREDICTION_ENGINE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_RULE_BASED_CONCEPT_PREDICTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_LAST_WORD_RULE_BASED_CONCEPT_PREDICTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SUFFIX_RULE_BASED_CONCEPT_PREDICTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJI_SUGGESTION_FROM_TOP_CANDIDATE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_PREDICTIONS_FROM_EMOJI_SHORTCUTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_BY_SPANS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_FOR_PREDICTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_RETAIN_EMOJI_SHORTCUT_CANDIDATES_ON_SELECTION));

        // Quick Emoji (fast access bar) and Magic Emoji (emojify wand button) flags
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

        // Glide typing and autocorrect decoder flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_UPDATE_STREAMING_TOUCH_DATA_SET_IS_GESTURE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_TWO_STAGE_DECODER));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SPACE_TRANSPOSITION_POST_CORRECTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_PK_SPATIAL_MODEL_FEATURES));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_PK_AUTO_CORRECTION_TRIGGER_PREDICTOR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_POST_CORRECTION_TRIGGER_PREDICTOR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_POPULATE_PREEMPTIVE_NEXT_KEY_PREDICTIONS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_AUTO_CORRECT_WITH_REPLACE_TEXT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_REVERT_AUTO_CORRECT_ON_PK));

        // Scan Text (OCR Camera Input) flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_OCR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_OCR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_CAMERA_PERMISSION_OVERLAY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_OCR_PASSWORD_ALLOWLIST));

        // Clipboard Screenshot & Image Floating Banner flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_CLIPBOARD_IMAGE_SUGGESTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SCREENSHOT_NOTIFICATION_FLOW));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SCREENSHOT_CHIP));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_CLIPBOARD_RECENT_SCREENSHOT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EXTENDED_CLIPBOARD_HISTORY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SYSTEM_CLIPBOARD_DIRECT_PASTE_FOR_QUICK_PASTE));

        // Emoji Picker Palette V2 flag is stock enforced; frequent emoji tab is dynamically configured via GboardEmojiSettings
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_FREQUENT_EMOJI_RECENT_TAB));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOJI_FREQUENT_RECENT_SWITCH_OPTION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_EMOJI_PALETTE_V2));

        // Super Insert / Quick Insert Hub flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_VK));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_ENABLE_DYNAMIC_MULTI_VIEW));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_CONTACTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_CLIPBOARD_QUERY_RESULTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_EXPRESSION_QUERY_RESULTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_LOCAL_IMAGE_RESULTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_BROWSING_HISTORY_RESULTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_NESTA_RESULTS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_BELOW_CURSOR));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SI_SHORTCUT_KEYS));

        // Smart Compose (In-Line Ghost Autocompletion) flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SMART_COMPOSE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_INLINE_SUGGESTIONS_ON_ALL_FIELDS));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_INLINE_COMPLETION));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_DELIGHT5_SMART_COMPOSE));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SMART_COMPOSE_STATUS));

        // Manual Incognito Mode Button flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_INCOGNITO_MODE_ENTRY_POINT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ALLOW_INCOGNITO_MODE_SWITCH));

        // Emogen (Generative AI Emoji Maker) flags
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_EMOGEN));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_EMOGEN_ENTRY_POINT));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_EMOGEN_FEATURE_ENABLED));
        Assert.assertTrue(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_EMOGEN_IN_ACCESS_POINTS));

        // Sign-to-Text (Signboard ASL Camera Translation) flags reverted
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SIGNBOARD));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SIGNBOARD_PROMPT_MANUAL_TRIGGER));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_ON_DEMAND_FEATURE_SPLIT));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_AUTO_SHOW_SIGNBOARD));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SIGNBOARD_AUTO_SHOW_PROMPT));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SIGNBOARD_DOWNLOAD));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SIGNBOARD_DOWNLOAD_DIALOG));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_SIGNBOARD_MOVE_AP_PROMPT));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_USE_4C_IN_SIGNBOARD));

        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardAiWritingToolsRuntime.FLAG_INTENDED_OOV_MIN_COUNT));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(
                GboardRoundedKeysSettings.FLAG_BELKA_ROUNDED_KEYBOARD));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(null));
        Assert.assertFalse(GboardAiWritingToolsRuntime.isStockPhenotypeEnforcedFlag(""));
    }

    @Test
    public void frequentEmojiTabRespectsGboardEmojiSettings() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardEmojiSettings.preferences(context);
        FrequentEmojiRecentTabFlagReceiver receiver = new FrequentEmojiRecentTabFlagReceiver();

        // Default: false (Recently used emojis)
        GboardEmojiSettings.writeFrequentEmojisEnabled(preferences, false);
        Object enforcedFalse = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(receiver, Boolean.TRUE);
        Assert.assertEquals(Boolean.FALSE, enforcedFalse);

        // When toggled on: true (Frequently used emojis)
        GboardEmojiSettings.writeFrequentEmojisEnabled(preferences, true);
        Object enforcedTrue = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(receiver, Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, enforcedTrue);

        // Reset
        GboardEmojiSettings.writeFrequentEmojisEnabled(preferences, false);
    }

    @Test
    public void intendedOovMinCountEnforcesTwoAndLogsDiagnostics() throws Exception {
        IntendedOovMinCountFlagReceiver receiver = new IntendedOovMinCountFlagReceiver();

        ShadowLog.clear();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Long.valueOf(4L));
        Assert.assertEquals(Long.valueOf(2L), enforced);
        Assert.assertEquals(1L, warningCount(
                "flag=intended_oov_min_count, original=Long(4), enforced=Long(2)"));

        // Already 2L -> no duplicate log
        ShadowLog.clear();
        Object unchanged = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Long.valueOf(2L));
        Assert.assertEquals(Long.valueOf(2L), unchanged);
        Assert.assertEquals(0L, warningCount(
                "flag=intended_oov_min_count, original=Long(2), enforced=Long(2)"));

        // Non-Long is preserved
        String nonLong = "not_a_long";
        Assert.assertSame(nonLong, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, nonLong));
    }

    @Test
    public void defaultPreferencesInitializeEmojiSuggestion() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences defaultPrefs =
                android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().clear().commit();

        Assert.assertFalse(defaultPrefs.contains("pref_key_enable_emoji_suggestion"));

        staticField("applicationContext").set(null, context);
        GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                new EmojiScaleFlagReceiver(), Boolean.FALSE);

        Assert.assertTrue(defaultPrefs.contains("pref_key_enable_emoji_suggestion"));
        Assert.assertTrue(defaultPrefs.getBoolean("pref_key_enable_emoji_suggestion", false));
        Assert.assertTrue(defaultPrefs.contains("spellchecker_enable_chips_ui"));
        Assert.assertTrue(defaultPrefs.getBoolean("spellchecker_enable_chips_ui", false));
        Assert.assertTrue(defaultPrefs.contains("enable_mark_misspelled_words"));
        Assert.assertTrue(defaultPrefs.getBoolean("enable_mark_misspelled_words", false));
        Assert.assertTrue(defaultPrefs.contains("pref_key_enable_grammar_checker"));
        Assert.assertTrue(defaultPrefs.getBoolean("pref_key_enable_grammar_checker", false));
        Assert.assertTrue(defaultPrefs.contains("pref_key_auto_correction"));
        Assert.assertTrue(defaultPrefs.getBoolean("pref_key_auto_correction", false));
        Assert.assertTrue(defaultPrefs.contains("show_suggestion_strip"));
        Assert.assertTrue(defaultPrefs.getBoolean("show_suggestion_strip", false));
    }

    private static long warningCount(String message) {
        String expected = "[gboard-writing-tools-18.0.3] " + message;
        return ShadowLog.getLogsForTag(TAG).stream()
                .filter(item -> item.type == android.util.Log.WARN)
                .filter(item -> expected.equals(item.msg))
                .count();
    }

    private static Field staticField(String name) throws Exception {
        Field field = GboardAiWritingToolsRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class FlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS;
    }

    private static final class DeviceIntelligenceFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_DEVICE_INTELLIGENCE;
    }

    private static final class EmojiScaleFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_EMOJI_SCALE_SUPPORTED;
    }

    private static final class IntendedOovMinCountFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_INTENDED_OOV_MIN_COUNT;
    }

    private static final class AccessPointsMenuRedesignFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_ACCESS_POINTS_MENU_REDESIGN;
    }

    private static final class AccessPointKeyboardFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_ACCESS_POINT_KEYBOARD;
    }

    private static final class AutofillImeIntegrationFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_AUTOFILL_IME_INTEGRATION;
    }

    private static final class SpellcheckerChipsUiFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELLCHECKER_CHIPS_UI;
    }

    private static final class SpellcheckerChipsUiWebViewFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELLCHECKER_CHIPS_UI_WEB_VIEW;
    }

    private static final class SpellCheckerExtensionFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_EXTENSION;
    }

    private static final class WritingHelperChipInSpellcheckerFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_CHIP_IN_SPELLCHECKER;
    }

    private static final class SpellCheckerHighlightOnWebInputFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_HIGHLIGHT_ON_WEB_INPUT;
    }

    private static final class SpellCheckerTrainingCacheFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_SPELL_CHECKER_TRAINING_CACHE;
    }

    @Test
    public void ocrLanguageTagsEnforcesStar() throws Exception {
        OcrLanguageTagsFlagReceiver receiver = new OcrLanguageTagsFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, "en-US");
        Assert.assertEquals("*", enforced);

        // Non-string preserved
        Long nonString = 123L;
        Assert.assertSame(nonString, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, nonString));
    }

    @Test
    public void smartComposeStatusEnforcesOne() throws Exception {
        SmartComposeStatusFlagReceiver receiver = new SmartComposeStatusFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Long.valueOf(0L));
        Assert.assertEquals(Long.valueOf(1L), enforced);

        // Boolean true also enforced via isStockPhenotypeEnforcedFlag
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE));
    }

    private static final class OcrLanguageTagsFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLED_OCR_LANGUAGE_TAGS;
    }

    private static final class SmartComposeStatusFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_SMART_COMPOSE_STATUS;
    }

    private static final class SuperInsertBrowsingHistoryFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_BROWSING_HISTORY_RESULTS;
    }

    private static final class SuperInsertNestaFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_SUPER_INSERT_NESTA_RESULTS;
    }

    @Test
    public void minCharNumLimitForSingleWordPredictionsEnforcesTwo() throws Exception {
        MinCharNumLimitFlagReceiver receiver = new MinCharNumLimitFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Long.valueOf(9L));
        Assert.assertEquals(Long.valueOf(2L), enforced);
    }

    @Test
    public void enablePkAutoCorrectionLocalesEnforcesStar() throws Exception {
        EnablePkAutoCorrectionLocalesFlagReceiver receiver =
                new EnablePkAutoCorrectionLocalesFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, "en-US");
        Assert.assertEquals("*", enforced);
    }

    @Test
    public void addPendingOovsMaxMsEnforces500() throws Exception {
        AddPendingOovsOnResetFlagReceiver resetReceiver =
                new AddPendingOovsOnResetFlagReceiver();
        AddPendingOovsOnCreateFlagReceiver createReceiver =
                new AddPendingOovsOnCreateFlagReceiver();
        Assert.assertEquals(Long.valueOf(500L),
                GboardAiWritingToolsRuntime.applyOverriddenFlagValue(resetReceiver, Long.valueOf(25L)));
        Assert.assertEquals(Long.valueOf(500L),
                GboardAiWritingToolsRuntime.applyOverriddenFlagValue(createReceiver, Long.valueOf(25L)));
    }

    @Test
    public void thresholdForCandidatesFromUserHistoryEnforcesMinusFive() throws Exception {
        ThresholdForCandidatesFlagReceiver receiver =
                new ThresholdForCandidatesFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Double.valueOf(0.0d));
        Assert.assertEquals(Double.valueOf(-5.0d), enforced);
    }

    @Test
    public void writingHelperServerTargetEnforcesProduction() throws Exception {
        WritingHelperServerTargetFlagReceiver receiver =
                new WritingHelperServerTargetFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, "other-host.googleapis.com");
        Assert.assertEquals("staging-gboard-imagen-pa.sandbox.googleapis.com", enforced);
    }

    @Test
    public void gboardImagenApiHostEnforcesProduction() throws Exception {
        GboardImagenApiHostFlagReceiver receiver =
                new GboardImagenApiHostFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, "other-host.googleapis.com");
        Assert.assertEquals("staging-gboard-imagen-pa.sandbox.googleapis.com", enforced);
    }

    @Test
    public void stopDeferringAccessPointsTasksEnforcesTrue() throws Exception {
        StopDeferringAccessPointsTasksFlagReceiver receiver =
                new StopDeferringAccessPointsTasksFlagReceiver();
        Object enforced = GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                receiver, Boolean.FALSE);
        Assert.assertEquals(Boolean.TRUE, enforced);
    }

    @Test
    public void defaultPreferencesInitializeAutoPasteAndScreenshotKeys() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences prefs =
                android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        staticField("applicationContext").set(null, context);

        Context resolved = GboardAiWritingToolsRuntime.resolveContext();
        Assert.assertNotNull(resolved);
        Assert.assertTrue(prefs.getBoolean("enable_auto_paste_chips", false));
        Assert.assertTrue(prefs.getBoolean("enable_screenshot_in_clipboard", false));
        Assert.assertTrue(prefs.getBoolean("clipboard_screenshot_enabled_at_least_once", false));
        Assert.assertTrue(prefs.getBoolean("enable_entity_extraction", false));
    }

    private static final class MinCharNumLimitFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_MIN_CHAR_NUM_LIMIT_FOR_SINGLE_WORD_PREDICTIONS;
    }

    private static final class EnablePkAutoCorrectionLocalesFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_PK_AUTO_CORRECTION_LOCALES;
    }

    private static final class AddPendingOovsOnResetFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ADD_PENDING_OOVS_ON_RESET_MAX_MS;
    }

    private static final class AddPendingOovsOnCreateFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ADD_PENDING_OOVS_ON_CREATE_MAX_MS;
    }

    private static final class ThresholdForCandidatesFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_THRESHOLD_FOR_CANDIDATES_FROM_USER_HISTORY;
    }

    private static final class WritingHelperServerTargetFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_SERVER_TARGET;
    }

    private static final class GboardImagenApiHostFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_GBOARD_IMAGEN_API_HOST;
    }

    private static final class StopDeferringAccessPointsTasksFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_STOP_DEFERRING_ACCESS_POINTS_TASKS;
    }

    private static final class RoundedKeysFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardRoundedKeysSettings.FLAG_BELKA_ROUNDED_KEYBOARD;
    }

    private static final class FrequentEmojiRecentTabFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardAiWritingToolsRuntime.FLAG_ENABLE_FREQUENT_EMOJI_RECENT_TAB;
    }

    private static final class GoogleSansFlexFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardGoogleSansSettings.FLAG_GOOGLE_SANS_FLEX;
    }

    private static final class UndoProactivePostCorrectionFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardUndoSettings.FLAG_UNDO_PROACTIVE_POST_CORRECTION;
    }

    private static final class UndoPromotionBannerThresholdFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardUndoSettings.FLAG_UNDO_PROMOTION_BANNER_THRESHOLD;
    }

    private static final class ThrowingSharedPreferencesContext extends ContextWrapper {
        private ThrowingSharedPreferencesContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            throw new IllegalStateException("settings unavailable");
        }
    }
}
