package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GboardAiWritingToolsSettingsScopeTest {
    @After
    public void tearDown() throws Exception {
        while (GboardAiWritingToolsRuntime.currentSettingsControllerScopeDepth() > 0) {
            GboardAiWritingToolsRuntime.exitSettingsControllerScope();
        }
        applicationContextField().set(null, null);
    }

    @Test
    public void nestedControllerScopeBypassesOnlyThreeTargetKeysWhileMasterIsOn()
            throws Exception {
        InMemorySharedPreferences preferences = new InMemorySharedPreferences();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        applicationContextField().set(null, new SharedPreferencesContext(preferences));

        GboardAiWritingToolsRuntime.enterSettingsControllerScope();
        GboardAiWritingToolsRuntime.enterSettingsControllerScope();

        Assert.assertEquals(2,
                GboardAiWritingToolsRuntime.currentSettingsControllerScopeDepth());
        Assert.assertTrue(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                GboardAiWritingToolsOfficialPreferences.WRITING_TOOLS_PREF_KEY));
        Assert.assertTrue(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                GboardAiWritingToolsOfficialPreferences.WRITING_TOOLS_CATEGORY_KEY));
        Assert.assertFalse(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(0x12345678));

        GboardAiWritingToolsRuntime.exitSettingsControllerScope();
        Assert.assertEquals(1,
                GboardAiWritingToolsRuntime.currentSettingsControllerScopeDepth());
        GboardAiWritingToolsRuntime.exitSettingsControllerScope();
        Assert.assertFalse(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY));
    }

    @Test
    public void disabledMasterNeverBypassesInsideControllerScope() throws Exception {
        InMemorySharedPreferences preferences = new InMemorySharedPreferences();
        GboardAiWritingToolsSettings.ensureDefaults(preferences);
        GboardAiWritingToolsSettings.writeEnabled(preferences, false);
        applicationContextField().set(null, new SharedPreferencesContext(preferences));
        GboardAiWritingToolsRuntime.enterSettingsControllerScope();

        Assert.assertFalse(GboardAiWritingToolsRuntime.shouldBypassSettingsRemoval(
                GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY));
    }

    private static Field applicationContextField() throws Exception {
        Field field = GboardAiWritingToolsRuntime.class.getDeclaredField("applicationContext");
        field.setAccessible(true);
        return field;
    }

    private static final class SharedPreferencesContext extends ContextWrapper {
        private final SharedPreferences preferences;

        private SharedPreferencesContext(SharedPreferences preferences) {
            super(null);
            this.preferences = preferences;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return preferences;
        }
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
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return defValue;
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
