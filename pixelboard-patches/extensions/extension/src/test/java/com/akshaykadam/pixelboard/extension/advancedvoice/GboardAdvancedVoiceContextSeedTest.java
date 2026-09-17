package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public final class GboardAdvancedVoiceContextSeedTest {
    @After
    public void tearDown() {
        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
    }

    @Test
    public void constructorContextMakesTheFirstVoiceDecisionUsePersistedSettings() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = GboardAdvancedVoiceSettings.preferences(context);
        preferences.edit()
                .putBoolean(GboardAdvancedVoiceSettings.PREF_KEY_ENABLED, true)
                .commit();

        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        GboardAdvancedVoice1803Runtime.seedApplicationContext(context);

        Assert.assertEquals(
                Boolean.TRUE,
                GboardAdvancedVoice1803Runtime.afterFlagValue(
                        new FlagReceiver("enable_nga"),
                        Boolean.FALSE));
    }

    @Test
    public void nullConstructorContextIsSafe() {
        GboardAdvancedVoice1803Runtime.seedApplicationContext(null);
    }

    private static final class FlagReceiver {
        private final String a;

        private FlagReceiver(String flagName) {
            a = flagName;
        }
    }
}
