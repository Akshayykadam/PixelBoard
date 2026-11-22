/*
 * PixelBoard - Gboard Enhancement Mod
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;

/**
 * Setting for skipping duplicate multilingual entries when switching languages with the globe
 * key (e.g. "English + German" vs "German + English"). Off by default, so stock cycling is kept
 * unless the user opts in. Read on every globe press, so changes apply immediately.
 */
public final class GboardLanguageCycleSettings {
    public static final String PREF_KEY_SKIP_DUPLICATE_PAIRS = "language_cycle_skip_duplicate_pairs";
    public static final boolean DEFAULT_SKIP_DUPLICATE_PAIRS = false;

    private static volatile Method currentApplication;

    private GboardLanguageCycleSettings() {
    }

    public static boolean readSkipDuplicatePairs(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_SKIP_DUPLICATE_PAIRS;
        }
        return preferences.getBoolean(PREF_KEY_SKIP_DUPLICATE_PAIRS, DEFAULT_SKIP_DUPLICATE_PAIRS);
    }

    public static boolean writeSkipDuplicatePairs(SharedPreferences preferences, boolean enabled) {
        if (preferences == null) {
            return false;
        }
        return preferences.edit().putBoolean(PREF_KEY_SKIP_DUPLICATE_PAIRS, enabled).commit();
    }

    /** Runtime check from the keyboard process; falls back to the default (off) on any failure. */
    public static boolean isSkipDuplicatePairsEnabled() {
        try {
            Context context = applicationContext();
            if (context == null) {
                return DEFAULT_SKIP_DUPLICATE_PAIRS;
            }
            return readSkipDuplicatePairs(GboardPatchesSettings.preferences(context));
        } catch (Throwable throwable) {
            return DEFAULT_SKIP_DUPLICATE_PAIRS;
        }
    }

    private static Context applicationContext() {
        try {
            Method method = currentApplication;
            if (method == null) {
                method = Class.forName("android.app.ActivityThread").getMethod("currentApplication");
                currentApplication = method;
            }
            Object application = method.invoke(null);
            return application instanceof Context ? (Context) application : null;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
