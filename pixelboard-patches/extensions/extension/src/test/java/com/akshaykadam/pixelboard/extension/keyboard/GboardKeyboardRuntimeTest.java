package com.akshaykadam.pixelboard.extension.keyboard;

import org.junit.Assert;
import org.junit.Test;

public class GboardKeyboardRuntimeTest {

    @Test
    public void testSanitizeKeyLabelWithEscapedQuestionMark() {
        Assert.assertEquals("?123", GboardKeyboardRuntime.sanitizeKeyLabel("\\?123"));
        Assert.assertEquals("?123", GboardKeyboardRuntime.sanitizeKeyLabel("\\\\?123"));
        Assert.assertEquals("?123", GboardKeyboardRuntime.sanitizeKeyLabel("\\\\\\?123"));
        Assert.assertEquals("?१२३", GboardKeyboardRuntime.sanitizeKeyLabel("\\?१२३"));
        Assert.assertEquals("?!@#", GboardKeyboardRuntime.sanitizeKeyLabel("\\?!@#"));
        Assert.assertEquals("123؟", GboardKeyboardRuntime.sanitizeKeyLabel("123\\؟"));
        Assert.assertEquals("123؟", GboardKeyboardRuntime.sanitizeKeyLabel("123\\\\؟"));
    }

    @Test
    public void testSanitizeKeyLabelWithCorruptedThemeAttribute() {
        Assert.assertEquals("?", GboardKeyboardRuntime.sanitizeKeyLabel("?attr/"));
        Assert.assertEquals("?foo", GboardKeyboardRuntime.sanitizeKeyLabel("?attr/foo"));
        Assert.assertEquals("?,¿", GboardKeyboardRuntime.sanitizeKeyLabel("?attr/,¿"));
        Assert.assertEquals("? ", GboardKeyboardRuntime.sanitizeKeyLabel("?attr/ "));
    }

    @Test
    public void testSanitizeKeyLabelLeavesNormalStringsUntouched() {
        Assert.assertNull(GboardKeyboardRuntime.sanitizeKeyLabel(null));
        Assert.assertEquals("", GboardKeyboardRuntime.sanitizeKeyLabel(""));
        Assert.assertEquals("?", GboardKeyboardRuntime.sanitizeKeyLabel("?"));
        Assert.assertEquals("a", GboardKeyboardRuntime.sanitizeKeyLabel("a"));
        Assert.assertEquals("?123", GboardKeyboardRuntime.sanitizeKeyLabel("?123"));
        Assert.assertEquals("123", GboardKeyboardRuntime.sanitizeKeyLabel("123"));
        Assert.assertEquals("Space", GboardKeyboardRuntime.sanitizeKeyLabel("Space"));

        String unmodified = "Regular Text";
        Assert.assertSame(unmodified, GboardKeyboardRuntime.sanitizeKeyLabel(unmodified));
    }
}
