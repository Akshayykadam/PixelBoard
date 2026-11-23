package com.akshaykadam.pixelboard.extension.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime helper for candidate strip emoji rendering, layout, and personal dictionary protection.
 *
 * 1. Ensures emoji glyphs in suggestion candidates render at full opacity (alpha 255)
 *    instead of adopting secondary candidate translucent text styles that make color
 *    emoji bitmap glyphs appear dull and washed out.
 * 2. Ensures suggested emoji candidates are positioned on the right side of the
 *    candidate strip (Slot 2 for a 3-slot strip) rather than replacing or appearing in the
 *    center slot, allowing verbatim text and text autocompletions to occupy the left and
 *    center slots.
 * 3. Prevents Gboard from auto-correcting away intended personal dictionary words or user-taught
 *    words on spacebar (fixing Issue #171). When the verbatim input (Slot 0) matches an entry in
 *    PersonalDictionary.db or user-taught words, autoCorrection is cleared on suggestions so that
 *    space commits the typed word verbatim.
 */
public final class GboardCandidateEmojiRuntime {

    private static final String PREF_NAME_TAUGHT_WORDS = "pixelboard_user_taught_words";
    private static final String KEY_TAUGHT_WORDS_SET = "taught_words";

    private static final Set<String> DICTIONARY_WORDS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> USER_TAUGHT_WORDS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<View> ATTACHED_VIEWS =
            Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
    private static final Object DB_LOCK = new Object();

    private static volatile long lastDbModified = -1L;
    private static volatile boolean taughtWordsLoaded = false;
    private static volatile String currentSlot0Word = null;

    private static volatile Field candidateTextField = null;
    private static volatile Field candidateSlotField = null;
    private static volatile Field candidateCapacityField = null;
    private static volatile Field candidateAutoCorrectionField = null;
    private static volatile Method candidateSoftKeyViewMethod = null;

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
     * Reorders candidates passed into LatinFixedCountCandidatesHolderView and protects
     * personal dictionary and user-taught words against unwanted auto-correction commits.
     *
     * @param holderView The LatinFixedCountCandidatesHolderView instance.
     * @param candidates The list of candidate objects (typically Loqn / Loqg).
     * @return The reordered candidates list.
     */
    public static List<?> reorderCandidatesForStrip(Object holderView, List<?> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        try {
            protectDictionaryCandidates(holderView, candidates);

            if (candidates.size() < 2) {
                return candidates;
            }

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
            // so Gboard's LatinFixedCountCandidatesHolderView
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

    /**
     * Inspects Candidate 0 (the user's verbatim input). If the typed word is present in
     * PersonalDictionary.db or user-taught words, clears autoCorrection on all suggestion candidates
     * so that spacebar inserts the verbatim typed word without substituting another word.
     */
    static void protectDictionaryCandidates(Object holderView, List<?> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        try {
            Object rawCandidate = candidates.get(0);
            CharSequence rawCs = getCandidateText(rawCandidate);
            if (rawCs == null || rawCs.length() == 0) {
                return;
            }
            String rawWord = rawCs.toString().trim().toLowerCase();
            Context context = resolveViewContext(holderView);
            if (context != null) {
                ensureDictionaryLoaded(context);
                ensureTaughtWordsLoaded(context);
                attachSlot0TouchListener(holderView, context, rawWord);
            }

            if (isWordProtected(context, rawWord)) {
                for (int i = 0; i < candidates.size(); i++) {
                    setCandidateAutoCorrection(candidates.get(i), false);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static Context resolveViewContext(Object holderView) {
        if (holderView instanceof View) {
            return ((View) holderView).getContext();
        }
        return null;
    }

    static boolean isWordProtected(Context context, String rawWord) {
        if (rawWord == null) {
            return false;
        }
        String clean = rawWord.trim().toLowerCase();
        if (clean.isEmpty()) {
            return false;
        }
        if (DICTIONARY_WORDS.contains(clean) || USER_TAUGHT_WORDS.contains(clean)) {
            return true;
        }
        if (context != null) {
            ensureDictionaryLoaded(context);
            ensureTaughtWordsLoaded(context);
            return DICTIONARY_WORDS.contains(clean) || USER_TAUGHT_WORDS.contains(clean);
        }
        return false;
    }

    static void ensureDictionaryLoaded(Context context) {
        if (context == null) {
            return;
        }
        try {
            File dbFile = context.getDatabasePath("PersonalDictionary.db");
            if (dbFile == null || !dbFile.exists()) {
                return;
            }
            long modified = dbFile.lastModified();
            if (modified == lastDbModified) {
                return;
            }
            synchronized (DB_LOCK) {
                if (modified == lastDbModified) {
                    return;
                }
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = SQLiteDatabase.openDatabase(
                            dbFile.getPath(), null,
                            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    cursor = db.rawQuery("SELECT word FROM entry", null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int col = cursor.getColumnIndex("word");
                        if (col >= 0) {
                            Set<String> reloaded = new HashSet<>();
                            do {
                                String w = cursor.getString(col);
                                if (w != null) {
                                    String t = w.trim().toLowerCase();
                                    if (!t.isEmpty()) {
                                        reloaded.add(t);
                                    }
                                }
                            } while (cursor.moveToNext());
                            DICTIONARY_WORDS.clear();
                            DICTIONARY_WORDS.addAll(reloaded);
                        }
                    }
                    lastDbModified = modified;
                } catch (Throwable ignored) {
                } finally {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Throwable ignored) {
                        }
                    }
                    if (db != null) {
                        try {
                            db.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static void ensureTaughtWordsLoaded(Context context) {
        if (taughtWordsLoaded || context == null) {
            return;
        }
        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME_TAUGHT_WORDS, Context.MODE_PRIVATE);
            Set<String> saved = prefs.getStringSet(KEY_TAUGHT_WORDS_SET, null);
            if (saved != null) {
                for (String s : saved) {
                    if (s != null && !s.isEmpty()) {
                        USER_TAUGHT_WORDS.add(s.trim().toLowerCase());
                    }
                }
            }
            taughtWordsLoaded = true;
        } catch (Throwable ignored) {
        }
    }

    static void recordUserTaughtWord(Context context, String word) {
        if (word == null) {
            return;
        }
        String clean = word.trim().toLowerCase();
        if (clean.isEmpty()) {
            return;
        }
        USER_TAUGHT_WORDS.add(clean);
        if (context != null) {
            try {
                SharedPreferences prefs =
                        context.getSharedPreferences(PREF_NAME_TAUGHT_WORDS, Context.MODE_PRIVATE);
                Set<String> existing = prefs.getStringSet(KEY_TAUGHT_WORDS_SET, null);
                Set<String> updated = new HashSet<>();
                if (existing != null) {
                    updated.addAll(existing);
                }
                updated.add(clean);
                prefs.edit().putStringSet(KEY_TAUGHT_WORDS_SET, updated).apply();
            } catch (Throwable ignored) {
            }
        }
    }

    static void attachSlot0TouchListener(Object holderView, final Context context, String currentRawWord) {
        currentSlot0Word = currentRawWord;
        Object softKeyView = getSoftKeyView(holderView, 0);
        if (softKeyView instanceof View) {
            View v = (View) softKeyView;
            if (!ATTACHED_VIEWS.contains(v)) {
                v.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        if (event != null && event.getAction() == MotionEvent.ACTION_UP) {
                            String wordToTeach = currentSlot0Word;
                            if (wordToTeach != null && !wordToTeach.isEmpty()) {
                                recordUserTaughtWord(context, wordToTeach);
                            }
                        }
                        return false;
                    }
                });
                ATTACHED_VIEWS.add(v);
            }
        }
    }

    static Object getSoftKeyView(Object holderView, int slot) {
        if (holderView == null) {
            return null;
        }
        try {
            if (candidateSoftKeyViewMethod == null) {
                try {
                    Method m = holderView.getClass().getMethod("B", int.class);
                    m.setAccessible(true);
                    candidateSoftKeyViewMethod = m;
                } catch (NoSuchMethodException e) {
                    for (Method m : holderView.getClass().getMethods()) {
                        if (m.getParameterTypes().length == 1
                                && m.getParameterTypes()[0] == int.class
                                && View.class.isAssignableFrom(m.getReturnType())) {
                            m.setAccessible(true);
                            candidateSoftKeyViewMethod = m;
                            break;
                        }
                    }
                }
            }
            if (candidateSoftKeyViewMethod != null) {
                return candidateSoftKeyViewMethod.invoke(holderView, slot);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static void setCandidateAutoCorrection(Object candidate, boolean autoCorrection) {
        if (candidate == null) {
            return;
        }
        try {
            if (candidateAutoCorrectionField == null) {
                try {
                    Field f = candidate.getClass().getDeclaredField("g");
                    f.setAccessible(true);
                    candidateAutoCorrectionField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : candidate.getClass().getDeclaredFields()) {
                        if (f.getType() == boolean.class && f.getName().equals("g")) {
                            f.setAccessible(true);
                            candidateAutoCorrectionField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateAutoCorrectionField != null) {
                candidateAutoCorrectionField.setBoolean(candidate, autoCorrection);
            }
        } catch (Throwable ignored) {
        }
    }

    static boolean isCandidateAutoCorrection(Object candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            if (candidateAutoCorrectionField == null) {
                try {
                    Field f = candidate.getClass().getDeclaredField("g");
                    f.setAccessible(true);
                    candidateAutoCorrectionField = f;
                } catch (NoSuchFieldException e) {
                    for (Field f : candidate.getClass().getDeclaredFields()) {
                        if (f.getType() == boolean.class && f.getName().equals("g")) {
                            f.setAccessible(true);
                            candidateAutoCorrectionField = f;
                            break;
                        }
                    }
                }
            }
            if (candidateAutoCorrectionField != null) {
                return candidateAutoCorrectionField.getBoolean(candidate);
            }
        } catch (Throwable ignored) {
        }
        return false;
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

    static void registerProtectedWordForTest(String word) {
        if (word != null) {
            USER_TAUGHT_WORDS.add(word.trim().toLowerCase());
        }
    }

    static void resetStateForTest() {
        DICTIONARY_WORDS.clear();
        USER_TAUGHT_WORDS.clear();
        ATTACHED_VIEWS.clear();
        lastDbModified = -1L;
        taughtWordsLoaded = false;
        currentSlot0Word = null;
    }
}
