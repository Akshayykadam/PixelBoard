package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.akshaykadam.pixelboard.extension.R;

public final class GboardFeatureGroup implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "GboardPatches";

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerSummary;
    private final String emptyTitle;
    private final String emptySummary;
    private final List<GboardPatchesSettingsContract.Feature> children;

    public GboardFeatureGroup(String entryTitle,
            String entrySummary,
            String headerBadge,
            String headerSummary,
            String emptyTitle,
            String emptySummary,
            List<GboardPatchesSettingsContract.Feature> children) {
        this.entryTitle = entryTitle;
        this.entrySummary = entrySummary;
        this.headerBadge = headerBadge;
        this.headerSummary = headerSummary;
        this.emptyTitle = emptyTitle;
        this.emptySummary = emptySummary;
        this.children = Collections.unmodifiableList(
                new ArrayList<GboardPatchesSettingsContract.Feature>(children));
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
    public boolean isAvailable(Context context) {
        return !availableChildren(context).isEmpty();
    }

    @Override
    public List<GboardPatchesSettingsContract.Feature> getNavigationChildren() {
        return children;
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        List<GboardPatchesSettingsContract.Feature> visibleChildren =
                availableChildren(host.getContext());
        if (visibleChildren.isEmpty()) {
            List<GboardPatchesSettingsContract.StatusBlock> statusBlocks =
                    new ArrayList<GboardPatchesSettingsContract.StatusBlock>();
            statusBlocks.add(new GboardPatchesSettingsContract.StatusBlock(
                    emptyTitle,
                    emptySummary));
            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    statusBlocks,
                    Collections.emptyList());
        } else {
            List<GboardPatchesSettingsContract.Row> rows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            for (GboardPatchesSettingsContract.Feature child : visibleChildren) {
                rows.add(new GboardPatchesSettingsContract.NavigationRow(
                        child.getEntryTitle(),
                        child.getEntrySummary(),
                        true,
                        () -> GboardPatchesSettingsContract.openFeature(host, child)));
            }
            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    Collections.singletonList(new GboardPatchesSettingsContract.Section(
                            GboardSettingsText.get(host.getContext(),
                                    R.string.gboard_patches_section_features),
                            rows)));
        }
    }

    private List<GboardPatchesSettingsContract.Feature> availableChildren(Context context) {
        List<GboardPatchesSettingsContract.Feature> visible =
                new ArrayList<GboardPatchesSettingsContract.Feature>();
        for (GboardPatchesSettingsContract.Feature child : children) {
            if (child == null) {
                continue;
            }
            try {
                if (child.isAvailable(context)) {
                    visible.add(child);
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "Failed to evaluate grouped feature availability: "
                        + child.getClass().getName(), throwable);
            }
        }
        return visible;
    }
}
