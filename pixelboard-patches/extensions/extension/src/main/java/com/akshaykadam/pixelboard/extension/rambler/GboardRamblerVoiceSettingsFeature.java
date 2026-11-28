/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.util.Log;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PixelBoard settings screen for Rambler AI Voice Dictation behavior, Verbatim mode,
 * custom instructions, and Indic transliteration native-script output.
 */
public final class GboardRamblerVoiceSettingsFeature
        implements GboardPatchesSettingsContract.Feature {

    private static final String TAG = "PixelBoard";
    private final String headerBadge;

    public GboardRamblerVoiceSettingsFeature(Context context) {
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
    }

    @Override
    public String getEntryTitle() {
        return "Rambler Voice Dictation";
    }

    @Override
    public String getEntrySummary() {
        return "Verbatim mode and classic voice fallback.";
    }

    @Override
    public boolean isAvailable(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(
                context,
                GboardPatchesFeatureAvailability.FEATURE_ADVANCED_VOICE_TYPING);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        if (host == null || host.getContext() == null) {
            return new GboardPatchesSettingsContract.Screen(
                    getEntryTitle(),
                    headerBadge,
                    getEntryTitle(),
                    "",
                    Collections.emptyList());
        }
        Context context = host.getContext();
        boolean ramblerEnabled = GboardRamblerSettings.isRamblerEnabled(context);
        boolean verbatimEnabled = GboardRamblerSettings.isVerbatimModeEnabled(context);

        List<GboardPatchesSettingsContract.Row> transcriptionRows =
                new ArrayList<GboardPatchesSettingsContract.Row>();
        transcriptionRows.add(new GboardPatchesSettingsContract.ToggleRow(
                "Use Rambler AI Dictation",
                "Route voice typing through Gemini Rambler. Turn off to use classic Gboard Voice Typing without AI rewriting or summarizing.",
                true,
                ramblerEnabled,
                value -> saveRamblerEnabled(host, context, value)));
        transcriptionRows.add(new GboardPatchesSettingsContract.ToggleRow(
                "Verbatim Dictation Mode",
                "Transcribe exact spoken words, repetitions, and sentence structure without summarizing, rephrasing, or dropping details.",
                ramblerEnabled,
                verbatimEnabled,
                value -> saveVerbatimMode(host, context, value)));

        return new GboardPatchesSettingsContract.Screen(
                getEntryTitle(),
                headerBadge,
                getEntryTitle(),
                "Configure Rambler AI rewriting and verbatim transcription.",
                Collections.emptyList(),
                Collections.singletonList(
                        new GboardPatchesSettingsContract.Section(
                                "Transcription & AI Cleanup",
                                transcriptionRows)),
                GboardPatchesSettingsContract.RefreshPolicy.none(),
                GboardPatchesSettingsContract.PanelStyle.FLAT);
    }

    private void saveRamblerEnabled(
            GboardPatchesSettingsContract.FeatureHost host,
            Context context,
            boolean enabled) {
        try {
            GboardRamblerSettings.writeRamblerEnabled(context, enabled);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to update Rambler enabled preference", t);
        }
        refreshSafely(host);
    }

    private void saveVerbatimMode(
            GboardPatchesSettingsContract.FeatureHost host,
            Context context,
            boolean enabled) {
        try {
            GboardRamblerSettings.writeVerbatimModeEnabled(context, enabled);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to update Verbatim Dictation Mode", t);
        }
        refreshSafely(host);
    }

    private void refreshSafely(GboardPatchesSettingsContract.FeatureHost host) {
        try {
            GboardPatchesSettingsContract.refresh(host);
        } catch (Throwable ignored) {
        }
    }
}
