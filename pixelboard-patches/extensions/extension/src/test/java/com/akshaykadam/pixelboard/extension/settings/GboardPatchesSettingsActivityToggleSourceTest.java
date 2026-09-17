package com.akshaykadam.pixelboard.extension.settings;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GboardPatchesSettingsActivityToggleSourceTest {
    @Test
    public void toggleRowsHidePlatformTrueFalseLabels() throws Exception {
        String source = new String(
                Files.readAllBytes(Path.of(
                        "src/main/java/com/akshaykadam/pixelboard/extension/settings/"
                                + "GboardPatchesSettingsActivity.java")),
                StandardCharsets.UTF_8);

        String method = extractMethod(source, "private View createToggleRow(");
        Assert.assertTrue(method.contains("switchView.setShowText(false);"));
    }

    private static String extractMethod(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        Assert.assertTrue("Missing method signature: " + signature, signatureIndex >= 0);

        int bodyStart = source.indexOf('{', signatureIndex);
        Assert.assertTrue("Missing method body start: " + signature, bodyStart >= 0);

        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(signatureIndex, index + 1);
                }
            }
        }
        throw new AssertionError("Unterminated method body: " + signature);
    }
}
