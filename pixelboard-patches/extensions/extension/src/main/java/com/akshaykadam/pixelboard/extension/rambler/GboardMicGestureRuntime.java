// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.extension.rambler;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * One native mic slot; the original key keeps rendering and all other controls keep native
 * delivery.
 */
public final class GboardMicGestureRuntime {
    private static final WeakHashMap<InputMethodService, WeakReference<GboardMicGestureRuntime>>
            HUBS = new WeakHashMap<>();
    private final InputMethodService ime;
    private final FrameLayout root;
    private final View original;
    private GboardMicGesture gesture;
    private final ViewTreeObserver.OnPreDrawListener positionListener =
            () -> {
                positionMic();
                return true;
            };
    private final View.OnAttachStateChangeListener attachListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    positionMic();
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    resetSession(ime);
                }
            };

    private GboardMicGestureRuntime(InputMethodService ime, View keyboard) {
        this.ime = ime;
        original = keyboard;
        root = new FrameLayout(ime);
        root.addView(original, new FrameLayout.LayoutParams(-1, -1));
        root.getViewTreeObserver().addOnPreDrawListener(positionListener);
        root.addOnAttachStateChangeListener(attachListener);
    }

    public static View wrap(InputMethodService ime, View original) {
        if (original == null) return null;
        GboardMicGestureRuntime old = get(ime);
        destroy(ime);
        if (old != null && original == old.root) original = old.original;
        if (old != null && original.getParent() == old.root) old.root.removeView(original);
        GboardMicGestureRuntime hub = new GboardMicGestureRuntime(ime, original);
        HUBS.put(ime, new WeakReference<>(hub));
        return hub.root;
    }

    private static GboardMicGestureRuntime get(InputMethodService ime) {
        WeakReference<GboardMicGestureRuntime> ref = HUBS.get(ime);
        return ref == null ? null : ref.get();
    }

    private void positionMic() {
        if (!GboardMicGestureSettings.isEnabled(ime)) {
            // Default: no gesture surface, state probes, delayed tap or changed accessibility.
            if (gesture != null) {
                gesture.dispose();
                gesture = null;
                GboardRamblerSession.unbind(ime);
            }
            return;
        }
        if (gesture == null) {
            GboardRamblerSession.bind(ime, original);
            gesture = new GboardMicGesture(ime, root, original);
        }
        GboardRamblerSession.observeToolbar(ime);
        gesture.updatePosition();
    }

    public static void settingsChanged() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(GboardMicGestureRuntime::settingsChanged);
            return;
        }
        for (WeakReference<GboardMicGestureRuntime> ref :
                new java.util.ArrayList<>(HUBS.values())) {
            GboardMicGestureRuntime hub = ref.get();
            if (hub != null) {
                invalidate(hub.ime);
                hub.positionMic();
            }
        }
    }

    public static void invalidate(InputMethodService ime) {
        GboardMicGestureRuntime hub = get(ime);
        if (hub != null && hub.gesture != null) hub.gesture.cancel();
    }

    public static void checkSession(InputMethodService ime) {
        GboardRamblerSession.checkScope(ime);
        invalidate(ime);
    }

    public static void resetSession(InputMethodService ime) {
        GboardRamblerSession.clear(ime, "keyboard lifecycle");
        invalidate(ime);
    }

    public static void destroy(InputMethodService ime) {
        GboardRamblerSession.unbind(ime);
        GboardMicGestureRuntime hub = get(ime);
        if (hub != null) {
            if (hub.gesture != null) hub.gesture.dispose();
            if (hub.root.getViewTreeObserver().isAlive())
                hub.root.getViewTreeObserver().removeOnPreDrawListener(hub.positionListener);
            hub.root.removeOnAttachStateChangeListener(hub.attachListener);
        }
        HUBS.remove(ime);
    }

    static boolean allowed(EditorInfo info) {
        if (info == null || info.inputType == InputType.TYPE_NULL) return false;
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        return !(cls == InputType.TYPE_CLASS_TEXT
                        && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
                && !(cls == InputType.TYPE_CLASS_NUMBER
                        && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }
}
