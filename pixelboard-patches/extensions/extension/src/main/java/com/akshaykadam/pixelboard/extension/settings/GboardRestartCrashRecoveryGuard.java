package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

final class GboardRestartCrashRecoveryGuard {
    static final String STOCK_CLEAN_FLAG_KEY = "crash_recovery_enable_clean_flag";
    static final String RESTORE_MARKER_KEY =
            "pref_restart_restore_crash_recovery_clean_flag";

    private GboardRestartCrashRecoveryGuard() {
    }

    static boolean prepare(Context context) {
        try {
            Context applicationContext = context == null
                    ? null
                    : context.getApplicationContext();
            Context lookupContext = applicationContext != null ? applicationContext : context;
            if (lookupContext == null) {
                return false;
            }
            return prepare(
                    stockPreferences(lookupContext),
                    GboardPatchesSettings.preferences(lookupContext));
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean prepare(
            SharedPreferences stockPreferences,
            SharedPreferences patchesPreferences) {
        if (stockPreferences == null || patchesPreferences == null) {
            return false;
        }
        final boolean cleanFlagEnabled;
        try {
            cleanFlagEnabled = stockPreferences.getBoolean(STOCK_CLEAN_FLAG_KEY, true);
        } catch (Throwable ignored) {
            return false;
        }
        if (!cleanFlagEnabled) {
            return true;
        }

        final boolean markerCommitted;
        try {
            markerCommitted = patchesPreferences.edit()
                    .putBoolean(RESTORE_MARKER_KEY, true)
                    .commit();
        } catch (Throwable ignored) {
            clearMarkerBestEffort(patchesPreferences);
            return false;
        }
        if (!markerCommitted) {
            clearMarkerBestEffort(patchesPreferences);
            return false;
        }

        try {
            if (stockPreferences.edit()
                    .putBoolean(STOCK_CLEAN_FLAG_KEY, false)
                    .commit()) {
                return true;
            }
        } catch (Throwable ignored) {
            // Roll back below and retain the marker if the rollback cannot be persisted.
        }
        rollbackAfterFailedPreparation(stockPreferences, patchesPreferences);
        return false;
    }

    static boolean restoreIfPending(Context context) {
        try {
            Context applicationContext = context == null
                    ? null
                    : context.getApplicationContext();
            Context lookupContext = applicationContext != null ? applicationContext : context;
            if (lookupContext == null) {
                return false;
            }
            return restoreIfPending(
                    stockPreferences(lookupContext),
                    GboardPatchesSettings.preferences(lookupContext));
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean restoreIfPending(
            SharedPreferences stockPreferences,
            SharedPreferences patchesPreferences) {
        if (stockPreferences == null || patchesPreferences == null) {
            return false;
        }
        try {
            if (!patchesPreferences.getBoolean(RESTORE_MARKER_KEY, false)) {
                return true;
            }
            if (!stockPreferences.edit()
                    .putBoolean(STOCK_CLEAN_FLAG_KEY, true)
                    .commit()) {
                return false;
            }
            return patchesPreferences.edit()
                    .remove(RESTORE_MARKER_KEY)
                    .commit();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void rollbackAfterFailedPreparation(
            SharedPreferences stockPreferences,
            SharedPreferences patchesPreferences) {
        try {
            if (stockPreferences.edit()
                    .putBoolean(STOCK_CLEAN_FLAG_KEY, true)
                    .commit()) {
                clearMarkerBestEffort(patchesPreferences);
            }
        } catch (Throwable ignored) {
            // Keep the marker so a later activity startup can retry the restoration.
        }
    }

    private static void clearMarkerBestEffort(SharedPreferences patchesPreferences) {
        try {
            patchesPreferences.edit()
                    .remove(RESTORE_MARKER_KEY)
                    .commit();
        } catch (Throwable ignored) {
            // The guard must never escape into the host app.
        }
    }

    private static SharedPreferences stockPreferences(Context context) {
        Context stockContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !context.isDeviceProtectedStorage()) {
            Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
            if (deviceProtectedContext == null) {
                throw new IllegalStateException(
                        "Device-protected context required for Gboard crash recovery settings.");
            }
            stockContext = deviceProtectedContext;
        }
        return PreferenceManager.getDefaultSharedPreferences(stockContext);
    }
}
