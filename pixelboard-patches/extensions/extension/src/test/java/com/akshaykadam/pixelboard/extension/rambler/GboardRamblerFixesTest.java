/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GboardRamblerFixesTest {

    public static final class FakeLanguageTag {
        public final String n;

        private FakeLanguageTag(String tag) {
            this.n = tag;
        }

        public static FakeLanguageTag f(String tag) {
            return new FakeLanguageTag(tag);
        }

        @Override
        public String toString() {
            return n;
        }
    }

    @After
    public void tearDown() {
        GboardRamblerVoiceScriptRuntime.resetForTests();
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
        GboardRamblerSettings.resetForTests();
    }

    @Test
    public void normalizesIndicTransliterationTagsToNativeScript() {
        Assert.assertEquals("mr-IN",
                GboardRamblerVoiceScriptRuntime.mapIndicTransliterationTag("mr-Latn-IN"));
        Assert.assertEquals("mr-IN",
                GboardRamblerVoiceScriptRuntime.mapIndicTransliterationTag("mr-Latn"));
        Assert.assertEquals("hi-IN",
                GboardRamblerVoiceScriptRuntime.mapIndicTransliterationTag("hi-Latn-IN"));
        Assert.assertEquals("bn-BD",
                GboardRamblerVoiceScriptRuntime.mapIndicTransliterationTag("bn-Latn-BD"));
        Assert.assertNull(GboardRamblerVoiceScriptRuntime.mapIndicTransliterationTag("en-US"));

        Object converted = GboardRamblerVoiceScriptRuntime.normalizePrimaryLanguageTag(
                FakeLanguageTag.f("mr-Latn-IN"));
        Assert.assertEquals("mr-IN", GboardRamblerVoiceScriptRuntime.readTagString(converted));
    }

    @Test
    public void filtersEnglishAndLatinSecondaryLocalesWhenPrimaryIsIndic() {
        FakeLanguageTag primary = FakeLanguageTag.f("mr-IN");
        Collection<?> filtered = GboardRamblerVoiceScriptRuntime.filterSecondaryLanguageTags(
                primary,
                Arrays.asList(
                        FakeLanguageTag.f("en-US"),
                        FakeLanguageTag.f("en-IN"),
                        FakeLanguageTag.f("mr-Latn-IN"),
                        FakeLanguageTag.f("hi-Latn-IN")));
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("hi-IN",
                GboardRamblerVoiceScriptRuntime.readTagString(filtered.iterator().next()));
    }

    @Test
    public void keepsSecondaryLocalesIntactWhenPrimaryIsEnglish() {
        FakeLanguageTag primary = FakeLanguageTag.f("en-US");
        Collection<?> filtered = GboardRamblerVoiceScriptRuntime.filterSecondaryLanguageTags(
                primary,
                Arrays.asList(
                        FakeLanguageTag.f("es-ES"),
                        FakeLanguageTag.f("mr-Latn-IN")));
        Assert.assertEquals(2, filtered.size());
    }

    @Test
    public void stripsHinglishOverrideRuleInNativeScriptMode() {
        String stripped = GboardRamblerVoiceScriptRuntime.resolveHinglishOverrideRule(
                "\n3. Hinglish Override: ...\n");
        Assert.assertEquals("", stripped);
    }

    @Test
    public void buildsIndicUserPreferencesDirectlyFromPrimaryTag() {
        java.util.List<String> mrPrefs = GboardRamblerVoiceScriptRuntime.buildIndicUserPreferences(
                null,
                FakeLanguageTag.f("mr-IN"),
                Collections.singletonList("existing rule"));
        Assert.assertNotNull(mrPrefs);
        Assert.assertEquals(2, mrPrefs.size());
        Assert.assertEquals("existing rule", mrPrefs.get(0));
        Assert.assertTrue(mrPrefs.get(1).contains("Devanagari"));

        java.util.List<String> mrXtPrefs = GboardRamblerVoiceScriptRuntime.buildIndicUserPreferences(
                null,
                FakeLanguageTag.f("mr-XT"),
                Collections.emptyList());
        Assert.assertNotNull(mrXtPrefs);
        Assert.assertEquals(1, mrXtPrefs.size());
        Assert.assertTrue(mrXtPrefs.get(0).contains("MR"));

        java.util.List<String> hiPrefs = GboardRamblerVoiceScriptRuntime.buildIndicUserPreferences(
                null,
                FakeLanguageTag.f("hi-IN"),
                Collections.emptyList());
        Assert.assertNotNull(hiPrefs);
        Assert.assertTrue(hiPrefs.get(0).contains("HI"));

        Assert.assertNull(GboardRamblerVoiceScriptRuntime.buildIndicUserPreferences(
                null,
                FakeLanguageTag.f("en-US"),
                Collections.emptyList()));
    }

    @Test
    public void settingsDefaultsAndPersistenceWorkAsExpected() {
        SharedPreferences prefs = new InMemorySharedPreferences();
        Assert.assertTrue(GboardRamblerSettings.isIndicNativeScriptEnabled(prefs));
        Assert.assertFalse(GboardRamblerSettings.isVerbatimModeEnabled(prefs));
        Assert.assertEquals("", GboardRamblerSettings.readCustomInstruction(prefs));

        Assert.assertTrue(GboardRamblerSettings.writeIndicNativeScriptEnabled(prefs, false));
        Assert.assertTrue(GboardRamblerSettings.writeVerbatimModeEnabled(prefs, true));
        Assert.assertTrue(GboardRamblerSettings.writeCustomInstruction(prefs, " Keep exact spelling "));

        Assert.assertFalse(GboardRamblerSettings.isIndicNativeScriptEnabled(prefs));
        Assert.assertTrue(GboardRamblerSettings.isVerbatimModeEnabled(prefs));
        Assert.assertEquals("Keep exact spelling", GboardRamblerSettings.readCustomInstruction(prefs));
    }

    @Test
    public void verbatimModeRestoresTranscriptForDictationRambleAndRewrite() {
        GboardRamblerDictationRuntimeTest.FakeAction action =
                new GboardRamblerDictationRuntimeTest.FakeAction();
        action.c = 5; // INTENT_RAMBLE
        action.d = "Well, um, I basically wanted to say we need it, we need it twice.";

        GboardRamblerDictationRuntimeTest.FakeSpan span =
                new GboardRamblerDictationRuntimeTest.FakeSpan();
        span.c = "We need it twice."; // summarized by server
        span.j = action;

        GboardRamblerDictationRuntimeTest.FakeProto proto =
                new GboardRamblerDictationRuntimeTest.FakeProto();
        proto.b = Collections.singletonList(span);

        // Default mode (verbatim off) keeps cleaned ramble span when not compose/errant diff.
        GboardRamblerDictationRuntime.filterVoiceText(null, proto);
        Assert.assertEquals("We need it twice.", span.c);

        // With Verbatim Mode enabled, full_transcript is restored cleanly for dictation/ramble/rewrite.
        SharedPreferences prefs = new InMemorySharedPreferences();
        GboardRamblerSettings.writeVerbatimModeEnabled(prefs, true);
        GboardRamblerSettings.setPreferencesForTests(prefs);

        GboardRamblerDictationRuntime.filterVoiceText(null, proto);
        Assert.assertEquals(
                "Well, um, I basically wanted to say we need it, we need it twice.",
                span.c);
        Assert.assertEquals(0, action.c);

        // JetsonLite skip cleanup threshold jumps to Long.MAX_VALUE when verbatim mode is ON.
        Assert.assertEquals(
                Long.valueOf(Long.MAX_VALUE),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.JETSON_LITE_SKIP_CLEANUP_BYTE_THRESHOLD_FLAG,
                        Long.valueOf(-1L)));

        // When Verbatim Mode is turned off again, threshold remains untouched.
        GboardRamblerSettings.writeVerbatimModeEnabled(prefs, false);
        Assert.assertEquals(
                Long.valueOf(-1L),
                GboardRambler1803StockPolicy.maybeForceStockRouteValue(
                        GboardRambler1803StockPolicy.JETSON_LITE_SKIP_CLEANUP_BYTE_THRESHOLD_FLAG,
                        Long.valueOf(-1L)));
    }

    @Test
    public void autoEnablesIndicNativeScriptEvenWhenPrefWasFalse() {
        SharedPreferences prefs = new InMemorySharedPreferences();
        GboardRamblerSettings.writeIndicNativeScriptEnabled(prefs, false);
        GboardRamblerSettings.setPreferencesForTests(prefs);

        Object normalized = GboardRamblerVoiceScriptRuntime.normalizePrimaryLanguageTag(
                FakeLanguageTag.f("mr-Latn-IN"));
        Assert.assertEquals("mr-IN", GboardRamblerVoiceScriptRuntime.readTagString(normalized));
        Assert.assertTrue(GboardRamblerSettings.isIndicNativeScriptEnabled(prefs));
        Assert.assertEquals(
                "",
                GboardRamblerVoiceScriptRuntime.resolveHinglishOverrideRule("keep-rule"));
    }

    private static final class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<String, Object>();

        @Override
        public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof Set ? (Set<String>) value : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).intValue() : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).longValue() : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).floatValue() : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<String, Object>();

                @Override
                public Editor putString(String key, String value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, Set<String> values) {
                    pending.put(key, values);
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    pending.put(key, null);
                    return this;
                }

                @Override
                public Editor clear() {
                    values.clear();
                    pending.clear();
                    return this;
                }

                @Override
                public boolean commit() {
                    apply();
                    return true;
                }

                @Override
                public void apply() {
                    for (Map.Entry<String, Object> entry : pending.entrySet()) {
                        if (entry.getValue() == null) {
                            values.remove(entry.getKey());
                        } else {
                            values.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            };
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }
    }
}
