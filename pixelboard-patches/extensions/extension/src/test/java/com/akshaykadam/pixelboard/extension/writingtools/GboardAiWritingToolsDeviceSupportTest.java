package com.akshaykadam.pixelboard.extension.writingtools;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class GboardAiWritingToolsDeviceSupportTest {

    @After
    public void tearDown() {
        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(null);
    }

    @Test
    public void huskyPixel8ProIsSupported() {
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 8 Pro", "husky"));
    }

    @Test
    public void standardPixel8And8aAreNotSupported() {
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 8", "shiba"));
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 8a", "akita"));
    }

    @Test
    public void pixel9SeriesDevicesAreSupported() {
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 9", "tokay"));
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 9 Pro", "caiman"));
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 9 Pro XL", "komodo"));
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 9 Pro Fold", "comet"));
    }

    @Test
    public void pixel10SeriesDevicesAreSupported() {
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 10", "frankel"));
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 10 Pro", "blazer"));
    }

    @Test
    public void unsupportedDevicesAreRejected() {
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Google", "Pixel 7 Pro", "cheetah"));
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice("Samsung", "Galaxy S24 Ultra", "e3q"));
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice(null, null, null));
    }

    @Test
    public void forcedDeviceSupportForTestingWorks() {
        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(true);
        Assert.assertTrue(GboardAiWritingToolsDeviceSupport.isSupportedDevice());

        GboardAiWritingToolsDeviceSupport.setForcedDeviceSupportForTesting(false);
        Assert.assertFalse(GboardAiWritingToolsDeviceSupport.isSupportedDevice());
    }
}
