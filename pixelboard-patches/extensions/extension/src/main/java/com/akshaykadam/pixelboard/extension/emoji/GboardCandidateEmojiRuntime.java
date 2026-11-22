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
        if (candidates == null || candidates.size() < 2) {
            return candidates;
        }

        try {
            int maxSlots = resolveCandidateStripCapacity(holderView);
            int rightSlot = Math.max(1, maxSlots - 1);

            List<Object> textCandidates = new ArrayList<>();
            List<Object> emojiCandidates = new ArrayList<>();

            for (int i = 0; i < candidates.size(); i++) {
                Object candidate = candidates.get(i);
                if (candidate == null) {
                    continue;
                }
                CharSequence text = getCandidateText(candidate);
                if (containsEmoji(text)) {
                    emojiCandidates.add(candidate);
                } else {
                    textCandidates.add(candidate);
                }
            }

            // Only reorder when there are both text candidates and at least one emoji candidate
            if (emojiCandidates.isEmpty() || textCandidates.isEmpty()) {
                return candidates;
            }

            // In a Latin strip with capacity maxSlots (typically 3):
            // Slot 0 (left): verbatim text (textCandidates.get(0))
            // Slot 1 (center): prediction autocompletion (textCandidates.get(1))
            // Slot 2 (right): primary suggested emoji (emojiCandidates.get(0))
            // Any remaining text candidates and emoji candidates follow after Slot 2.
            List<Object> reordered = new ArrayList<>(candidates.size());

            // 1. Top text candidates before the right slot (e.g. index 0 and index 1 for maxSlots = 3)
            int textSlotsBeforeRight = Math.min(textCandidates.size(), rightSlot);
            for (int i = 0; i < textSlotsBeforeRight; i++) {
                reordered.add(textCandidates.get(i));
            }

            // 2. Primary emoji candidate at the right slot (e.g. index 2 for maxSlots = 3)
            reordered.add(emojiCandidates.get(0));

            // 3. Overflow text candidates (index 2 onwards)
            for (int i = textSlotsBeforeRight; i < textCandidates.size(); i++) {
                reordered.add(textCandidates.get(i));
            }

            // 4. Overflow emoji candidates (index 1 onwards)
            for (int i = 1; i < emojiCandidates.size(); i++) {
                reordered.add(emojiCandidates.get(i));
            }

            // Assign each candidate its sequential slot (0, 1, 2...)
            // so Gboard's LatinFixedCountCandidatesHolderView (and its Lewj/Ltbm iterators)
            // places candidate 0 in Slot 0 (left), candidate 1 in Slot 1 (center), and candidate 2 in Slot 2 (right).
            for (int i = 0; i < reordered.size(); i++) {
                setCandidateSlot(reordered.get(i), i);
            }

            // Reconstruct in-place if mutable as a failsafe
            try {
                @SuppressWarnings("unchecked")
                List<Object> mutable = (List<Object>) candidates;
                mutable.clear();
                mutable.addAll(reordered);
            } catch (Throwable ignored) {
            }

            return reordered;
        } catch (Throwable ignored) {
            return candidates;
        }
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
