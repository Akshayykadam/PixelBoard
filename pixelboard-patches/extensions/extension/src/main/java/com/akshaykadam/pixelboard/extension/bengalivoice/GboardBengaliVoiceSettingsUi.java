package com.akshaykadam.pixelboard.extension.bengalivoice;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Adds the selector after the native per-language category has been rebuilt. */
public final class GboardBengaliVoiceSettingsUi {
    private static final String TAG = "BengaliVoiceSettings";
    private static final String ROW_KEY = "pixelboard_bengali_voice_mode";
    private static final String PREFERENCE_CLASS = "androidx.preference.Preference";
    private static final String GROUP_CLASS = "androidx.preference.PreferenceGroup";

    private GboardBengaliVoiceSettingsUi() {
    }

    public static void afterLanguageSettingsRebuilt(Object fragment) {
        if (fragment == null) {
            return;
        }
        try {
            Object group = field(fragment.getClass(), "ar").get(fragment);
            if (group == null) {
                return;
            }
            ClassLoader loader = group.getClass().getClassLoader();
            attach(fragment,
                    Class.forName(PREFERENCE_CLASS, false, loader),
                    Class.forName(GROUP_CLASS, false, loader));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Log.w(TAG, "Unable to attach Bengali voice typing preference", exception);
        }
    }

    // Both production and tests exercise the same reflective adapter and attachment path.
    static void attach(Object fragment, Class<?> preferenceClass, Class<?> groupClass)
            throws ReflectiveOperationException {
        Object group = field(fragment.getClass(), "ar").get(fragment);
        if (group == null) {
            return;
        }
        Object language = field(fragment.getClass(), "am").get(fragment);
        String languageTag = language == null ? null : language.toString();
        NativePreferences api = new NativePreferences(preferenceClass, groupClass);
        Object previous = api.find.invoke(group, ROW_KEY);
        if (!GboardBengaliVoicePolicy.isBengali(languageTag)) {
            // A reused fragment must not retain the selector for a different language.
            if (previous != null) {
                api.remove.invoke(group, previous);
            }
            return;
        }
        Context context = (Context) api.context.get(group);
        if (context == null) {
            return;
        }
        Object row = api.constructor.newInstance(context);
        api.key.invoke(row, ROW_KEY);
        api.title.invoke(row, text(context, R.string.gboard_bengali_voice_title));
        api.summary.invoke(row, summary(context, languageTag));
        api.order.invoke(row, Integer.MAX_VALUE - 1);
        api.persistent.invoke(row, false);
        WeakReference<Object> rowReference = new WeakReference<>(row);
        Object listener = Proxy.newProxyInstance(
                api.click.getType().getClassLoader(),
                new Class<?>[]{api.click.getType()},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return args != null && args.length == 1 && proxy == args[0];
                        }
                        return "BengaliVoicePreferenceClick";
                    }
                    Object currentRow = rowReference.get();
                    if (currentRow == null) {
                        return false;
                    }
                    return showDialog(context, languageTag, currentRow, api);
                });
        // The preference owns its listener. No global fragment/activity references are kept.
        api.click.set(row, listener);
        boolean wasVisible = api.visibleState.getBoolean(group);
        boolean removedPrevious = false;
        try {
            if (previous != null) {
                api.remove.invoke(group, previous);
                removedPrevious = true;
            }
            api.add.invoke(group, row);
            api.visible.invoke(group, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            try {
                api.remove.invoke(group, row);
                if (removedPrevious) {
                    api.add.invoke(group, previous);
                }
                api.visible.invoke(group, wasVisible);
            } catch (ReflectiveOperationException | RuntimeException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    private static boolean showDialog(Context context, String languageTag, Object row,
            NativePreferences api) {
        try {
            String mode = GboardBengaliVoiceSettings.readMode(context, languageTag);
            CharSequence[] choices = {
                    text(context, R.string.gboard_bengali_voice_standard_choice),
                    text(context, R.string.gboard_bengali_voice_rambler_choice),
            };
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(text(context, R.string.gboard_bengali_voice_dialog_title))
                    .setSingleChoiceItems(choices,
                            GboardBengaliVoicePolicy.RAMBLER.equals(mode) ? 1 : 0,
                            (selectedDialog, selected) -> {
                                String selectedMode = selected == 1
                                        ? GboardBengaliVoicePolicy.RAMBLER
                                        : GboardBengaliVoicePolicy.STANDARD;
                                if (GboardBengaliVoiceSettings.writeMode(
                                        context, languageTag, selectedMode)) {
                                    try {
                                        api.summary.invoke(row, summary(context, languageTag));
                                    } catch (ReflectiveOperationException exception) {
                                        Log.w(TAG, "Unable to refresh voice typing summary", exception);
                                    }
                                    selectedDialog.dismiss();
                                } else {
                                    selectedDialog.dismiss();
                                    Toast.makeText(context,
                                            text(context, R.string.gboard_bengali_voice_save_failed),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.show();
            return true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to show Bengali voice typing choices", exception);
            return false;
        }
    }

    private static String summary(Context context, String languageTag) {
        int label = GboardBengaliVoicePolicy.RAMBLER.equals(
                GboardBengaliVoiceSettings.readMode(context, languageTag))
                ? R.string.gboard_bengali_voice_rambler
                : R.string.gboard_bengali_voice_standard;
        return GboardSettingsText.format(context, R.string.gboard_bengali_voice_summary,
                text(context, label));
    }

    private static String text(Context context, int resource) {
        return GboardSettingsText.get(context, resource);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field result = current.getDeclaredField(name);
                result.setAccessible(true);
                return result;
            } catch (NoSuchFieldException ignored) {
                // Native fields can be declared on Preference or an inherited fragment.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class NativePreferences {
        final Constructor<?> constructor;
        final Field context;
        final Field click;
        final Field visibleState;
        final Method key;
        final Method title;
        final Method summary;
        final Method order;
        final Method persistent;
        final Method visible;
        final Method find;
        final Method add;
        final Method remove;

        NativePreferences(Class<?> preferenceClass, Class<?> groupClass)
                throws ReflectiveOperationException {
            constructor = preferenceClass.getConstructor(Context.class);
            context = field(preferenceClass, "j");
            click = field(preferenceClass, "o");
            visibleState = field(preferenceClass, "z");
            key = preferenceClass.getMethod("P", String.class);
            title = preferenceClass.getMethod("V", CharSequence.class);
            summary = preferenceClass.getMethod("n", CharSequence.class);
            order = preferenceClass.getMethod("Q", int.class);
            persistent = preferenceClass.getMethod("O", boolean.class);
            visible = preferenceClass.getMethod("W", boolean.class);
            find = groupClass.getMethod("l", CharSequence.class);
            add = groupClass.getMethod("an", preferenceClass);
            remove = groupClass.getMethod("ao", preferenceClass);
            if (!click.getType().isInterface()) {
                throw new IllegalStateException("Unsupported native preference listener");
            }
        }
    }
}
