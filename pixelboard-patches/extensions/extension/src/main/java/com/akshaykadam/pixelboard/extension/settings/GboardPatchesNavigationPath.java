package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GboardPatchesNavigationPath {
    private GboardPatchesNavigationPath() {
    }

    static ArrayList<String> capture(
            List<GboardPatchesSettingsContract.Feature> backStack,
            GboardPatchesSettingsContract.Feature currentFeature) {
        ArrayList<String> path = new ArrayList<String>();
        if (currentFeature == null) {
            return path;
        }
        if (backStack != null) {
            for (GboardPatchesSettingsContract.Feature feature : backStack) {
                if (feature != null) {
                    path.add(feature.getClass().getName());
                }
            }
        }
        path.add(currentFeature.getClass().getName());
        return path;
    }

    static List<GboardPatchesSettingsContract.Feature> resolve(
            Context context,
            List<GboardPatchesSettingsContract.Feature> roots,
            List<String> pathIds) {
        if (pathIds == null || pathIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GboardPatchesSettingsContract.Feature> candidates = roots;
        List<GboardPatchesSettingsContract.Feature> resolved =
                new ArrayList<GboardPatchesSettingsContract.Feature>();
        for (String pathId : pathIds) {
            GboardPatchesSettingsContract.Feature match = findAvailable(
                    context, candidates, pathId);
            if (match == null) {
                return Collections.emptyList();
            }
            resolved.add(match);
            try {
                candidates = match.getNavigationChildren();
            } catch (Throwable ignored) {
                return Collections.emptyList();
            }
        }
        return resolved;
    }

    private static GboardPatchesSettingsContract.Feature findAvailable(
            Context context,
            List<GboardPatchesSettingsContract.Feature> candidates,
            String pathId) {
        if (pathId == null || candidates == null) {
            return null;
        }
        for (GboardPatchesSettingsContract.Feature feature : candidates) {
            if (feature == null || !pathId.equals(feature.getClass().getName())) {
                continue;
            }
            try {
                return feature.isAvailable(context) ? feature : null;
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
