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
 * Settings helper for Google Sans Flex typography on keyboard keycaps.
 *
 * <p>Manages the user preference toggle. At runtime, the
 * {@code GboardAiWritingToolsRuntime} flag override hook forces Gboard's
 * font and theme Phenotype flags ({@code google_sans_flex},
 * {@code platform_google_sans_flex}, and {@code gm3_expressive})
 * to {@code true} when enabled.</p>
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardGoogleSansSettings {
    public static final String PREF_KEY_ENABLED = "google_sans_enabled";
    public static final boolean DEFAULT_ENABLED = true;

    // Phenotype flag names that gate Google Sans Flex keycaps
    public static final String FLAG_GOOGLE_SANS_FLEX = "google_sans_flex";
    public static final String FLAG_PLATFORM_GOOGLE_SANS_FLEX = "platform_google_sans_flex";
    public static final String FLAG_GM3_EXPRESSIVE = "gm3_expressive";

    private static volatile Boolean cachedEnabled;

    private GboardGoogleSansSettings() {
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
     * Returns {@code true} if the given flag name is one of the Google Sans typography flags.
     */
    public static boolean isGoogleSansPhenotypeFlag(String flagName) {
        return FLAG_GOOGLE_SANS_FLEX.equals(flagName)
                || FLAG_PLATFORM_GOOGLE_SANS_FLEX.equals(flagName)
                || FLAG_GM3_EXPRESSIVE.equals(flagName);
    }

    public static Object computeOverrideValue(String flagName, Object originalResult, boolean enabled) {
        if (isGoogleSansPhenotypeFlag(flagName) && originalResult instanceof Boolean) {
            return Boolean.valueOf(enabled);
        }
        return originalResult;
    }
}
