package com.akshaykadam.pixelboard.extension.settings;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.akshaykadam.pixelboard.extension.R;

public final class GboardSettingsTextTest {
    @Test
    public void authoritativeCopyResolvesEnglishWithoutCallerFallback() {
        Assert.assertEquals(
                "Advanced settings",
                GboardSettingsText.resolveStableTextForTesting(
                        R.string.gboard_patches_header_title,
                        "en"));
    }

    @Test
    public void localeNormalizationAndFormattingStayBehindTheSeam() {
        Assert.assertEquals(
                "System default (English)",
                GboardSettingsText.resolveStableTextForTesting(
                        R.string.gboard_patches_language_system_value,
                        "en",
                        "English"));
    }

    @Test
    public void authorityEliminatesLegacyCallerFallbackDrift() {
        Assert.assertEquals(
                "",
                GboardSettingsText.resolveStableTextForTesting(
                        R.string.gboard_patches_group_keyboard_header_summary,
                        "en"));
        Assert.assertEquals(
                "Controls Gboard AI Writing Tools availability, keyboard support, "
                        + "and backend routing.",
                GboardSettingsText.resolveStableTextForTesting(
                        R.string.gboard_patches_ai_writing_tools_summary,
                        "en"));
    }

    @Test
    public void everyGeneratedSettingsStringHasEnglishCoverage()
            throws Exception {
        for (Field field : R.string.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) {
                continue;
            }
            int resId = field.getInt(null);
            Assert.assertNotNull(
                    field.getName() + " missing English settings text",
                    GboardSettingsText.resolveStableTextForTesting(resId, "en"));
        }
    }
}
