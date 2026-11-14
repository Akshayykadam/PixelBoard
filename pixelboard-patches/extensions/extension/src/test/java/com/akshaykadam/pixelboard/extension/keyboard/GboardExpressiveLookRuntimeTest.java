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
    public void sunburstFitsInsideTheKey() {
        Assert.assertTrue(GboardExpressiveLookRuntime.SUNBURST_SCALE > 0.5f);
        Assert.assertTrue(GboardExpressiveLookRuntime.SUNBURST_SCALE < 1f);
    }
}
