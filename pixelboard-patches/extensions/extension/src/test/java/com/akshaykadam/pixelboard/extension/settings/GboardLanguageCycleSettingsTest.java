package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardLanguageCycleSettingsTest {

    @Test
    public void skipDuplicatePairsIsOffByDefault() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);
        preferences.edit().clear().commit();

        Assert.assertFalse(GboardLanguageCycleSettings.readSkipDuplicatePairs(preferences));
        Assert.assertFalse(GboardLanguageCycleSettings.isSkipDuplicatePairsEnabled());
        Assert.assertFalse(GboardLanguageCycleSettings.readSkipDuplicatePairs(null));
    }

    @Test
    public void writeIsReadBackImmediately() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);

        Assert.assertTrue(GboardLanguageCycleSettings.writeSkipDuplicatePairs(preferences, true));
        Assert.assertTrue(GboardLanguageCycleSettings.readSkipDuplicatePairs(preferences));
        Assert.assertTrue(GboardLanguageCycleSettings.isSkipDuplicatePairsEnabled());

        Assert.assertTrue(GboardLanguageCycleSettings.writeSkipDuplicatePairs(preferences, false));
        Assert.assertFalse(GboardLanguageCycleSettings.readSkipDuplicatePairs(preferences));
        Assert.assertFalse(GboardLanguageCycleSettings.isSkipDuplicatePairsEnabled());
    }

    @Test
    public void writeWithoutPreferencesIsRejected() {
        Assert.assertFalse(GboardLanguageCycleSettings.writeSkipDuplicatePairs(null, true));
    }
}
