package com.akshaykadam.pixelboard.extension.keyboard;

import android.view.View;

public final class GboardKeyboardRuntime {
    private GboardKeyboardRuntime() {}

    /**
     * Performs haptic feedback for key presses, explicitly passing
     * {@code HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING} (0x1) to ensure
     * keypress haptics function as configured in Gboard settings even on OEMs
     * like Honor MagicOS / Huawei EMUI when system-wide vibration is disabled (Issue #7).
     *
     * @param view the View triggering the haptic feedback
     * @param feedbackConstant the haptic feedback constant (e.g. KEYPRESS_STANDARD)
     * @return true if the haptic feedback was successfully performed, false otherwise
     */
    public static boolean performHapticFeedback(View view, int feedbackConstant) {
        if (view == null) {
            return false;
        }
        try {
            return view.performHapticFeedback(feedbackConstant, 1 /* FLAG_IGNORE_GLOBAL_SETTING */);
        } catch (Throwable t) {
            return view.performHapticFeedback(feedbackConstant);
        }
    }

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
