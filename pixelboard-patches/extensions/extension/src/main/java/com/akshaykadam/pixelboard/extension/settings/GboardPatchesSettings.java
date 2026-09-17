package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class GboardPatchesSettings {
    public static final String PREF_FILE = "gboard_patches_settings";
    public static final String PREF_KEY_SETTINGS_UI_LANGUAGE =
            "pref_settings_ui_language";
    public static final String DEFAULT_SETTINGS_UI_LANGUAGE =
            GboardSettingsLocaleManager.LANGUAGE_SYSTEM;

    private GboardPatchesSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        Context applicationContext = context == null ? null : context.getApplicationContext();
        Context lookupContext = applicationContext != null ? applicationContext : context;
        if (lookupContext == null) {
            throw new IllegalStateException("Context required for Gboard patch settings.");
        }
        return lookupContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static void ensureDefaults(Context context) {
        if (context == null) {
            return;
        }
        ensureDefaults(preferences(context));
    }

    public static void ensureDefaults(SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }
        if (!preferences.contains(PREF_KEY_SETTINGS_UI_LANGUAGE)) {
            preferences.edit()
                    .putString(
                            PREF_KEY_SETTINGS_UI_LANGUAGE,
                            DEFAULT_SETTINGS_UI_LANGUAGE)
                    .apply();
        }
    }
}
