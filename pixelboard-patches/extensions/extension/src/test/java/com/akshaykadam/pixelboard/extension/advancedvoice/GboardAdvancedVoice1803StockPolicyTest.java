package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

public final class GboardAdvancedVoice1803StockPolicyTest {
    @Test
    public void sixFormalBooleanFlagsAreEnabledWithStrictTyping() {
        String[] flags = new String[] {
                GboardAdvancedVoice1803StockPolicy.ENABLE_NGA_FLAG,
                GboardAdvancedVoice1803StockPolicy.ENABLE_ADVANCED_FEATURES_FLAG,
                GboardAdvancedVoice1803StockPolicy.ENABLE_DICTATION_SPLIT_INSTALL_FLAG,
                GboardAdvancedVoice1803StockPolicy.HANDLE_FALLBACK_INSIDE_SD_STACK_FLAG,
                GboardAdvancedVoice1803StockPolicy.IMMEDIATELY_END_DICTATION_ON_KEYBOARD_HIDDEN_FLAG,
                GboardAdvancedVoice1803StockPolicy.ENABLE_SODA_LONGFORM_EXPERIMENT_FLAG
        };
        for (String flag : flags) {
            Assert.assertSame(Boolean.TRUE,
                    GboardAdvancedVoice1803StockPolicy.maybeForceStockFlag(
                            flag, Boolean.FALSE));
        }
        Object wrongType = "false";
        Assert.assertSame(wrongType,
                GboardAdvancedVoice1803StockPolicy.maybeForceStockFlag(
                        flags[0], wrongType));
    }

    @Test
    public void stickyMicBackgroundPreservesTheStockValue() {
        Assert.assertSame(Boolean.FALSE,
                GboardAdvancedVoice1803StockPolicy.maybeForceStockFlag(
                        GboardAdvancedVoice1803StockPolicy.ENABLE_STICKY_MIC_BACKGROUND_FLAG,
                        Boolean.FALSE));
        Assert.assertFalse(GboardAdvancedVoice1803StockPolicy.isTargetFlagName(
                GboardAdvancedVoice1803StockPolicy.ENABLE_STICKY_MIC_BACKGROUND_FLAG));
    }
}
