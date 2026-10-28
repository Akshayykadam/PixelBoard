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
 * Settings helper for Gboard Undo button in suggestion strip and proactive undo actions.
 *
 * <p>Controls the user preference for displaying an interactive 1-tap Undo button in
 * Gboard's suggestion strip after text replacements, deletions, and autocorrection.
 * At runtime, the {@code GboardAiWritingToolsRuntime} flag override hook forces Gboard's
 * Undo module Phenotype flags to active states when enabled.</p>
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardUndoSettings {
    public static final String PREF_KEY_ENABLED = "undo_button_enabled";
    public static final boolean DEFAULT_ENABLED = true;

    // Phenotype flags for Gboard UndoUiExtension and UndoStack
    public static final String FLAG_UNDO_PROACTIVE_POST_CORRECTION =
            "undo_proactive_post_correction";
    public static final String FLAG_UNDO_PROACTIVE_AUTO_CORRECTION_COOPERATIVE =
            "undo_proactive_auto_correction_cooperative";
    public static final String FLAG_UNDO_ENABLE_CHUNK_FUSION =
            "undo_enable_chunk_fusion";
    public static final String FLAG_UNDO_STOP_DEFERRED_RANGE_NOTIFICATION =
            "undo_stop_deferred_range_notification";
    public static final String FLAG_UNDO_STOP_PK_HANDLING =
            "undo_stop_pk_handling";
    public static final String FLAG_NGA_ENABLE_UNDO_DELETE =
            "nga_enable_undo_delete";
    public static final String FLAG_UNDO_PROMOTION_BANNER_THRESHOLD =
            "undo_promotion_banner_threshold";
    public static final String FLAG_UNDO_MAX_CHUNK =
            "undo_max_chunk";
    public static final String FLAG_UNDO_TRUNCATE_SUFFIX_LEN =
            "undo_truncate_suffix_len";
    public static final String FLAG_UNDO_SUPPRESS_APP_PATTERN =
            "undo_suppress_app_pattern";

    private static volatile Boolean cachedEnabled;

    private GboardUndoSettings() {
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
     * Returns {@code true} if the given flag name is one of Gboard's Undo flags.
     */
    public static boolean isUndoPhenotypeFlag(String flagName) {
        return FLAG_UNDO_PROACTIVE_POST_CORRECTION.equals(flagName)
                || FLAG_UNDO_PROACTIVE_AUTO_CORRECTION_COOPERATIVE.equals(flagName)
                || FLAG_UNDO_ENABLE_CHUNK_FUSION.equals(flagName)
                || FLAG_UNDO_STOP_DEFERRED_RANGE_NOTIFICATION.equals(flagName)
                || FLAG_UNDO_STOP_PK_HANDLING.equals(flagName)
                || FLAG_NGA_ENABLE_UNDO_DELETE.equals(flagName)
                || FLAG_UNDO_PROMOTION_BANNER_THRESHOLD.equals(flagName)
                || FLAG_UNDO_MAX_CHUNK.equals(flagName)
                || FLAG_UNDO_TRUNCATE_SUFFIX_LEN.equals(flagName)
                || FLAG_UNDO_SUPPRESS_APP_PATTERN.equals(flagName);
    }

    public static Object computeOverrideValue(String flagName, Object originalResult, boolean enabled) {
        if (flagName == null) {
            return originalResult;
        }
        if (enabled) {
            if ((FLAG_UNDO_PROACTIVE_POST_CORRECTION.equals(flagName)
                    || FLAG_UNDO_PROACTIVE_AUTO_CORRECTION_COOPERATIVE.equals(flagName)
                    || FLAG_UNDO_ENABLE_CHUNK_FUSION.equals(flagName)
                    || FLAG_NGA_ENABLE_UNDO_DELETE.equals(flagName))
                    && originalResult instanceof Boolean) {
                return Boolean.TRUE;
            }
            if ((FLAG_UNDO_STOP_DEFERRED_RANGE_NOTIFICATION.equals(flagName)
                    || FLAG_UNDO_STOP_PK_HANDLING.equals(flagName))
                    && originalResult instanceof Boolean) {
                return Boolean.FALSE;
            }
            if (FLAG_UNDO_PROMOTION_BANNER_THRESHOLD.equals(flagName)
                    && originalResult instanceof Long) {
                return 1L;
            }
            if (FLAG_UNDO_MAX_CHUNK.equals(flagName)
                    && originalResult instanceof Long) {
                return 100L;
            }
            if (FLAG_UNDO_TRUNCATE_SUFFIX_LEN.equals(flagName)
                    && originalResult instanceof Long) {
                return 64L;
            }
            if (FLAG_UNDO_SUPPRESS_APP_PATTERN.equals(flagName)
                    && originalResult instanceof String) {
                return "";
            }
        } else {
            if ((FLAG_UNDO_PROACTIVE_POST_CORRECTION.equals(flagName)
                    || FLAG_UNDO_PROACTIVE_AUTO_CORRECTION_COOPERATIVE.equals(flagName)
                    || FLAG_NGA_ENABLE_UNDO_DELETE.equals(flagName))
                    && originalResult instanceof Boolean) {
                return Boolean.FALSE;
            }
        }
        return originalResult;
    }
}
