// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.extension.rambler;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;

/** Native mic rendering (including its listening tick) and geometry remain untouched. */
public final class GboardMicGesture {
    static final long DOUBLE_TAP_MS = 300;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean pending, secondTap;
    private long deadline;
    private View pendingTarget;
    private InputConnection pendingConnection;
    private EditorInfo pendingEditor;
    private final Runnable singleTap = this::singleTap;
    private final InputMethodService ime;
    private final FrameLayout root;
    private final View original;
    private final View surface;
    private View nativeTarget;
    private int nativeAccessibility;
    private boolean disposed, tracking, rejected, committed;
    private int pointerId;
    private View pressedTarget;
    private InputConnection pressedConnection;
    private EditorInfo pressedEditor;

    public GboardMicGesture(InputMethodService service, FrameLayout host, View keyboard) {
        ime = service;
        root = host;
        original = keyboard;
        surface =
                new View(ime) {
                    @Override
                    public boolean onTouchEvent(MotionEvent event) {
                        return true;
                    }
                };
        surface.setBackgroundColor(Color.TRANSPARENT);
        surface.setContentDescription(
                com.akshaykadam.pixelboard.extension.settings.GboardSettingsText.get(
                        ime,
                        com.akshaykadam.pixelboard.extension.R.string
                                .gboard_patches_mic_gesture_accessibility));
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        surface.setVisibility(View.GONE);
        surface.setOnClickListener(
                view -> {
                    commit(pending && validPending() && SystemClock.uptimeMillis() < deadline);
                });
        surface.setOnTouchListener((view, event) -> onTouch(event));
        root.addView(surface, new FrameLayout.LayoutParams(0, 0));
    }

    private static boolean isPress(Object action) throws ReflectiveOperationException {
        Object kind = action.getClass().getField("c").get(action);
        return kind instanceof Enum && "PRESS".equals(((Enum<?>) kind).name());
    }

    private static boolean directlyLaunches(Object action) throws ReflectiveOperationException {
        if (action == null || !isPress(action)) return false;
        Object[] keys = (Object[]) action.getClass().getField("d").get(action);
        if (keys == null) return false;
        boolean launch = false;
        for (Object key : keys) {
            if (key == null) continue;
            int code = key.getClass().getField("c").getInt(key);
            if (code == -10066) return false;
            if (code == -10042 || code == -10217) launch = true;
        }
        return launch;
    }

    static boolean hasLaunchIdentity(Object definition) {
        if (definition == null) return false;
        try {
            Object[] actions = (Object[]) definition.getClass().getField("f").get(definition);
            if (actions == null) return false;
            for (Object action : actions) {
                if (action == null || !isPress(action)) continue;
                if (directlyLaunches(action)) return true;
                Object[] keys = (Object[]) action.getClass().getField("d").get(action);
                if (keys == null) continue;
                for (Object key : keys) {
                    if (key == null || key.getClass().getField("c").getInt(key) != -40002) continue;
                    Object payload = key.getClass().getField("e").get(key);
                    if (payload == null) continue;
                    Object selectedKind = payload.getClass().getField("b").get(payload);
                    if (selectedKind != action.getClass().getField("c").get(action)) continue;
                    Object accessPoint = payload.getClass().getField("c").get(payload);
                    if (accessPoint == null) continue;
                    Object mapping = accessPoint.getClass().getField("j").get(accessPoint);
                    // mmi.f wraps an individual action; mmb.b selects that exact original action.
                    // Scanning all map values could replace a Stop key because a secondary action
                    // launches.
                    if (mapping instanceof java.util.Map
                            && directlyLaunches(((java.util.Map<?, ?>) mapping).get(selectedKind)))
                        return true;
                }
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
        return false;
    }

    private boolean isLaunchKey(View view) {
        if (!view.isShown()
                || !view.isEnabled()
                || view.getAlpha() <= 0f
                || view.getWidth() <= 0
                || view.getHeight() <= 0) return false;
        boolean softKey = false;
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().endsWith("SoftKeyView")) {
                softKey = true;
                break;
            }
        }
        if (!softKey) return false;
        try {
            return hasLaunchIdentity(view.getClass().getField("e").get(view));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private View findLaunchKey(View view) {
        if (isLaunchKey(view)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findLaunchKey(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    public void updatePosition() {
        if (disposed) return;
        if (pending && !validPending()) {
            clearPending();
        }
        if (!GboardMicGestureSettings.isEnabled(ime)
                || !GboardMicGestureRuntime.allowed(ime.getCurrentInputEditorInfo())
                || ime.getCurrentInputConnection() == null) {
            cancel();
            restoreTarget();
            surface.setVisibility(View.GONE);
            return;
        }
        if (nativeTarget != null
                && (!isLaunchKey(nativeTarget) || !nativeTarget.isAttachedToWindow())) {
            cancel();
            restoreTarget();
        }
        if (nativeTarget == null) {
            View found = findLaunchKey(original);
            if (found == null) {
                surface.setVisibility(View.GONE);
                return;
            }

            nativeTarget = found;
            nativeAccessibility = found.getImportantForAccessibility();
        }
        // The native icon remains visible; only the accessible click belongs to the surface.
        if (nativeTarget.getImportantForAccessibility()
                != View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
            nativeTarget.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        int[] key = new int[2], host = new int[2];
        nativeTarget.getLocationInWindow(key);
        root.getLocationInWindow(host);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) surface.getLayoutParams();
        int left = key[0] - host[0], top = key[1] - host[1];

        if (lp.leftMargin != left
                || lp.topMargin != top
                || lp.width != nativeTarget.getWidth()
                || lp.height != nativeTarget.getHeight()) {
            int slop = ViewConfiguration.get(ime).getScaledTouchSlop();
            // A native pressed-state relayout must not erase the already-detected second tap.
            // Freeze the hit rectangle for this short gesture only; large moves still cancel.
            if ((tracking || pending)
                    && (!tracking || sameField())
                    && (!pending || validPending())
                    && lp.width == nativeTarget.getWidth()
                    && lp.height == nativeTarget.getHeight()
                    && Math.abs(lp.leftMargin - left) <= slop
                    && Math.abs(lp.topMargin - top) <= slop) {

                return;
            }

            cancel();
            lp.leftMargin = left;
            lp.topMargin = top;
            lp.width = nativeTarget.getWidth();
            lp.height = nativeTarget.getHeight();
            surface.setLayoutParams(lp);
        }
        surface.setVisibility(View.VISIBLE);
    }

    private void restoreTarget() {
        if (nativeTarget == null) return;
        nativeTarget.setImportantForAccessibility(nativeAccessibility);
        nativeTarget = null;
    }

    private boolean sameField() {
        return GboardMicGestureSettings.isEnabled(ime)
                && !disposed
                && nativeTarget != null
                && nativeTarget == pressedTarget
                && isLaunchKey(nativeTarget)
                && root.isAttachedToWindow()
                && pressedConnection != null
                && pressedConnection == ime.getCurrentInputConnection()
                && pressedEditor == ime.getCurrentInputEditorInfo()
                && GboardMicGestureRuntime.allowed(pressedEditor);
    }

    private boolean inside(float x, float y) {
        return x >= 0 && y >= 0 && x < surface.getWidth() && y < surface.getHeight();
    }

    private boolean onTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                finishStream();
                if (nativeTarget == null || !isLaunchKey(nativeTarget)) {
                    clearPending();
                    return true;
                }
                secondTap = pending && validPending() && SystemClock.uptimeMillis() < deadline;
                if (secondTap) {
                    main.removeCallbacks(singleTap);
                } else clearPending();
                tracking = true;
                pointerId = event.getPointerId(0);
                pressedTarget = nativeTarget;
                pressedConnection = ime.getCurrentInputConnection();
                pressedEditor = ime.getCurrentInputEditorInfo();
                surface.setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!tracking) return true;
                int at = event.findPointerIndex(pointerId);
                if (at < 0 || !inside(event.getX(at), event.getY(at))) reject();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                if (tracking) reject();
                return true;
            case MotionEvent.ACTION_UP:

                // Dispatch on release; do not post a second Android PerformClick.
                if (tracking
                        && !rejected
                        && !committed
                        && sameField()
                        && inside(event.getX(), event.getY())) commit(secondTap);
                else {
                    clearPending();
                }
                finishStream();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancel();
                finishStream();
                return true;
            default:
                return true;
        }
    }

    private boolean validButton() {
        return GboardMicGestureSettings.isEnabled(ime)
                && !disposed
                && nativeTarget != null
                && nativeTarget.isAttachedToWindow()
                && isLaunchKey(nativeTarget)
                && root.isAttachedToWindow()
                && GboardMicGestureRuntime.allowed(ime.getCurrentInputEditorInfo())
                && ime.getCurrentInputConnection() != null;
    }

    private boolean validPending() {
        return validButton()
                && nativeTarget == pendingTarget
                && pendingConnection == ime.getCurrentInputConnection()
                && pendingEditor == ime.getCurrentInputEditorInfo();
    }

    private void clearPending() {
        main.removeCallbacks(singleTap);
        pending = false;
        pendingTarget = null;
        pendingConnection = null;
        pendingEditor = null;
    }

    private void reject() {

        rejected = true;
        clearPending();
        surface.setPressed(false);
    }

    private boolean ramblerActive() {
        return GboardMicBridge.isRamblerActive(ime);
    }

    private boolean quickSwitch() {
        return GboardMicGestureSettings.MODE_QUICK_SWITCH.equals(
                GboardMicGestureSettings.mode(ime));
    }

    private void failed(RuntimeException error) {
        clearPending();

        android.util.Log.e("PixelBoardVoice", "Native mic action failed", error);
    }

    private void commit(boolean doubleCandidate) {
        if (!validButton() || (tracking && (rejected || committed || !sameField()))) {
            return;
        }
        committed = tracking; // Native preference/event callbacks can cancel reentrantly.
        surface.setPressed(false);
        try {
            // Native toolbar/session signals, with the explicitly authorized accepted-start
            // backstop.
            if (ramblerActive()) {
                clearPending();
                GboardMicBridge.finishGoogleRambler(ime);
            } else if (doubleCandidate && pending && validPending()) {

                clearPending();
                if (quickSwitch()) {
                    GboardMicBridge.switchGoogleMode(ime); // Selection only; nothing starts.
                } else {
                    GboardMicBridge.startGoogleRambler(ime); // Native marked access-point route.
                }
            } else {
                clearPending();
                pending = true;
                pendingTarget = nativeTarget;
                pendingConnection = ime.getCurrentInputConnection();
                pendingEditor = ime.getCurrentInputEditorInfo();

                deadline = SystemClock.uptimeMillis() + DOUBLE_TAP_MS;
                main.postDelayed(singleTap, DOUBLE_TAP_MS);
            }
        } catch (RuntimeException error) {
            failed(error);
        }
    }

    private void singleTap() {
        boolean valid = pending && validPending();
        clearPending();
        if (!valid) {
            return;
        }
        try {
            // Another native action may have begun listening during the idle window.
            // A delayed idle tap must never finish that new recording.
            if (!ramblerActive()) {
                if (quickSwitch()) {
                    GboardMicBridge.startGoogleSelected(ime);
                } else {
                    GboardMicBridge.startGoogleStandard(ime);
                }
            }

        } catch (RuntimeException error) {
            failed(error);
        }
    }

    public void cancel() {

        clearPending();
        if (tracking) rejected = true;
        pressedTarget = null;
        pressedConnection = null;
        pressedEditor = null;
        surface.setPressed(false);
    }

    private void finishStream() {
        pressedTarget = null;
        pressedConnection = null;
        pressedEditor = null;
        surface.setPressed(false);
        tracking = rejected = committed = secondTap = false;
    }

    public void dispose() {
        disposed = true;
        cancel();
        finishStream();
        restoreTarget();
        root.removeView(surface);
    }
}
