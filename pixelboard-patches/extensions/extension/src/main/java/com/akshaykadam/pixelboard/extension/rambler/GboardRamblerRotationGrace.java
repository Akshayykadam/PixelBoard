/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.util.Log;

import java.util.function.LongSupplier;

/**
 * Keeps a Rambler session alive across a screen rotation.
 *
 * <p>A rotation makes Gboard clear and rebuild its input method data, which deactivates the
 * Agentic Dictation extension and reactivates it about 100 ms later. Gboard's
 * {@code agentic_dictation_deactivation_delay_ms} grace period exists for that gap: a reactivation
 * inside it cancels the pending stop. PixelBoard forces the delay to 0 so closing the keyboard
 * stops Rambler instantly, which also made every rotation abort the session (issue #146).</p>
 *
 * <p>This keeps the instant stop, and only hands Gboard a short grace period while the display
 * has just changed orientation or size.</p>
 */
public final class GboardRamblerRotationGrace {
    private static final String TAG = "GboardPatches";
    private static final String LOG_PREFIX = "[rambler-rotation-grace] ";

    /** Grace Gboard waits before stopping Rambler; reactivation after a rotation takes ~100 ms. */
    static final long DEACTIVATION_GRACE_MS = 1500L;
    /** How long after a display change a deactivation still counts as part of the rotation. */
    static final long DISPLAY_CHANGE_WINDOW_MS = 2000L;

    private static final int UNKNOWN = Integer.MIN_VALUE;

    private static final Object LOCK = new Object();
    private static volatile boolean listening;
    private static int orientation = UNKNOWN;
    private static int widthDp = UNKNOWN;
    private static int heightDp = UNKNOWN;
    private static volatile long displayChangedAt;
    private static volatile boolean displayChanged;
    private static volatile LongSupplier clock;

    private GboardRamblerRotationGrace() {
    }

    /** Returns the deactivation delay to hand Gboard: a grace period mid-rotation, else 0. */
    static long deactivationDelayMs() {
        ensureListening();
        if (!displayChanged) {
            return 0L;
        }
        long delay = deactivationDelayMs(nowUptimeMs());
        if (delay > 0L) {
            logInfo("display changed, keeping Rambler alive for " + delay + " ms");
        }
        return delay;
    }

    static long deactivationDelayMs(long nowUptimeMs) {
        if (!displayChanged) {
            return 0L;
        }
        long elapsed = nowUptimeMs - displayChangedAt;
        return elapsed >= 0L && elapsed <= DISPLAY_CHANGE_WINDOW_MS ? DEACTIVATION_GRACE_MS : 0L;
    }

    /** Starts tracking display changes; cheap to call on every flag read. */
    static void ensureListening() {
        if (listening) {
            return;
        }
        Context context = GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            if (listening) {
                return;
            }
            try {
                record(context.getResources().getConfiguration());
                context.registerComponentCallbacks(new ComponentCallbacks() {
                    @Override
                    public void onConfigurationChanged(Configuration newConfig) {
                        record(newConfig);
                    }

                    @Override
                    public void onLowMemory() {
                    }
                });
                listening = true;
            } catch (Throwable failure) {
                logInfo("could not track display changes: " + failure);
            }
        }
    }

    private static void record(Configuration configuration) {
        if (configuration == null) {
            return;
        }
        onDisplayConfiguration(configuration.orientation, configuration.screenWidthDp,
                configuration.screenHeightDp, nowUptimeMs());
    }

    private static long nowUptimeMs() {
        LongSupplier override = clock;
        return override != null ? override.getAsLong() : SystemClock.uptimeMillis();
    }

    static void onDisplayConfiguration(int newOrientation, int newWidthDp, int newHeightDp,
            long nowUptimeMs) {
        synchronized (LOCK) {
            boolean known = orientation != UNKNOWN;
            boolean changed = newOrientation != orientation
                    || newWidthDp != widthDp
                    || newHeightDp != heightDp;
            orientation = newOrientation;
            widthDp = newWidthDp;
            heightDp = newHeightDp;
            if (known && changed) {
                displayChangedAt = nowUptimeMs;
                displayChanged = true;
            }
        }
    }

    private static void logInfo(String message) {
        try {
            Log.i(TAG, LOG_PREFIX + message);
        } catch (Throwable ignored) {
        }
    }

    static void setClockForTests(LongSupplier testClock) {
        clock = testClock;
    }

    static void resetForTests() {
        synchronized (LOCK) {
            clock = null;
            listening = false;
            orientation = UNKNOWN;
            widthDp = UNKNOWN;
            heightDp = UNKNOWN;
            displayChangedAt = 0L;
            displayChanged = false;
        }
    }
}
