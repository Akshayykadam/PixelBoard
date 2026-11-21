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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Keeps a Rambler session alive across a screen rotation.
 *
 * <p>A rotation makes Gboard clear and rebuild its input method data, which deactivates the
 * Agentic Dictation extension and reactivates it about 80 ms later. Gboard's
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

    /** Baseline delay Gboard waits before stopping Rambler, preventing timing races on flips/rotations. */
    static final long BASELINE_DEACTIVATION_DELAY_MS = 1000L;
    /** Grace Gboard waits before stopping Rambler; reactivation after a rotation takes ~80 ms. */
    static final long DEACTIVATION_GRACE_MS = 2500L;
    /** How long after a display change a deactivation still counts as part of the rotation. */
    static final long DISPLAY_CHANGE_WINDOW_MS = 2000L;

    private static final int UNKNOWN = Integer.MIN_VALUE;
    private static final int MAX_INFO_LOGS = 30;
    private static final AtomicInteger INFO_LOG_COUNT = new AtomicInteger();

    private static final Object LOCK = new Object();
    private static volatile boolean listening;
    private static int orientation = UNKNOWN;
    private static int widthDp = UNKNOWN;
    private static int heightDp = UNKNOWN;
    private static int displayRotation = UNKNOWN;
    /** Uptime until which deactivations get the grace; 0 until the display first changes. */
    private static volatile long graceUntil;
    private static volatile LongSupplier clock;

    private GboardRamblerRotationGrace() {
    }

    /** Returns the deactivation delay to hand Gboard: a grace period mid-rotation, else baseline delay. */
    static long deactivationDelayMs() {
        long delay = deactivationDelayMs(nowUptimeMs());
        if (delay > BASELINE_DEACTIVATION_DELAY_MS) {
            logInfo("display changed, keeping Rambler alive for " + delay + " ms");
        }
        return delay;
    }

    static long deactivationDelayMs(long nowUptimeMs) {
        return nowUptimeMs <= graceUntil ? DEACTIVATION_GRACE_MS : BASELINE_DEACTIVATION_DELAY_MS;
    }

    /**
     * Starts tracking display changes. Gboard reads the deactivation delay only inside
     * {@code onDeactivate}, so this must run on earlier flag reads: tracking that starts during
     * the first rotation would take the rotated display as its baseline. Cheap once started.
     */
    static void ensureListening() {
        if (listening) {
            return;
        }
        ensureListening(GboardRambler1803OfficialSelectionRuntime.applicationContext());
    }

    static void ensureListening(final Context context) {
        if (listening || context == null) {
            return;
        }
        synchronized (LOCK) {
            if (listening) {
                return;
            }
            try {
                // Register before taking the baseline, so a change in between is not missed.
                context.registerComponentCallbacks(new ComponentCallbacks() {
                    @Override
                    public void onConfigurationChanged(Configuration newConfig) {
                        record(context, newConfig);
                    }

                    @Override
                    public void onLowMemory() {
                    }
                });
                record(context, context.getResources().getConfiguration());
            } catch (Throwable failure) {
                logInfo("could not track display changes: " + failure);
            }
            // Never retry a failed registration on every flag read.
            listening = true;
        }
    }

    private static void record(Configuration configuration) {
        record(null, configuration);
    }

    private static void record(Context context, Configuration configuration) {
        int rot = UNKNOWN;
        if (context != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    android.view.Display display = context.getDisplay();
                    if (display != null) {
                        rot = display.getRotation();
                    }
                }
                if (rot == UNKNOWN) {
                    android.view.WindowManager wm = (android.view.WindowManager)
                            context.getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null && wm.getDefaultDisplay() != null) {
                        rot = wm.getDefaultDisplay().getRotation();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        onDisplayConfigurationWithRotation(configuration.orientation, configuration.screenWidthDp,
                configuration.screenHeightDp, rot, nowUptimeMs());
    }

    private static long nowUptimeMs() {
        LongSupplier override = clock;
        return override != null ? override.getAsLong() : SystemClock.uptimeMillis();
    }

    static void onDisplayConfiguration(int newOrientation, int newWidthDp, int newHeightDp,
            long nowUptimeMs) {
        onDisplayConfigurationWithRotation(newOrientation, newWidthDp, newHeightDp, UNKNOWN, nowUptimeMs);
    }

    static void onDisplayConfigurationWithRotation(int newOrientation, int newWidthDp, int newHeightDp,
            int newRotation, long nowUptimeMs) {
        synchronized (LOCK) {
            boolean known = orientation != UNKNOWN;
            boolean rotKnown = displayRotation != UNKNOWN;
            boolean rotChanged = newRotation != UNKNOWN && (!rotKnown || newRotation != displayRotation);
            boolean configChanged = newOrientation != orientation
                    || newWidthDp != widthDp
                    || newHeightDp != heightDp;
            orientation = newOrientation;
            widthDp = newWidthDp;
            heightDp = newHeightDp;
            if (newRotation != UNKNOWN) {
                displayRotation = newRotation;
            }
            if ((known && configChanged) || (rotKnown && rotChanged)) {
                graceUntil = nowUptimeMs + DISPLAY_CHANGE_WINDOW_MS;
            }
        }
    }

    private static void logInfo(String message) {
        try {
            if (INFO_LOG_COUNT.getAndIncrement() >= MAX_INFO_LOGS) {
                return;
            }
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
            INFO_LOG_COUNT.set(0);
            listening = false;
            orientation = UNKNOWN;
            widthDp = UNKNOWN;
            heightDp = UNKNOWN;
            displayRotation = UNKNOWN;
            graceUntil = 0L;
        }
    }
}
