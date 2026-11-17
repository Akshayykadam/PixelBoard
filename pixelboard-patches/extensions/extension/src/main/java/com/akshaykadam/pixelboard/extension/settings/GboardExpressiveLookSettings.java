/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Settings helper for the Material 3 Expressive keyboard look (issue #133).
 *
 * <p>When enabled, the {@code GboardAiWritingToolsRuntime} flag override hook
 * turns on Gboard's native top-rounded keyboard module
 * ({@code top_rounded_keyboard}, {@code always_top_rounded_keyboard_for_debug},
 * {@code top_rounded_keyboard_radius}). That module's per-view callback is also
 * where {@code GboardExpressiveLookRuntime} applies the sunburst toolbar
 * buttons and hides the hairline above the rounded top.</p>
 */
public final class GboardExpressiveLookSettings {
    public static final String PREF_KEY_ENABLED = "expressive_look_enabled";
    public static final boolean DEFAULT_ENABLED = true;

    public static final String FLAG_TOP_ROUNDED_KEYBOARD = "top_rounded_keyboard";
    public static final String FLAG_ALWAYS_TOP_ROUNDED_KEYBOARD =
            "always_top_rounded_keyboard_for_debug";
    public static final String FLAG_TOP_ROUNDED_KEYBOARD_RADIUS = "top_rounded_keyboard_radius";

    /** Material 3 extra-large corner, in dp. Gboard's own default is 20. */
    public static final double TOP_CORNER_RADIUS_DP = 28.0d;

    private static volatile Boolean cachedEnabled;

    private GboardExpressiveLookSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    public static boolean readEnabled(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_ENABLED;
        }
        return preferences.getBoolean(PREF_KEY_ENABLED, DEFAULT_ENABLED);
    }

    public static void ensureDefaults(Context context, SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }
        if (!preferences.contains(PREF_KEY_ENABLED)) {
            preferences.edit()
                    .putBoolean(PREF_KEY_ENABLED, DEFAULT_ENABLED)
                    .apply();
        }
    }

    public static boolean isEnabled(Context context) {
        Boolean cached = cachedEnabled;
        if (cached != null) {
            return cached.booleanValue();
        }
        if (context == null) {
            return DEFAULT_ENABLED;
        }
        try {
            SharedPreferences prefs = preferences(context);
            boolean enabled = readEnabled(prefs);
            cachedEnabled = Boolean.valueOf(enabled);
            return enabled;
        } catch (Throwable throwable) {
            return DEFAULT_ENABLED;
        }
    }

    public static void invalidateCache() {
        cachedEnabled = null;
    }

    public static boolean writeEnabled(SharedPreferences preferences, boolean enabled) {
        if (preferences == null) {
            return false;
        }
        invalidateCache();
        return preferences.edit().putBoolean(PREF_KEY_ENABLED, enabled).commit();
    }

    /**
     * Returns {@code true} if the given flag name is one of the top-rounded keyboard flags.
     */
    public static boolean isExpressiveLookPhenotypeFlag(String flagName) {
        return FLAG_TOP_ROUNDED_KEYBOARD.equals(flagName)
                || FLAG_ALWAYS_TOP_ROUNDED_KEYBOARD.equals(flagName)
                || FLAG_TOP_ROUNDED_KEYBOARD_RADIUS.equals(flagName);
    }

    public static Object computeOverrideValue(String flagName, Object originalResult, boolean enabled) {
        if (!enabled) {
            return originalResult;
        }
        if ((FLAG_TOP_ROUNDED_KEYBOARD.equals(flagName)
                || FLAG_ALWAYS_TOP_ROUNDED_KEYBOARD.equals(flagName))
                && originalResult instanceof Boolean) {
            return Boolean.TRUE;
        }
        if (FLAG_TOP_ROUNDED_KEYBOARD_RADIUS.equals(flagName) && originalResult instanceof Double) {
            return Double.valueOf(TOP_CORNER_RADIUS_DP);
        }
        return originalResult;
    }
}
