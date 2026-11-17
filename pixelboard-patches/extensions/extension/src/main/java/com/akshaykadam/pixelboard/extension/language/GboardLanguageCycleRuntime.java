/*
 * PixelBoard - Gboard Enhancement Mod
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.language;

import android.util.Log;

import com.akshaykadam.pixelboard.extension.settings.GboardLanguageCycleSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * Globe-key language cycling without duplicate multilingual entries.
 *
 * <p>With multilingual typing, Gboard keeps one input method entry per primary language, so a
 * pair such as English + German appears twice in the globe-key rotation ("English + German"
 * and "German + English") although both type identically. {@link #nextEntry} replaces the
 * rotation list's next-entry lookup inside switchToNextInputMethodEntry and skips an entry
 * whose language set and layout are already covered by an earlier entry in the Languages list.
 * This works for any languages and for entries with more than one secondary language.
 *
 * <p>Only the globe-key path is affected: the long-press language picker, the stored entries
 * and multilingual typing itself are unchanged. Any failure falls back to Gboard's own result.
 */
public final class GboardLanguageCycleRuntime {
    private static final String TAG = "PixelBoardLangCycle";

    // Gboard 18.6.3 input method entry (oyq): i() primary language tag, k() the entries active
    // as multilingual secondaries, q() layout/variant. Kept in step with the patch's target.
    // k() is used instead of j() (multilingual languages) because j() holds the secondary
    // ImeDef's tag, which can be language-only ("de") while the same language as a primary
    // entry is "de-DE".
    private static final String ENTRY_PRIMARY_LANGUAGE = "i";
    private static final String ENTRY_MULTILINGUAL_ENTRIES = "k";
    private static final String ENTRY_LAYOUT = "q";
    // DynamicRotationList (qyf).g(entry, wrap): the stock next-entry lookup.
    private static final String ROTATION_NEXT_ENTRY = "g";

    private static final Map<Class<?>, Method> NEXT_ENTRY_METHODS = new WeakHashMap<>();

    private GboardLanguageCycleRuntime() {
    }

    /**
     * Drop-in for {@code rotationList.g(entry, wrap)}: returns the next entry in the rotation,
     * skipping entries that duplicate an earlier entry's language set and layout.
     */
    public static Object nextEntry(Object rotationList, Object entry, boolean wrap) {
        if (!GboardLanguageCycleSettings.isSkipDuplicatePairsEnabled()) {
            return stockNextEntry(rotationList, entry, wrap);
        }
        Method next = nextEntryMethod(rotationList, entry);
        if (next == null) {
            // Cannot happen on a matching Gboard build; never break the globe key.
            Log.w(TAG, "next-entry lookup not found; globe key left unchanged");
            return null;
        }
        Object candidate = invoke(next, rotationList, entry, wrap);
        try {
            List<?> entries = entriesInListOrder(rotationList);
            if (candidate == null || entries == null) {
                return candidate;
            }
            Object current = candidate;
            // Bounded walk: at most one lap of the rotation.
            for (int step = 0; step <= entries.size() && current != null; step++) {
                if (!isShadowedDuplicate(current, entries) || current.equals(entry)) {
                    return current;
                }
                current = invoke(next, rotationList, current, wrap);
            }
            return current == null ? null : candidate;
        } catch (Throwable t) {
            Log.w(TAG, "dedupe failed; using stock next entry", t);
            return candidate;
        }
    }

    /** Gboard's own next-entry lookup, used when the setting is off. */
    private static Object stockNextEntry(Object rotationList, Object entry, boolean wrap) {
        Method next = nextEntryMethod(rotationList, entry);
        return next == null ? null : invoke(next, rotationList, entry, wrap);
    }

    /** True when an earlier entry in the Languages list has the same language set and layout. */
    private static boolean isShadowedDuplicate(Object candidate, List<?> entries) throws Exception {
        String key = dedupeKey(candidate);
        if (key == null) {
            return false;
        }
        for (Object other : entries) {
            if (other == candidate) {
                return false;
            }
            if (other != null && key.equals(dedupeKey(other))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Order-independent key: sorted {primary} ∪ primaries of the multilingual secondary
     * entries, plus the layout. English + German and German + English on QWERTY both become
     * "[de-DE, en-US]|qwerty". Returns null for entries without multilingual secondaries,
     * which are never skipped.
     */
    private static String dedupeKey(Object entry) throws Exception {
        Object secondaries = call(entry, ENTRY_MULTILINGUAL_ENTRIES);
        if (!(secondaries instanceof Collection) || ((Collection<?>) secondaries).isEmpty()) {
            return null;
        }
        TreeSet<String> languages = new TreeSet<>();
        for (Object secondary : (Collection<?>) secondaries) {
            if (secondary == null) {
                continue;
            }
            Object tag = call(secondary, ENTRY_PRIMARY_LANGUAGE);
            languages.add(String.valueOf(tag != null ? tag : secondary));
        }
        Object primary = call(entry, ENTRY_PRIMARY_LANGUAGE);
        if (primary != null) {
            languages.add(primary.toString());
        }
        if (languages.size() < 2) {
            return null;
        }
        return languages + "|" + Objects.toString(call(entry, ENTRY_LAYOUT), "");
    }

    /**
     * The rotation list stores its entries (in Languages-list order) as a List field and the
     * rotation order as an int[] field; find the List by type rather than by obfuscated name.
     */
    private static List<?> entriesInListOrder(Object rotationList) throws IllegalAccessException {
        for (Field field : rotationList.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(rotationList);
            if (value instanceof List) {
                return (List<?>) value;
            }
        }
        return null;
    }

    /** The rotation list's {@code (Entry, boolean) -> Entry} method. */
    private static synchronized Method nextEntryMethod(Object rotationList, Object entry) {
        if (rotationList == null || entry == null) {
            return null;
        }
        Class<?> owner = rotationList.getClass();
        Method cached = NEXT_ENTRY_METHODS.get(owner);
        if (cached != null) {
            return cached;
        }
        // The rotation list class is merged with unrelated code by R8, so prefer the known
        // name and only accept an (Entry, boolean) -> Entry shape with a real entry type.
        Method match = null;
        for (Method method : owner.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            boolean shape = params.length == 2
                    && params[1] == boolean.class
                    && params[0] != Object.class
                    && params[0].isInstance(entry)
                    && method.getReturnType() == params[0]
                    && !Modifier.isStatic(method.getModifiers());
            if (!shape) {
                continue;
            }
            if (ROTATION_NEXT_ENTRY.equals(method.getName())) {
                match = method;
                break;
            }
            if (match == null) {
                match = method;
            }
        }
        if (match != null) {
            match.setAccessible(true);
            NEXT_ENTRY_METHODS.put(owner, match);
        }
        return match;
    }

    private static Object invoke(Method method, Object target, Object entry, boolean wrap) {
        try {
            return method.invoke(target, entry, wrap);
        } catch (Throwable t) {
            Log.w(TAG, "next-entry lookup failed", t);
            return null;
        }
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = findNoArgMethod(target.getClass(), name);
        return method == null ? null : method.invoke(target);
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // keep looking in superclasses
            }
        }
        return null;
    }
}
