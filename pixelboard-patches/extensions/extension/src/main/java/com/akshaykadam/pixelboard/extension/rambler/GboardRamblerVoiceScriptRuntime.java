/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ensures Indic transliteration keyboard layouts (e.g. Marathi Transliteration mr-Latn-IN)
 * produce native script (Devanagari, etc.) during voice dictation instead of Romanized text
 * (Issue #192).
 */
public final class GboardRamblerVoiceScriptRuntime {

    private static final String TAG = "PixelBoardVoiceScript";

    private static final Map<String, String> TRANSLITERATION_TO_NATIVE = createTransliterationMap();
    private static final Set<String> INDIC_LANG_CODES = new HashSet<String>(Arrays.asList(
            "bn", "gu", "hi", "kn", "kok", "ks", "mai", "ml", "mni", "mr",
            "ne", "or", "pa", "sa", "sat", "sd", "si", "ta", "te", "ur"));
    private static final Map<String, String> INDIC_SCRIPT_DESCRIPTIONS = createScriptDescriptionMap();

    private static volatile String activeIndicNativeTag;

    private GboardRamblerVoiceScriptRuntime() {
    }

    private static Map<String, String> createTransliterationMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("bn-latn", "bn-IN");
        map.put("bn-latn-in", "bn-IN");
        map.put("bn-latn-bd", "bn-BD");
        map.put("gu-latn", "gu-IN");
        map.put("gu-latn-in", "gu-IN");
        map.put("hi-latn", "hi-IN");
        map.put("hi-latn-in", "hi-IN");
        map.put("hi-xt", "hi-IN");
        map.put("kn-latn", "kn-IN");
        map.put("kn-latn-in", "kn-IN");
        map.put("ml-latn", "ml-IN");
        map.put("ml-latn-in", "ml-IN");
        map.put("mr-latn", "mr-IN");
        map.put("mr-latn-in", "mr-IN");
        map.put("mr-xa", "mr-IN");
        map.put("mr-xt", "mr-IN");
        map.put("ne-latn", "ne-IN");
        map.put("ne-latn-in", "ne-IN");
        map.put("ne-latn-np", "ne-NP");
        map.put("or-latn", "or-IN");
        map.put("or-latn-in", "or-IN");
        map.put("pa-latn", "pa-Guru");
        map.put("pa-latn-in", "pa-Guru");
        map.put("sa-latn", "sa-IN");
        map.put("sa-latn-in", "sa-IN");
        map.put("ta-latn", "ta-IN");
        map.put("ta-latn-in", "ta-IN");
        map.put("te-latn", "te-IN");
        map.put("te-latn-in", "te-IN");
        map.put("ur-latn", "ur-IN");
        map.put("ur-latn-in", "ur-IN");
        map.put("ur-latn-pk", "ur-PK");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> createScriptDescriptionMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mr", "Marathi in native Devanagari script (मराठी)");
        map.put("hi", "Hindi in native Devanagari script (हिन्दी)");
        map.put("ne", "Nepali in native Devanagari script (नेपाली)");
        map.put("sa", "Sanskrit in native Devanagari script");
        map.put("bn", "Bengali in native Bengali script");
        map.put("gu", "Gujarati in native Gujarati script");
        map.put("kn", "Kannada in native Kannada script");
        map.put("ml", "Malayalam in native Malayalam script");
        map.put("or", "Odia in native Odia script");
        map.put("pa", "Punjabi in native Gurmukhi script");
        map.put("ta", "Tamil in native Tamil script");
        map.put("te", "Telugu in native Telugu script");
        map.put("ur", "Urdu in native Perso-Arabic script");
        return Collections.unmodifiableMap(map);
    }

    /**
     * Hook called on primary Lrzf returned from VoiceImeUtils (e.g. Luya.aV).
     * Maps Indic -Latn transliteration tags such as mr-Latn-IN -> mr-IN so S3/Gemini
     * transcribes in the native script instead of triggering Romanization.
     */
    public static Object normalizePrimaryLanguageTag(Object languageTag) {
        if (languageTag == null) {
            activeIndicNativeTag = null;
            return null;
        }
        Context context = GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (!GboardRamblerSettings.isIndicNativeScriptEnabled(context)) {
            activeIndicNativeTag = null;
            return languageTag;
        }
        String rawTag = readTagString(languageTag);
        String mapped = mapIndicTransliterationTag(rawTag);
        if (mapped == null) {
            activeIndicNativeTag = isIndicLanguageTag(rawTag) ? rawTag : null;
            return languageTag;
        }
        activeIndicNativeTag = mapped;
        Object converted = createLanguageTag(languageTag.getClass(), mapped);
        return converted != null ? converted : languageTag;
    }

    /**
     * Hook called on secondary LanguageTag collection in VoiceImeUtils (e.g. Lkla.s) before
     * attaching to S3RequestMutator. When an Indic native-script language is primary, removes
     * English and Latin-script entries that would otherwise trigger Hinglish/Roman override rules.
     */
    public static Collection<?> filterSecondaryLanguageTags(
            Object primaryLanguageTag,
            Object secondaryLanguageTags) {
        if (!(secondaryLanguageTags instanceof Collection<?>)) {
            return Collections.emptyList();
        }
        Collection<?> original = (Collection<?>) secondaryLanguageTags;
        Context context = GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (!GboardRamblerSettings.isIndicNativeScriptEnabled(context)) {
            return original;
        }
        String primaryTag = readTagString(primaryLanguageTag);
        if (!isIndicLanguageTag(primaryTag)) {
            return original;
        }

        List<Object> filtered = new ArrayList<Object>(original.size());
        Set<String> seenTags = new LinkedHashSet<String>();
        if (primaryTag != null) {
            seenTags.add(primaryTag.toLowerCase(Locale.ROOT));
        }

        for (Object item : original) {
            if (item == null) {
                continue;
            }
            String tag = readTagString(item);
            if (tag == null || tag.isEmpty()) {
                continue;
            }
            String mapped = mapIndicTransliterationTag(tag);
            Object candidate = item;
            String candidateTag = tag;
            if (mapped != null) {
                Object converted = createLanguageTag(item.getClass(), mapped);
                if (converted != null) {
                    candidate = converted;
                    candidateTag = mapped;
                }
            }
            if (shouldExcludeSecondaryInIndicNativeMode(candidateTag)) {
                continue;
            }
            String key = candidateTag.toLowerCase(Locale.ROOT);
            if (seenTags.add(key)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /**
     * Hooked into JetsonLiteHandler prompt assembly (Lfgn.d) to strip the hardcoded
     * Hinglish/Romanization override rule when native-script Indic dictation is enabled.
     */
    public static String resolveHinglishOverrideRule(String stockRule) {
        Context context = GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (GboardRamblerSettings.isIndicNativeScriptEnabled(context)) {
            return "";
        }
        return stockRule != null ? stockRule : "";
    }

    /**
     * Returns a short S3 script reinforcement directive when an Indic primary language is active
     * and native script enforcement is enabled, or null when not applicable.
     */
    public static String buildActiveIndicScriptInstruction(Context context) {
        if (!GboardRamblerSettings.isIndicNativeScriptEnabled(context)) {
            return null;
        }
        return buildIndicScriptInstructionForTag(activeIndicNativeTag);
    }

    /**
     * Enriches session user_preferences inside VoiceImeUtils.getSessionParams using the resolved
     * primary LanguageTag so the Indic Devanagari/native script instruction is always sent on the
     * very first Rambler session without relying on custom-instruction UI.
     */
    public static List<String> buildIndicUserPreferences(
            Context context,
            Object primaryLanguageTag,
            List<?> existingPreferences) {
        if (!GboardRamblerSettings.isIndicNativeScriptEnabled(context)) {
            return null;
        }
        String rawTag = readTagString(primaryLanguageTag);
        String mapped = mapIndicTransliterationTag(rawTag);
        String effectiveTag = mapped != null ? mapped : rawTag;
        String instruction = buildIndicScriptInstructionForTag(effectiveTag);
        if (instruction == null) {
            return null;
        }
        List<String> updated = new ArrayList<String>(
                (existingPreferences != null ? existingPreferences.size() : 0) + 1);
        if (existingPreferences != null) {
            for (Object item : existingPreferences) {
                if (item instanceof String) {
                    String s = (String) item;
                    if (instruction.equals(s)) {
                        return null;
                    }
                    updated.add(s);
                }
            }
        }
        updated.add(instruction);
        return updated;
    }

    private static String buildIndicScriptInstructionForTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        String baseLang = baseLanguageCode(tag);
        String desc = INDIC_SCRIPT_DESCRIPTIONS.get(baseLang);
        if (desc == null) {
            return null;
        }
        return "Write all " + baseLang.toUpperCase(Locale.ROOT)
                + " speech using " + desc
                + ". Never output Romanized or Latin transliteration for spoken "
                + baseLang.toUpperCase(Locale.ROOT) + " words.";
    }

    static String mapIndicTransliterationTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        String normalized = tag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        String direct = TRANSLITERATION_TO_NATIVE.get(normalized);
        if (direct != null) {
            return direct;
        }
        if (normalized.contains("-latn")) {
            String base = baseLanguageCode(normalized);
            if (INDIC_LANG_CODES.contains(base)) {
                String fallback = TRANSLITERATION_TO_NATIVE.get(base + "-latn");
                if (fallback != null) {
                    return fallback;
                }
            }
        }
        return null;
    }

    static boolean isIndicLanguageTag(String tag) {
        String base = baseLanguageCode(tag);
        return INDIC_LANG_CODES.contains(base);
    }

    private static boolean shouldExcludeSecondaryInIndicNativeMode(String tag) {
        if (tag == null || tag.isEmpty()) {
            return true;
        }
        String lower = tag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (lower.equals("en") || lower.startsWith("en-")) {
            return true;
        }
        return lower.contains("-latn");
    }

    private static String baseLanguageCode(String tag) {
        if (tag == null) {
            return "";
        }
        String lower = tag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = lower.indexOf('-');
        return dash >= 0 ? lower.substring(0, dash) : lower;
    }

    static String readTagString(Object languageTag) {
        if (languageTag == null) {
            return null;
        }
        try {
            Field nField = findField(languageTag.getClass(), "n");
            if (nField != null) {
                Object val = nField.get(languageTag);
                if (val instanceof String) {
                    return (String) val;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed reading LanguageTag.n", t);
        }
        String fallback = languageTag.toString();
        return (fallback == null || fallback.contains("@")) ? null : fallback;
    }

    static Object createLanguageTag(Class<?> tagClass, String tagString) {
        if (tagClass == null || tagString == null || tagString.isEmpty()) {
            return null;
        }
        String[] preferredNames = new String[]{"f", "g", "fromString"};
        for (String name : preferredNames) {
            try {
                Method m = tagClass.getDeclaredMethod(name, String.class);
                if (Modifier.isStatic(m.getModifiers())
                        && tagClass.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object created = m.invoke(null, tagString);
                    if (created != null && tagString.equalsIgnoreCase(readTagString(created))) {
                        return created;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        for (Method m : tagClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1
                    && params[0] == String.class
                    && tagClass.isAssignableFrom(m.getReturnType())) {
                try {
                    m.setAccessible(true);
                    Object created = m.invoke(null, tagString);
                    if (created != null && tagString.equalsIgnoreCase(readTagString(created))) {
                        return created;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    static void resetForTests() {
        activeIndicNativeTag = null;
    }
}
