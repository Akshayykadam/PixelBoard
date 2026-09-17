package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GboardOfflineSpeechSupportSourceTest {
    @Test
    public void activityQueriesExplicitSpeechServicesRecognizerAndScopesLifecycle()
            throws Exception {
        Path root = repositoryRoot();
        String contract = read(root,
                "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/"
                        + "settings/GboardPatchesSettingsContract.java");
        String feature = read(root,
                "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/"
                        + "advancedvoice/GboardAdvancedVoiceSettingsFeature.java");
        String activity = read(root,
                "extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/"
                        + "settings/GboardPatchesSettingsActivity.java");

        Assert.assertTrue(contract.contains("default boolean requiresOfflineSpeechLanguages()"));
        Assert.assertTrue(contract.contains("class OfflineSpeechLanguages"));
        Assert.assertTrue(contract.contains("LOADING"));
        Assert.assertTrue(contract.contains("AVAILABLE"));
        Assert.assertTrue(contract.contains("UNSUPPORTED"));
        Assert.assertTrue(contract.contains("UNAVAILABLE"));
        Assert.assertTrue(contract.contains("ERROR"));
        Assert.assertTrue(feature.contains("return true;"));
        Assert.assertTrue(activity.contains("GoogleTTSRecognitionService"));
        Assert.assertTrue(activity.contains("SpeechRecognizer.createSpeechRecognizer("));
        Assert.assertTrue(activity.contains("RecognizerIntent.ACTION_RECOGNIZE_SPEECH"));
        Assert.assertTrue(activity.contains("RecognizerIntent.LANGUAGE_MODEL_FREE_FORM"));
        Assert.assertTrue(activity.contains("RecognizerIntent.EXTRA_PREFER_OFFLINE"));
        Assert.assertTrue(activity.contains("getInstalledOnDeviceLanguages()"));
        Assert.assertTrue(activity.contains("OFFLINE_SPEECH_LANGUAGE_QUERY_TIMEOUT_MS = 10_000L"));
        Assert.assertTrue(activity.contains("offlineSpeechLanguageQueryGeneration"));
        Assert.assertTrue(activity.contains("waiting for timeout or a later support result"));
        Assert.assertTrue(activity.contains("refreshOfflineSpeechLanguagesForCurrentFeature();"));
        Assert.assertTrue(activity.contains("offlineSpeechRecognizer.destroy()"));
        Assert.assertFalse(activity.contains("createOnDeviceSpeechRecognizer("));
    }

    private static String read(Path root, String relativePath) throws Exception {
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
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
