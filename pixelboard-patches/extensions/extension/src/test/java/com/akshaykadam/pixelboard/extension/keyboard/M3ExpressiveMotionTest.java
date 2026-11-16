package com.akshaykadam.pixelboard.extension.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class M3ExpressiveMotionTest {

    @Test
    public void springStartsAtZeroAndEndsAtOne() {
        assertEquals(0f, M3ExpressiveMotion.spring(0f, 0.58f, 1.35f), 0.0001f);
        assertEquals(1f, M3ExpressiveMotion.spring(1f, 0.58f, 1.35f), 0.0001f);
        assertEquals(0f, M3ExpressiveMotion.spring(-0.2f, 0.65f, 1.25f), 0.0001f);
        assertEquals(1f, M3ExpressiveMotion.spring(1.3f, 0.65f, 1.25f), 0.0001f);
    }

    @Test
    public void fastSpatialSpringOvershootsBeforeSettling() {
        float peak = 0f;
        for (int i = 1; i < 100; i++) {
            float t = i / 100f;
            peak = Math.max(peak, M3ExpressiveMotion.spatialFast().getInterpolation(t));
        }
        assertTrue("Expected expressive overshoot above 1.0, was " + peak, peak > 1.04f && peak < 1.25f);
        assertEquals(1f, M3ExpressiveMotion.spatialFast().getInterpolation(1f), 0.0001f);
        assertEquals(1f, M3ExpressiveMotion.spatialDefault().getInterpolation(1f), 0.0001f);
    }

    @Test
    public void smoothSpatialGlidesWithoutBouncing() {
        float previous = 0f;
        for (int i = 1; i <= 100; i++) {
            float value = M3ExpressiveMotion.spatialSmooth().getInterpolation(i / 100f);
            assertTrue("Smooth glide should not overshoot, was " + value, value <= 1.005f);
            assertTrue("Smooth glide should never move backwards", value >= previous - 0.0001f);
            previous = value;
        }
        assertEquals(1f, M3ExpressiveMotion.spatialSmooth().getInterpolation(1f), 0.0001f);
        assertTrue(M3ExpressiveMotion.DURATION_SMOOTH_SPATIAL_MS
                > M3ExpressiveMotion.DURATION_DEFAULT_SPATIAL_MS);
    }

    @Test
    public void alignedRotationStepsForwardBySunburstTooth() {
        assertEquals(30f, GboardExpressiveLookRuntime.nextAlignedRotation(0f), 0.001f);
        assertEquals(60f, GboardExpressiveLookRuntime.nextAlignedRotation(30f), 0.001f);
        assertEquals(60f, GboardExpressiveLookRuntime.nextAlignedRotation(31.5f), 0.001f);
        assertEquals(120f, GboardExpressiveLookRuntime.nextAlignedRotation(90f), 0.001f);
    }

    @Test
    public void keyPressScaleUsesSofterHorizontalSquishForWideKeys() {
        float[] regular = GboardExpressiveLookRuntime.keyPressScale(100, 120);
        float[] wide = GboardExpressiveLookRuntime.keyPressScale(480, 120);

        assertEquals(regular[0], regular[1], 0.001f);
        assertTrue(regular[0] < 1.0f && regular[0] > 0.85f);
        assertTrue(wide[0] > regular[0]);
        assertTrue(wide[1] < 1.0f);
    }

    @Test
    public void sunburstRadiusSupportsAnimatedDepthMorph() {
        float subtleValley = GboardExpressiveLookRuntime.sunburstRadius(40f, Math.PI / 12, 0.06f);
        float deepValley = GboardExpressiveLookRuntime.sunburstRadius(40f, Math.PI / 12, 0.095f);
        assertTrue("Pressed depth should deepen valleys", deepValley < subtleValley);
        assertEquals(40f, GboardExpressiveLookRuntime.sunburstRadius(40f, 0.0, 0.095f), 0.001f);
    }
}
