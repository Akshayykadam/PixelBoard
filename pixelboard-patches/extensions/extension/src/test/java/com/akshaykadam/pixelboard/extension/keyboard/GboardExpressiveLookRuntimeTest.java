package com.akshaykadam.pixelboard.extension.keyboard;

import org.junit.Assert;
import org.junit.Test;

public class GboardExpressiveLookRuntimeTest {

    @Test
    public void sunburstPeaksTouchTheRadius() {
        Assert.assertEquals(50f, GboardExpressiveLookRuntime.sunburstRadius(50f, 0), 0.001f);
        Assert.assertEquals(50f, GboardExpressiveLookRuntime.sunburstRadius(
                50f, 2 * Math.PI / GboardExpressiveLookRuntime.SUNBURST_WAVES), 0.001f);
    }

    @Test
    public void sunburstValleysAreGentle() {
        double valley = Math.PI / GboardExpressiveLookRuntime.SUNBURST_WAVES;
        float expected = 50f * (1 - 2 * GboardExpressiveLookRuntime.SUNBURST_DEPTH);
        Assert.assertEquals(expected, GboardExpressiveLookRuntime.sunburstRadius(50f, valley), 0.001f);
        Assert.assertTrue(expected > 45f);
    }

    @Test
    public void activeToolbarKeysMorphIntoACircle() {
        // Gboard marks the open menu key and the dictating mic key as activated.
        Assert.assertEquals(0f, GboardExpressiveLookRuntime.sunburstTargetDepth(false, true), 0.0001f);
        Assert.assertEquals(0f, GboardExpressiveLookRuntime.sunburstTargetDepth(true, true), 0.0001f);
        Assert.assertEquals(1f, GboardExpressiveLookRuntime.sunburstTargetScale(false, true), 0.0001f);
    }

    @Test
    public void restingToolbarKeysStaySunbursts() {
        Assert.assertEquals(GboardExpressiveLookRuntime.SUNBURST_DEPTH,
                GboardExpressiveLookRuntime.sunburstTargetDepth(false, false), 0.0001f);
        Assert.assertEquals(1f, GboardExpressiveLookRuntime.sunburstTargetScale(false, false), 0.0001f);
    }

    @Test
    public void pressingSquishesTheShapeAndSoftensItTowardACircle() {
        float pressedDepth = GboardExpressiveLookRuntime.sunburstTargetDepth(true, false);
        Assert.assertTrue(pressedDepth > 0f);
        Assert.assertTrue(pressedDepth < GboardExpressiveLookRuntime.SUNBURST_DEPTH);
        Assert.assertTrue(GboardExpressiveLookRuntime.sunburstTargetScale(true, false) < 1f);
        Assert.assertTrue(GboardExpressiveLookRuntime.sunburstTargetScale(true, true) < 1f);
    }

    @Test
    public void flatDepthDrawsAPerfectCircle() {
        for (int i = 0; i < 24; i++) {
            double theta = 2 * Math.PI * i / 24;
            Assert.assertEquals(50f, GboardExpressiveLookRuntime.sunburstRadius(50f, theta, 0f), 0.001f);
        }
    }

    @Test
    public void toolbarTopSpacingOnlyAppliesToTheSunburstToolbarRow() {
        int spacing = GboardExpressiveLookRuntime.TOOLBAR_TOP_SPACING_DP;
        Assert.assertEquals(spacing, GboardExpressiveLookRuntime.toolbarTopSpacingDp(true, true));
        // Emoji / GIF headers reuse the same holder but have no sunburst keys.
        Assert.assertEquals(0, GboardExpressiveLookRuntime.toolbarTopSpacingDp(true, false));
        // Emoji row shown above the toolbar.
        Assert.assertEquals(0, GboardExpressiveLookRuntime.toolbarTopSpacingDp(false, true));
    }

    @Test
    public void keyboardEntranceSlideSkipsViewsGboardHasTransformed() {
        Assert.assertTrue(GboardExpressiveLookRuntime.canSlideKeyboardEntrance(1f, 1f, 0f, 0f));
        // One-handed mode shrinks and shifts the keyboard view.
        Assert.assertFalse(GboardExpressiveLookRuntime.canSlideKeyboardEntrance(0.8f, 0.8f, 0f, 0f));
        Assert.assertFalse(GboardExpressiveLookRuntime.canSlideKeyboardEntrance(1f, 1f, 120f, 0f));
        Assert.assertFalse(GboardExpressiveLookRuntime.canSlideKeyboardEntrance(1f, 1f, 0f, 40f));
    }

    @Test
    public void sunburstFitsInsideTheKey() {
        Assert.assertTrue(GboardExpressiveLookRuntime.SUNBURST_SCALE > 0.5f);
        Assert.assertTrue(GboardExpressiveLookRuntime.SUNBURST_SCALE < 1f);
    }
}
