package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class GboardRambler1803StockPolicyTest {
    @After
    public void tearDown() {
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
    }

    @Test
    public void formalBooleanAndLongFlagsRetainTheirTypes() {
        Assert.assertSame(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.SHOW_RAMBLER_DICT_SETTINGS_FLAG,
                        Boolean.FALSE));
        Assert.assertEquals(Long.valueOf(2L),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.AD_ACTIVATION_TYPE_FLAG,
                        Long.valueOf(0L)));
        Integer wrongType = Integer.valueOf(2);
        Assert.assertSame(wrongType,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.AD_ACTIVATION_TYPE_FLAG,
                        wrongType));
    }

    @Test
    public void agenticCapabilityFollowsOfficialSelectionAndScopes() {
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(false);
        Assert.assertSame(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));

        GboardRambler1803OfficialSelectionRuntime.enterVoiceSettingsScope();
        Assert.assertSame(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));
        GboardRambler1803OfficialSelectionRuntime.exitVoiceSettingsScope();

        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(true);
        Assert.assertSame(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));
        GboardRambler1803OfficialSelectionRuntime.enterDefaultSelectionSuppression();
        Assert.assertSame(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));
        Assert.assertSame(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.CONFIG_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));
        Assert.assertSame(Boolean.FALSE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_JETSON_IN_TOOLBAR_FLAG,
                        Boolean.FALSE));

        GboardRambler1803OfficialSelectionRuntime.exitDefaultSelectionSuppression();
        Assert.assertSame(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.CONFIG_AGENTIC_DICTATION_FLAG,
                        Boolean.FALSE));
        Assert.assertSame(Boolean.TRUE,
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.ENABLE_JETSON_IN_TOOLBAR_FLAG,
                        Boolean.FALSE));
        Assert.assertTrue(
                GboardRambler1803StockPolicy.isTargetFlagName(
                        GboardRambler1803StockPolicy.ENABLE_JETSON_IN_TOOLBAR_FLAG));
        Assert.assertTrue(
                GboardRambler1803StockPolicy.isTargetFlagName(
                        GboardRambler1803StockPolicy.CONFIG_AGENTIC_DICTATION_FLAG));
    }
}
