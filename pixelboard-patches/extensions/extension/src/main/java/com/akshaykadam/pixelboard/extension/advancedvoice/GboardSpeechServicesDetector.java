package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.Context;
import android.content.pm.PackageManager;

public final class GboardSpeechServicesDetector {
    public static final String PACKAGE_NAME = "com.google.android.tts";

    private GboardSpeechServicesDetector() {
    }

    public static Status detect(Context context) {
        if (context == null) {
            return Status.UNKNOWN;
        }
        try {
            Context applicationContext = context.getApplicationContext();
            Context lookupContext = applicationContext != null ? applicationContext : context;
            PackageManager packageManager = lookupContext.getPackageManager();
            if (packageManager == null) {
                return Status.UNKNOWN;
            }
            packageManager.getApplicationInfo(PACKAGE_NAME, 0);
            return Status.PRESENT;
        } catch (PackageManager.NameNotFoundException exception) {
            return Status.ABSENT;
        } catch (Throwable throwable) {
            return Status.UNKNOWN;
        }
    }

    public enum Status {
        PRESENT,
        ABSENT,
        UNKNOWN
    }
}
