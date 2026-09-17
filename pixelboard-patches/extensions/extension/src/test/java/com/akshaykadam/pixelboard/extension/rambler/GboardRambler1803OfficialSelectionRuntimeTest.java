package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class GboardRambler1803OfficialSelectionRuntimeTest {
    @After
    public void tearDown() {
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
    }

    @Test
    public void officialSelectorStateControlsAgenticCapabilityOutsideSettings() {
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(false);
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());

        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(true);
        Assert.assertTrue(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
    }

    @Test
    public void voiceSettingsScopeTemporarilyExposesBothOfficialChoices() {
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(false);
        GboardRambler1803OfficialSelectionRuntime.enterVoiceSettingsScope();
        GboardRambler1803OfficialSelectionRuntime.enterVoiceSettingsScope();

        Assert.assertTrue(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
        GboardRambler1803OfficialSelectionRuntime.exitVoiceSettingsScope();
        Assert.assertTrue(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
        GboardRambler1803OfficialSelectionRuntime.exitVoiceSettingsScope();
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
    }

    @Test
    public void defaultSelectionSuppressionWinsAndBalancedExitRestoresOfficialState() {
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(true);
        GboardRambler1803OfficialSelectionRuntime.enterVoiceSettingsScope();
        GboardRambler1803OfficialSelectionRuntime.enterDefaultSelectionSuppression();
        GboardRambler1803OfficialSelectionRuntime.enterDefaultSelectionSuppression();

        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
        GboardRambler1803OfficialSelectionRuntime.exitDefaultSelectionSuppression();
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
        GboardRambler1803OfficialSelectionRuntime.exitDefaultSelectionSuppression();
        Assert.assertTrue(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());

        GboardRambler1803OfficialSelectionRuntime.exitVoiceSettingsScope();
        Assert.assertTrue(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
    }
}
