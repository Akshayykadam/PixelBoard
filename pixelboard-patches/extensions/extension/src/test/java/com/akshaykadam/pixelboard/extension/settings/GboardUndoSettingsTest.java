package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardUndoSettingsTest {

    @Test
    public void defaultUndoButtonIsTrue() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardUndoSettings.preferences(context);
        preferences.edit().clear().commit();
        GboardUndoSettings.invalidateCache();

        Assert.assertTrue(GboardUndoSettings.readEnabled(preferences));
        Assert.assertTrue(GboardUndoSettings.isEnabled(context));
    }

    @Test
    public void readAndWriteTogglesPreferenceAndInvalidatesCache() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardUndoSettings.preferences(context);

        Assert.assertTrue(GboardUndoSettings.writeEnabled(preferences, true));
        Assert.assertTrue(GboardUndoSettings.readEnabled(preferences));
        Assert.assertTrue(GboardUndoSettings.isEnabled(context));

        Assert.assertTrue(GboardUndoSettings.writeEnabled(preferences, false));
        Assert.assertFalse(GboardUndoSettings.readEnabled(preferences));
        Assert.assertFalse(GboardUndoSettings.isEnabled(context));
    }

    @Test
    public void isUndoPhenotypeFlagRecognizesAllUndoFlags() {
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_proactive_post_correction"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_proactive_auto_correction_cooperative"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_enable_chunk_fusion"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_stop_deferred_range_notification"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_stop_pk_handling"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("nga_enable_undo_delete"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_promotion_banner_threshold"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_max_chunk"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_truncate_suffix_len"));
        Assert.assertTrue(GboardUndoSettings.isUndoPhenotypeFlag("undo_suppress_app_pattern"));

        Assert.assertFalse(GboardUndoSettings.isUndoPhenotypeFlag("writing_tools"));
        Assert.assertFalse(GboardUndoSettings.isUndoPhenotypeFlag("google_sans_flex"));
        Assert.assertFalse(GboardUndoSettings.isUndoPhenotypeFlag(null));
    }

    @Test
    public void computeOverrideValueEnforcesUndoFlagsWhenEnabled() {
        // Booleans
        Assert.assertSame(Boolean.TRUE, GboardUndoSettings.computeOverrideValue(
                "undo_proactive_post_correction", Boolean.FALSE, true));
        Assert.assertSame(Boolean.TRUE, GboardUndoSettings.computeOverrideValue(
                "undo_proactive_auto_correction_cooperative", Boolean.FALSE, true));
        Assert.assertSame(Boolean.TRUE, GboardUndoSettings.computeOverrideValue(
                "undo_enable_chunk_fusion", Boolean.FALSE, true));
        Assert.assertSame(Boolean.TRUE, GboardUndoSettings.computeOverrideValue(
                "nga_enable_undo_delete", Boolean.FALSE, true));
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                "undo_stop_deferred_range_notification", Boolean.TRUE, true));
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                "undo_stop_pk_handling", Boolean.TRUE, true));

        // Longs
        Assert.assertEquals(1L, GboardUndoSettings.computeOverrideValue(
                "undo_promotion_banner_threshold", 3L, true));
        Assert.assertEquals(100L, GboardUndoSettings.computeOverrideValue(
                "undo_max_chunk", 10L, true));
        Assert.assertEquals(64L, GboardUndoSettings.computeOverrideValue(
                "undo_truncate_suffix_len", 16L, true));

        // Strings
        Assert.assertEquals("", GboardUndoSettings.computeOverrideValue(
                "undo_suppress_app_pattern", "com.some.app", true));
    }

    @Test
    public void computeOverrideValueDisablesProactiveUndoWhenDisabled() {
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                "undo_proactive_post_correction", Boolean.TRUE, false));
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                "undo_proactive_auto_correction_cooperative", Boolean.TRUE, false));
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                "nga_enable_undo_delete", Boolean.TRUE, false));

        // Non-proactive flags remain at original value when disabled
        Assert.assertEquals(3L, GboardUndoSettings.computeOverrideValue(
                "undo_promotion_banner_threshold", 3L, false));
        Assert.assertEquals(50L, GboardUndoSettings.computeOverrideValue(
                "undo_max_chunk", 50L, false));
    }

    @Test
    public void computeOverrideValuePreservesUnexpectedTypesAndNulls() {
        Assert.assertEquals("invalid", GboardUndoSettings.computeOverrideValue(
                "undo_proactive_post_correction", "invalid", true));
        Assert.assertEquals("invalid", GboardUndoSettings.computeOverrideValue(
                "undo_promotion_banner_threshold", "invalid", true));
        Assert.assertEquals(123, GboardUndoSettings.computeOverrideValue(
                "undo_suppress_app_pattern", 123, true));
        Assert.assertSame(Boolean.FALSE, GboardUndoSettings.computeOverrideValue(
                null, Boolean.FALSE, true));
        Assert.assertNull(GboardUndoSettings.computeOverrideValue(
                "undo_proactive_post_correction", null, true));
    }

    @Test
    public void ensureDefaultsSetsPreferenceIfNotPresent() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardUndoSettings.preferences(context);
        preferences.edit().clear().commit();

        GboardUndoSettings.ensureDefaults(context, preferences);
        Assert.assertTrue(preferences.contains(GboardUndoSettings.PREF_KEY_ENABLED));
        Assert.assertTrue(GboardUndoSettings.readEnabled(preferences));

        // If explicitly set to false, ensureDefaults does not overwrite
        preferences.edit().putBoolean(GboardUndoSettings.PREF_KEY_ENABLED, false).commit();
        GboardUndoSettings.ensureDefaults(context, preferences);
        Assert.assertFalse(GboardUndoSettings.readEnabled(preferences));
    }
}
