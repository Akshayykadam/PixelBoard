// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.content.SharedPreferences;

import com.akshaykadam.pixelboard.extension.advancedvoice.GboardAdvancedVoiceSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

import java.util.Map;

/** What a tap on the Gboard 18.4.1 microphone does. */
public final class GboardMicGestureSettings {
    public static final String PREF_KEY_MODE = "pref_mic_tap_mode";

    /** Gboard's own microphone: no gesture surface and no tap delay. */
    public static final String MODE_DEFAULT = "default";

    /** Tap starts Standard after 300 ms; double tap starts Rambler. */
    public static final String MODE_HYBRID = "hybrid";

    /** Double tap only switches Gboard's Standard/Rambler selection; tap starts that mode. */
    public static final String MODE_QUICK_SWITCH = "quick_switch";

    /** Earlier test builds of this setting saved an on/off switch; on carries over as Hybrid. */
    static final String LEGACY_PREF_KEY_ENABLED = "pref_rambler_double_tap_enabled";

    private static final java.util.WeakHashMap<Context, SharedPreferences> PREFERENCES =
            new java.util.WeakHashMap<>();

    private GboardMicGestureSettings() {}

    private static synchronized SharedPreferences preferences(Context context) {
        Context app = context.getApplicationContext();
        if (app == null) app = context;
        SharedPreferences preferences = PREFERENCES.get(app);
        if (preferences == null) {
            preferences = GboardPatchesSettings.preferences(app);
            PREFERENCES.put(app, preferences);
        }
        return preferences;
    }

    /** Missing or unknown values mean Default. */
    public static String readMode(SharedPreferences preferences) {
        if (preferences == null) return MODE_DEFAULT;
        Map<String, ?> values = preferences.getAll();
        Object mode = values.get(PREF_KEY_MODE);
        if (mode == null) {
            return Boolean.TRUE.equals(values.get(LEGACY_PREF_KEY_ENABLED))
                    ? MODE_HYBRID
                    : MODE_DEFAULT;
        }
        return MODE_HYBRID.equals(mode) || MODE_QUICK_SWITCH.equals(mode)
                ? (String) mode
                : MODE_DEFAULT;
    }

    /** The active mode. The tap modes also need Advanced Voice Typing to be enabled. */
    public static String mode(Context context) {
        SharedPreferences preferences = preferences(context);
        String mode = readMode(preferences);
        if (!MODE_DEFAULT.equals(mode) && !GboardAdvancedVoiceSettings.readEnabled(preferences)) {
            return MODE_DEFAULT;
        }
        return mode;
    }

    /** True while Hybrid or Quick switch is active. */
    public static boolean isEnabled(Context context) {
        return !MODE_DEFAULT.equals(mode(context));
    }

    public static boolean writeMode(Context context, String mode) {
        String value =
                MODE_HYBRID.equals(mode) || MODE_QUICK_SWITCH.equals(mode) ? mode : MODE_DEFAULT;
        boolean committed =
                preferences(context)
                        .edit()
                        .putString(PREF_KEY_MODE, value)
                        .remove(LEGACY_PREF_KEY_ENABLED)
                        .commit();
        if (committed) GboardMicGestureRuntime.settingsChanged();
        return committed;
    }

    /** Other supported Gboard versions retain their original microphone route. */
    public static boolean isSupported(Context context) {
        try {
            ClassLoader loader = context.getClassLoader();
            Class.forName("pbb", false, loader)
                    .getMethod("aw", Class.forName("oas", false, loader));
            Class.forName("qou", false, loader).getMethod("I", Context.class);
            Class.forName("mvq", false, loader).getMethod("a", Context.class);
            return true;
        } catch (ReflectiveOperationException error) {
            return false;
        }
    }
}
