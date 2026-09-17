package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GboardAdvancedVoice1803RuntimeSettingsTest {
    private static final String ENABLED_KEY = "pref_advanced_voice_typing_enabled";
    private static final String BACKEND_KEY = "pref_advanced_voice_backend";
    private static final String ZH_TW_PUNCTUATION_KEY =
            "pref_advanced_voice_zh_tw_punctuation_enabled";

    @Test
    public void nullAndEmptyPreferencesDefaultEnabled() {
        GboardAdvancedVoice1803RuntimeSettings.Snapshot nullSnapshot =
                GboardAdvancedVoice1803RuntimeSettings.snapshotFromPreferences(null);
        Assert.assertTrue(nullSnapshot.enabled);
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED, nullSnapshot.effectiveMode);

        GboardAdvancedVoice1803RuntimeSettings.Snapshot emptySnapshot =
                GboardAdvancedVoice1803RuntimeSettings.snapshotFromPreferences(
                        new TestSharedPreferences());
        Assert.assertTrue(emptySnapshot.enabled);
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED, emptySnapshot.effectiveMode);
    }

    @Test
    public void booleanAndLegacyStringTrueEnableTheRuntimeGate() {
        TestSharedPreferences booleanPreferences = new TestSharedPreferences();
        booleanPreferences.values.put(ENABLED_KEY, Boolean.TRUE);
        Assert.assertTrue(GboardAdvancedVoice1803RuntimeSettings.snapshotFromPreferences(
                booleanPreferences).enabled);

        TestSharedPreferences stringPreferences = new TestSharedPreferences();
        stringPreferences.values.put(ENABLED_KEY, "true");
        Assert.assertTrue(GboardAdvancedVoice1803RuntimeSettings.snapshotFromPreferences(
                stringPreferences).enabled);
    }

    @Test
    public void legacyBackendPreferenceNoLongerOverridesTheOfficialSelector() {
        Assert.assertEquals(GboardVoiceInputMode.STANDARD,
                snapshot(false, GboardAdvancedVoiceSettings.BACKEND_RAMBLER)
                        .effectiveMode);
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED,
                snapshot(true, GboardAdvancedVoiceSettings.BACKEND_ADVANCED)
                        .effectiveMode);
        GboardAdvancedVoice1803RuntimeSettings.Snapshot rambler = snapshot(
                true,
                GboardAdvancedVoiceSettings.BACKEND_RAMBLER);
        Assert.assertEquals(GboardVoiceInputMode.ADVANCED, rambler.effectiveMode);
    }

    @Test
    public void backendStoreSeedsAdvancedAndSanitizesUnknownValues() {
        TestSharedPreferences preferences = new TestSharedPreferences();
        GboardAdvancedVoiceSettings.ensureDefaults(preferences);
        Assert.assertEquals(GboardAdvancedVoiceSettings.BACKEND_ADVANCED,
                GboardAdvancedVoiceSettings.readBackend(preferences));

        Assert.assertTrue(GboardAdvancedVoiceSettings.writeBackend(
                preferences,
                GboardAdvancedVoiceSettings.BACKEND_RAMBLER));
        Assert.assertEquals(GboardAdvancedVoiceSettings.BACKEND_RAMBLER,
                GboardAdvancedVoiceSettings.readBackend(preferences));

        Assert.assertTrue(GboardAdvancedVoiceSettings.writeBackend(preferences, "invalid"));
        Assert.assertEquals(GboardAdvancedVoiceSettings.BACKEND_ADVANCED,
                GboardAdvancedVoiceSettings.readBackend(preferences));
    }

    @Test
    public void publicAccessorHonorsTestOverride() {
        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        try {
            GboardAdvancedVoice1803RuntimeSettings.setEnabledOverrideForTest(false);
            Assert.assertFalse(GboardAdvancedVoice1803RuntimeSettings.isEnabled());

            GboardAdvancedVoice1803RuntimeSettings.setEnabledOverrideForTest(true);
            Assert.assertTrue(GboardAdvancedVoice1803RuntimeSettings.isEnabled());

            GboardAdvancedVoice1803RuntimeSettings.setBackendOverrideForTest(
                    GboardAdvancedVoiceSettings.BACKEND_RAMBLER);
            Assert.assertTrue(GboardAdvancedVoice1803RuntimeSettings.isEnabled());
            Assert.assertFalse(GboardAdvancedVoice1803RuntimeSettings.isRamblerEnabled());
        } finally {
            GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        }
    }

    @Test
    public void unavailableApplicationContextDoesNotPoisonTheProcessSnapshot()
            throws Exception {
        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        try {
            GboardAdvancedVoice1803RuntimeSettings.Snapshot transientSnapshot =
                    GboardAdvancedVoice1803RuntimeSettings.snapshot();
            Assert.assertTrue(transientSnapshot.enabled);

            Field cachedSnapshot = GboardAdvancedVoice1803RuntimeSettings.class
                    .getDeclaredField("cachedSnapshot");
            cachedSnapshot.setAccessible(true);
            Assert.assertNull(cachedSnapshot.get(null));
        } finally {
            GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        }
    }

    private static GboardAdvancedVoice1803RuntimeSettings.Snapshot snapshot(
            boolean enabled,
            String backend) {
        TestSharedPreferences preferences = new TestSharedPreferences();
        preferences.values.put(ENABLED_KEY, Boolean.valueOf(enabled));
        preferences.values.put(BACKEND_KEY, backend);
        return GboardAdvancedVoice1803RuntimeSettings.snapshotFromPreferences(preferences);
    }

    private static final class TestSharedPreferences implements SharedPreferences {
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
            return value instanceof Boolean ? ((Boolean) value).booleanValue() : defValue;
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
                public Editor putStringSet(String key, Set<String> value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    pending.put(key, Integer.valueOf(value));
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    pending.put(key, Long.valueOf(value));
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    pending.put(key, Float.valueOf(value));
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    pending.put(key, Boolean.valueOf(value));
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
