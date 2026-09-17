package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

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
