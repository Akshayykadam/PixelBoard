package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GboardOfflineSpeechLanguageFormatterTest {
    @Test
    public void formatsDeduplicatesAndSortsInstalledLanguageTags() throws Exception {
        Assert.assertEquals(
                Arrays.asList("English (United States)", "Japanese (Japan)"),
                format(Arrays.asList("ja-JP", "en-US", "en-US"), Locale.US));
    }

    @Test
    public void ignoresMissingAndUndeterminedLanguageTags() throws Exception {
        Assert.assertEquals(
                Collections.emptyList(),
                format(Arrays.asList("", "und", null), Locale.US));
    }

    @SuppressWarnings("unchecked")
    private static List<String> format(List<String> tags, Locale locale) throws Exception {
        try {
            Class<?> formatter = Class.forName(
                    "com.akshaykadam.pixelboard.extension.advancedvoice."
                            + "GboardOfflineSpeechLanguageFormatter");
            Method method = formatter.getMethod("format", List.class, Locale.class);
            return (List<String>) method.invoke(null, tags, locale);
        } catch (ClassNotFoundException exception) {
            Assert.fail("GboardOfflineSpeechLanguageFormatter is missing");
            return Collections.emptyList();
        }
    }
}
