package com.akshaykadam.pixelboard.extension.bengalivoice;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class GboardBengaliVoiceSettingsTest {
    @Test
    public void nonBengaliAndInvalidModesNeverWrite() {
        Store store = new Store();
        Assert.assertNull(GboardBengaliVoiceSettings.readMode(store.preferences, "en-US"));
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "en-US", "rambler"));
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-BD", "unknown"));
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn--BD", "standard"));
        Assert.assertEquals(0, store.commits);
        Assert.assertTrue(store.values.isEmpty());
    }

    @Test
    public void nullPreferencesAndContextHaveSafeDefaults() {
        Assert.assertEquals("standard", GboardBengaliVoiceSettings.readMode(
                (SharedPreferences) null, "bn-BD"));
        Assert.assertNull(GboardBengaliVoiceSettings.readMode((SharedPreferences) null, null));
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(
                (SharedPreferences) null, "bn-BD", "standard"));
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode((Context) null, "bn-BD", "rambler"));
    }

    @Test
    public void regionChoicesPersistWithoutChangingTheGlobalSelector() {
        Store store = new Store();
        store.values.put("enable_jetson", Boolean.FALSE);
        Assert.assertTrue(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-BD", "rambler"));
        Assert.assertTrue(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-IN", "standard"));
        Assert.assertEquals("rambler", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-Latn-BD"));
        Assert.assertEquals("standard", GboardBengaliVoiceSettings.readMode(store.preferences, "bn_IN"));
        Assert.assertEquals(Boolean.FALSE, store.values.get("enable_jetson"));
        Assert.assertEquals(3, store.values.size());
    }

    @Test
    public void failedCommitAndInvalidStoredValuesDoNotProduceFalseSuccess() {
        Store store = new Store();
        store.commitSucceeds = false;
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-BD", "rambler"));
        Assert.assertEquals("standard", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-BD"));
        Assert.assertFalse(store.values.containsKey(GboardBengaliVoiceSettings.preferenceKey("bn-BD")));
        store.values.put(GboardBengaliVoiceSettings.preferenceKey("bn-BD"), Integer.valueOf(2));
        Assert.assertEquals("standard", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-BD"));
    }

    @Test
    public void failedCommitRestoresPreviousRamblerChoiceAndOnlyItsOwnKey() {
        Store store = new Store();
        store.values.put(GboardBengaliVoiceSettings.preferenceKey("bn-BD"), "rambler");
        store.values.put(GboardBengaliVoiceSettings.preferenceKey("bn-IN"), "standard");
        store.values.put("enable_jetson", Boolean.TRUE);
        store.commitSucceeds = false;
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-BD", "standard"));
        Assert.assertEquals("rambler", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-BD"));
        Assert.assertEquals("standard", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-IN"));
        Assert.assertEquals(Boolean.TRUE, store.values.get("enable_jetson"));
        Assert.assertEquals(3, store.values.size());
    }

    @Test
    public void exceptionAfterMemoryUpdateBestEffortRestoresThePreviousChoice() {
        Store store = new Store();
        store.values.put(GboardBengaliVoiceSettings.preferenceKey("bn-BD"), "rambler");
        store.throwOnCommit = true;
        Assert.assertFalse(GboardBengaliVoiceSettings.writeMode(store.preferences, "bn-BD", "standard"));
        Assert.assertEquals("rambler", GboardBengaliVoiceSettings.readMode(store.preferences, "bn-BD"));
    }

    private static final class Store {
        final Map<String, Object> values = new HashMap<String, Object>();
        final SharedPreferences preferences;
        boolean commitSucceeds = true;
        boolean throwOnCommit;
        int commits;

        Store() {
            preferences = (SharedPreferences) Proxy.newProxyInstance(
                    SharedPreferences.class.getClassLoader(),
                    new Class<?>[]{SharedPreferences.class},
                    (proxy, method, args) -> {
                        if ("getAll".equals(method.getName())) return new HashMap<String, Object>(values);
                        if ("edit".equals(method.getName())) return editor();
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        SharedPreferences.Editor editor() {
            final Map<String, Object> pending = new HashMap<String, Object>();
            return (SharedPreferences.Editor) Proxy.newProxyInstance(
                    SharedPreferences.Editor.class.getClassLoader(),
                    new Class<?>[]{SharedPreferences.Editor.class},
                    (proxy, method, args) -> {
                        if ("putString".equals(method.getName())) {
                            pending.put((String) args[0], args[1]);
                            return proxy;
                        }
                        if ("remove".equals(method.getName())) {
                            pending.put((String) args[0], null);
                            return proxy;
                        }
                        if ("commit".equals(method.getName())) {
                            commits++;
                            // Android updates the live map before attempting the disk write.
                            for (Map.Entry<String, Object> entry : pending.entrySet()) {
                                if (entry.getValue() == null) values.remove(entry.getKey());
                                else values.put(entry.getKey(), entry.getValue());
                            }
                            if (throwOnCommit) throw new IllegalStateException("Simulated storage failure");
                            return commitSucceeds;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
