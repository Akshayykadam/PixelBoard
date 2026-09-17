package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

public final class GboardVoiceInputModeTest {
    @Test
    public void masterOffAlwaysSelectsStandard() {
        Assert.assertEquals(GboardVoiceInputMode.STANDARD,
                GboardVoiceInputMode.resolve(false, GboardAdvancedVoiceSettings.BACKEND_ADVANCED));
        Assert.assertEquals(GboardVoiceInputMode.STANDARD,
                GboardVoiceInputMode.resolve(false, GboardAdvancedVoiceSettings.BACKEND_RAMBLER));
    }

    @Test
    public void enabledMasterSelectsConfiguredBackendAndDefaultsSafely() {
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED,
                GboardVoiceInputMode.resolve(true, GboardAdvancedVoiceSettings.BACKEND_ADVANCED));
        Assert.assertEquals(GboardVoiceInputMode.RAMBLER,
                GboardVoiceInputMode.resolve(true, GboardAdvancedVoiceSettings.BACKEND_RAMBLER));
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED,
                GboardVoiceInputMode.resolve(true, "unknown"));
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED,
                GboardVoiceInputMode.resolve(true, null));
    }
}
