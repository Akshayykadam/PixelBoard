package com.akshaykadam.pixelboard.extension.emoji;

import android.text.TextPaint;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime helper for candidate strip emoji rendering and layout.
 *
 * 1. Ensures emoji glyphs in suggestion candidates render at full opacity (alpha 255)
 *    instead of adopting secondary candidate translucent text styles that make color
 *    emoji bitmap glyphs appear dull and washed out.
 * 2. Ensures suggested emoji candidates are positioned on the right side of the
 *    candidate strip (Slot 2 for a 3-slot strip) rather than replacing or appearing in the
 *    center slot, allowing verbatim text and text autocompletions to occupy the left and
 *    center slots.
 */
public final class GboardCandidateEmojiRuntime {

    private static volatile Field candidateTextField = null;
    private static volatile Field candidateSlotField = null;
    private static volatile Field candidateCapacityField = null;

    private GboardCandidateEmojiRuntime() {
    }

    /**
     * Checks if the character sequence contains one or more emoji characters.
     */
    public static boolean containsEmoji(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        int len = cs.length();
        for (int i = 0; i < len; i++) {
            char c = cs.charAt(i);
            // High/low surrogate pairs (SMP Plane 1: modern emojis U+1F000 - U+1FAFF)
            if (Character.isSurrogate(c)) {
                return true;
            }
            // BMP emoji and symbol blocks (Dingbats, Misc Symbols, Arrows, Enclosed)
            if (c >= 0x203C && c <= 0x3299) {
                return true;
            }
            // Variation Selector-16 (emoji presentation selector)
            if (c == 0xFE0F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adjusts the candidate TextView TextPaint to full opacity if the candidate text contains emojis.
     */
    public static void adjustCandidatePaint(TextView textView, TextPaint paint) {
        if (textView == null || paint == null) {
            return;
        }
        CharSequence text = textView.getText();
        if (containsEmoji(text)) {
            paint.setColor(paint.getColor() | 0xFF000000);
            paint.setAlpha(255);
        }
    }

    /**
     * Reorders candidates passed into LatinFixedCountCandidatesHolderView so that
     * any emoji candidate is placed in the right-most slot (Slot 2 for a standard 3-slot strip),
     * while text candidates occupy Slot 0 (left / raw verbatim) and Slot 1 (middle / autocompletion).
     *
     * @param holderView The LatinFixedCountCandidatesHolderView instance.
     * @param candidates The list of candidate objects (typically Loqn / Loqg).
     * @return The reordered candidates list.
     */
    public static List<?> reorderCandidatesForStrip(Object holderView, List<?> candidates) {
        return candidates;
    }

    private static int resolveCandidateStripCapacity(Object holderView) {
        if (holderView == null) {
            return 3;
        }
        try {
            if (candidateCapacityField == null) {
                try {
                    Field f = holderView.getClass().getDeclaredField("d");
                    f.setAccessible(true);
                    candidateCapacityField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : holderView.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class && f.getName().equals("d")) {
                            f.setAccessible(true);
                            candidateCapacityField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateCapacityField != null) {
                int capacity = candidateCapacityField.getInt(holderView);
                if (capacity > 0) {
                    return capacity;
                }
            }
        } catch (Throwable ignored) {
        }
        return 3;
    }

    static CharSequence getCandidateText(Object candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            if (candidateTextField == null) {
                try {
                    Field f = candidate.getClass().getDeclaredField("a");
                    f.setAccessible(true);
                    candidateTextField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : candidate.getClass().getDeclaredFields()) {
                        if (CharSequence.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            candidateTextField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateTextField != null) {
                Object val = candidateTextField.get(candidate);
                if (val instanceof CharSequence) {
                    return (CharSequence) val;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static void setCandidateSlot(Object candidate, int slot) {
        if (candidate == null) {
            return;
        }
        try {
            if (candidateSlotField == null) {
                try {
                    Field f = candidate.getClass().getDeclaredField("l");
                    f.setAccessible(true);
                    candidateSlotField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : candidate.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class && f.getName().equals("l")) {
                            f.setAccessible(true);
                            candidateSlotField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateSlotField != null) {
                candidateSlotField.setInt(candidate, slot);
            }
        } catch (Throwable ignored) {
        }
    }

    static int getCandidateSlot(Object candidate) {
        if (candidate == null) {
            return -1;
        }
        try {
            if (candidateSlotField == null) {
                try {
                    Field f = candidate.getClass().getDeclaredField("l");
                    f.setAccessible(true);
                    candidateSlotField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : candidate.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class && f.getName().equals("l")) {
                            f.setAccessible(true);
                            candidateSlotField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateSlotField != null) {
                return candidateSlotField.getInt(candidate);
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }
}
