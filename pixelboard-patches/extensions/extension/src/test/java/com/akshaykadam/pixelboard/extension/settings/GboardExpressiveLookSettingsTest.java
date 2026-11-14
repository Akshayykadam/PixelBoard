package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardExpressiveLookSettingsTest {

    @Test
    public void defaultExpressiveLookIsTrue() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardExpressiveLookSettings.preferences(context);
        preferences.edit().clear().commit();
        GboardExpressiveLookSettings.invalidateCache();

        Assert.assertTrue(GboardExpressiveLookSettings.readEnabled(preferences));
        Assert.assertTrue(GboardExpressiveLookSettings.isEnabled(context));
    }

    @Test
    public void readAndWriteTogglesPreferenceAndInvalidatesCache() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardExpressiveLookSettings.preferences(context);

        Assert.assertTrue(GboardExpressiveLookSettings.writeEnabled(preferences, false));
        Assert.assertFalse(GboardExpressiveLookSettings.readEnabled(preferences));
        Assert.assertFalse(GboardExpressiveLookSettings.isEnabled(context));

        Assert.assertTrue(GboardExpressiveLookSettings.writeEnabled(preferences, true));
        Assert.assertTrue(GboardExpressiveLookSettings.readEnabled(preferences));
        Assert.assertTrue(GboardExpressiveLookSettings.isEnabled(context));
    }

    @Test
    public void isExpressiveLookPhenotypeFlagRecognizesAllFlags() {
        Assert.assertTrue(GboardExpressiveLookSettings.isExpressiveLookPhenotypeFlag("top_rounded_keyboard"));
        Assert.assertTrue(GboardExpressiveLookSettings.isExpressiveLookPhenotypeFlag(
                "always_top_rounded_keyboard_for_debug"));
        Assert.assertTrue(GboardExpressiveLookSettings.isExpressiveLookPhenotypeFlag(
                "top_rounded_keyboard_radius"));

        Assert.assertFalse(GboardExpressiveLookSettings.isExpressiveLookPhenotypeFlag("belka_rounded_keyboard"));
        Assert.assertFalse(GboardExpressiveLookSettings.isExpressiveLookPhenotypeFlag(null));
    }

    @Test
    public void computeOverrideValueEnablesRoundedTopWhenOn() {
        Assert.assertSame(Boolean.TRUE, GboardExpressiveLookSettings.computeOverrideValue(
                "top_rounded_keyboard", Boolean.FALSE, true));
        Assert.assertSame(Boolean.TRUE, GboardExpressiveLookSettings.computeOverrideValue(
                "always_top_rounded_keyboard_for_debug", Boolean.FALSE, true));
        Assert.assertEquals(Double.valueOf(GboardExpressiveLookSettings.TOP_CORNER_RADIUS_DP),
                GboardExpressiveLookSettings.computeOverrideValue(
                        "top_rounded_keyboard_radius", Double.valueOf(20.0d), true));
    }

    @Test
    public void computeOverrideValueKeepsStockValuesWhenOff() {
        Assert.assertSame(Boolean.FALSE, GboardExpressiveLookSettings.computeOverrideValue(
                "top_rounded_keyboard", Boolean.FALSE, false));
        Double stockRadius = Double.valueOf(20.0d);
        Assert.assertSame(stockRadius, GboardExpressiveLookSettings.computeOverrideValue(
                "top_rounded_keyboard_radius", stockRadius, false));
    }

    @Test
    public void computeOverrideValueIgnoresUnexpectedTypes() {
        Long unexpected = Long.valueOf(1L);
        Assert.assertSame(unexpected, GboardExpressiveLookSettings.computeOverrideValue(
                "top_rounded_keyboard", unexpected, true));
    }
}
