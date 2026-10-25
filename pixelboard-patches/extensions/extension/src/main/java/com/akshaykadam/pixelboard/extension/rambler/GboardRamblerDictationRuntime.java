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
import java.util.List;

/**
 * Runtime interceptor for Rambler Agentic Dictation voice text results.
 *
 * <p>When Rambler processes speech that resembles an instruction or question,
 * the backend server may classify the user's intent as an agentic command
 * (e.g. INTENT_COMPOSE = 4, INTENT_REWRITE = 3) and generate an AI answer,
 * placing the answer into {@code aajg.c} and the user's verbatim speech transcript
 * into {@code aajf.d} (full_transcript). This helper restores the user's verbatim
 * transcript into the committed text span and resets the intent to dictation (0),
 * ensuring speech-to-text always outputs what was spoken rather than an AI response.</p>
 */
public final class GboardRamblerDictationRuntime {

    private static final String TAG = "PixelBoardDictation";

    private GboardRamblerDictationRuntime() {
    }

    /**
     * Inspects incoming voice text spans and ensures the committed text is the verbatim
     * spoken transcript rather than an AI-generated prompt answer.
     *
     * @param voiceTextProto the Laajc proto instance
     */
    public static void filterVoiceText(Object voiceTextProto) {
        if (voiceTextProto == null) {
            return;
        }
        try {
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
                // span is Laajg
                Field actionField = findField(span.getClass(), "j");
                if (actionField == null) {
                    continue;
                }
                actionField.setAccessible(true);
                Object action = actionField.get(span);
                if (action == null) {
                    continue;
                }
                // action is Laajf
                Field transcriptField = findField(action.getClass(), "d");
                if (transcriptField == null) {
                    continue;
                }
                transcriptField.setAccessible(true);
                Object transcriptObj = transcriptField.get(action);
                if (!(transcriptObj instanceof String)) {
                    continue;
                }
                String transcript = (String) transcriptObj;
                if (transcript.isEmpty()) {
                    continue;
                }

                Field intentField = findField(action.getClass(), "c");
                if (intentField == null) {
                    continue;
                }
                intentField.setAccessible(true);
                int intent = intentField.getInt(action);

                // Intent 0 = DEFAULT_INTENT_DICTATION.
                // Intent 1 = INTENT_EDIT (e.g. "delete that", "clear").
                // Intent 2 = INTENT_EDIT_HISTORY (e.g. "undo", "redo").
                // Intent 3 = INTENT_REWRITE (Contextual commands e.g. "Fix grammar", "Translate to...").
                // Intent 4 = INTENT_COMPOSE (AI answers to prompts / questions).
                // Intent 5 = INTENT_RAMBLE.
                //
                // We only restore verbatim transcript for INTENT_COMPOSE (4) where the backend
                // generated an AI prompt/question answer rather than transcribing speech.
                // Contextual voice commands (INTENT_REWRITE = 3, INTENT_EDIT = 1, INTENT_EDIT_HISTORY = 2)
                // must NOT be overwritten, allowing Gboard to properly execute commands like
                // "Translate to Bengali", "Fix grammar", "Clear that", or "Undo" on previously
                // dictated text (#90).
                if (intent == 4) {
                    Field textSpanField = findField(span.getClass(), "c");
                    if (textSpanField != null) {
                        textSpanField.setAccessible(true);
                        textSpanField.set(span, transcript);
                        intentField.setInt(action, 0);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to filter voice text transcript", t);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
