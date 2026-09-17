package com.akshaykadam.pixelboard.extension.settings;

import android.content.Context;

/** Reusable in-memory adapter for feature tests crossing the settings intent seam. */
public class GboardPatchesSettingsTestHost implements GboardPatchesSettingsContract.Host {
    private final Context context;
    private int refreshCount;
    private GboardPatchesSettingsContract.Feature openedFeature;
    private GboardPatchesSettingsContract.ManagedDialogAction managedDialogAction;
    private int managedDialogDismissCount;

    public GboardPatchesSettingsTestHost() {
        this(null);
    }

    public GboardPatchesSettingsTestHost(Context context) {
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void refresh() {
        refreshCount++;
    }

    @Override
    public void openFeature(GboardPatchesSettingsContract.Feature feature) {
        openedFeature = feature;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public GboardPatchesSettingsContract.Feature getOpenedFeature() {
        return openedFeature;
    }

    @Override
    public void showManagedDialog(GboardPatchesSettingsContract.ManagedDialogAction action) {
        managedDialogAction = action;
        if (action != null) {
            GboardManagedDialogRunner.run(
                    action,
                    () -> {
                    },
                    () -> managedDialogDismissCount++);
        }
    }

    public GboardPatchesSettingsContract.ManagedDialogAction getManagedDialogAction() {
        return managedDialogAction;
    }

    public int getManagedDialogDismissCount() {
        return managedDialogDismissCount;
    }

    @Override
    public void showChoiceDialog(String title, String[] labels, String[] values,
            String currentValue, String customValue, Runnable customAction,
            GboardPatchesSettingsContract.StringValueConsumer valueConsumer) {
    }

    @Override
    public void showPositiveIntegerDialog(String title, String hint, int initialValue,
            GboardPatchesSettingsContract.PositiveIntegerConsumer consumer) {
    }

    @Override
    public void showTextInputDialog(String title, String hint, String initialValue,
            GboardPatchesSettingsContract.TextValueConsumer consumer) {
    }

    @Override
    public void showPreviewDialog(GboardPatchesSettingsContract.PreviewSpec previewSpec) {
    }

    @Override
    public void createTextDocument(String fileName, String mimeType, String text,
            Runnable completionAction) {
    }

    @Override
    public void openTextDocument(String[] mimeTypes,
            GboardPatchesSettingsContract.StringValueConsumer valueConsumer) {
    }
}
