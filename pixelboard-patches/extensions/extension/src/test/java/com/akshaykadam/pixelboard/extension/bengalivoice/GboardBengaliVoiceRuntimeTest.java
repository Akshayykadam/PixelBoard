package com.akshaykadam.pixelboard.extension.bengalivoice;

import android.content.Context;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import com.akshaykadam.pixelboard.extension.rambler.GboardRambler1803OfficialSelectionRuntime;
import com.akshaykadam.pixelboard.extension.rambler.GboardRambler1803StockPolicy;

@RunWith(RobolectricTestRunner.class)
public final class GboardBengaliVoiceRuntimeTest {
    @After
    public void tearDownNativeBindings() {
        GboardBengaliVoiceRuntime.resetForTests();
        CurrentLocaleProvider.locale = null;
    }

    @Test
    public void standardAndRamblerOverrideBothGlobalSelectionValues() {
        Assert.assertFalse(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.STANDARD, true));
        Assert.assertFalse(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.STANDARD, false));
        Assert.assertTrue(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.RAMBLER, false));
        Assert.assertTrue(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.RAMBLER, true));
        Assert.assertTrue(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.INHERIT, true));
        Assert.assertFalse(GboardBengaliVoiceRuntime.applyRouteOverride(
                GboardBengaliVoiceRuntime.INHERIT, false));
    }

    @Test
    public void persistedBengaliChoicesDoNotLeakToOtherLocales() {
        Context context = RuntimeEnvironment.getApplication();
        Assert.assertTrue(GboardBengaliVoiceSettings.writeMode(
                context, "bn-BD", GboardBengaliVoicePolicy.RAMBLER));
        Assert.assertTrue(GboardBengaliVoiceSettings.writeMode(
                context, "bn-IN", GboardBengaliVoicePolicy.STANDARD));
        Assert.assertEquals(GboardBengaliVoiceRuntime.RAMBLER,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(context, "bn-Beng-BD"));
        Assert.assertEquals(GboardBengaliVoiceRuntime.STANDARD,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(context, "bn-IN"));
        Assert.assertEquals(GboardBengaliVoiceRuntime.INHERIT,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(context, "en-US"));
        Assert.assertEquals(GboardBengaliVoiceRuntime.INHERIT,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(context, null));
        Assert.assertEquals(GboardBengaliVoiceRuntime.RAMBLER,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(context, "bn-BD"));
    }

    @Test
    public void currentInputLocaleWinsOverDisplayAndDefaultLocales() throws Exception {
        Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            CurrentLocaleProvider.locale = Locale.forLanguageTag("bn-BD");
            Assert.assertEquals("bn-BD", GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                    CurrentLocaleProvider.class.getMethod("e")));
        } finally {
            Locale.setDefault(previousDefault);
        }
    }

    @Test
    public void absentAndUnexpectedNativeProvidersNeverUseUiLocale() throws Exception {
        CurrentLocaleProvider.locale = null;
        Assert.assertNull(GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                CurrentLocaleProvider.class.getMethod("e")));
        Assert.assertNull(GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(null));
        Assert.assertNull(GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                WrongLocaleProvider.class.getMethod("e")));
        Assert.assertNull(GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                InstanceLocaleProvider.class.getMethod("e")));
    }

    @Test
    public void malformedTagCannotBeTruncatedIntoABengaliOverride() {
        Assert.assertEquals(GboardBengaliVoiceRuntime.INHERIT,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(
                        RuntimeEnvironment.getApplication(), "bn--BD"));
    }

    @Test
    public void nestedLocaleResolutionIsGuardedAndLaterQueriesRemainLive() throws Exception {
        CurrentLocaleProvider.locale = Locale.forLanguageTag("bn-BD");
        ReentrantLocaleProvider.nestedResult = "not queried";
        Assert.assertEquals("bn-BD", GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                ReentrantLocaleProvider.class.getMethod("e")));
        Assert.assertNull(ReentrantLocaleProvider.nestedResult);
        CurrentLocaleProvider.locale = Locale.US;
        Assert.assertEquals("en-US", GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                CurrentLocaleProvider.class.getMethod("e")));
    }

    @Test
    public void explicitStandardChangesAgenticFlagsWithoutDisablingLifecycleFlags() {
        Assert.assertEquals(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceLanguageRouteValue(
                        "enable_agentic_dictation", Boolean.TRUE, GboardBengaliVoiceRuntime.STANDARD));
        Assert.assertEquals(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceLanguageRouteValue(
                        "enable_agentic_dictation", Boolean.FALSE, GboardBengaliVoiceRuntime.RAMBLER));
        Assert.assertEquals(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceLanguageRouteValue(
                        "immediately_end_dictation_on_keyboard_hidden", Boolean.TRUE,
                        GboardBengaliVoiceRuntime.STANDARD));
        Assert.assertEquals(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceLanguageRouteValue(
                        "enable_nga", Boolean.FALSE, GboardBengaliVoiceRuntime.STANDARD));
    }

    @Test
    public void oldAndUnknownBaseVersionsCannotUse1853NativeBindings() {
        Assert.assertTrue(GboardBengaliVoiceRuntime.isSupportedNativeVersionName(
                "18.5.3.993608717-release-arm64-v8a"));
        Assert.assertFalse(GboardBengaliVoiceRuntime.isSupportedNativeVersionName(
                "18.4.1.985164140-release-arm64-v8a"));
        Assert.assertFalse(GboardBengaliVoiceRuntime.isSupportedNativeVersionName("1.3.1"));
        Assert.assertFalse(GboardBengaliVoiceRuntime.isSupportedNativeVersionName(null));
        Assert.assertEquals(GboardBengaliVoiceRuntime.INHERIT,
                GboardBengaliVoiceRuntime.routeOverrideForLocale(null, "bn-BD"));
    }

    @Test
    public void nativeSettingsAndDefaultInitializationScopesPreserveGlobalSelection() {
        Assert.assertTrue(GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed());
        GboardRambler1803OfficialSelectionRuntime.enterVoiceSettingsScope();
        try {
            Assert.assertFalse(GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed());
        } finally {
            GboardRambler1803OfficialSelectionRuntime.exitVoiceSettingsScope();
        }
        GboardRambler1803OfficialSelectionRuntime.enterDefaultSelectionSuppression();
        try {
            Assert.assertFalse(GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed());
        } finally {
            GboardRambler1803OfficialSelectionRuntime.exitDefaultSelectionSuppression();
        }
        Assert.assertTrue(GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed());
    }

    public static final class CurrentLocaleProvider {
        static Locale locale;

        public static Locale e() {
            return locale;
        }

        public static Locale r() {
            return Locale.ENGLISH;
        }
    }

    public static final class WrongLocaleProvider {
        public static String e() {
            return "bn-BD";
        }
    }

    public static final class InstanceLocaleProvider {
        public Locale e() {
            return Locale.forLanguageTag("bn-BD");
        }
    }

    public static final class ReentrantLocaleProvider {
        static String nestedResult;

        public static Locale e() throws Exception {
            nestedResult = GboardBengaliVoiceRuntime.languageTagFromLocaleGetter(
                    CurrentLocaleProvider.class.getMethod("e"));
            return CurrentLocaleProvider.locale;
        }
    }
}
