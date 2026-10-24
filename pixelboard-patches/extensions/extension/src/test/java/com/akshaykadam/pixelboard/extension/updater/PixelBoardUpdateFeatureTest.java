/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.updater;

import org.junit.Assert;
import org.junit.Test;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;

public class PixelBoardUpdateFeatureTest {

    @Test
    public void featureMetadataMatchesExpectedTitles() {
        PixelBoardUpdateFeature feature = new PixelBoardUpdateFeature(null);
        Assert.assertEquals("PixelBoard updates", feature.getEntryTitle());
        Assert.assertTrue(feature.isAvailable(null));
        Assert.assertNotNull(feature.getEntrySummary());
    }

    @Test
    public void buildScreenCreatesSectionsAndStatusBlock() {
        PixelBoardUpdateFeature feature = new PixelBoardUpdateFeature(null);
        GboardPatchesSettingsContract.FeatureHost host = new GboardPatchesSettingsContract.FeatureHost() {
            @Override
            public android.content.Context getContext() {
                return null;
            }

            @Override
            public void submit(GboardPatchesSettingsContract.Intent intent) {
            }
        };

        GboardPatchesSettingsContract.Screen screen = feature.buildScreen(host);
        Assert.assertNotNull(screen);
        Assert.assertEquals("PixelBoard updates", screen.getToolbarTitle());
        Assert.assertEquals("Updates", screen.getHeaderTitle());
        Assert.assertFalse(screen.isRestartActionVisible());
        Assert.assertTrue(screen.getStatusBlocks().isEmpty());
        Assert.assertFalse(screen.getSections().isEmpty());
    }

    @Test
    public void contractRowsSupportAnimatingFlag() {
        GboardPatchesSettingsContract.CommandRow defaultRow =
                new GboardPatchesSettingsContract.CommandRow("Title", "Summary", true, () -> {});
        Assert.assertFalse(defaultRow.isAnimating());

        GboardPatchesSettingsContract.CommandRow animatingRow =
                new GboardPatchesSettingsContract.CommandRow("Title", "Summary", false, () -> {}, null, null, true);
        Assert.assertTrue(animatingRow.isAnimating());

        GboardPatchesSettingsContract.PrimaryCommandRow defaultPrimary =
                new GboardPatchesSettingsContract.PrimaryCommandRow("Title", "Summary", true, () -> {});
        Assert.assertFalse(defaultPrimary.isAnimating());

        GboardPatchesSettingsContract.PrimaryCommandRow animatingPrimary =
                new GboardPatchesSettingsContract.PrimaryCommandRow("Title", "Summary", false, () -> {}, true);
        Assert.assertTrue(animatingPrimary.isAnimating());
    }

    @Test
    public void checkingAndDownloadingStatusesProduceAnimatingRow() {
        PixelBoardUpdateManager updateManager = PixelBoardUpdateManager.getInstance();
        PixelBoardUpdateFeature feature = new PixelBoardUpdateFeature(null);
        GboardPatchesSettingsContract.FeatureHost host = new GboardPatchesSettingsContract.FeatureHost() {
            @Override
            public android.content.Context getContext() {
                return null;
            }

            @Override
            public void submit(GboardPatchesSettingsContract.Intent intent) {
            }
        };

        try {
            updateManager.setCurrentStatusForTest(PixelBoardUpdateManager.Status.CHECKING);
            GboardPatchesSettingsContract.Screen checkingScreen = feature.buildScreen(host);
            GboardPatchesSettingsContract.Section actionSection = checkingScreen.getSections().get(0);
            GboardPatchesSettingsContract.PrimaryCommandRow checkingRow =
                    (GboardPatchesSettingsContract.PrimaryCommandRow) actionSection.getItems().get(0);
            Assert.assertTrue("Row must animate when checking", checkingRow.isAnimating());
            Assert.assertFalse("Row must be disabled when checking", checkingRow.isEnabled());

            updateManager.setCurrentStatusForTest(PixelBoardUpdateManager.Status.DOWNLOADING);
            GboardPatchesSettingsContract.Screen downloadingScreen = feature.buildScreen(host);
            GboardPatchesSettingsContract.Section downloadingSection = downloadingScreen.getSections().get(0);
            GboardPatchesSettingsContract.PrimaryCommandRow downloadingRow =
                    (GboardPatchesSettingsContract.PrimaryCommandRow) downloadingSection.getItems().get(0);
            Assert.assertTrue("Row must animate when downloading", downloadingRow.isAnimating());
            Assert.assertFalse("Row must be disabled when downloading", downloadingRow.isEnabled());

            updateManager.setCurrentStatusForTest(PixelBoardUpdateManager.Status.IDLE);
            GboardPatchesSettingsContract.Screen idleScreen = feature.buildScreen(host);
            GboardPatchesSettingsContract.Section idleSection = idleScreen.getSections().get(0);
            GboardPatchesSettingsContract.PrimaryCommandRow idleRow =
                    (GboardPatchesSettingsContract.PrimaryCommandRow) idleSection.getItems().get(0);
            Assert.assertFalse("Row must not animate when idle", idleRow.isAnimating());
            Assert.assertTrue("Row must be enabled when idle", idleRow.isEnabled());
        } finally {
            updateManager.setCurrentStatusForTest(PixelBoardUpdateManager.Status.IDLE);
        }
    }
}
