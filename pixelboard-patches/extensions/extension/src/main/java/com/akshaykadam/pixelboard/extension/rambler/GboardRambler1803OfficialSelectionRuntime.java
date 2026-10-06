package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import java.lang.reflect.Method;

/**
 * Keeps Agentic capability exposure aligned with Gboard's official selector.
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardRambler1803OfficialSelectionRuntime {
    private static final ThreadLocal<Integer> VOICE_SETTINGS_SCOPE_DEPTH =
            new ThreadLocal<Integer>();
    private static final ThreadLocal<Integer> DEFAULT_SELECTION_SUPPRESSION_DEPTH =
            new ThreadLocal<Integer>();

    private static volatile Boolean officialRamblerSelected;

    private GboardRambler1803OfficialSelectionRuntime() {
    }

    public static boolean shouldEnableAgenticDictation() {
        if (isDefaultSelectionSuppressed()) {
            return false;
        }
        if (isVoiceSettingsScopeActive()) {
            return true;
        }
        Boolean selected = officialRamblerSelected;
        if (selected == null) {
            selected = readOfficialSelection();
        }
        return selected == null || selected.booleanValue();
    }

    public static void enterVoiceSettingsScope() {
        VOICE_SETTINGS_SCOPE_DEPTH.set(Integer.valueOf(depth(VOICE_SETTINGS_SCOPE_DEPTH) + 1));
    }

    public static void exitVoiceSettingsScope() {
        decrement(VOICE_SETTINGS_SCOPE_DEPTH);
    }

    public static void updateOfficialSelection(boolean selected) {
        if (!selected && !hasExplicitlyDisabledPreference()) {
            return;
        }
        officialRamblerSelected = Boolean.valueOf(selected);
    }

    public static void enterDefaultSelectionSuppression() {
        DEFAULT_SELECTION_SUPPRESSION_DEPTH.set(Integer.valueOf(
                depth(DEFAULT_SELECTION_SUPPRESSION_DEPTH) + 1));
    }

    public static void exitDefaultSelectionSuppression() {
        decrement(DEFAULT_SELECTION_SUPPRESSION_DEPTH);
    }

    private static boolean isVoiceSettingsScopeActive() {
        return depth(VOICE_SETTINGS_SCOPE_DEPTH) > 0;
    }

    private static boolean isDefaultSelectionSuppressed() {
        return depth(DEFAULT_SELECTION_SUPPRESSION_DEPTH) > 0;
    }

    private static int depth(ThreadLocal<Integer> scope) {
        Integer value = scope.get();
        return value == null ? 0 : value.intValue();
    }

    private static void decrement(ThreadLocal<Integer> scope) {
        int next = depth(scope) - 1;
        if (next <= 0) {
            scope.remove();
        } else {
            scope.set(Integer.valueOf(next));
        }
    }

    private static Boolean readOfficialSelection() {
        try {
            Context context = resolveContext();
            if (context == null) {
                return null;
            }
            SharedPreferences prefs = resolveStockPreferences(context);
            if (prefs != null && prefs.contains("enable_jetson")) {
                boolean val = prefs.getBoolean("enable_jetson", true);
                officialRamblerSelected = Boolean.valueOf(val);
                return officialRamblerSelected;
            }

            ClassLoader loader = context.getClassLoader();

            // 1. In 18.3.1+: check aaeo.a(Context) only if it confirms true
            try {
                Class<?> support = Class.forName("aaeo", false, loader);
                Method selection = support.getDeclaredMethod("a", Context.class);
                selection.setAccessible(true);
                Object value = selection.invoke(null, context);
                if (Boolean.TRUE.equals(value)) {
                    officialRamblerSelected = Boolean.TRUE;
                    return Boolean.TRUE;
                }
            } catch (Throwable ignored) {
            }

            // 2. In 18.0.3: check mqk.a(Context) only if it confirms true
            try {
                Class<?> support = Class.forName("mqk", false, loader);
                Method selection = support.getDeclaredMethod("a", Context.class);
                selection.setAccessible(true);
                Object value = selection.invoke(null, context);
                if (Boolean.TRUE.equals(value)) {
                    officialRamblerSelected = Boolean.TRUE;
                    return Boolean.TRUE;
                }
            } catch (Throwable ignored) {
            }

            officialRamblerSelected = Boolean.TRUE;
            return Boolean.TRUE;
        } catch (Throwable ignored) {
            // Application or the exact formal selector may not be ready yet.
        }
        return null;
    }

    private static boolean hasExplicitlyDisabledPreference() {
        Context context = resolveContext();
        if (context == null) {
            return true;
        }
        try {
            SharedPreferences prefs = resolveStockPreferences(context);
            if (prefs != null && prefs.contains("enable_jetson")) {
                return !prefs.getBoolean("enable_jetson", true);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static SharedPreferences resolveStockPreferences(Context context) {
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

    private static Context resolveContext() {
        Context context = reflectedContext("android.app.ActivityThread", "currentApplication");
        if (context == null) {
            context = reflectedContext("android.app.AppGlobals", "getInitialApplication");
        }
        return context;
    }

    private static Context reflectedContext(String className, String methodName) {
        try {
            Class<?> owner = Class.forName(className);
            Method method = owner.getMethod(methodName);
            Object value = method.invoke(null);
            if (value instanceof Context) {
                Context ctx = (Context) value;
                Context app = ctx.getApplicationContext();
                return app != null ? app : ctx;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static void resetForTests() {
        VOICE_SETTINGS_SCOPE_DEPTH.remove();
        DEFAULT_SELECTION_SUPPRESSION_DEPTH.remove();
        officialRamblerSelected = null;
    }
}
