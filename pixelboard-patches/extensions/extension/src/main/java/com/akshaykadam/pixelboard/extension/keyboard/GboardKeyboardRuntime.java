package com.akshaykadam.pixelboard.extension.keyboard;

import android.view.View;
import java.util.Locale;

public final class GboardKeyboardRuntime {
    private static final String ACTION_PRESS = "PRESS";
    private static final String ACTION_LONG_PRESS = "LONG_PRESS";
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

    /**
     * Evaluates whether a key event should recapitalize the active text selection and, if so,
     * returns the next cased replacement string. Returns {@code null} when the key event is not
     * a Shift/Enter Caps trigger press, when selection is non-empty/uncased, or when already handled.
     */
    public static String nextSelectionCaseForKeyEvent(
            int keyCode,
            Object action,
            int source,
            CharSequence selectedText) {
        if (!isSelectionCapsKeyCode(keyCode) || !isSelectionCapsTriggerAction(keyCode, action)) {
            return null;
        }
        return nextSelectionCaseText(selectedText);
    }

    private static boolean isSelectionCapsKeyCode(int keyCode) {
        return keyCode == -10032
                || keyCode == -10033
                || keyCode == -10012
                || keyCode == -10013
                || keyCode == 59
                || keyCode == 60
                || keyCode == 115;
    }

    private static boolean isSelectionCapsTriggerAction(int keyCode, Object action) {
        // Softkey Shift/unlock emits UP (-10033 / -10013); PRESS and hardware Shift release are also handled.
        if (action == null) {
            return true;
        }
        if (isActionNamed(action, ACTION_PRESS)) {
            return true;
        }
        return (keyCode == -10033 || keyCode == -10013) && isActionNamed(action, "UP");
    }

    /**
     * Computes the next case form when the user taps Shift/Enter Caps with non-empty selected text.
     * Cycles across lowercase -> Title Case -> UPPERCASE -> lowercase (and starts mixed-case text
     * at Title Case -> UPPERCASE -> lowercase). Returns {@code null} when there are no cased letters.
     */
    public static String nextSelectionCaseText(CharSequence selectedText) {
        if (selectedText == null || selectedText.length() == 0) {
            return null;
        }
        String current = selectedText.toString();
        Locale locale = Locale.getDefault();
        String lower = current.toLowerCase(locale);
        String upper = current.toUpperCase(locale);
        if (lower.equals(upper)) {
            return null;
        }
        String title = toTitleCase(current, locale);
        String next;
        if (current.equals(upper)) {
            next = lower;
        } else if (current.equals(title)) {
            next = upper;
        } else {
            next = title;
        }
        if (next.equals(current)) {
            next = current.equals(upper) ? lower : upper;
        }
        return next;
    }

    /**
     * Gates TouchActionBundle key popups so normal PRESS actions never show a keypress popup
     * when the user has turned off "Popup on keypress", even if a key sets alwaysShowPopup
     * or has multiple press KeyDatas. Non-PRESS actions (such as SLIDE_* or LONG_PRESS) preserve
     * existing behavior.
     */
    public static boolean shouldShowKeyPopup(
            boolean popupOnKeypress,
            Object action,
            boolean alwaysShowPopup,
            int keyDataCount) {
        if (isPressPopupBlocked(popupOnKeypress, action)) {
            return false;
        }
        return (popupOnKeypress || alwaysShowPopup || keyDataCount > 1)
                && !isActionNamed(action, ACTION_LONG_PRESS);
    }

    /**
     * Returns {@code true} when a direct showPopup call for a normal PRESS action must be
     * suppressed because "Popup on keypress" is disabled.
     */
    public static boolean isPressPopupBlocked(boolean popupOnKeypress, Object action) {
        return !popupOnKeypress && isActionNamed(action, ACTION_PRESS);
    }

    private static boolean isActionNamed(Object action, String expectedName) {
        if (action == null) {
            return false;
        }
        if (action instanceof Enum<?>) {
            return expectedName.equals(((Enum<?>) action).name());
        }
        return expectedName.equals(action.toString());
    }

    private static String toTitleCase(String text, Locale locale) {
        StringBuilder out = new StringBuilder(text.length());
        boolean startOfWord = true;
        int length = text.length();
        for (int index = 0; index < length; ) {
            int cp = text.codePointAt(index);
            int charCount = Character.charCount(cp);
            int nextIndex = index + charCount;
            if (Character.isLetter(cp)) {
                if (startOfWord) {
                    int titleCp = Character.toTitleCase(cp);
                    out.appendCodePoint(titleCp);
                } else {
                    out.append(text.substring(index, nextIndex).toLowerCase(locale));
                }
                startOfWord = false;
            } else if (Character.isDigit(cp)) {
                out.appendCodePoint(cp);
                startOfWord = false;
            } else if ((cp == '\'' || cp == '\u2019') && !startOfWord && hasLetterAt(text, nextIndex)) {
                out.appendCodePoint(cp);
                startOfWord = false;
            } else {
                out.appendCodePoint(cp);
                startOfWord = true;
            }
            index = nextIndex;
        }
        return out.toString();
    }

    private static boolean hasLetterAt(String text, int index) {
        return index < text.length() && Character.isLetter(text.codePointAt(index));
    }
}
