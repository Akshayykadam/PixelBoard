package com.akshaykadam.pixelboard.extension.calculator;

import org.junit.Assert;
import org.junit.Test;

public final class GboardCalculatorSafetyTest {
    @Test
    public void booleanValueUsesFallbackWhenSnapshotReadFails() {
        Assert.assertTrue(GboardCalculatorSafety.booleanValue(() -> true, false));
        Assert.assertFalse(GboardCalculatorSafety.booleanValue(() -> {
            throw new IllegalStateException("preference read failed");
        }, false));
    }

    @Test
    public void runContainsCallbackFailures() {
        GboardCalculatorSafety.run(() -> {
            throw new IllegalStateException("callback failed");
        });
    }
}
