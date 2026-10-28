package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardGoogleSansSettingsTest {

    @Test
    public void defaultGoogleSansIsTrue() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardGoogleSansSettings.preferences(context);
        preferences.edit().clear().commit();
        GboardGoogleSansSettings.invalidateCache();

        Assert.assertTrue(GboardGoogleSansSettings.readEnabled(preferences));
        Assert.assertTrue(GboardGoogleSansSettings.isEnabled(context));
    }

    @Test
    public void readAndWriteTogglesPreferenceAndInvalidatesCache() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardGoogleSansSettings.preferences(context);

        Assert.assertTrue(GboardGoogleSansSettings.writeEnabled(preferences, true));
        Assert.assertTrue(GboardGoogleSansSettings.readEnabled(preferences));
        Assert.assertTrue(GboardGoogleSansSettings.isEnabled(context));

        Assert.assertTrue(GboardGoogleSansSettings.writeEnabled(preferences, false));
        Assert.assertFalse(GboardGoogleSansSettings.readEnabled(preferences));
        Assert.assertFalse(GboardGoogleSansSettings.isEnabled(context));
    }

    @Test
    public void isGoogleSansPhenotypeFlagRecognizesAllFlags() {
        Assert.assertTrue(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag("google_sans_flex"));
        Assert.assertTrue(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag("platform_google_sans_flex"));
        Assert.assertTrue(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag("gm3_expressive"));

        Assert.assertFalse(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag("writing_tools"));
        Assert.assertFalse(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag("undo_button_enabled"));
        Assert.assertFalse(GboardGoogleSansSettings.isGoogleSansPhenotypeFlag(null));
    }

    @Test
    public void computeOverrideValueEnforcesStates() {
        Assert.assertSame(Boolean.TRUE, GboardGoogleSansSettings.computeOverrideValue(
                "google_sans_flex", Boolean.FALSE, true));
        Assert.assertSame(Boolean.FALSE, GboardGoogleSansSettings.computeOverrideValue(
                "google_sans_flex", Boolean.TRUE, false));
        Assert.assertSame(Boolean.TRUE, GboardGoogleSansSettings.computeOverrideValue(
                "platform_google_sans_flex", Boolean.FALSE, true));
        Assert.assertSame(Boolean.FALSE, GboardGoogleSansSettings.computeOverrideValue(
                "platform_google_sans_flex", Boolean.TRUE, false));
        Assert.assertSame(Boolean.TRUE, GboardGoogleSansSettings.computeOverrideValue(
                "gm3_expressive", Boolean.FALSE, true));
        Assert.assertSame(Boolean.FALSE, GboardGoogleSansSettings.computeOverrideValue(
                "gm3_expressive", Boolean.TRUE, false));

        // Non-boolean or non-Google Sans flags preserved
        Assert.assertEquals("test", GboardGoogleSansSettings.computeOverrideValue(
                "other_flag", "test", true));
    }

    @Test
    public void computeOverrideValuePreservesUnexpectedTypesAndNulls() {
        Assert.assertEquals("invalid", GboardGoogleSansSettings.computeOverrideValue(
                "google_sans_flex", "invalid", true));
        Assert.assertNull(GboardGoogleSansSettings.computeOverrideValue(
                "google_sans_flex", null, true));
        Assert.assertSame(Boolean.FALSE, GboardGoogleSansSettings.computeOverrideValue(
                null, Boolean.FALSE, true));
    }

    @Test
    public void ensureDefaultsSetsPreferenceIfNotPresent() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardGoogleSansSettings.preferences(context);
        preferences.edit().clear().commit();

        GboardGoogleSansSettings.ensureDefaults(context, preferences);
        Assert.assertTrue(preferences.contains(GboardGoogleSansSettings.PREF_KEY_ENABLED));
        Assert.assertTrue(GboardGoogleSansSettings.readEnabled(preferences));

        // If explicitly set to false, ensureDefaults does not overwrite
        preferences.edit().putBoolean(GboardGoogleSansSettings.PREF_KEY_ENABLED, false).commit();
        GboardGoogleSansSettings.ensureDefaults(context, preferences);
        Assert.assertFalse(GboardGoogleSansSettings.readEnabled(preferences));
    }
}
