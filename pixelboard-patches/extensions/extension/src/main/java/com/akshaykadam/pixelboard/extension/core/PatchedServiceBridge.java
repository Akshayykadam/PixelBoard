package com.akshaykadam.pixelboard.extension.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class PatchedServiceBridge {
    public interface OnServiceBoundListener {
        void onServiceBound(Context boundContext, Object service);
    }

    private PatchedServiceBridge() {
    }

    public static void initialize(Context context) {
        // The patched app keeps settings in-process, so no extra bridge setup is required.
    }

    public static void registerOnServiceBoundListener(OnServiceBoundListener listener) {
        // The patched app does not expose a separate remote service bridge.
    }

    public static SharedPreferences remotePreferences(String prefFile) {
        return null;
    }
}
