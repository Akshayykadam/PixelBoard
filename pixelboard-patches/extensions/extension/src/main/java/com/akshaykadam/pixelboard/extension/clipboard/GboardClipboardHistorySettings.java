/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

/**
 * Settings model for the Clipboard History Duration preference.
 *
 * <p>Stores the user-selected clipboard history retention window in hours as a
 * numeric string, matching Gboard's native {@code clipboard_history_retention}
 * preference:</p>
 * <ul>
 *   <li>{@link #DURATION_1_HOUR} ("1") – 1 hour</li>
 *   <li>{@link #DURATION_24_HOURS} ("24") – 24 hours (Current maximum)</li>
 *   <li>{@link #DURATION_7_DAYS} ("168") – 7 days</li>
 *   <li>{@link #DURATION_30_DAYS} ("720") – 30 days</li>
 *   <li>{@link #DURATION_NEVER} ("876000") – Never (Unlimited / Manual deletion only, 100 years)</li>
 * </ul>
 */
public final class GboardClipboardHistorySettings {

    /** SharedPreferences key for the clipboard history duration value. */
    public static final String PREF_KEY_HISTORY_DURATION = "clipboard_history_duration";
    public static final String GBOARD_NATIVE_PREF_KEY = "clipboard_history_retention";

    // Duration values in hours (matching Gboard's native slider entryValues)
    public static final String DURATION_1_HOUR    = "1";
    public static final String DURATION_24_HOURS  = "24";
    public static final String DURATION_7_DAYS    = "168";
    public static final String DURATION_30_DAYS   = "720";
    public static final String DURATION_NEVER     = "876000";

    /** Default duration: 24 hours (matches Gboard's built-in maximum). */
    public static final String DEFAULT_DURATION = DURATION_24_HOURS;

    /**
     * Duration in milliseconds for each token.
     * 100 years for {@link #DURATION_NEVER} (3,153,600,000,000 ms) guarantees
     * (now - retention) < 0, so SQLite timestamp comparison matches 0 rows.
     */
    public static final long DURATION_MS_1_HOUR   = 60L * 60L * 1_000L;
    public static final long DURATION_MS_24_HOURS = 24L * DURATION_MS_1_HOUR;
    public static final long DURATION_MS_7_DAYS   = 7L  * 24L * DURATION_MS_1_HOUR;
    public static final long DURATION_MS_30_DAYS  = 30L * 24L * DURATION_MS_1_HOUR;
    public static final long DURATION_MS_NEVER    = 876_000L * DURATION_MS_1_HOUR;

    /** Ordered list of all valid duration values (matches slider step order). */
    public static final String[] ALL_DURATIONS = {
            DURATION_1_HOUR,
            DURATION_24_HOURS,
            DURATION_7_DAYS,
            DURATION_30_DAYS,
            DURATION_NEVER
    };

    private GboardClipboardHistorySettings() {
    }

    /**
     * Returns PixelBoard's shared preferences for clipboard history settings
     * (device-protected storage via {@link GboardPatchesSettings#preferences}).
     */
    public static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    public static void ensureDefaults(SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }
        if (!preferences.contains(PREF_KEY_HISTORY_DURATION)) {
            preferences.edit()
                    .putString(PREF_KEY_HISTORY_DURATION, DEFAULT_DURATION)
                    .apply();
        }
    }

    public static String readDuration(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_DURATION;
        }
        try {
            return sanitize(preferences.getString(PREF_KEY_HISTORY_DURATION, DEFAULT_DURATION));
        } catch (ClassCastException ignored) {
            return DEFAULT_DURATION;
        }
    }

    public static boolean writeDuration(SharedPreferences preferences, String duration) {
        if (preferences == null) {
            return false;
        }
        try {
            return preferences.edit()
                    .putString(PREF_KEY_HISTORY_DURATION, sanitize(duration))
                    .commit();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void syncToGboardPreferences(Context context, String duration) {
        if (context == null) return;
        try {
            SharedPreferences gboardPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (gboardPrefs != null) {
                gboardPrefs.edit().putString(GBOARD_NATIVE_PREF_KEY, sanitize(duration)).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Converts the stored value into the corresponding duration in milliseconds.
     */
    public static long toMillis(String duration) {
        if (duration == null) {
            return DURATION_MS_24_HOURS;
        }
        switch (sanitize(duration)) {
            case DURATION_1_HOUR:   return DURATION_MS_1_HOUR;
            case DURATION_7_DAYS:   return DURATION_MS_7_DAYS;
            case DURATION_30_DAYS:  return DURATION_MS_30_DAYS;
            case DURATION_NEVER:    return DURATION_MS_NEVER;
            case DURATION_24_HOURS: // fall-through
            default:                return DURATION_MS_24_HOURS;
        }
    }

    /** Returns a valid token, falling back to the default if unrecognised. */
    public static String sanitize(String duration) {
        if (duration != null) {
            if ("1h".equals(duration)) return DURATION_1_HOUR;
            if ("24h".equals(duration)) return DURATION_24_HOURS;
            if ("7d".equals(duration)) return DURATION_7_DAYS;
            if ("30d".equals(duration)) return DURATION_30_DAYS;
            if ("never".equals(duration)) return DURATION_NEVER;
            for (String valid : ALL_DURATIONS) {
                if (valid.equals(duration)) {
                    return duration;
                }
            }
        }
        return DEFAULT_DURATION;
    }
}
