package com.akshaykadam.pixelboard.extension.keyboard;

public final class GboardKeyboardRuntime {
    private GboardKeyboardRuntime() {}

    /**
     * Sanitizes softkey labels at runtime.
     * Strips escaping backslashes from question marks (e.g. "\?123" -> "?123", "\\?123" -> "?123",
     * "\123؟" -> "123؟") that arise from AAPT/ARSCLib XML attribute escaping or on-device layout caches,
     * and recovers question mark keys corrupted into theme attribute placeholders (e.g. "?attr/" -> "?").
     */
    public static CharSequence sanitizeKeyLabel(CharSequence text) {
        if (text == null || text.length() == 0) {
            return text;
        }
        String s = text.toString();
        String result = s;
        if (result.contains("?attr/")) {
            result = result.replace("?attr/", "?");
        }
        if (result.indexOf('\\') != -1 && (result.contains("\\?") || result.contains("\\\u061f"))) {
            result = result.replaceAll("\\\\+(\\?|\\u061f)", "$1");
        }
        return result.equals(s) ? text : result;
    }
}
