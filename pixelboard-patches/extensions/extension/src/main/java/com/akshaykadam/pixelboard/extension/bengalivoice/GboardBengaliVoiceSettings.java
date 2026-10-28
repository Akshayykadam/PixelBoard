package com.akshaykadam.pixelboard.extension.bengalivoice;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

/** Persists a voice choice per Bengali region without changing global dictation preferences. */
public final class GboardBengaliVoiceSettings {
    private static final String KEY_PREFIX = "pref_bengali_voice_mode.";

    private GboardBengaliVoiceSettings() {
    }

    /** Returns null when this language has no Bengali-specific override. */
    public static String readMode(Context context, String languageTag) {
        if (!GboardBengaliVoicePolicy.isBengali(languageTag)) {
            return null;
        }
        try {
            return readMode(GboardPatchesSettings.preferences(context), languageTag);
        } catch (RuntimeException unavailablePreferences) {
            return GboardBengaliVoicePolicy.DEFAULT_MODE;
        }
    }

    static String readMode(SharedPreferences preferences, String languageTag) {
        String key = preferenceKey(languageTag);
        if (key == null) {
            return null;
        }
        if (preferences == null) {
            return GboardBengaliVoicePolicy.DEFAULT_MODE;
        }
        return GboardBengaliVoicePolicy.modeOrDefault(preferences.getAll().get(key));
    }

    public static boolean writeMode(Context context, String languageTag, String mode) {
        if (context == null || !GboardBengaliVoicePolicy.isBengali(languageTag)
                || !GboardBengaliVoicePolicy.isValidMode(mode)) {
            return false;
        }
        try {
            return writeMode(GboardPatchesSettings.preferences(context), languageTag, mode);
        } catch (RuntimeException unavailablePreferences) {
            return false;
        }
    }

    static boolean writeMode(SharedPreferences preferences, String languageTag, String mode) {
        String key = preferenceKey(languageTag);
        if (preferences == null || key == null || !GboardBengaliVoicePolicy.isValidMode(mode)) {
            return false;
        }
        synchronized (preferences) {
            final Map<String, ?> previousValues;
            try {
                previousValues = preferences.getAll();
            } catch (RuntimeException unavailablePreferences) {
                return false;
            }
            if (previousValues == null) {
                return false;
            }
            boolean previouslyPresent = previousValues.containsKey(key);
            String previousMode = GboardBengaliVoicePolicy.modeOrDefault(previousValues.get(key));
            try {
                if (preferences.edit().putString(key, mode).commit()) {
                    return true;
                }
            } catch (RuntimeException failedWrite) {
                // A commit can update live memory before failing to write to storage.
            }
            // Restore only this choice, so a failed save does not normally leave
            // routing on a new live value while the UI reports the old selection.
            // This is best effort: unavailable storage cannot guarantee disk rollback.
            try {
                SharedPreferences.Editor rollback = preferences.edit();
                if (previouslyPresent) rollback.putString(key, previousMode);
                else rollback.remove(key);
                rollback.commit();
            } catch (RuntimeException failedRollback) {
                // Preserve the failure result even when restoration is unavailable.
            }
            return false;
        }
    }

    static String preferenceKey(String languageTag) {
        String normalized = GboardBengaliVoicePolicy.normalizeBengaliTag(languageTag);
        return normalized == null ? null : KEY_PREFIX + normalized;
    }
}
