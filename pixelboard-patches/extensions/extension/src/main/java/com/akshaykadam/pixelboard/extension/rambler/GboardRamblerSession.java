// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.extension.rambler;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Native toolbar identity first; bounded accepted-start backstop second. */
public final class GboardRamblerSession {
    private static final long TIMEOUT_MS =
            3000; // Startup grace only; visible native tick owns the session afterward.
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<InputMethodService, WeakReference<View>> VIEWS =
            new WeakHashMap<>();
    private static final WeakHashMap<InputMethodService, Lease> ACCEPTED = new WeakHashMap<>();

    private GboardRamblerSession() {}

    private static final class Lease {
        final WeakReference<InputConnection> connection;
        final WeakReference<EditorInfo> editor;
        final WeakReference<Object> window;
        final boolean viewBound;
        final long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        WeakReference<Object> observedRecordingOwner;
        Runnable expiry;

        Lease(InputMethodService ime, View view) {
            connection = new WeakReference<>(ime.getCurrentInputConnection());
            editor = new WeakReference<>(ime.getCurrentInputEditorInfo());
            window = new WeakReference<>(view == null ? null : view.getWindowToken());
            viewBound = view != null;
        }
    }

    public static void bind(InputMethodService ime, View original) {
        clear(ime, "view replaced");
        VIEWS.put(ime, new WeakReference<>(original));
    }

    private static View view(InputMethodService ime) {
        WeakReference<View> ref = VIEWS.get(ime);
        return ref == null ? null : ref.get();
    }

    public static void unbind(InputMethodService ime) {
        clear(ime, "destroy");
        VIEWS.remove(ime);
    }

    public static void clear(InputMethodService ime, String reason) {
        Lease lease = ACCEPTED.remove(ime);
        if (lease != null) {
            MAIN.removeCallbacks(lease.expiry);
            android.util.Log.i("PixelBoardVoice", "Rambler lease retired: " + reason);
        }
    }

    /**
     * Called only after the marked Rambler START returned AW=true, never for finish or rejection.
     */
    public static void accepted(
            InputMethodService ime, InputConnection connection, EditorInfo editor) {
        if (connection == null
                || connection != ime.getCurrentInputConnection()
                || editor != ime.getCurrentInputEditorInfo()) {
            clear(ime, "start field changed");
            return;
        }
        View original = view(ime);
        if (original != null && (!original.isAttachedToWindow() || !original.isShown())) {
            clear(ime, "start window hidden");
            return;
        }
        clear(ime, "new accepted start");
        Lease lease = new Lease(ime, original);
        WeakReference<InputMethodService> owner = new WeakReference<>(ime);
        lease.expiry =
                () -> {
                    InputMethodService current = owner.get();
                    if (current != null && ACCEPTED.get(current) == lease)
                        clear(current, "startup grace expired (3000ms)");
                };
        ACCEPTED.put(ime, lease);
        MAIN.postDelayed(lease.expiry, TIMEOUT_MS);

        android.util.Log.i("PixelBoardVoice", "Rambler lease armed: 3000ms startup grace");
    }

    private static boolean acceptedValid(InputMethodService ime) {
        Lease lease = ACCEPTED.get(ime);
        if (lease == null) return false;
        String reason = null;
        View original = view(ime);
        if (SystemClock.uptimeMillis() >= lease.deadline) reason = "startup grace expired (3000ms)";
        else if (lease.connection.get() == null
                || lease.connection.get() != ime.getCurrentInputConnection()
                || lease.editor.get() != ime.getCurrentInputEditorInfo()) reason = "field changed";
        else if (lease.viewBound
                && (original == null
                        || !original.isAttachedToWindow()
                        || !original.isShown()
                        || original.getWindowToken() != lease.window.get()))
            reason = "window changed/hidden";
        if (reason != null) {
            clear(ime, reason);
            return false;
        }
        return true;
    }

    public static void checkScope(InputMethodService ime) {
        acceptedValid(ime);
    }

    public static String signal(InputMethodService ime) {
        boolean accepted = acceptedValid(ime);
        if (tickIn(view(ime))) {
            clear(ime, "native tick observed; startup bridge complete");
            return "TOOLBAR_TICK";
        }
        return accepted ? "ACCEPTED_START" : "NONE";
    }

    /**
     * Called on native frame rendering, so a tick which appears/disappears between taps retires a
     * pending startup lease. Taps run signal() themselves, so ordinary typing skips the scan.
     */
    public static void observeToolbar(InputMethodService ime) {
        if (ACCEPTED.containsKey(ime)) signal(ime);
    }

    /**
     * Only a recording-to-idle transition on the same observed native instance retires a fresh
     * lease.
     */
    public static void observeNativeState(
            InputMethodService ime, Object owner, boolean recording, boolean terminalIdle) {
        Lease lease = ACCEPTED.get(ime);
        if (lease == null) return;
        if (recording) lease.observedRecordingOwner = new WeakReference<>(owner);
        else if (terminalIdle
                && lease.observedRecordingOwner != null
                && lease.observedRecordingOwner.get() == owner)
            clear(ime, "observed native recorder returned to idle");
    }

    private static boolean press(Object action) throws ReflectiveOperationException {
        Object kind = action.getClass().getField("c").get(action);
        return kind instanceof Enum && "PRESS".equals(((Enum<?>) kind).name());
    }

    private static boolean markedPress(Object action) throws ReflectiveOperationException {
        if (action == null || !press(action)) return false;
        Object[] keys = (Object[]) action.getClass().getField("d").get(action);
        if (keys != null)
            for (Object key : keys)
                if (key != null && key.getClass().getField("c").getInt(key) == -10217) return true;
        return false;
    }

    /** fgk.G() builds this exact highlighted tick for WAITING/LISTENING, wrapped by mmi.f(). */
    private static boolean tickDefinition(Object definition) throws ReflectiveOperationException {
        if (definition == null) return false;
        Object[] actions = (Object[]) definition.getClass().getField("f").get(definition);
        if (actions == null) return false;
        for (Object action : actions) {
            if (action == null || !press(action)) continue;
            Object[] keys = (Object[]) action.getClass().getField("d").get(action);
            if (keys == null) continue;
            for (Object key : keys) {
                if (key == null || key.getClass().getField("c").getInt(key) != -40002) continue;
                Object wrapper = key.getClass().getField("e").get(key);
                if (wrapper == null) continue;
                Object kind = wrapper.getClass().getField("b").get(wrapper);
                if (kind != action.getClass().getField("c").get(action)) continue;
                Object point = wrapper.getClass().getField("c").get(wrapper);
                if (point == null) continue;
                if (!"push_to_ramble".equals(point.getClass().getField("b").get(point))) continue;
                if (point.getClass().getField("c").getInt(point) != 0x7f08040a) continue;
                Object params = point.getClass().getField("r").get(point);
                Object mapping = point.getClass().getField("j").get(point);
                if (params instanceof Map
                        && Boolean.TRUE.equals(((Map<?, ?>) params).get("highlighted"))
                        && mapping instanceof Map
                        && markedPress(((Map<?, ?>) mapping).get(kind))) return true;
            }
        }
        return false;
    }

    private static boolean tickIn(View candidate) {
        if (candidate == null
                || !candidate.isAttachedToWindow()
                || !candidate.isShown()
                || candidate.getAlpha() <= 0) return false;
        if (candidate.isEnabled())
            try {
                if (tickDefinition(candidate.getClass().getField("e").get(candidate))) return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        if (candidate instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) candidate;
            for (int i = 0; i < group.getChildCount(); i++)
                if (tickIn(group.getChildAt(i))) return true;
        }
        return false;
    }
}
