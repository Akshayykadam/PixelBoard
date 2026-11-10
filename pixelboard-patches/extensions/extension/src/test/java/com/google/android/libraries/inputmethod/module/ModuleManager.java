// SPDX-License-Identifier: GPL-3.0-only
package com.google.android.libraries.inputmethod.module;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Native boundary simulation: flag readiness stays cached until updateFlagReady re-checks it. */
public final class ModuleManager {
    private static ModuleManager current = new ModuleManager();
    private final Map<Class<?>, ModuleInitListener> listeners = new LinkedHashMap<>();

    public static ModuleManager getInstance(Context context) {
        return current;
    }

    public static void reset() {
        current = new ModuleManager();
    }

    public Map<Class<?>, ModuleInitListener> getModuleInitListenersMap() {
        return listeners;
    }

    public ModuleInitListener register(Class<?> type, BooleanSupplier flags, Runnable load) {
        ModuleInitListener listener = new ModuleInitListener(flags, load);
        listeners.put(type, listener);
        return listener;
    }

    public static final class ModuleInitListener {
        private final BooleanSupplier flags;
        private final Runnable load;
        private boolean isFlagReady;
        public int refreshes;

        private ModuleInitListener(BooleanSupplier flags, Runnable load) {
            this.flags = flags;
            this.load = load;
        }

        private void updateFlagReady() {
            refreshes++;
            boolean ready = flags.getAsBoolean();
            if (ready && !isFlagReady) load.run();
            isFlagReady = ready;
        }
    }
}
