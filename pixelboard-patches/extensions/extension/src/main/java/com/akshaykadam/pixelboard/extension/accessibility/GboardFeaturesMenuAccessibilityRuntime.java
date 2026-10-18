package com.akshaykadam.pixelboard.extension.accessibility;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

public final class GboardFeaturesMenuAccessibilityRuntime {

    private static final long DEBOUNCE_MILLIS = 150L;
    static volatile boolean forceAccessibilityEnabledForTesting = false;
    static volatile long lastVisibilityEventTime = 0L;

    private GboardFeaturesMenuAccessibilityRuntime() {
    }

    public static void onAccessPointsPanelInflated(View view) {
        if (view == null) {
            return;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                view.setAccessibilityPaneTitle(resolvePaneTitle(view));
            }
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        } catch (Throwable ignored) {
        }
    }

    public static void onAccessPointsPanelVisibilityChanged(final View view) {
        if (view == null || !isVisible(view)) {
            return;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                if (view.getAccessibilityPaneTitle() == null) {
                    view.setAccessibilityPaneTitle(resolvePaneTitle(view));
                }
            }
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            Context context = view.getContext();
            if (context == null) {
                return;
            }
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null || (!am.isEnabled() && !forceAccessibilityEnabledForTesting)) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastVisibilityEventTime < DEBOUNCE_MILLIS) {
                return;
            }
            lastVisibilityEventTime = now;

            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

            view.post(new Runnable() {
                int attempts = 0;

                @Override
                public void run() {
                    try {
                        if (!isVisible(view)) {
                            return;
                        }
                        View target = findFirstAccessibleAction(view);
                        if (target != null) {
                            target.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                        } else if (attempts < 3) {
                            attempts++;
                            view.postDelayed(this, 50);
                        } else {
                            view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    public static View findFirstAccessibleAction(View root) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return null;
        }
        if (root.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO ||
            root.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
            return null;
        }
        if (isAccessibleAction(root)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = vg.getChildAt(i);
                View match = findFirstAccessibleAction(child);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    public static boolean isAccessibleAction(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO ||
            view.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
            return false;
        }
        String className = view.getClass().getName();
        if (className.contains("AccessPointsPanel") ||
            className.contains("ViewPager") ||
            className.equals("android.widget.FrameLayout") ||
            className.equals("android.widget.LinearLayout") ||
            className.equals("androidx.constraintlayout.widget.ConstraintLayout")) {
            return false;
        }
        if (className.contains("SoftKeyView") || className.contains("Button")) {
            return true;
        }
        if ((view.isClickable() || view.isFocusable()) &&
            (view.getContentDescription() != null || (view instanceof TextView && ((TextView) view).getText().length() > 0))) {
            return true;
        }
        return false;
    }

    public static CharSequence resolvePaneTitle(View view) {
        if (view != null) {
            try {
                Context context = view.getContext();
                if (context != null) {
                    int resId = context.getResources().getIdentifier("string_0x7f14085b", "string", context.getPackageName());
                    if (resId != 0) {
                        CharSequence title = context.getText(resId);
                        if (title != null && title.length() > 0) {
                            return title;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return "Features menu";
    }

    static void resetForTesting() {
        forceAccessibilityEnabledForTesting = false;
        lastVisibilityEventTime = 0L;
    }

    static boolean isVisible(View view) {
        if (view == null) {
            return false;
        }
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        return view.getParent() == null || view.isShown();
    }
}
