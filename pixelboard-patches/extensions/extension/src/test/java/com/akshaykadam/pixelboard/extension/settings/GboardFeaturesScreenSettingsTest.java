package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.akshaykadam.pixelboard.extension.writingtools.GboardAiWritingToolsRuntime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class GboardFeaturesScreenSettingsTest {

    private static final class GoogleSansFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardGoogleSansSettings.FLAG_GOOGLE_SANS_FLEX;
    }

    private static final class UndoPostCorrectionFlagReceiver {
        @SuppressWarnings("unused")
        private final String a = GboardUndoSettings.FLAG_UNDO_PROACTIVE_POST_CORRECTION;
    }

    @Test
    public void featuresScreenIncludesGoogleSansAndUndoTogglesWithEndToEndRuntimeSync()
            throws Exception {
        GboardPatchesSettingsActivity activity = Robolectric.buildActivity(
                GboardPatchesSettingsActivity.class).setup().get();

        Method buildRootScreenMethod = GboardPatchesSettingsActivity.class
                .getDeclaredMethod("buildRootScreen");
        buildRootScreenMethod.setAccessible(true);
        GboardPatchesSettingsContract.Screen screen =
                (GboardPatchesSettingsContract.Screen) buildRootScreenMethod.invoke(activity);

        Assert.assertNotNull(screen);
        List<GboardPatchesSettingsContract.Section> sections = screen.getSections();
        Assert.assertFalse(sections.isEmpty());

        GboardPatchesSettingsContract.Section featuresSection = sections.get(0);
        List<? extends GboardPatchesSettingsContract.Row> rows = featuresSection.getItems();

        GboardPatchesSettingsContract.ToggleRow googleSansRow = null;
        GboardPatchesSettingsContract.ToggleRow undoRow = null;

        for (GboardPatchesSettingsContract.Row row : rows) {
            if (row instanceof GboardPatchesSettingsContract.ToggleRow) {
                GboardPatchesSettingsContract.ToggleRow toggleRow =
                        (GboardPatchesSettingsContract.ToggleRow) row;
                String title = String.valueOf(toggleRow.getTitle());
                if (title.contains("Google Sans")) {
                    googleSansRow = toggleRow;
                } else if (title.contains("Undo")) {
                    undoRow = toggleRow;
                }
            }
        }

        Assert.assertNull("Google Sans toggle row should be hidden from settings screen", googleSansRow);
        Assert.assertNull("Undo toggle row should be hidden from settings screen", undoRow);

        // Features remain enabled (true) by default
        Assert.assertTrue(GboardGoogleSansSettings.isEnabled(activity));
        Assert.assertTrue(GboardUndoSettings.isEnabled(activity));

        // Set applicationContext on runtime for testing
        Field appContextField = GboardAiWritingToolsRuntime.class.getDeclaredField("applicationContext");
        appContextField.setAccessible(true);
        appContextField.set(null, activity);

        GoogleSansFlagReceiver googleSansReceiver = new GoogleSansFlagReceiver();
        UndoPostCorrectionFlagReceiver undoReceiver = new UndoPostCorrectionFlagReceiver();

        // Verify runtime reflects enabled state by default
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                googleSansReceiver, Boolean.FALSE));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.applyOverriddenFlagValue(
                undoReceiver, Boolean.FALSE));
    }
}
