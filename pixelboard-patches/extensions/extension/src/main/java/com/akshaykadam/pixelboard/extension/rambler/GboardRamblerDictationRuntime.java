/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
    private static final String KEY_USER_PREFERENCES = "user_preferences";
    private static final Pattern INSTRUCTION_TAG_PATTERN =
            Pattern.compile("(?i)</?(user_instruction|custom_instruction)[^>]*>");
    private static final String CUSTOM_INSTRUCTION_WRAPPER =
            "Extremely important!!!:\n"
                    + "<custom_instruction>\n"
                    + "The text between <user_instruction> and </user_instruction> contains "
                    + "user-defined preferences for dictation text output.\n"
                    + "Rules:\n"
                    + "1. Scope: Apply only to dictation text preferences (e.g., verbatim fidelity, "
                    + "spelling, grammar, language and script conventions, vocabulary, formatting, "
                    + "punctuation, casing, and tone).\n"
                    + "2. Conditions & Grounding: Apply strictly when relevant to the spoken input "
                    + "and active context.\n"
                    + "3. Priority: Customizes dictation styling, verbatim fidelity, and script conventions.\n\n"
                    + "<user_instruction>\n%s\n</user_instruction>\n</custom_instruction>\n";

    private GboardRamblerDictationRuntime() {
    }

    /**
     * Injects PixelBoard verbatim / custom dictation instructions and Indic script preferences
     * into Rambler's session bundle ("user_preferences" -> S3 user_preferences header).
     */
    public static Bundle applyDictationPreferences(Object extension, Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        try {
            Context context = resolveExtensionContext(extension);
            if (context == null) {
                context = GboardRambler1803OfficialSelectionRuntime.applicationContext();
            }
            boolean verbatimMode = GboardRamblerSettings.isVerbatimModeEnabled(context);
            String customInstruction = GboardRamblerSettings.readCustomInstruction(context);
            String indicInstruction =
                    GboardRamblerVoiceScriptRuntime.buildActiveIndicScriptInstruction(context);

            List<String> directives = new ArrayList<String>(3);
            if (verbatimMode) {
                directives.add(GboardRamblerSettings.STRICT_VERBATIM_INSTRUCTION);
            }
            if (indicInstruction != null && !indicInstruction.isEmpty()) {
                directives.add(indicInstruction);
            }
            if (customInstruction != null && !customInstruction.isEmpty()) {
                String sanitized = INSTRUCTION_TAG_PATTERN.matcher(customInstruction)
                        .replaceAll("")
                        .trim();
                if (!sanitized.isEmpty()) {
                    directives.add(sanitized);
                }
            }
            if (directives.isEmpty()) {
                return bundle;
            }

            String combinedDirective = String.join("\n", directives);
            ArrayList<String> userPrefs = bundle.getStringArrayList(KEY_USER_PREFERENCES);
            ArrayList<String> updated = userPrefs != null
                    ? new ArrayList<String>(userPrefs)
                    : new ArrayList<String>(2);

            boolean replacedExisting = false;
            for (int i = 0; i < updated.size(); i++) {
                String existing = updated.get(i);
                if (existing != null && existing.contains("</user_instruction>")) {
                    updated.set(i, existing.replace(
                            "</user_instruction>",
                            "\n" + combinedDirective + "\n</user_instruction>"));
                    replacedExisting = true;
                    break;
                }
            }
            if (!replacedExisting) {
                updated.add(String.format(Locale.ROOT, CUSTOM_INSTRUCTION_WRAPPER, combinedDirective));
            }
            bundle.putStringArrayList(KEY_USER_PREFERENCES, updated);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to apply dictation user_preferences", t);
        }
        return bundle;
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

                Context extContext = resolveExtensionContext(extension);
                if (extContext == null) {
                    extContext = GboardRambler1803OfficialSelectionRuntime.applicationContext();
                }
                boolean verbatimMode = GboardRamblerSettings.isVerbatimModeEnabled(extContext);
                String lower = transcript != null ? transcript.trim().toLowerCase(Locale.ROOT) : "";
                boolean explicitCommand = intent == 1 || intent == 2
                        || (selectedText != null && !selectedText.trim().isEmpty())
                        || isEditCommand(lower) || isRewriteCommand(lower);

                // In Verbatim Dictation Mode (Issue #193), always prefer the unedited full_transcript
                // when provided by S3/Lite unless the user issued an explicit edit/rewrite command.
                if (verbatimMode && !explicitCommand && !transcript.isEmpty()
                        && (intent == 0 || intent == 3 || intent == 4 || intent == 5)) {
                    spanText = transcript;
                    textSpanField.set(span, transcript);
                    intentField.setInt(action, 0);
                    clearDiffField(action);
                    intent = 0;
                    continue;
                }

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

                // Check whether an errant diff was generated (e.g. Issue #134 Test 2, where Rambler
                // mistook newly spoken text at the end of the document for an edit of preceding text).
                // In contrast, genuine mid-sentence edits (Issue #167, where cursor is placed mid-text
                // or diff has a suffix) and explicit voice commands must retain their diffs.
                // In Microsoft Word / Office (Issue #112), diff deletion desynchronizes the cursor to (0, 0),
                // so all diffs are treated as errant to commit verbatim speech text cleanly.
                boolean isWord = isWordOrOfficeApp(extension);
                boolean isErrantDiff = isWord || isErrantDictationDiff(
                        action, transcript, textBefore, textAfter, selectedText, intent);

                if (isErrantDiff) {
                    clearDiffField(action);
                    if (intent != 0 && intent != 5) {
                        intentField.setInt(action, 0);
                    }
                    if (!transcript.isEmpty()) {
                        textSpanField.set(span, transcript);
                    }
                } else {
                    // Legitimate diff or normal dictation without an errant diff.
                    // If Rambler dropped spoken text or echoed textBefore during dictation when NO diff is present:
                    Field diffField = findField(action.getClass(), "e");
                    Object diff = diffField != null ? diffField.get(action) : null;
                    if (diff == null && (intent == 0 || intent == 5) && !transcript.isEmpty()
                            && (spanText == null || spanText.isEmpty() || spanText.equals(textBefore))) {
                        textSpanField.set(span, transcript);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to filter voice text transcript", t);
        }
    }

    private static boolean isErrantDictationDiff(
            Object action,
            String transcript,
            String textBefore,
            String textAfter,
            String selectedText,
            int intent) {
        if (action == null) {
            return false;
        }

        Field diffField = findField(action.getClass(), "e");
        if (diffField == null) {
            return false;
        }
        Object diff;
        try {
            diff = diffField.get(action);
        } catch (Throwable t) {
            return false;
        }
        if (diff == null) {
            return false;
        }

        // 1. If text is actively selected, the user specifically highlighted text to operate on.
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            return false;
        }

        // 2. If there is text after the cursor in the editor, the user placed the cursor
        // mid-sentence to edit or insert text (Issue #167).
        if (textAfter != null && !textAfter.trim().isEmpty()) {
            return false;
        }

        String diffPrefix = "";
        String diffToReplace = "";
        String diffSuffix = "";
        try {
            Field cField = findField(diff.getClass(), "c");
            if (cField != null && cField.get(diff) instanceof String) {
                diffPrefix = (String) cField.get(diff);
            }
            Field dField = findField(diff.getClass(), "d");
            if (dField != null && dField.get(diff) instanceof String) {
                diffToReplace = (String) dField.get(diff);
            }
            Field eField = findField(diff.getClass(), "e");
            if (eField != null && eField.get(diff) instanceof String) {
                diffSuffix = (String) eField.get(diff);
            }
        } catch (Throwable ignored) {
        }

        // 3. If the diff has a non-empty suffix, the text being replaced is surrounded
        // by following text — it is an in-place word edit (Issue #167).
        if (diffSuffix != null && !diffSuffix.trim().isEmpty()) {
            return false;
        }

        // 4. Intent 1 (INTENT_EDIT) and Intent 2 (INTENT_EDIT_HISTORY) are always genuine actions.
        if (intent == 1 || intent == 2) {
            return false;
        }

        // 5. If the transcript is an explicit edit or rewrite command, it is an intentional action.
        String lower = transcript != null ? transcript.trim().toLowerCase(Locale.ROOT) : "";
        if (isEditCommand(lower) || isRewriteCommand(lower)) {
            return false;
        }

        // 6. Check if diff is trying to replace the tail of textBefore when the user is dictating
        // at the end of the field (Issue #134, Test 2).
        // In Issue #134:
        // textBefore ended with diffToReplace (e.g. "прокурор."), diffPrefix was preceding words,
        // diffSuffix is empty, and transcript starts with diffPrefix followed by new words.
        if (!diffPrefix.isEmpty() && diffPrefix.length() >= 4) {
            String normTranscript = normalizeForComparison(transcript);
            String normPrefix = normalizeForComparison(diffPrefix);
            if (!normPrefix.isEmpty() && normTranscript.startsWith(normPrefix)) {
                if (textBefore != null && !textBefore.isEmpty()) {
                    String trimmedBefore = textBefore.trim();
                    if (!diffToReplace.isEmpty() && trimmedBefore.endsWith(diffToReplace.trim())) {
                        return true;
                    }
                    if ((diffPrefix + diffToReplace).trim().equals(trimmedBefore)) {
                        return true;
                    }
                }
                if (!diffToReplace.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isEditCommand(String lower) {
        if (lower.startsWith("delete") || lower.startsWith("clear") || lower.startsWith("erase")
                || lower.startsWith("remove") || lower.startsWith("scratch that")
                || lower.startsWith("undo") || lower.startsWith("redo") || lower.startsWith("revert")
                || lower.startsWith("add ") || lower.startsWith("insert ") || lower.startsWith("put ")
                || lower.startsWith("добавь") || lower.startsWith("вставь") || lower.startsWith("поставь")
                || lower.startsWith("удали") || lower.startsWith("стереть") || lower.startsWith("очисти")
                || lower.startsWith("отмени")) {
            return true;
        }
        return lower.equals("delete") || lower.equals("clear") || lower.equals("undo") || lower.equals("redo");
    }

    private static boolean isRewriteCommand(String lower) {
        if (lower.startsWith("add ") || lower.startsWith("insert ") || lower.startsWith("put ")
                || lower.startsWith("добавь") || lower.startsWith("вставь") || lower.startsWith("поставь")) {
            return true;
        }
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

    private static boolean isWordOrOfficeApp(Object extension) {
        if (extension == null) {
            return false;
        }
        try {
            String targetPackage = null;
            Field gField = findField(extension.getClass(), "g");
            if (gField == null) gField = findField(extension.getClass(), "m");
            if (gField == null) gField = findField(extension.getClass(), "N");
            if (gField != null) {
                Object editorInfo = gField.get(extension);
                if (editorInfo instanceof android.view.inputmethod.EditorInfo) {
                    targetPackage = ((android.view.inputmethod.EditorInfo) editorInfo).packageName;
                }
            }
            if (targetPackage == null || targetPackage.isEmpty()) {
                Method vMethod = findMethod(extension.getClass(), "v");
                Object operator = vMethod != null ? vMethod.invoke(extension) : null;
                if (operator != null) {
                    Method lMethod = findMethod(operator.getClass(), "l");
                    if (lMethod != null) {
                        Object editorInfo = lMethod.invoke(operator);
                        if (editorInfo instanceof android.view.inputmethod.EditorInfo) {
                            targetPackage = ((android.view.inputmethod.EditorInfo) editorInfo).packageName;
                        }
                    }
                }
            }
            if (targetPackage != null) {
                String lower = targetPackage.toLowerCase(Locale.ROOT);
                return lower.contains("office.word") || lower.contains("officehub") || lower.startsWith("com.microsoft.office");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Context resolveExtensionContext(Object extension) {
        if (extension == null) {
            return null;
        }
        try {
            Field bField = findField(extension.getClass(), "b");
            if (bField != null) {
                Object val = bField.get(extension);
                if (val instanceof Context) {
                    return (Context) val;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
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
