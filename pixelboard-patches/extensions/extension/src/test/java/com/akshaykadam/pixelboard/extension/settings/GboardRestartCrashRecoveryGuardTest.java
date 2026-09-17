package com.akshaykadam.pixelboard.extension.settings;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GboardRestartCrashRecoveryGuardTest {
    @Test
    public void prepareTemporarilyDisablesEnabledStockCleanupAndStoresMarker() {
        TestSharedPreferences stock = new TestSharedPreferences();
        TestSharedPreferences patches = new TestSharedPreferences();
        stock.values.put(
                GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY,
                Boolean.TRUE);

        Assert.assertTrue(GboardRestartCrashRecoveryGuard.prepare(stock, patches));

        Assert.assertEquals(
                Boolean.FALSE,
                stock.values.get(GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY));
        Assert.assertEquals(
                Boolean.TRUE,
                patches.values.get(GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY));
        Assert.assertEquals(1, stock.commitCalls);
        Assert.assertEquals(1, patches.commitCalls);
    }

    @Test
    public void prepareLeavesDisabledStockCleanupUntouched() {
        TestSharedPreferences stock = new TestSharedPreferences();
        TestSharedPreferences patches = new TestSharedPreferences();
        stock.values.put(
                GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY,
                Boolean.FALSE);

        Assert.assertTrue(GboardRestartCrashRecoveryGuard.prepare(stock, patches));

        Assert.assertEquals(
                Boolean.FALSE,
                stock.values.get(GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY));
        Assert.assertFalse(patches.values.containsKey(
                GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY));
        Assert.assertEquals(0, stock.commitCalls);
        Assert.assertEquals(0, patches.commitCalls);
    }

    @Test
    public void prepareCleansMarkerAndFailsWhenStockWriteFails() {
        TestSharedPreferences stock = new TestSharedPreferences();
        TestSharedPreferences patches = new TestSharedPreferences();
        stock.values.put(
                GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY,
                Boolean.TRUE);
        stock.failNextCommit = true;

        Assert.assertFalse(GboardRestartCrashRecoveryGuard.prepare(stock, patches));

        Assert.assertEquals(
                Boolean.TRUE,
                stock.values.get(GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY));
        Assert.assertFalse(patches.values.containsKey(
                GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY));
        Assert.assertEquals(2, stock.commitCalls);
        Assert.assertEquals(2, patches.commitCalls);
    }

    @Test
    public void restoreEnablesStockCleanupBeforeRemovingPendingMarker() {
        TestSharedPreferences stock = new TestSharedPreferences();
        TestSharedPreferences patches = new TestSharedPreferences();
        stock.values.put(
                GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY,
                Boolean.FALSE);
        patches.values.put(
                GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY,
                Boolean.TRUE);

        Assert.assertTrue(GboardRestartCrashRecoveryGuard.restoreIfPending(stock, patches));

        Assert.assertEquals(
                Boolean.TRUE,
                stock.values.get(GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY));
        Assert.assertFalse(patches.values.containsKey(
                GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY));
        Assert.assertEquals(1, stock.commitCalls);
        Assert.assertEquals(1, patches.commitCalls);
    }

    @Test
    public void restoreRetainsMarkerWhenStockWriteFails() {
        TestSharedPreferences stock = new TestSharedPreferences();
        TestSharedPreferences patches = new TestSharedPreferences();
        stock.values.put(
                GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY,
                Boolean.FALSE);
        patches.values.put(
                GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY,
                Boolean.TRUE);
        stock.failNextCommit = true;

        Assert.assertFalse(GboardRestartCrashRecoveryGuard.restoreIfPending(stock, patches));

        Assert.assertEquals(
                Boolean.FALSE,
                stock.values.get(GboardRestartCrashRecoveryGuard.STOCK_CLEAN_FLAG_KEY));
        Assert.assertEquals(
                Boolean.TRUE,
                patches.values.get(GboardRestartCrashRecoveryGuard.RESTORE_MARKER_KEY));
        Assert.assertEquals(1, stock.commitCalls);
        Assert.assertEquals(0, patches.commitCalls);
    }

    private static final class TestSharedPreferences implements SharedPreferences {
        final Map<String, Object> values = new HashMap<>();
        int commitCalls;
        boolean failNextCommit;

        @Override
        public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(new HashMap<>(values));
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value == null ? defValue : (String) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            return value == null ? defValues : (Set<String>) value;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value == null ? defValue : ((Number) value).intValue();
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value == null ? defValue : ((Number) value).longValue();
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value == null ? defValue : ((Number) value).floatValue();
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value == null ? defValue : ((Boolean) value).booleanValue();
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new TestEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }

        private final class TestEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clear;

            @Override
            public Editor putString(String key, String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> value) {
                pending.put(key, value == null ? null : new HashSet<>(value));
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
                removals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clear = true;
                return this;
            }

            @Override
            public boolean commit() {
                commitCalls++;
                if (failNextCommit) {
                    failNextCommit = false;
                    return false;
                }
                applyChanges();
                return true;
            }

            @Override
            public void apply() {
                applyChanges();
            }

            private void applyChanges() {
                if (clear) {
                    values.clear();
                }
                for (String key : removals) {
                    values.remove(key);
                }
                values.putAll(pending);
            }
        }
    }
}
