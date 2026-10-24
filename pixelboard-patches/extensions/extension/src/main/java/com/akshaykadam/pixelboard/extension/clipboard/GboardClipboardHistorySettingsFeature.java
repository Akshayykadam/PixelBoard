/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

/**
 * Settings feature that exposes the Clipboard History Duration slider.
 *
 * <p>Presents five discrete retention steps:
 * <ol>
 *   <li>1 hour</li>
 *   <li>24 hours (Gboard default / previous maximum)</li>
 *   <li>7 days</li>
 *   <li>30 days</li>
 *   <li>Never (unlimited — manual deletion only)</li>
 * </ol>
 *
 * <p>The chosen value is stored via {@link GboardClipboardHistorySettings} and
 * is read at runtime by {@code GboardAiWritingToolsRuntime} to override the
 * {@code clipboard_history_duration_ms} Phenotype flag.</p>
 */
public final class GboardClipboardHistorySettingsFeature
        implements GboardPatchesSettingsContract.Feature {

    private static final String TAG = "GboardPatches";

    private final String headerBadge;
    private final String entryTitle;
    private final String entrySummary;
    private final String headerSummary;
    private final String errorTitle;
    private final String errorSummary;
    private final String durationTitle;
    private final String durationSummary;
    private final String durationDialogTitle;
    private final String[] durationLabels;
    private final String[] durationValues;
    private final String sectionBehavior;

    public GboardClipboardHistorySettingsFeature(Context context) {
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        entryTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_title);
        entrySummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_summary);
        headerSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_header_summary);
        errorTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_error_title);
        errorSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_error_summary);
        durationTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_duration_title);
        durationSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_duration_summary);
        durationDialogTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_duration_dialog_title);
        durationLabels = new String[]{
                GboardSettingsText.get(context,
                        R.string.gboard_patches_clipboard_history_duration_1h),
                GboardSettingsText.get(context,
                        R.string.gboard_patches_clipboard_history_duration_24h),
                GboardSettingsText.get(context,
                        R.string.gboard_patches_clipboard_history_duration_7d),
                GboardSettingsText.get(context,
                        R.string.gboard_patches_clipboard_history_duration_30d),
                GboardSettingsText.get(context,
                        R.string.gboard_patches_clipboard_history_duration_never),
        };
        durationValues = GboardClipboardHistorySettings.ALL_DURATIONS.clone();
        sectionBehavior = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_section_behavior);
    }

    @Override
    public String getEntryTitle() {
        return entryTitle;
    }

    @Override
    public String getEntrySummary() {
        return entrySummary;
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            if (host == null || host.getContext() == null) {
                return buildErrorScreen();
            }

            SharedPreferences preferences =
                    GboardClipboardHistorySettings.preferences(host.getContext());
            GboardClipboardHistorySettings.ensureDefaults(preferences);

            String currentDuration = GboardClipboardHistorySettings.readDuration(preferences);
            String currentLabel = labelFor(currentDuration);

            List<GboardPatchesSettingsContract.Row> behaviorRows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            behaviorRows.add(new GboardPatchesSettingsContract.SelectorRow(
                    durationTitle,
                    durationSummary,
                    currentLabel,
                    true,
                    () -> showDurationDialog(host, preferences, currentDuration)));

            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    Collections.singletonList(new GboardPatchesSettingsContract.Section(
                            sectionBehavior,
                            behaviorRows)),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to render Clipboard History settings screen", throwable);
            return buildErrorScreen();
        }
    }

    private GboardPatchesSettingsContract.Screen buildErrorScreen() {
        List<GboardPatchesSettingsContract.StatusBlock> statusBlocks =
                new ArrayList<GboardPatchesSettingsContract.StatusBlock>();
        statusBlocks.add(new GboardPatchesSettingsContract.StatusBlock(
                errorTitle,
                errorSummary,
                GboardPatchesSettingsContract.StatusTone.WARNING));
        return new GboardPatchesSettingsContract.Screen(
                entryTitle,
                headerBadge,
                entryTitle,
                headerSummary,
                statusBlocks,
                Collections.emptyList());
    }

    private void showDurationDialog(GboardPatchesSettingsContract.FeatureHost host,
            SharedPreferences preferences, String currentDuration) {
        GboardPatchesSettingsContract.showChoiceDialog(
                host,
                durationDialogTitle,
                durationLabels,
                durationValues,
                currentDuration,
                /* customValue= */ null,
                /* customAction= */ () -> {},
                value -> {
                    GboardClipboardHistorySettings.writeDuration(preferences, value);
                    GboardClipboardHistorySettings.syncToGboardPreferences(host.getContext(), value);
                    GboardPatchesSettingsContract.refresh(host);
                });
    }

    /** Returns the human-readable label for the given duration token. */
    private String labelFor(String duration) {
        for (int i = 0; i < durationValues.length; i++) {
            if (durationValues[i].equals(duration)) {
                return durationLabels[i];
            }
        }
        // Fallback: return the 24h label (matches DEFAULT_DURATION)
        return durationLabels.length > 1 ? durationLabels[1] : duration;
    }
}
