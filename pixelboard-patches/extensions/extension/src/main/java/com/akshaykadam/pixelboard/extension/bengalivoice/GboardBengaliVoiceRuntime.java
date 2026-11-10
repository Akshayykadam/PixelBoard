package com.akshaykadam.pixelboard.extension.bengalivoice;

import android.content.Context;
import android.content.pm.PackageInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import com.akshaykadam.pixelboard.extension.rambler.GboardRambler1803OfficialSelectionRuntime;

/** Per-input routing for the native Gboard 18.5.3 dictation selector. */
public final class GboardBengaliVoiceRuntime {
    public static final int INHERIT = 0;
    public static final int STANDARD = 1;
    public static final int RAMBLER = 2;

    private static final ThreadLocal<Boolean> RESOLVING_INPUT_LOCALE = new ThreadLocal<Boolean>();
    private static volatile Context applicationContext;
    private static volatile NativeBindings nativeBindings;

    private GboardBengaliVoiceRuntime() {
    }

    /**
     * Runs after the native selector has read and observed the global preference.
     * It changes only the returned route, never the saved global selector or its cache.
     */
    public static boolean afterNativeSelection(boolean originalSelection) {
        return applyRouteOverride(activeRouteOverride(), originalSelection);
    }

    /** 0 preserves the native route; 1 explicitly selects Standard; 2 selects Rambler. */
    public static int activeRouteOverride() {
        if (!GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed()) {
            return INHERIT;
        }
        return activeRouteOverride(resolveApplicationContext());
    }

    private static int activeRouteOverride(Context context) {
        if (!GboardRambler1803OfficialSelectionRuntime.isLanguageOverrideAllowed()) {
            return INHERIT;
        }
        return routeOverrideForLocale(context, activeKeyboardLanguageTag(context));
    }

    public static int routeOverrideForLocale(Context context, String languageTag) {
        if (context == null || !GboardBengaliVoicePolicy.isBengali(languageTag)) {
            return INHERIT;
        }
        try {
            String mode = GboardBengaliVoiceSettings.readMode(context, languageTag);
            if (GboardBengaliVoicePolicy.STANDARD.equals(mode)) {
                return STANDARD;
            }
            if (GboardBengaliVoicePolicy.RAMBLER.equals(mode)) {
                return RAMBLER;
            }
        } catch (Throwable ignored) {
            // A missing preference/context must preserve the existing native route.
        }
        return INHERIT;
    }

    /**
     * Reads the current selected input locale each time, so a keyboard language
     * switch cannot retain a previous language's route. Never uses the UI locale.
     */
    public static String activeKeyboardLanguageTag(Context context) {
        NativeBindings bindings = bindings(context);
        return bindings == null ? null : languageTagFromLocaleGetter(bindings.currentLocale);
    }

    public static boolean isSupportedNativeVersion(Context context) {
        NativeBindings bindings = bindings(context);
        return bindings != null && bindings.supportedVersion;
    }

    private static NativeBindings bindings(Context context) {
        if (context == null) {
            return null;
        }
        try {
            ClassLoader loader = context.getClassLoader();
            String packageName = context.getPackageName();
            NativeBindings cached = nativeBindings;
            if (cached != null && cached.loader == loader && cached.packageName.equals(packageName)) {
                return cached;
            }
            // Package/version and reflection handles are immutable for an installed
            // app process. Only the selected locale returned by the method stays live.
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    packageName, 0);
            boolean supported = info != null && isSupportedNativeVersionName(info.versionName);
            Method currentLocale = null;
            if (supported) {
                try {
                    Class<?> notification = Class.forName("pem", false, loader);
                    Method candidate = notification.getDeclaredMethod("e");
                    if (Modifier.isStatic(candidate.getModifiers())
                            && candidate.getReturnType() == Locale.class) {
                        candidate.setAccessible(true);
                        currentLocale = candidate;
                    }
                } catch (ReflectiveOperationException unavailableBinding) {
                    // An unexpected native layout must keep the native route.
                }
            }
            NativeBindings resolved = new NativeBindings(loader, packageName, supported, currentLocale);
            nativeBindings = resolved;
            return resolved;
        } catch (Throwable ignored) {
            // Do not cache an unavailable context before application initialization.
            return null;
        }
    }

    static boolean isSupportedNativeVersionName(String versionName) {
        return versionName != null && versionName.startsWith("18.5.3.");
    }

    static String languageTagFromLocaleGetter(Method currentLocale) {
        if (currentLocale == null || Boolean.TRUE.equals(RESOLVING_INPUT_LOCALE.get())) {
            return null;
        }
        RESOLVING_INPUT_LOCALE.set(Boolean.TRUE);
        try {
            // In 18.5.3 pem.e() reads currentEntry.i().t(), returning null if absent.
            // pem.g() and pev.r() can use a default/display locale and are unsuitable.
            // Avoid pev.f(): building a subtype itself queries the hooked flags.
            if (!Modifier.isStatic(currentLocale.getModifiers())
                    || currentLocale.getReturnType() != Locale.class) {
                return null;
            }
            currentLocale.setAccessible(true);
            Object value = currentLocale.invoke(null);
            if (!(value instanceof Locale)) {
                return null;
            }
            Locale locale = (Locale) value;
            return locale.getLanguage().isEmpty() ? null : locale.toLanguageTag();
        } catch (Throwable ignored) {
            return null;
        } finally {
            RESOLVING_INPUT_LOCALE.remove();
        }
    }

    static boolean applyRouteOverride(int override, boolean originalSelection) {
        if (override == STANDARD) return false;
        if (override == RAMBLER) return true;
        return originalSelection;
    }

    private static Context resolveApplicationContext() {
        Context cached = applicationContext;
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getMethod("currentApplication");
            Object application = currentApplication.invoke(null);
            if (application instanceof Context) {
                Context context = (Context) application;
                Context app = context.getApplicationContext();
                Context resolved = app != null ? app : context;
                applicationContext = resolved;
                return resolved;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static void resetForTests() {
        applicationContext = null;
        nativeBindings = null;
        RESOLVING_INPUT_LOCALE.remove();
    }

    private static final class NativeBindings {
        final ClassLoader loader;
        final String packageName;
        final boolean supportedVersion;
        final Method currentLocale;

        NativeBindings(ClassLoader loader, String packageName, boolean supportedVersion, Method currentLocale) {
            this.loader = loader;
            this.packageName = packageName;
            this.supportedVersion = supportedVersion;
            this.currentLocale = currentLocale;
        }
    }
}
