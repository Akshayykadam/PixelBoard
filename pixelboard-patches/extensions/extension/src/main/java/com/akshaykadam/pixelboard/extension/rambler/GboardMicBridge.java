// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.extension.rambler;

import android.inputmethodservice.InputMethodService;
import android.os.Looper;

/** Version-pinned adapter to Gboard 18.4.1; no alternate Google recognizer is introduced. */
public final class GboardMicBridge {
    private static final int JETSON_KEY = 0x7f140954;
    private static final String MODULE_MANAGER =
            "com.google.android.libraries.inputmethod.module.ModuleManager";

    private static InputMethodService keyboard(InputMethodService service) {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Keyboard action requires main thread");
        if (!nativeClass(service, "pbb").isInstance(service)
                || service.getCurrentInputConnection() == null)
            throw new IllegalStateException("Text field unavailable");
        return service;
    }

    public static void beforeEvent(InputMethodService service, Object event) {
        if (!GboardMicGestureSettings.isEnabled(service) || event == null) return;
        int code = (Integer) call(event, "a", new Class<?>[0]);
        if (code == -10042 || code == -10066)
            GboardRamblerSession.clear(service, "native Standard action");
        if (code == -10042 || code == -10217) GboardMicGestureRuntime.invalidate(service);
    }

    public static void startGoogleStandard(InputMethodService service) {
        if (!GboardMicGestureSettings.isEnabled(service)) return;
        // A second microphone press stops the current Rambler recording before any mode change.
        if (isRamblerActive(service)) {
            finishGoogleRambler(service);
            return;
        }
        selectGoogleMode(service, false, true);
    }

    /** Quick switch tap: start the mode currently selected in Gboard's own Dictation type. */
    public static void startGoogleSelected(InputMethodService service) {
        if (!GboardMicGestureSettings.isEnabled(service)) return;
        if (ramblerSelected(service)) {
            startGoogleRambler(service);
        } else {
            startGoogleStandard(service);
        }
    }

    /** Quick switch double tap: select the other dictation mode without starting it. */
    public static void switchGoogleMode(InputMethodService service) {
        if (!GboardMicGestureSettings.isEnabled(service)) return;
        GboardRamblerSession.clear(service, "mode switch");
        selectGoogleMode(service, !ramblerSelected(service), false);
    }

    private static boolean ramblerSelected(InputMethodService service) {
        return readPreference(nativePreferences(service), JETSON_KEY, false);
    }

    /**
     * Native b() filters by activation. Its q map also retains already-created inactive modules.
     */
    private static Object ramblerWrapper(InputMethodService service)
            throws ReflectiveOperationException {
        Object manager = nativeField(keyboard(service), "pbb", "F", service.getClassLoader());
        if (manager == null) return null;
        ClassLoader loader = service.getClassLoader();
        // fdf registers Rambler as qab(356, mvr.class, fde.class): the registry is keyed by the
        // interface mvr, and the wrapper then holds the fde instance (checked in loadedRambler).
        Class<?> extension = Class.forName("mvr", false, loader);
        java.lang.reflect.Field registry =
                Class.forName("obl", false, loader).getDeclaredField("q");
        registry.setAccessible(true);
        Object wrappers = registry.get(manager);
        if (!(wrappers instanceof java.util.Map))
            throw new IllegalStateException("Invalid native extension registry");
        return ((java.util.Map<?, ?>) wrappers)
                .get(extension); // lookup only: never allocate/activate an extension
    }

    private static Object loadedRambler(InputMethodService service)
            throws ReflectiveOperationException {
        Object wrapper = ramblerWrapper(service);
        return wrapper == null ? null : loadedRambler(wrapper, service.getClassLoader());
    }

    private static Object loadedRambler(Object wrapper, ClassLoader loader)
            throws ReflectiveOperationException {
        Object loaded = nativeField(wrapper, "obr", "h", loader);
        if (loaded != null && !Class.forName("fde", false, loader).isInstance(loaded))
            throw new IllegalStateException("Unexpected native Rambler extension");
        return loaded;
    }

    /**
     * Classic recorder telemetry only; Lite has a different recorder. Never use this alone to
     * select Standard.
     */
    public static boolean isRamblerListening(InputMethodService service) {
        try {
            return classicListening(loadedRambler(service), service.getClassLoader());
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw new IllegalStateException("Cannot read native Rambler microphone state", error);
        }
    }

    private static boolean classicListening(Object loaded, ClassLoader loader)
            throws ReflectiveOperationException {
        if (loaded == null) return false;
        Object agentic = nativeField(loaded, "fde", "g", loader);
        if (agentic == null) return false;
        Object handler = nativeField(agentic, "fdq", "d", loader);
        if (handler == null) return false;
        Object voice = nativeField(handler, "kls", "e", loader);
        if (voice == null) return false;
        Object recorder = nativeMethod(voice, "kme", "b", loader);
        if (recorder == null) return false;
        Object state = nativeField(recorder, "kmc", "i", loader);
        return state != null && (Boolean) nativeMethod(state, "kmp", "e", loader);
    }

    /** Fresh native snapshot; no cached/requested mode or native state writes. */
    private static final class RamblerState {
        final boolean active, busy, liteRecording, classicRecording, core;
        final String stage, module;

        RamblerState(
                String stage,
                boolean liteRecording,
                boolean classicRecording,
                String module,
                boolean core) {
            this.stage = stage;
            this.module = module;
            this.core = core;
            this.liteRecording = liteRecording;
            this.classicRecording = classicRecording;
            busy = "PROCESSING".equals(stage) || "RETRYING".equals(stage);
            active =
                    liteRecording
                            || classicRecording
                            || busy
                            || "WAITING".equals(stage)
                            || "LISTENING".equals(stage);
        }

        String describe() {
            return "RAMBLER_ACTIVE="
                    + (active ? 1 : 0)
                    + " STAGE="
                    + stage
                    + " LITE_REC="
                    + (liteRecording ? 1 : 0)
                    + " CLASSIC="
                    + (classicRecording ? 1 : 0)
                    + " MOD="
                    + module
                    + " CORE="
                    + (core ? 1 : 0);
        }
    }

    private static RamblerState ramblerState(InputMethodService service) {
        try {
            ClassLoader loader = service.getClassLoader();
            Object wrapper = ramblerWrapper(service);
            Object loaded = wrapper == null ? null : loadedRambler(wrapper, loader);
            if (loaded == null) return new RamblerState("UNLOADED", false, false, "none", false);
            boolean activated = (Boolean) nativeField(wrapper, "obr", "l", loader);
            Object ui = nativeField(loaded, "fde", "c", loader);
            String stage = "NO_UI";
            if (ui != null) {
                Object value = nativeMethod(ui, "ffr", "h", loader);
                if (!Class.forName("fgf", false, loader).isInstance(value)
                        || !(value instanceof Enum))
                    throw new IllegalStateException("Unexpected native Rambler session stage");
                stage = ((Enum<?>) value).name();
                if (!java.util.Arrays.asList(
                                "IDLING", "WAITING", "LISTENING", "PROCESSING", "RETRYING", "ERROR")
                        .contains(stage))
                    throw new IllegalStateException(
                            "Unknown native Rambler session stage: " + stage);
            }
            boolean liteRecording = false, core = ui != null;
            boolean lite = Class.forName("fdj", false, loader).isInstance(loaded);
            if (lite) {
                Object recorder = privateNativeField(loaded, "fdj", "E", loader);
                liteRecording =
                        recorder != null && (Boolean) nativeMethod(recorder, "ifs", "b", loader);
                core =
                        core
                                && recorder != null
                                && privateNativeField(loaded, "fdj", "F", loader) != null;
            }
            boolean classicRecording = classicListening(loaded, loader);
            GboardRamblerSession.observeNativeState(
                    service,
                    loaded,
                    liteRecording || classicRecording,
                    "IDLING".equals(stage) || "ERROR".equals(stage));
            return new RamblerState(
                    stage,
                    liteRecording,
                    classicRecording,
                    (lite ? "Lite" : "Classic") + "/" + (activated ? 1 : 0),
                    core);
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw new IllegalStateException("Cannot read native Rambler session state", error);
        }
    }

    /** Read-only state description for adapter contract tests. */
    public static String describeRamblerState(InputMethodService service) {
        return ramblerState(service).describe() + " SIGNAL=" + GboardRamblerSession.signal(service);
    }

    private static String activeSignal(InputMethodService service) {
        String signal = GboardRamblerSession.signal(service);
        try {
            RamblerState state = ramblerState(service);
            signal =
                    GboardRamblerSession.signal(
                            service); // The native observation may just have retired a stale lease.
            if ("NONE".equals(signal) && state.active) signal = "NATIVE_SESSION";
        } catch (RuntimeException error) {
            if ("NONE".equals(signal)) throw error;
        }

        return signal;
    }

    public static boolean isRamblerActive(InputMethodService service) {
        return !"NONE".equals(activeSignal(service));
    }

    private static Object privateNativeField(
            Object target, String owner, String name, ClassLoader loader)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = Class.forName(owner, false, loader).getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object nativeField(Object target, String owner, String name, ClassLoader loader)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = Class.forName(owner, false, loader).getField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object nativeMethod(Object target, String owner, String name, ClassLoader loader)
            throws ReflectiveOperationException {
        java.lang.reflect.Method method = Class.forName(owner, false, loader).getMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    public static void startGoogleRambler(InputMethodService service) {
        if (!GboardMicGestureSettings.isEnabled(service)) return;
        // -10217 toggles. Check tick/startup authority before any native lookup can fail.
        if (isRamblerActive(service)) {
            finishGoogleRambler(service);
            return;
        }
        selectGoogleMode(service, true, true);
    }

    /** Finish through the native tick route, with no preference or official-selection writes. */
    public static void finishGoogleRambler(InputMethodService service) {
        if (!GboardMicGestureSettings.isEnabled(service)) return;
        String signal = activeSignal(service);
        if ("NONE".equals(signal)) {
            return;
        }
        if ("NATIVE_SESSION".equals(signal)) {
            // Supplemental module telemetry can protect processing, but cannot authorize an
            // idle-start toggle.
            android.util.Log.i(
                    "PixelBoardVoice", "finish deferred: no visible tick or fresh startup lease");
            return;
        }

        GboardRamblerSession.clear(service, "finish tap");

        dispatchRambler(keyboard(service));
    }

    private static void dispatchRambler(InputMethodService controller) {
        Object event = event(controller, -10217);
        try {
            java.lang.reflect.Field marker = nativeClass(controller, "mnp").getField("a");
            marker.setAccessible(true);
            java.lang.reflect.Field source = nativeClass(controller, "oas").getField("l");
            source.setAccessible(true);
            source.set(event, marker.get(null));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }

        boolean accepted =
                (Boolean)
                        call(
                                controller,
                                "aw",
                                new Class<?>[] {nativeClass(controller, "oas")},
                                event);

        if (!accepted)
            throw new IllegalStateException("Rambler is unavailable in this keyboard session.");
    }

    /** Gboard's own Dictation type preference (enable_jetson) in its native preference store. */
    private static Object nativePreferences(InputMethodService service) {
        if (!"enable_jetson".equals(service.getString(JETSON_KEY)))
            throw new IllegalStateException("Unsupported Google dictation preference");
        return callStatic(
                nativeClass(service, "qou"),
                "I",
                new Class<?>[] {android.content.Context.class},
                service);
    }

    /**
     * Use the pinned keyboard's own preference writer, preserving native preference notifications.
     * Without start (Quick switch), it returns once the mode is selected.
     */
    private static void selectGoogleMode(
            InputMethodService service, boolean rambler, boolean start) {
        final InputMethodService controller = keyboard(service);
        if (start && !rambler && isRamblerActive(service)) {
            finishGoogleRambler(service);
            return;
        }
        final android.view.inputmethod.InputConnection connection =
                service.getCurrentInputConnection();
        final android.view.inputmethod.EditorInfo editor = service.getCurrentInputEditorInfo();
        Object preferences = nativePreferences(service);
        boolean previous = readPreference(preferences, JETSON_KEY, false);
        // Quick switch to Rambler writes the preference last: the readiness refresh creates the
        // Rambler extension, and only then is its enable_jetson listener there to hear the change
        // (that is what swaps the mic icon).
        boolean writeLast = rambler && !start;
        try {
            if (!writeLast) savePreference(preferences, rambler);
            GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(rambler);

            // Capability flags can remain true inside native Settings scope; they are not the
            // selected mode.
            if (connection != service.getCurrentInputConnection()
                    || editor != service.getCurrentInputEditorInfo())
                throw new IllegalStateException("Text field changed before dictation could start.");
            if (rambler) refreshRamblerReadiness(service);
            if (writeLast) savePreference(preferences, rambler);
            if (!start) return;
            if (rambler) {
                // Native klr.d / uwm.cn: enter onboarding/quota/session setup, which creates its
                // own jetson request.
                RamblerState ready = ramblerState(service);

                // A selection notification can synchronously reveal/change the native session.
                if (ready.active) {

                    return;
                }
                dispatchRambler(controller);
                GboardRamblerSession.accepted(service, connection, editor);

            } else {
                // Preference listeners/selector calls may run reentrantly after the first readback.
                if (readPreference(preferences, JETSON_KEY, true))
                    throw new IllegalStateException("Standard selection changed before dispatch");
                GboardRamblerSession.clear(service, "Standard start");
                android.util.Log.i(
                        "PixelBoardVoice",
                        "Standard native selection=false verified before -10042");
                call(
                        controller,
                        "dG",
                        new Class<?>[] {nativeClass(service, "oas")},
                        event(service, -10042));
            }
        } catch (RuntimeException error) {
            try {
                writePreference(preferences, JETSON_KEY, previous);
                GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(previous);
            } catch (RuntimeException restoreError) {
                error.addSuppressed(restoreError);
            }
            throw new IllegalStateException("Google dictation mode could not be selected", error);
        }
    }

    private static void savePreference(Object preferences, boolean rambler) {
        writePreference(preferences, JETSON_KEY, rambler);
        if (readPreference(preferences, JETSON_KEY, !rambler) != rambler)
            throw new IllegalStateException("Google mode was not saved");
    }

    /**
     * The native module listener caches whether Rambler's flags allow it to load. In a keyboard
     * process that started with Standard selected, that answer stays false after we select Rambler,
     * so ask the listener to check again. fdf registers it under the interface mvr.
     */
    private static void refreshRamblerReadiness(InputMethodService service) {
        try {
            ClassLoader loader = service.getClassLoader();
            Class<?> manager = Class.forName(MODULE_MANAGER, false, loader);
            Object listeners =
                    manager.getMethod("getModuleInitListenersMap")
                            .invoke(
                                    manager.getMethod("getInstance", android.content.Context.class)
                                            .invoke(null, service));
            Object listener =
                    listeners instanceof java.util.Map
                            ? ((java.util.Map<?, ?>) listeners)
                                    .get(Class.forName("mvr", false, loader))
                            : null;
            if (listener == null) return;
            java.lang.reflect.Method refresh =
                    Class.forName(MODULE_MANAGER + "$ModuleInitListener", false, loader)
                            .getDeclaredMethod("updateFlagReady");
            refresh.setAccessible(true);
            refresh.invoke(listener);
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.w("PixelBoardVoice", "Rambler readiness refresh failed", error);
        }
    }

    private static Class<?> nativeClass(InputMethodService service, String name) {
        try {
            return Class.forName(name, false, service.getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object event(InputMethodService service, int code) {
        return callStatic(
                nativeClass(service, "oas"),
                "e",
                new Class<?>[] {int.class, Object.class},
                code,
                "");
    }

    private static boolean readPreference(Object preferences, int key, boolean fallback) {
        return (Boolean)
                call(preferences, "x", new Class<?>[] {int.class, boolean.class}, key, fallback);
    }

    private static void writePreference(Object preferences, int key, boolean value) {
        call(preferences, "q", new Class<?>[] {int.class, boolean.class}, key, value);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... arguments) {
        return invoke(target.getClass(), target, name, types, arguments);
    }

    private static Object callStatic(
            Class<?> owner, String name, Class<?>[] types, Object... arguments) {
        return invoke(owner, null, name, types, arguments);
    }

    private static Object invoke(
            Class<?> owner, Object target, String name, Class<?>[] types, Object[] arguments) {
        try {
            java.lang.reflect.Method method = owner.getMethod(name, types);
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Native mic contract: " + name, error);
        }
    }
}
