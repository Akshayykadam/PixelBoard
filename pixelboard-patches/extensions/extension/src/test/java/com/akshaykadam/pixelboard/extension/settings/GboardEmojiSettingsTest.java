package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardEmojiSettingsTest {

    @Test
    public void defaultFrequentEmojisIsFalse() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardEmojiSettings.preferences(context);
        preferences.edit().clear().commit();
        GboardEmojiSettings.invalidateCache();

        Assert.assertFalse(GboardEmojiSettings.readFrequentEmojisEnabled(preferences));
        Assert.assertFalse(GboardEmojiSettings.isFrequentEmojisEnabled(context));
    }

    @Test
    public void readAndWriteTogglesPreferenceAndInvalidatesCache() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardEmojiSettings.preferences(context);

        Assert.assertTrue(GboardEmojiSettings.writeFrequentEmojisEnabled(preferences, true));
        Assert.assertTrue(GboardEmojiSettings.readFrequentEmojisEnabled(preferences));
        Assert.assertTrue(GboardEmojiSettings.isFrequentEmojisEnabled(context));

        Assert.assertTrue(GboardEmojiSettings.writeFrequentEmojisEnabled(preferences, false));
        Assert.assertFalse(GboardEmojiSettings.readFrequentEmojisEnabled(preferences));
        Assert.assertFalse(GboardEmojiSettings.isFrequentEmojisEnabled(context));
    }
}
