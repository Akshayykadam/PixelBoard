package com.akshaykadam.pixelboard.extension.bengalivoice;

import org.junit.Assert;
import org.junit.Test;

public final class GboardBengaliVoicePolicyTest {
    @Test
    public void onlyBengaliLanguagesHaveAnOverride() {
        for (String tag : new String[]{"bn", "bn-BD", "bn-IN", "bn_BD", "BN-bd", "bn-Beng-BD"}) {
            Assert.assertTrue(tag, GboardBengaliVoicePolicy.isBengali(tag));
        }
        for (String tag : new String[]{null, "", "en-US", "en-BD", "zh-CN", "hi-IN", "x-bn", "bn--BD"}) {
            Assert.assertFalse(String.valueOf(tag), GboardBengaliVoicePolicy.isBengali(tag));
        }
    }

    @Test
    public void voiceChoiceIsSharedAcrossLayoutsButNotRegions() {
        Assert.assertEquals("bn-BD", GboardBengaliVoicePolicy.normalizeBengaliTag("bn-Latn-BD"));
        Assert.assertEquals("bn-BD", GboardBengaliVoicePolicy.normalizeBengaliTag("bn-BD-u-nu-beng"));
        Assert.assertEquals("bn-IN", GboardBengaliVoicePolicy.normalizeBengaliTag("bn_IN"));
        Assert.assertNull(GboardBengaliVoicePolicy.normalizeBengaliTag("en-BD"));
    }

    @Test
    public void missingOrInvalidValuesDefaultToStandard() {
        Assert.assertEquals("standard", GboardBengaliVoicePolicy.modeOrDefault(null));
        Assert.assertEquals("standard", GboardBengaliVoicePolicy.modeOrDefault("unknown"));
        Assert.assertEquals("standard", GboardBengaliVoicePolicy.modeOrDefault(2));
        Assert.assertEquals("rambler", GboardBengaliVoicePolicy.modeOrDefault("rambler"));
    }
}
