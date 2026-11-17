/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.keyboard;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;

/**
 * Material 3 Expressive spring and easing motion primitives shared across
 * keyboard animations (sunburst toolbar buttons, key presses, popups,
 * and panel/toolbar transitions).
 */
public final class M3ExpressiveMotion {
    public static final long DURATION_PRESS_IN_MS = 65L;
    public static final long DURATION_SUNBURST_PRESS_MS = 110L;
    public static final long DURATION_FAST_SPATIAL_MS = 190L;
    public static final long DURATION_DEFAULT_SPATIAL_MS = 280L;
    public static final long DURATION_EFFECTS_FAST_MS = 140L;
    /** Long, calm glide for layout shifts the eye follows, such as toolbar rows moving. */
    public static final long DURATION_SMOOTH_SPATIAL_MS = 420L;

    static final float FAST_SPATIAL_DAMPING = 0.58f;
    static final float FAST_SPATIAL_CYCLES = 1.35f;
    static final float DEFAULT_SPATIAL_DAMPING = 0.65f;
    static final float DEFAULT_SPATIAL_CYCLES = 1.25f;
    /** Near-critical damping: eases in and settles without a visible bounce. */
    static final float SMOOTH_SPATIAL_DAMPING = 0.92f;
    static final float SMOOTH_SPATIAL_CYCLES = 0.95f;

    private static final TimeInterpolator SPATIAL_FAST =
            new SpringInterpolator(FAST_SPATIAL_DAMPING, FAST_SPATIAL_CYCLES);
    private static final TimeInterpolator SPATIAL_DEFAULT =
            new SpringInterpolator(DEFAULT_SPATIAL_DAMPING, DEFAULT_SPATIAL_CYCLES);
    private static final TimeInterpolator SPATIAL_SMOOTH =
            new SpringInterpolator(SMOOTH_SPATIAL_DAMPING, SMOOTH_SPATIAL_CYCLES);
    private static final TimeInterpolator PRESS_IN = Build.VERSION.SDK_INT >= 21
            ? new PathInterpolator(0.2f, 0f, 0f, 1f)
            : new DecelerateInterpolator(1.5f);
    private static final TimeInterpolator EFFECTS_FAST = Build.VERSION.SDK_INT >= 21
            ? new PathInterpolator(0.31f, 0.94f, 0.34f, 1f)
            : new DecelerateInterpolator();

    private M3ExpressiveMotion() {
    }

    public static TimeInterpolator spatialFast() {
        return SPATIAL_FAST;
    }

    public static TimeInterpolator spatialDefault() {
        return SPATIAL_DEFAULT;
    }

    public static TimeInterpolator spatialSmooth() {
        return SPATIAL_SMOOTH;
    }

    public static TimeInterpolator pressIn() {
        return PRESS_IN;
    }

    public static TimeInterpolator effectsFast() {
        return EFFECTS_FAST;
    }

    /**
     * Returns {@code true} when system animations are enabled and duration scale is non-zero.
     */
    public static boolean shouldAnimate(Context context) {
        if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
            return false;
        }
        if (context == null) {
            return true;
        }
        try {
            float scale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f);
            return scale > 0.0f;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Analytical damped-harmonic spring curve mapped onto {@code [0, 1]}, guaranteed to
     * start at {@code 0f} and settle cleanly at {@code 1f} with an expressive overshoot
     * when {@code dampingRatio < 1f}.
     */
    static float spring(float t, float dampingRatio, float naturalCycles) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        float raw = rawSpring(t, dampingRatio, naturalCycles);
        float end = rawSpring(1f, dampingRatio, naturalCycles);
        return raw + t * (1f - end);
    }

    private static float rawSpring(float t, float dampingRatio, float naturalCycles) {
        double zeta = Math.max(0.15, Math.min(0.99, dampingRatio));
        double omega = 2.0 * Math.PI * Math.max(0.25, naturalCycles);
        double omegaD = omega * Math.sqrt(1.0 - zeta * zeta);
        double decay = Math.exp(-zeta * omega * t);
        double cos = Math.cos(omegaD * t);
        double sin = Math.sin(omegaD * t);
        return (float) (1.0 - decay * (cos + (zeta / Math.sqrt(1.0 - zeta * zeta)) * sin));
    }

    static final class SpringInterpolator implements TimeInterpolator {
        private final float dampingRatio;
        private final float naturalCycles;

        SpringInterpolator(float dampingRatio, float naturalCycles) {
            this.dampingRatio = dampingRatio;
            this.naturalCycles = naturalCycles;
        }

        @Override
        public float getInterpolation(float input) {
            return spring(input, dampingRatio, naturalCycles);
        }
    }
}
