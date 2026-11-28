/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

import java.util.Map;

/**
 * Persists PixelBoard's Rambler voice dictation fixes (Issues #192 and #193).
 */
public final class GboardRamblerSettings {

    public static final String PREF_KEY_INDIC_NATIVE_SCRIPT =
            "pref_rambler_indic_native_script";
    public static final String PREF_KEY_VERBATIM_MODE =
            "pref_rambler_verbatim_mode";
    public static final String PREF_KEY_CUSTOM_INSTRUCTION =
            "pref_rambler_custom_instruction";

    public static final String STOCK_PREF_ENABLE_JETSON = "enable_jetson";
    public static final String STOCK_PREF_CUSTOM_INSTRUCTION =
            "pref_key_rambler_custom_instruction";

    public static final boolean DEFAULT_INDIC_NATIVE_SCRIPT = true;
    public static final boolean DEFAULT_VERBATIM_MODE = false;
    public static final boolean DEFAULT_RAMBLER_ENABLED = true;

    public static final String STRICT_VERBATIM_INSTRUCTION =
            "Transcribe strictly verbatim word-for-word as spoken. "
                    + "Preserve exact wording, repetitions, filler words, and sentence structure. "
                    + "Do not summarize, condense, rephrase, paraphrase, or omit details.";

    private static volatile SharedPreferences preferencesForTests;

    private GboardRamblerSettings() {
    }

    static void setPreferencesForTests(SharedPreferences preferences) {
        preferencesForTests = preferences;
    }

    static void resetForTests() {
        preferencesForTests = null;
    }

    public static SharedPreferences preferences(Context context) {
        SharedPreferences override = preferencesForTests;
        if (override != null) {
            return override;
        }
        if (context == null) {
            return null;
        }
        try {
            return GboardPatchesSettings.preferences(context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isIndicNativeScriptEnabled(Context context) {
        SharedPreferences prefs = preferences(context);
        if (prefs != null && !readBoolean(prefs, PREF_KEY_INDIC_NATIVE_SCRIPT, DEFAULT_INDIC_NATIVE_SCRIPT)) {
            writeBoolean(prefs, PREF_KEY_INDIC_NATIVE_SCRIPT, true);
        }
        return true;
    }

    public static boolean isIndicNativeScriptEnabled(SharedPreferences preferences) {
        return readBoolean(
                preferences,
                PREF_KEY_INDIC_NATIVE_SCRIPT,
                DEFAULT_INDIC_NATIVE_SCRIPT);
    }

    public static boolean writeIndicNativeScriptEnabled(Context context, boolean enabled) {
        return writeBoolean(preferences(context), PREF_KEY_INDIC_NATIVE_SCRIPT, enabled);
    }

    public static boolean writeIndicNativeScriptEnabled(
            SharedPreferences preferences,
            boolean enabled) {
        return writeBoolean(preferences, PREF_KEY_INDIC_NATIVE_SCRIPT, enabled);
    }

    public static boolean isVerbatimModeEnabled(Context context) {
        return readBoolean(
                preferences(context),
                PREF_KEY_VERBATIM_MODE,
                DEFAULT_VERBATIM_MODE);
    }

    public static boolean isVerbatimModeEnabled(SharedPreferences preferences) {
        return readBoolean(
                preferences,
                PREF_KEY_VERBATIM_MODE,
                DEFAULT_VERBATIM_MODE);
    }

    public static boolean writeVerbatimModeEnabled(Context context, boolean enabled) {
        return writeBoolean(preferences(context), PREF_KEY_VERBATIM_MODE, enabled);
    }

    public static boolean writeVerbatimModeEnabled(
            SharedPreferences preferences,
            boolean enabled) {
        return writeBoolean(preferences, PREF_KEY_VERBATIM_MODE, enabled);
    }

    public static boolean isRamblerEnabled(Context context) {
        SharedPreferences stock = stockPreferences(context);
        if (stock != null && stock.contains(STOCK_PREF_ENABLE_JETSON)) {
            return readBoolean(stock, STOCK_PREF_ENABLE_JETSON, DEFAULT_RAMBLER_ENABLED);
        }
        return DEFAULT_RAMBLER_ENABLED;
    }

    public static boolean writeRamblerEnabled(Context context, boolean enabled) {
        SharedPreferences stock = stockPreferences(context);
        boolean ok = false;
        if (stock != null) {
            ok = stock.edit().putBoolean(STOCK_PREF_ENABLE_JETSON, enabled).commit();
        }
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(enabled);
        return ok;
    }

    public static String readCustomInstruction(Context context) {
        String custom = readString(preferences(context), PREF_KEY_CUSTOM_INSTRUCTION, "");
        if (!custom.isEmpty()) {
            return custom;
        }
        if (preferencesForTests != null) {
            return "";
        }
        SharedPreferences stock = stockPreferences(context);
        if (stock != null) {
            return readString(stock, STOCK_PREF_CUSTOM_INSTRUCTION, "");
        }
        return "";
    }

    public static String readCustomInstruction(SharedPreferences preferences) {
        return readString(preferences, PREF_KEY_CUSTOM_INSTRUCTION, "");
    }

    public static boolean writeCustomInstruction(Context context, String instruction) {
        String sanitized = instruction == null ? "" : instruction.trim();
        SharedPreferences patchPrefs = preferences(context);
        boolean ok = false;
        if (patchPrefs != null) {
            ok = patchPrefs.edit().putString(PREF_KEY_CUSTOM_INSTRUCTION, sanitized).commit();
        }
        SharedPreferences stock = stockPreferences(context);
        if (stock != null) {
            boolean stockOk = stock.edit()
                    .putString(STOCK_PREF_CUSTOM_INSTRUCTION, sanitized)
                    .commit();
            ok = ok || stockOk;
        }
        return ok;
    }

    public static boolean writeCustomInstruction(
            SharedPreferences preferences,
            String instruction) {
        if (preferences == null) {
            return false;
        }
        String sanitized = instruction == null ? "" : instruction.trim();
        return preferences.edit().putString(PREF_KEY_CUSTOM_INSTRUCTION, sanitized).commit();
    }

    private static SharedPreferences stockPreferences(Context context) {
        if (context == null) {
            return null;
        }
        Context target = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !context.isDeviceProtectedStorage()) {
            try {
                Context deContext = context.createDeviceProtectedStorageContext();
                if (deContext != null) {
                    target = deContext;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            return PreferenceManager.getDefaultSharedPreferences(target);
        } catch (Throwable ignored) {
            try {
                return PreferenceManager.getDefaultSharedPreferences(context);
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private static boolean readBoolean(
            SharedPreferences preferences,
            String key,
            boolean defaultValue) {
        if (preferences == null) {
            return defaultValue;
        }
        Map<String, ?> values = preferences.getAll();
        Object raw = values.get(key);
        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        if (raw instanceof String) {
            if ("true".equalsIgnoreCase((String) raw)) {
                return true;
            }
            if ("false".equalsIgnoreCase((String) raw)) {
                return false;
            }
        }
        return defaultValue;
    }

    private static boolean writeBoolean(
            SharedPreferences preferences,
            String key,
            boolean value) {
        if (preferences == null) {
            return false;
        }
        return preferences.edit().putBoolean(key, value).commit();
    }

    private static String readString(
            SharedPreferences preferences,
            String key,
            String defaultValue) {
        if (preferences == null) {
            return defaultValue;
        }
        Object raw = preferences.getAll().get(key);
        if (raw instanceof String) {
            return ((String) raw).trim();
        }
        return defaultValue;
    }
}
