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
 * Settings helper for Emoji drawer recents tab mode (Recently Used vs Frequently Used).
 *
 * <p>In Gboard 18.5+, Google removed the in-drawer switch toggle between recently used
 * and frequently used emojis. Gboard now relies on the Phenotype flag
 * {@code enable_frequent_emoji_recent_Tab}: when {@code true}, it replaces the recents tab
 * with frequently used emojis; when {@code false}, it shows recently used emojis.</p>
 *
 * <p>This helper manages the user preference in PixelBoard Settings, defaulting to
 * {@code false} (Recently Used emojis), and allowing users to enable Frequently Used emojis
 * if desired.</p>
 */
public final class GboardEmojiSettings {
    public static final String PREF_KEY_FREQUENT_EMOJIS_ENABLED = "frequent_emojis_enabled";
    public static final boolean DEFAULT_FREQUENT_EMOJIS_ENABLED = false;

    private static volatile Boolean cachedFrequentEmojisEnabled;

    private GboardEmojiSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    public static boolean readFrequentEmojisEnabled(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_FREQUENT_EMOJIS_ENABLED;
        }
        return preferences.getBoolean(PREF_KEY_FREQUENT_EMOJIS_ENABLED, DEFAULT_FREQUENT_EMOJIS_ENABLED);
    }

    public static boolean isFrequentEmojisEnabled(Context context) {
        Boolean cached = cachedFrequentEmojisEnabled;
        if (cached != null) {
            return cached.booleanValue();
        }
        if (context == null) {
            return DEFAULT_FREQUENT_EMOJIS_ENABLED;
        }
        try {
            SharedPreferences prefs = preferences(context);
            boolean enabled = readFrequentEmojisEnabled(prefs);
            cachedFrequentEmojisEnabled = Boolean.valueOf(enabled);
            return enabled;
        } catch (Throwable throwable) {
            return DEFAULT_FREQUENT_EMOJIS_ENABLED;
        }
    }

    public static void invalidateCache() {
        cachedFrequentEmojisEnabled = null;
    }

    public static boolean writeFrequentEmojisEnabled(SharedPreferences preferences, boolean enabled) {
        if (preferences == null) {
            return false;
        }
        invalidateCache();
        return preferences.edit().putBoolean(PREF_KEY_FREQUENT_EMOJIS_ENABLED, enabled).commit();
    }
}
