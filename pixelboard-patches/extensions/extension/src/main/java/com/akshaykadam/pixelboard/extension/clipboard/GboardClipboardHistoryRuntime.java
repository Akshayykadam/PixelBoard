/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime helper for Gboard Clipboard History Duration Slider.
 *
 * <p>Configures Gboard's native {@code LabelSummarySliderPreference} in
 * {@code setting_clipboard.xml} with discrete retention choices:</p>
 * <ol>
 *   <li>1 hour ("1")</li>
 *   <li>24 hours (Current maximum) ("24")</li>
 *   <li>7 days ("168")</li>
 *   <li>30 days ("720")</li>
 *   <li>Never (Unlimited / Manual deletion only) ("876000", 100 years)</li>
 * </ol>
 */
public final class GboardClipboardHistoryRuntime {

    private static final String TAG = "PixelBoardClipboard";

    public static final CharSequence[] RETENTION_LABELS = new CharSequence[] {
            "1 hour",
            "24 hours (Current maximum)",
            "7 days",
            "30 days",
            "Never (Unlimited / Manual deletion only)"
    };

    public static final CharSequence[] RETENTION_VALUES = new CharSequence[] {
            GboardClipboardHistorySettings.DURATION_1_HOUR,
            GboardClipboardHistorySettings.DURATION_24_HOURS,
            GboardClipboardHistorySettings.DURATION_7_DAYS,
            GboardClipboardHistorySettings.DURATION_30_DAYS,
            GboardClipboardHistorySettings.DURATION_NEVER
    };

    private GboardClipboardHistoryRuntime() {
    }

    /**
     * Configures the native Gboard LabelSummarySliderPreference with our discrete duration steps.
     *
     * @param preference the LabelSummarySliderPreference instance
     * @param context the settings Activity/fragment context
     */
    public static void configureClipboardHistorySlider(Object preference, Context context) {
        if (preference == null) {
            return;
        }
        try {
            Field kField = findField(preference.getClass(), "K");
            if (kField != null) {
                kField.setAccessible(true);
                kField.set(preference, RETENTION_LABELS.clone());
            }

            Field lField = findField(preference.getClass(), "L");
            if (lField != null) {
                lField.setAccessible(true);
                lField.set(preference, RETENTION_VALUES.clone());
            }

            int maxIndex = RETENTION_VALUES.length - 1; // 4

            // Determine target index from currently persisted string or fallback to index 1 ("24" / 24 hours)
            int targetIndex = 1;
            try {
                Method wMethod = findMethod(preference.getClass(), "w", String.class);
                if (wMethod != null) {
                    wMethod.setAccessible(true);
                    String savedValue = (String) wMethod.invoke(preference, "");
                    if (savedValue != null && !savedValue.isEmpty()) {
                        for (int i = 0; i < RETENTION_VALUES.length; i++) {
                            if (RETENTION_VALUES[i].equals(savedValue)) {
                                targetIndex = i;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // CRITICAL FIX: Update preference value field 'b' to a valid index <= maxIndex
            // BEFORE setting max, preventing Material Slider's value > valueTo IllegalStateException
            Field bField = findField(preference.getClass(), "b");
            if (bField != null) {
                bField.setAccessible(true);
                int currentB = bField.getInt(preference);
                if (currentB > maxIndex || currentB < 0) {
                    bField.setInt(preference, targetIndex);
                }
            }

            // If the Material Slider view is already attached, safely clamp its value before valueTo
            Field aField = findField(preference.getClass(), "a");
            if (aField != null) {
                aField.setAccessible(true);
                Object slider = aField.get(preference);
                if (slider != null) {
                    try {
                        Method setValMethod = findMethod(slider.getClass(), "H", float.class);
                        if (setValMethod == null) {
                            setValMethod = findMethod(slider.getClass(), "setValue", float.class);
                        }
                        if (setValMethod != null) {
                            setValMethod.setAccessible(true);
                            setValMethod.invoke(slider, (float) targetIndex);
                        }

                        Method setMaxValMethod = findMethod(slider.getClass(), "w", float.class);
                        if (setMaxValMethod == null) {
                            setMaxValMethod = findMethod(slider.getClass(), "setValueTo", float.class);
                        }
                        if (setMaxValMethod != null) {
                            setMaxValMethod.setAccessible(true);
                            setMaxValMethod.invoke(slider, (float) maxIndex);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "Failed to update slider view values", t);
                    }
                }
            }

            // Set max on preference
            Field dField = findField(preference.getClass(), "d");
            if (dField != null) {
                dField.setAccessible(true);
                dField.setInt(preference, maxIndex);
            }

            Method setMaxMethod = findMethod(preference.getClass(), "al", int.class);
            if (setMaxMethod != null) {
                setMaxMethod.setAccessible(true);
                setMaxMethod.invoke(preference, maxIndex);
            }

            // Clamp progress and persist via ap(targetIndex, false)
            Method apMethod = findMethod(preference.getClass(), "ap", int.class, boolean.class);
            if (apMethod != null) {
                apMethod.setAccessible(true);
                apMethod.invoke(preference, targetIndex, false);
            }

            // Notify preference change
            Method notifyChangedMethod = findMethod(preference.getClass(), "d");
            if (notifyChangedMethod != null) {
                notifyChangedMethod.setAccessible(true);
                notifyChangedMethod.invoke(preference);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to configure clipboard history slider", t);
        }
    }

    /**
     * Returns the retention window in milliseconds based on user preference.
     *
     * @param context Context
     * @param original original milliseconds calculated by Gboard
     * @return retention window in milliseconds
     */
    public static long getRetentionLimitMs(Context context, long original) {
        if (context == null) {
            return original;
        }
        try {
            String duration = GboardClipboardHistorySettings.readDuration(
                    GboardClipboardHistorySettings.preferences(context));
            return GboardClipboardHistorySettings.toMillis(duration);
        } catch (Throwable ignored) {
            return original;
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

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
