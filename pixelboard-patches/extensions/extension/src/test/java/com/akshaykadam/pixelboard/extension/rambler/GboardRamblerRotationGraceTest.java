package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class GboardRamblerRotationGraceTest {
    private static final int PORTRAIT = 1;
    private static final int LANDSCAPE = 2;

    @After
    public void tearDown() {
        GboardRamblerRotationGrace.resetForTests();
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
    }

    @Test
    public void deactivationDelayFlagCarriesTheGraceOnlyMidRotation() {
        long[] now = {1_000L};
        GboardRamblerRotationGrace.setClockForTests(() -> now[0]);
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(true);
        String flag = GboardRambler1803StockPolicy.AGENTIC_DICTATION_DEACTIVATION_DELAY_MS_FLAG;

        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, now[0]);
        Assert.assertEquals(Long.valueOf(0L),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(flag, Long.valueOf(5000L)));

        now[0] = 5_000L;
        GboardRamblerRotationGrace.onDisplayConfiguration(LANDSCAPE, 751, 475, now[0]);
        now[0] = 5_016L;
        Assert.assertEquals(Long.valueOf(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(flag, Long.valueOf(5000L)));

        now[0] = 9_000L;
        Assert.assertEquals(Long.valueOf(0L),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(flag, Long.valueOf(5000L)));
    }

    @Test
    public void noGraceBeforeAnyDisplayChange() {
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs(1_010L));
    }

    @Test
    public void rotationGrantsGraceInsideTheWindowOnly() {
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        GboardRamblerRotationGrace.onDisplayConfiguration(LANDSCAPE, 751, 475, 5_000L);

        // Rotation deactivates Rambler twice within ~150 ms; both must keep the session.
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs(5_019L));
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs(5_138L));
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs(
                        5_000L + GboardRamblerRotationGrace.DISPLAY_CHANGE_WINDOW_MS));
        // Once the window passes, closing the keyboard stops Rambler instantly again.
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs(
                5_000L + GboardRamblerRotationGrace.DISPLAY_CHANGE_WINDOW_MS + 1L));
    }

    @Test
    public void laterRotationStartsANewWindow() {
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        GboardRamblerRotationGrace.onDisplayConfiguration(LANDSCAPE, 751, 475, 5_000L);
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs(30_000L));

        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 30_000L);
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs(30_020L));
    }

    @Test
    public void unrelatedConfigurationChangesGrantNoGrace() {
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        // Same display shape, e.g. a locale, theme or font scale change.
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 5_000L);
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs(5_010L));
    }

    @Test
    public void screenSizeChangeWithoutOrientationChangeGrantsGrace() {
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        GboardRamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 600, 751, 5_000L);
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs(5_010L));
    }
}
