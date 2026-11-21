package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class GboardRamblerMinimizeKeyRuntimeTest {
    @After
    public void tearDown() {
        GboardRamblerMinimizeKeyRuntime.resetForTests();
    }

    @Test
    public void nothingIsClearedBeforeTheMinimizeKeyIsPlaced() {
        GboardRamblerMinimizeKeyRuntime.seedSlotKeyIdForTests(0x7f0b04f9);
        Assert.assertEquals(0, GboardRamblerMinimizeKeyRuntime.clearMinimizeKey());
    }

    @Test
    public void placedMinimizeKeyIsClearedExactlyOnce() {
        GboardRamblerMinimizeKeyRuntime.seedSlotKeyIdForTests(0x7f0b04f9);
        Assert.assertEquals(0x7f0b04f9, GboardRamblerMinimizeKeyRuntime.placeMinimizeKey());
        Assert.assertEquals(0x7f0b04f9, GboardRamblerMinimizeKeyRuntime.clearMinimizeKey());
        Assert.assertEquals(0, GboardRamblerMinimizeKeyRuntime.clearMinimizeKey());
    }

    @Test
    public void unresolvedSlotKeepsStockPlacement() {
        Assert.assertEquals(0, GboardRamblerMinimizeKeyRuntime.placeMinimizeKey());
        Assert.assertEquals(0, GboardRamblerMinimizeKeyRuntime.clearMinimizeKey());
        GboardRamblerMinimizeKeyRuntime.revealMinimizeKey(null);
    }
}
