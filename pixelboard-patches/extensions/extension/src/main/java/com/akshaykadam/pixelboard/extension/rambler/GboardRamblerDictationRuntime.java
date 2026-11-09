/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Runtime interceptor for Rambler Agentic Dictation voice text results.
 *
 * <p>Handles:
 * <ol>
 *   <li>Restoring verbatim transcripts for AI prompt/question answers (INTENT_COMPOSE = 4).</li>
 *   <li>Preventing dropping of repeated phrases (Issue #134, Test 1).</li>
 *   <li>Preventing diff-replacement from overwriting existing text when dictating similar phrases (Issue #134, Test 2).</li>
 *   <li>Preserving genuine voice editing commands ("make it all caps", "translate to...", "clear that", "undo").</li>
 * </ol>
 * </p>
 */
public final class GboardRamblerDictationRuntime {

    private static final String TAG = "PixelBoardDictation";

    private GboardRamblerDictationRuntime() {
    }

    /**
     * Backward-compatible entrypoint without extension instance.
     *
     * @param voiceTextProto the VoiceText proto instance
     */
    public static void filterVoiceText(Object voiceTextProto) {
        filterVoiceText(null, voiceTextProto);
    }

    /**
     * Inspects incoming voice text spans, ensuring verbatim spoken transcripts are preserved,
     * repeated phrases are appended rather than dropped, and preceding text is not mutated
     * by diff-match-patch unless an explicit voice editing command was spoken.
     *
     * @param extension      the AgenticDictationExtension instance (may be null)
     * @param voiceTextProto the VoiceText proto instance
     */
    public static void filterVoiceText(Object extension, Object voiceTextProto) {
        if (voiceTextProto == null) {
            return;
        }
        try {
            String textBefore = "";
            String selectedText = "";
            String textAfter = "";

            if (extension != null) {
                try {
                    Field iField = findField(extension.getClass(), "i");
                    Object oxs = iField != null ? iField.get(extension) : null;
                    if (oxs != null) {
                        Method nMethod = findMethod(oxs.getClass(), "n");
                        if (nMethod != null) {
                            Object beforeCs = nMethod.invoke(oxs);
                            if (beforeCs != null) {
                                textBefore = beforeCs.toString();
                            }
                        }
                        Method lMethod = findMethod(oxs.getClass(), "l");
                        if (lMethod != null) {
                            Object selCs = lMethod.invoke(oxs);
                            if (selCs != null) {
                                selectedText = selCs.toString();
                            }
                        }
                        Method mMethod = findMethod(oxs.getClass(), "m");
                        if (mMethod != null) {
                            Object afterCs = mMethod.invoke(oxs);
                            if (afterCs != null) {
                                textAfter = afterCs.toString();
                            }
                        }
                    }

                    if (textBefore.isEmpty()) {
                        Method vMethod = findMethod(extension.getClass(), "v");
                        Object operator = vMethod != null ? vMethod.invoke(extension) : null;
                        if (operator != null) {
                            Method kMethod = findMethod(operator.getClass(), "K", int.class);
                            if (kMethod != null) {
                                Object beforeCs = kMethod.invoke(operator, 4000);
                                if (beforeCs != null) {
                                    textBefore = beforeCs.toString();
                                }
                            }
                            Method lOpMethod = findMethod(operator.getClass(), "L", int.class);
                            if (lOpMethod != null && textAfter.isEmpty()) {
                                Object afterCs = lOpMethod.invoke(operator, 4000);
                                if (afterCs != null) {
                                    textAfter = afterCs.toString();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Failed to retrieve surrounding text from extension", t);
                }
            }

            Field spansField = findField(voiceTextProto.getClass(), "b");
            if (spansField == null) {
                return;
            }
            spansField.setAccessible(true);
            Object spansObj = spansField.get(voiceTextProto);
            if (!(spansObj instanceof List)) {
                return;
            }
            List<?> spans = (List<?>) spansObj;
            for (Object span : spans) {
                if (span == null) {
                    continue;
                }
                Field actionField = findField(span.getClass(), "j");
                if (actionField == null) {
                    continue;
                }
                actionField.setAccessible(true);
                Object action = actionField.get(span);
                if (action == null) {
                    continue;
                }

                Field transcriptField = findField(action.getClass(), "d");
                String transcript = "";
                if (transcriptField != null) {
                    transcriptField.setAccessible(true);
                    Object transcriptObj = transcriptField.get(action);
                    if (transcriptObj instanceof String) {
                        transcript = (String) transcriptObj;
                    }
                }

                Field intentField = findField(action.getClass(), "c");
                if (intentField == null) {
                    continue;
                }
                intentField.setAccessible(true);
                int intent = intentField.getInt(action);

                Field textSpanField = findField(span.getClass(), "c");
                if (textSpanField == null) {
                    continue;
                }
                textSpanField.setAccessible(true);
                String spanText = "";
                Object spanTextObj = textSpanField.get(span);
                if (spanTextObj instanceof String) {
                    spanText = (String) spanTextObj;
                }

                // Intent 0 = DEFAULT_INTENT_DICTATION.
                // Intent 1 = INTENT_EDIT (e.g. "delete that", "clear").
                // Intent 2 = INTENT_EDIT_HISTORY (e.g. "undo", "redo").
                // Intent 3 = INTENT_REWRITE (Contextual commands e.g. "Fix grammar", "Translate to...").
                // Intent 4 = INTENT_COMPOSE (AI answers to prompts / questions).
                // Intent 5 = INTENT_RAMBLE.

                // Restore verbatim transcript for INTENT_COMPOSE (4) where the backend
                // generated an AI prompt/question answer rather than transcribing speech.
                if (intent == 4) {
                    if (!transcript.isEmpty()) {
                        spanText = transcript;
                        textSpanField.set(span, transcript);
                        intentField.setInt(action, 0);
                        clearDiffField(action);
                        intent = 0;
                    }
                }

                // Check whether this turn represents an explicit voice editing command
                // (e.g. "Translate to French", "Make it all caps", "Clear that", "Undo").
                boolean isCommand = isVoiceCommand(intent, transcript, selectedText, action);

                if (!isCommand) {
                    // Regular dictation: the user spoke text to insert, not a command.
                    // If an errant diff was generated (e.g. Test 2 where Rambler mistook speech
                    // for an edit of preceding text), clear it and set intent to dictation (0).
                    clearDiffField(action);
                    if (intent != 0 && intent != 5) {
                        intentField.setInt(action, 0);
                    }

                    String textToInsert = !transcript.isEmpty() ? transcript : spanText;
                    if (!textToInsert.isEmpty()) {
                        // Check if spanText ALREADY starts with textBefore, ends with textAfter,
                        // and contains a non-empty delta (normal dictation path).
                        boolean hasPrefixAndDelta = !textBefore.isEmpty()
                                && spanText != null
                                && spanText.startsWith(textBefore)
                                && spanText.endsWith(textAfter)
                                && spanText.length() > (textBefore.length() + textAfter.length());

                        if (!hasPrefixAndDelta) {
                            // If spanText equals textBefore (Test 1, repeated phrase) or does NOT
                            // start with textBefore (Test 2, similar phrase), format spanText with
                            // textBefore + separator + textToInsert + textAfter.
                            // Gboard's native logic will cleanly extract delta = separator + textToInsert
                            // and commit it at the cursor without dropping or diff-overwriting!
                            String formattedText;
                            if (!textBefore.isEmpty()) {
                                boolean needsSpace = needsSpaceBetween(textBefore, textToInsert);
                                formattedText = textBefore + (needsSpace ? " " : "") + textToInsert + textAfter;
                            } else {
                                formattedText = textToInsert + textAfter;
                            }
                            textSpanField.set(span, formattedText);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to filter voice text transcript", t);
        }
    }

    private static boolean isVoiceCommand(int intent, String transcript, String selectedText, Object action) {
        if (transcript == null || transcript.isEmpty()) {
            return false;
        }

        // If the user actively selected text in the editor, and intent is edit/rewrite,
        // they specifically highlighted text to operate on.
        if (selectedText != null && !selectedText.trim().isEmpty() && (intent == 1 || intent == 3)) {
            return true;
        }

        String lower = transcript.trim().toLowerCase(Locale.ROOT);

        // Intent 1 (INTENT_EDIT) and Intent 2 (INTENT_EDIT_HISTORY):
        if (intent == 1 || intent == 2) {
            if (isEditCommand(lower)) {
                return true;
            }
        }

        // Intent 3 (INTENT_REWRITE):
        if (intent == 3) {
            if (isRewriteCommand(lower)) {
                return true;
            }
            // Check if Rambler's diff indicates an echo/correction of surrounding text
            // rather than a genuine command (Test 2).
            if (isDiffDictationEcho(action, transcript)) {
                return false;
            }
        }

        return false;
    }

    private static boolean isDiffDictationEcho(Object action, String transcript) {
        try {
            Field diffField = findField(action.getClass(), "e");
            if (diffField == null) {
                return false;
            }
            diffField.setAccessible(true);
            Object diff = diffField.get(action);
            if (diff == null) {
                return false;
            }

            Field cField = findField(diff.getClass(), "c");
            String prefix = cField != null ? (String) cField.get(diff) : null;
            if (prefix == null) {
                prefix = "";
            }

            String normTranscript = normalizeForComparison(transcript);
            String normPrefix = normalizeForComparison(prefix);

            if (!normPrefix.isEmpty() && normTranscript.startsWith(normPrefix)) {
                return true;
            }
            if (!normPrefix.isEmpty() && normPrefix.length() > 5 && normTranscript.contains(normPrefix)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isEditCommand(String lower) {
        if (lower.startsWith("delete") || lower.startsWith("clear") || lower.startsWith("erase")
                || lower.startsWith("remove") || lower.startsWith("scratch that")
                || lower.startsWith("undo") || lower.startsWith("redo") || lower.startsWith("revert")
                || lower.startsWith("удали") || lower.startsWith("стереть") || lower.startsWith("очисти")
                || lower.startsWith("отмени")) {
            return true;
        }
        return lower.equals("delete") || lower.equals("clear") || lower.equals("undo") || lower.equals("redo");
    }

    private static boolean isRewriteCommand(String lower) {
        if (lower.startsWith("translate") || lower.startsWith("переведи")
                || lower.startsWith("traduire") || lower.startsWith("traducir") || lower.startsWith("übersetz")) {
            return true;
        }
        if (lower.startsWith("make it") || lower.startsWith("make this") || lower.startsWith("сделай")) {
            return true;
        }
        if (lower.startsWith("capitalize") || lower.startsWith("uppercase") || lower.startsWith("lowercase")
                || lower.startsWith("all caps")) {
            return true;
        }
        if (lower.startsWith("fix grammar") || lower.startsWith("fix spelling") || lower.startsWith("proofread")
                || lower.startsWith("check grammar") || lower.startsWith("исправь ошибки")
                || lower.startsWith("исправь грамматику") || lower.startsWith("проверь грамматику")) {
            return true;
        }
        if (lower.startsWith("rewrite") || lower.startsWith("rephrase") || lower.startsWith("paraphrase")
                || lower.startsWith("перефразируй") || lower.startsWith("перепиши")) {
            return true;
        }
        if (lower.startsWith("replace ") || lower.startsWith("change ")
                || lower.startsWith("замени ") || lower.startsWith("поменяй ")) {
            return true;
        }
        return false;
    }

    private static void clearDiffField(Object action) {
        try {
            Field diffField = findField(action.getClass(), "e");
            if (diffField != null) {
                diffField.setAccessible(true);
                diffField.set(action, null);
            }
            Field bField = findField(action.getClass(), "b");
            if (bField != null) {
                bField.setAccessible(true);
                int b = bField.getInt(action);
                bField.setInt(action, b & ~0x10);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean needsSpaceBetween(String textBefore, String textToInsert) {
        if (textBefore == null || textBefore.isEmpty()) {
            return false;
        }
        if (textToInsert == null || textToInsert.isEmpty()) {
            return false;
        }
        char lastChar = textBefore.charAt(textBefore.length() - 1);
        if (Character.isWhitespace(lastChar)) {
            return false;
        }
        char firstChar = textToInsert.charAt(0);
        if (Character.isWhitespace(firstChar)) {
            return false;
        }
        if (firstChar == '.' || firstChar == ',' || firstChar == '!' || firstChar == '?'
                || firstChar == ';' || firstChar == ':' || firstChar == ')' || firstChar == ']'
                || firstChar == '}' || firstChar == '—' || firstChar == '-') {
            return false;
        }
        if (lastChar == '(' || lastChar == '[' || lastChar == '{' || lastChar == '"'
                || lastChar == '\'' || lastChar == '«') {
            return false;
        }
        return true;
    }

    private static String normalizeForComparison(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method m = current.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
