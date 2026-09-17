package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GboardSpeechServicesNavigationSourceTest {
    @Test
    public void activityOpensOfficialSpeechAppsWithStoreAndToastFallbacks()
            throws Exception {
        String activity = new String(Files.readAllBytes(repositoryRoot().resolve(
                "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/"
                        + "settings/GboardPatchesSettingsActivity.java")), StandardCharsets.UTF_8);

        Assert.assertTrue(activity.contains("public void openSpeechRecognitionAndSynthesisStoreListing()"));
        Assert.assertTrue(activity.contains("public void openLiveTranscribeLanguageManager()"));
        Assert.assertTrue(activity.contains("com.google.android.tts"));
        Assert.assertTrue(activity.contains("com.google.audio.hearing.visualization.accessibility.scribe"));
        Assert.assertTrue(activity.contains("getLaunchIntentForPackage("));
        Assert.assertTrue(activity.contains("market://details?id="));
        Assert.assertTrue(activity.contains("com.android.vending"));
        Assert.assertTrue(activity.contains("https://play.google.com/store/apps/details?id=com.google.android.tts"));
        Assert.assertTrue(activity.contains("tryStartActivity("));
        Assert.assertTrue(activity.contains("showSafeToast("));
        Assert.assertTrue(activity.contains("GboardSettingsText.get("));
        Assert.assertFalse(activity.contains(
                "getString(R.string.gboard_patches_advanced_voice_speech_services_open_failed)"));
        Assert.assertFalse(activity.contains(
                "getString(R.string.gboard_patches_advanced_voice_live_transcribe_open_failed)"));
        Assert.assertFalse(activity.contains("com.google.recognition.action.DOWNLOAD_LANGUAGE"));
        Assert.assertFalse(activity.contains("su -c"));
    }

    @Test
    public void navigationFailureToastCannotEscapeIntoTheHostApp() throws Exception {
        String activity = new String(Files.readAllBytes(repositoryRoot().resolve(
                "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/"
                        + "settings/GboardPatchesSettingsActivity.java")), StandardCharsets.UTF_8);

        int helperStart = activity.indexOf("private void showSafeToast(");
        Assert.assertTrue("safe toast helper must exist", helperStart >= 0);
        int helperEnd = activity.indexOf("\n    }", helperStart);
        Assert.assertTrue("safe toast helper must have a body", helperEnd > helperStart);
        String helper = activity.substring(helperStart, helperEnd);
        Assert.assertTrue(helper.contains("try {"));
        Assert.assertTrue(helper.contains("catch (Throwable"));
        Assert.assertTrue(helper.contains("GboardSettingsText.get("));
        Assert.assertTrue(helper.contains("Toast.makeText("));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }
}
