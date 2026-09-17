package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GboardPatchesNavigationPathTest {
    @Test
    public void captureRootScreenReturnsEmptyPath() {
        Assert.assertTrue(GboardPatchesNavigationPath.capture(
                Collections.emptyList(), null).isEmpty());
    }

    @Test
    public void captureAndResolveNestedPage() {
        TestLeaf leaf = new TestLeaf(true);
        TestGroup group = new TestGroup(leaf);
        List<String> path = GboardPatchesNavigationPath.capture(
                Collections.singletonList(group), leaf);

        Assert.assertEquals(
                Arrays.asList(group.getClass().getName(), leaf.getClass().getName()),
                path);
        Assert.assertEquals(
                Arrays.asList(group, leaf),
                GboardPatchesNavigationPath.resolve(
                        null,
                        Collections.singletonList(group),
                        path));
    }

    @Test
    public void missingOrUnavailablePageFailsClosed() {
        TestLeaf unavailableLeaf = new TestLeaf(false);
        TestGroup group = new TestGroup(unavailableLeaf);

        Assert.assertTrue(GboardPatchesNavigationPath.resolve(
                null,
                Collections.singletonList(group),
                Arrays.asList(group.getClass().getName(), "missing.Page"))
                .isEmpty());
        Assert.assertTrue(GboardPatchesNavigationPath.resolve(
                null,
                Collections.singletonList(group),
                Arrays.asList(
                        group.getClass().getName(),
                        unavailableLeaf.getClass().getName()))
                .isEmpty());
    }

    private static final class TestGroup implements GboardPatchesSettingsContract.Feature {
        private final List<GboardPatchesSettingsContract.Feature> children;

        TestGroup(GboardPatchesSettingsContract.Feature... children) {
            this.children = Arrays.asList(children);
        }

        @Override
        public String getEntryTitle() {
            return "group";
        }

        @Override
        public String getEntrySummary() {
            return "group";
        }

        @Override
        public List<GboardPatchesSettingsContract.Feature> getNavigationChildren() {
            return children;
        }

        @Override
        public GboardPatchesSettingsContract.Screen buildScreen(
                GboardPatchesSettingsContract.FeatureHost host) {
            return null;
        }
    }

    private static final class TestLeaf implements GboardPatchesSettingsContract.Feature {
        private final boolean available;

        TestLeaf(boolean available) {
            this.available = available;
        }

        @Override
        public String getEntryTitle() {
            return "leaf";
        }

        @Override
        public String getEntrySummary() {
            return "leaf";
        }

        @Override
        public boolean isAvailable(Context context) {
            return available;
        }

        @Override
        public GboardPatchesSettingsContract.Screen buildScreen(
                GboardPatchesSettingsContract.FeatureHost host) {
            return null;
        }
    }
}
