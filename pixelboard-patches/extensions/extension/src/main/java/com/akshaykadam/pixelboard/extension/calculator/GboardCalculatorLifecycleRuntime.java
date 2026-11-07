package com.akshaykadam.pixelboard.extension.calculator;

import android.content.Context;
import android.os.Build;
import android.content.SharedPreferences;
import android.view.View;

/** Patched-APK adapter between lifecycle delegates, preferences, and portable runtime. */
public final class GboardCalculatorLifecycleRuntime {
    // Kept local so the feature-scoped RVE never links against shared settings classes.
    private static final String PREF_FILE = "gboard_patches_settings";
    private static final String PREF_KEY_ENABLED = "pref_simple_calculator_enabled";
    private static final boolean DEFAULT_ENABLED = false;

    private GboardCalculatorLifecycleRuntime() {
    }

    public static void onInputViewStarted(Object receiver) {
        boolean enabled = GboardCalculatorSafety.booleanValue(
                () -> readEnabled(receiver), false);
        GboardCalculatorSafety.run(() -> GboardCalculatorRuntime.onInputViewStarted(
                receiver, enabled));
    }

    public static void onSelectionUpdated(Object receiver, View inputView,
            int selectionStart, int selectionEnd) {
        GboardCalculatorSafety.run(() -> GboardCalculatorRuntime.onSelectionUpdated(
                receiver, inputView, selectionStart, selectionEnd));
    }

    public static void onInputWindowHidden() {
        GboardCalculatorSafety.run(GboardCalculatorRuntime::onInputWindowHidden);
    }

    private static boolean readEnabled(Object receiver) {
        if (!(receiver instanceof Context context)) {
            return false;
        }
        Context applicationContext = context.getApplicationContext();
        Context lookupContext = applicationContext != null ? applicationContext : context;
        // The settings UI stores preferences in device-protected storage
        // (see GboardPatchesSettings.preferences); read from the same place,
        // otherwise the toggle value is never seen here.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (!lookupContext.isDeviceProtectedStorage()) {
                    Context deContext = lookupContext.createDeviceProtectedStorageContext();
                    if (deContext != null) {
                        try {
                            deContext.moveSharedPreferencesFrom(lookupContext, PREF_FILE);
                        } catch (Throwable ignored) {
                            // May fail while credential storage is locked.
                        }
                        lookupContext = deContext;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        SharedPreferences preferences = lookupContext.getSharedPreferences(
                PREF_FILE, Context.MODE_PRIVATE);
        Object raw = preferences.getAll().get(PREF_KEY_ENABLED);
        if (raw instanceof Boolean value) {
            return value.booleanValue();
        }
        if (raw instanceof String value) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
        }
        return DEFAULT_ENABLED;
    }
}
