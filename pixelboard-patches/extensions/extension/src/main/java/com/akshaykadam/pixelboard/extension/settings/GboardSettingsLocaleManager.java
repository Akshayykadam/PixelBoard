package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public final class GboardSettingsLocaleManager {
    public static final String LANGUAGE_SYSTEM = "system";
    public static final String LANGUAGE_ENGLISH = "en";

    private GboardSettingsLocaleManager() {
    }

    public static String readLanguagePreference(SharedPreferences preferences) {
        if (preferences == null) {
            return LANGUAGE_SYSTEM;
        }
        return normalizePreferenceValue(
                preferences.getString(
                        GboardPatchesSettings.PREF_KEY_SETTINGS_UI_LANGUAGE,
                        GboardSettingsLocaleManager.LANGUAGE_SYSTEM));
    }

    public static void writeLanguagePreference(Context context, String value) {
        if (context == null) {
            return;
        }
        GboardPatchesSettings.preferences(context)
                .edit()
                .putString(
                        GboardPatchesSettings.PREF_KEY_SETTINGS_UI_LANGUAGE,
                        normalizePreferenceValue(value))
                .apply();
    }

    public static String resolveEffectiveLanguageTag(String preferenceValue, Locale deviceLocale) {
        String normalizedPreference = normalizePreferenceValue(preferenceValue);
        if (LANGUAGE_ENGLISH.equals(normalizedPreference)) {
            return normalizedPreference;
        }
        return LANGUAGE_ENGLISH;
    }

    public static Context wrapContext(Context base) {
        if (base == null) {
            return null;
        }
        try {
            SharedPreferences preferences = GboardPatchesSettings.preferences(base);
            Locale locale = toLocale(resolveEffectiveLanguageTag(
                    readLanguagePreference(preferences),
                    extractDeviceLocale(base)));
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(locale);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(new android.os.LocaleList(locale));
            }
            return base.createConfigurationContext(configuration);
        } catch (Throwable ignored) {
            return base;
        }
    }

    private static Locale extractDeviceLocale(Context context) {
        if (context == null || context.getResources() == null) {
            return Locale.getDefault();
        }
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration == null) {
            return Locale.getDefault();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && configuration.getLocales() != null
                && !configuration.getLocales().isEmpty()) {
            return configuration.getLocales().get(0);
        }
        Locale locale = configuration.locale;
        return locale != null ? locale : Locale.getDefault();
    }

    private static Locale toLocale(String languageTag) {
        return Locale.ENGLISH;
    }

    private static String normalizePreferenceValue(String value) {
        if (LANGUAGE_ENGLISH.equals(value)) {
            return LANGUAGE_ENGLISH;
        }
        return LANGUAGE_SYSTEM;
    }
}

