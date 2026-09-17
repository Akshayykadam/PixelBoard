package com.akshaykadam.pixelboard.extension.settings;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public final class GboardPatchesSettingsFeatureHostTest {
    @Test
    public void featureFacingHostExposesOnlyStateAndOneIntentSink() {
        Set<String> methods = new TreeSet<String>();
        for (Method method : GboardPatchesSettingsContract.FeatureHost.class.getDeclaredMethods()) {
            methods.add(method.getName());
        }

        Assert.assertEquals(
                new TreeSet<String>(Arrays.asList(
                        "getContext", "getOfflineSpeechLanguages", "submit")),
                methods);
    }

    @Test
    public void featuresBuildAgainstNarrowHostInsteadOfPlatformAdapter() throws Exception {
        Method buildScreen = GboardPatchesSettingsContract.Feature.class.getMethod(
                "buildScreen",
                GboardPatchesSettingsContract.FeatureHost.class);

        Assert.assertEquals(
                GboardPatchesSettingsContract.Screen.class,
                buildScreen.getReturnType());
    }

    @Test
    public void reusableInMemoryAdapterExecutesFeatureIntents() {
        GboardPatchesSettingsTestHost host = new GboardPatchesSettingsTestHost();

        GboardPatchesSettingsContract.refresh(host);

        Assert.assertEquals(1, host.getRefreshCount());
    }

    @Test
    public void activitySubmitsInitialRenderOnlyFromResume() throws Exception {
        String source = new String(Files.readAllBytes(Path.of(
                "src/main/java/com/akshaykadam/pixelboard/extension/settings/"
                        + "GboardPatchesSettingsActivity.java")), StandardCharsets.UTF_8);
        String onCreate = source.substring(
                source.indexOf("protected void onCreate"),
                source.indexOf("protected void onNewIntent"));

        Assert.assertFalse(onCreate.contains("scheduleDeferredRender()"));
        Assert.assertTrue(source.contains("GboardPatchesSettingsOrchestrator.Event.resume()"));
        Assert.assertTrue(source.contains("GboardPatchesSettingsContract.Intent intent = "
                + "effect.getPayload()"));
        Assert.assertFalse(source.contains(
                "instanceof GboardPatchesSettingsContract.Intent"));
    }

    @Test
    public void reusableAdapterCapturesManagedDialogAndCompletesDismissCallback() {
        GboardPatchesSettingsTestHost host = new GboardPatchesSettingsTestHost();
        AtomicReference<Runnable> dismiss = new AtomicReference<Runnable>();
        GboardPatchesSettingsContract.ManagedDialogAction action = onDismiss -> {
            dismiss.set(onDismiss);
            return true;
        };

        GboardPatchesSettingsContract.showManagedDialog(host, action);

        Assert.assertSame(action, host.getManagedDialogAction());
        Assert.assertNotNull(dismiss.get());
        dismiss.get().run();
        Assert.assertEquals(1, host.getManagedDialogDismissCount());
    }

    @Test
    public void managedDialogNoShowAndDuplicateDismissCompleteExactlyOnce() {
        GboardPatchesSettingsTestHost noShowHost = new GboardPatchesSettingsTestHost();
        GboardPatchesSettingsContract.showManagedDialog(noShowHost, onDismiss -> false);
        Assert.assertEquals(1, noShowHost.getManagedDialogDismissCount());

        GboardPatchesSettingsTestHost duplicateHost = new GboardPatchesSettingsTestHost();
        GboardPatchesSettingsContract.showManagedDialog(duplicateHost, onDismiss -> {
            onDismiss.run();
            onDismiss.run();
            return true;
        });
        Assert.assertEquals(1, duplicateHost.getManagedDialogDismissCount());
    }
}
