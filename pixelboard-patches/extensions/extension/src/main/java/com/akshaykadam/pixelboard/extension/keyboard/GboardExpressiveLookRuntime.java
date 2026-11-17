/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.keyboard;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.akshaykadam.pixelboard.extension.settings.GboardExpressiveLookSettings;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Material 3 Expressive look and motion (issue #133): sunburst-shaped toolbar
 * buttons with press/rotation morphs, no divider line above the emoji row at
 * the rounded keyboard top, spring key-press/popup physics, and smooth panel
 * and toolbar spacing transitions.
 *
 * <p>Called from Gboard's top-rounded keyboard module, {@code SoftKeyView}, and
 * {@code BasicPopupView}. Every step is best effort and leaves the stock look
 * in place when the expected views are not found.</p>
 */
public final class GboardExpressiveLookRuntime {
    private static final String TAG = "GboardPatches";

    static final int SUNBURST_WAVES = 12;
    static final float SUNBURST_DEPTH = 0.04f;
    /** Pressed keys soften about halfway toward a circle; active keys become one. */
    static final float SUNBURST_PRESSED_DEPTH = 0.02f;
    static final float SUNBURST_ACTIVE_DEPTH = 0f;
    /** Sunburst diameter relative to the key; matches Gboard's own 34dp icon circle. */
    static final float SUNBURST_SCALE = 0.78f;
    static final float SUNBURST_PRESSED_SCALE = 0.9f;
    static final float SUNBURST_ACTIVE_PRESSED_SCALE = 0.94f;
    static final float SUNBURST_STEP_DEGREES = 360f / SUNBURST_WAVES;
    private static final int SUNBURST_PATH_STEPS = 216;
    private static final int FALLBACK_SUNBURST_FILL = 0xFFD7C4FF;
    private static final int FALLBACK_SUNBURST_ICON = 0xFF2A1450;
    private static final int RIPPLE_COLOR = 0x33000000;

    static final String MENU_KEY_ID = "key_pos_header_access_points_menu";
    static final String MIC_KEY_ID = "key_pos_header_power_key";
    static final String TOOLBAR_HOLDER_ID = "keyboard_header_view_holder";
    /** android:tag on the emoji row (fast access bar) container in Gboard's layout. */
    static final String EMOJI_ROW_DIVIDER_TAG = ".divider.horizontal.for-fast-access-bar";
    /** Extra space above the toolbar when it is the top row, clear of the rounded corners. */
    static final int TOOLBAR_TOP_SPACING_DP = 8;

    private static final float KEY_PRESS_SCALE_REGULAR = 0.92f;
    private static final float KEY_PRESS_SCALE_WIDE_X = 0.96f;
    private static final float KEY_PRESS_SCALE_WIDE_Y = 0.90f;
    private static final float KEY_WIDE_ASPECT_THRESHOLD = 1.45f;
    private static final float POPUP_START_SCALE_X = 0.84f;
    private static final float POPUP_START_SCALE_Y = 0.80f;
    private static final int POPUP_START_TRANSLATION_DP = 5;
    private static final float POPUP_START_ALPHA = 0.70f;
    private static final long POPUP_ANIM_DURATION_MS = 165L;
    private static final int PANEL_ENTRANCE_TRANSLATION_DP = 10;
    private static final float TRANSFORM_EPSILON = 0.01f;
    private static final float PANEL_ENTRANCE_ALPHA = 0.88f;
    private static final long PANEL_ENTRANCE_DURATION_MS = 230L;

    /** Views found on the last keyboard view change, re-checked before every frame. */
    private static final class Targets implements ViewTreeObserver.OnPreDrawListener {
        WeakReference<View> menuKey = new WeakReference<View>(null);
        WeakReference<View> micKey = new WeakReference<View>(null);
        WeakReference<View> emojiRow = new WeakReference<View>(null);
        WeakReference<View> toolbarHolder = new WeakReference<View>(null);

        @Override
        public boolean onPreDraw() {
            try {
                styleSunburst(menuKey.get());
                styleSunburst(micKey.get());
                hideTopDivider(emojiRow.get());
                spaceToolbarFromTop(toolbarHolder.get(), menuKey.get(), micKey.get());
            } catch (Throwable throwable) {
                Log.w(TAG, "[expressive] pre-draw failed", throwable);
            }
            return true;
        }
    }

    private static final Map<View, Targets> targetsByRoot =
            Collections.synchronizedMap(new WeakHashMap<View, Targets>());
    private static final Map<View, Boolean> activeKeyboardViews =
            Collections.synchronizedMap(new WeakHashMap<View, Boolean>());
    private static final Map<View, Boolean> keyPressStates =
            Collections.synchronizedMap(new WeakHashMap<View, Boolean>());
    private static final Map<View, Boolean> pendingPopupAnimations =
            Collections.synchronizedMap(new WeakHashMap<View, Boolean>());
    private static final Map<View, Boolean> toolbarMarginInitialized =
            Collections.synchronizedMap(new WeakHashMap<View, Boolean>());
    private static volatile String lastSummary;

    private GboardExpressiveLookRuntime() {
    }

    public static void onKeyboardView(View view) {
        try {
            if (view == null || !GboardExpressiveLookSettings.isEnabled(view.getContext())) {
                return;
            }
            // Runs after the module has applied its rounded outline.
            view.post(() -> applySafely(view));
        } catch (Throwable throwable) {
            Log.w(TAG, "[expressive] failed to schedule", throwable);
        }
    }

    /**
     * Called from {@code SoftKeyView.setPressed(boolean)} to give keys tactile
     * Material 3 Expressive spring feedback without delaying touch dispatch.
     */
    public static void onSoftKeyPressed(View key, boolean pressed) {
        try {
            if (key == null || !key.isAttachedToWindow() || key.getVisibility() != View.VISIBLE) {
                if (key != null && !pressed) {
                    keyPressStates.remove(key);
                }
                return;
            }
            if (!GboardExpressiveLookSettings.isEnabled(key.getContext())
                    || !M3ExpressiveMotion.shouldAnimate(key.getContext())) {
                resetKeyScaleIfNeeded(key);
                return;
            }
            // Sunburst toolbar keys animate their own background & icon directly.
            if (key.getBackground() instanceof SunburstRipple) {
                return;
            }
            Boolean previous = keyPressStates.get(key);
            if (!pressed && (previous == null || !previous.booleanValue())) {
                return;
            }
            if (pressed && Boolean.TRUE.equals(previous)) {
                return;
            }
            keyPressStates.put(key, Boolean.valueOf(pressed));

            int width = key.getWidth();
            int height = key.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            key.setPivotX(width * 0.5f);
            key.setPivotY(height * 0.5f);
            key.animate().cancel();
            if (pressed) {
                float[] pressScale = keyPressScale(width, height);
                key.animate()
                        .scaleX(pressScale[0])
                        .scaleY(pressScale[1])
                        .setDuration(M3ExpressiveMotion.DURATION_PRESS_IN_MS)
                        .setInterpolator(M3ExpressiveMotion.pressIn())
                        .start();
            } else {
                key.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(M3ExpressiveMotion.DURATION_FAST_SPATIAL_MS)
                        .setInterpolator(M3ExpressiveMotion.spatialFast())
                        .start();
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "[expressive] softkey animation failed", throwable);
        }
    }

    /**
     * Called from {@code BasicPopupView} when a key preview popup is bound and
     * about to appear, adding a crisp bottom-anchored spring pop-in.
     */
    public static void onPopupView(View popup) {
        try {
            if (popup == null || !GboardExpressiveLookSettings.isEnabled(popup.getContext())
                    || !M3ExpressiveMotion.shouldAnimate(popup.getContext())) {
                return;
            }
            if (Boolean.TRUE.equals(pendingPopupAnimations.get(popup))) {
                return;
            }
            pendingPopupAnimations.put(popup, Boolean.TRUE);
            ViewTreeObserver observer = popup.getViewTreeObserver();
            if (!observer.isAlive()) {
                pendingPopupAnimations.remove(popup);
                return;
            }
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    try {
                        ViewTreeObserver currentObserver = popup.getViewTreeObserver();
                        if (currentObserver.isAlive()) {
                            currentObserver.removeOnPreDrawListener(this);
                        }
                        pendingPopupAnimations.remove(popup);
                        animatePopupEntrance(popup);
                    } catch (Throwable throwable) {
                        Log.w(TAG, "[expressive] popup animation failed", throwable);
                    }
                    return true;
                }
            });
        } catch (Throwable throwable) {
            Log.w(TAG, "[expressive] failed to schedule popup animation", throwable);
        }
    }

    static void applySafely(View view) {
        try {
            View root = view.getRootView();
            if (root.getWidth() == 0 || root.getHeight() == 0) {
                return;
            }
            Targets targets = targetsByRoot.get(root);
            if (targets == null) {
                targets = new Targets();
                targetsByRoot.put(root, targets);
                root.getViewTreeObserver().addOnPreDrawListener(targets);
            }
            View menuKey = findViewByIdName(root, MENU_KEY_ID);
            View micKey = findViewByIdName(root, MIC_KEY_ID);
            if (menuKey != null) {
                targets.menuKey = new WeakReference<View>(menuKey);
            }
            if (micKey != null) {
                targets.micKey = new WeakReference<View>(micKey);
            }
            View emojiRow = findViewByTag(root, EMOJI_ROW_DIVIDER_TAG);
            if (emojiRow != null) {
                targets.emojiRow = new WeakReference<View>(emojiRow);
            }
            View toolbarHolder = findViewByIdName(root, TOOLBAR_HOLDER_ID);
            if (toolbarHolder != null) {
                targets.toolbarHolder = new WeakReference<View>(toolbarHolder);
            }
            targets.onPreDraw();
            animateKeyboardEntranceIfNew(view);
            logIfChanged("[expressive] menu=" + (targets.menuKey.get() != null)
                    + " mic=" + (targets.micKey.get() != null)
                    + " emojiRow=" + (targets.emojiRow.get() != null)
                    + " toolbar=" + (targets.toolbarHolder.get() != null));
        } catch (Throwable throwable) {
            Log.w(TAG, "[expressive] failed to apply", throwable);
        }
    }

    private static void logIfChanged(String summary) {
        if (!summary.equals(lastSummary)) {
            lastSummary = summary;
            Log.w(TAG, summary);
        }
    }

    // ---- Keyboard / panel & popup entrance animations ------------------------

    private static void animateKeyboardEntranceIfNew(View view) {
        if (!view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE
                || view.getWidth() == 0 || view.getHeight() == 0) {
            return;
        }
        if (Boolean.TRUE.equals(activeKeyboardViews.get(view))) {
            return;
        }
        activeKeyboardViews.put(view, Boolean.TRUE);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                activeKeyboardViews.remove(v);
            }
        });
        if (!M3ExpressiveMotion.shouldAnimate(view.getContext())) {
            return;
        }
        // Gboard positions the keyboard itself with view transforms (one-handed
        // mode shrinks and shifts it), so the pivot, scale and view.animate() are
        // left alone; only an alpha fade and a translationY slide run, through
        // separate animators, and the slide is skipped once Gboard has moved it.
        if (Math.abs(view.getAlpha() - 1.0f) < 0.01f) {
            ObjectAnimator alphaAnim =
                    ObjectAnimator.ofFloat(view, View.ALPHA, PANEL_ENTRANCE_ALPHA, 1.0f);
            alphaAnim.setDuration(M3ExpressiveMotion.DURATION_EFFECTS_FAST_MS);
            alphaAnim.setInterpolator(M3ExpressiveMotion.effectsFast());
            alphaAnim.start();
        }
        if (!canSlideKeyboardEntrance(view.getScaleX(), view.getScaleY(),
                view.getTranslationX(), view.getTranslationY())) {
            return;
        }
        ObjectAnimator slide = ObjectAnimator.ofFloat(
                view, View.TRANSLATION_Y, dp(view, PANEL_ENTRANCE_TRANSLATION_DP), 0f);
        slide.setDuration(PANEL_ENTRANCE_DURATION_MS);
        slide.setInterpolator(M3ExpressiveMotion.spatialDefault());
        slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Gboard started transforming the keyboard mid-slide: hand it back.
                if (Math.abs(view.getScaleX() - 1.0f) > TRANSFORM_EPSILON
                        || Math.abs(view.getScaleY() - 1.0f) > TRANSFORM_EPSILON
                        || Math.abs(view.getTranslationX()) > TRANSFORM_EPSILON) {
                    float ours = (Float) animation.getAnimatedValue();
                    animation.removeUpdateListener(this);
                    animation.cancel();
                    if (Math.abs(view.getTranslationY() - ours) < TRANSFORM_EPSILON) {
                        view.setTranslationY(0f);
                    }
                }
            }
        });
        slide.start();
    }

    /**
     * The entrance slide only runs on a keyboard view Gboard has not scaled or
     * shifted itself (for example in one-handed or floating mode).
     */
    static boolean canSlideKeyboardEntrance(
            float scaleX, float scaleY, float translationX, float translationY) {
        return Math.abs(scaleX - 1.0f) <= TRANSFORM_EPSILON
                && Math.abs(scaleY - 1.0f) <= TRANSFORM_EPSILON
                && Math.abs(translationX) <= TRANSFORM_EPSILON
                && Math.abs(translationY) <= TRANSFORM_EPSILON;
    }

    private static void animatePopupEntrance(View popup) {
        if (!popup.isAttachedToWindow() || popup.getVisibility() != View.VISIBLE
                || popup.getWidth() == 0 || popup.getHeight() == 0) {
            return;
        }
        // Leave stock scale animators (if present on a layout) undisturbed.
        if (Math.abs(popup.getScaleX() - 1.0f) > 0.01f
                || Math.abs(popup.getScaleY() - 1.0f) > 0.01f) {
            return;
        }
        popup.setPivotX(popup.getWidth() * 0.5f);
        popup.setPivotY(popup.getHeight());
        popup.setScaleX(POPUP_START_SCALE_X);
        popup.setScaleY(POPUP_START_SCALE_Y);
        popup.setTranslationY(dp(popup, POPUP_START_TRANSLATION_DP));
        popup.setAlpha(POPUP_START_ALPHA);
        popup.animate().cancel();
        popup.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationY(0f)
                .setDuration(POPUP_ANIM_DURATION_MS)
                .setInterpolator(M3ExpressiveMotion.spatialFast())
                .start();
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(popup, View.ALPHA, POPUP_START_ALPHA, 1.0f);
        alphaAnim.setDuration(M3ExpressiveMotion.DURATION_EFFECTS_FAST_MS);
        alphaAnim.setInterpolator(M3ExpressiveMotion.effectsFast());
        alphaAnim.start();
    }

    static float[] keyPressScale(int width, int height) {
        if (height > 0 && width > height * KEY_WIDE_ASPECT_THRESHOLD) {
            return new float[] {KEY_PRESS_SCALE_WIDE_X, KEY_PRESS_SCALE_WIDE_Y};
        }
        return new float[] {KEY_PRESS_SCALE_REGULAR, KEY_PRESS_SCALE_REGULAR};
    }

    private static void resetKeyScaleIfNeeded(View key) {
        if (key.getScaleX() != 1.0f || key.getScaleY() != 1.0f) {
            key.animate().cancel();
            key.setScaleX(1.0f);
            key.setScaleY(1.0f);
        }
    }

    // ---- Emoji row top divider -----------------------------------------------

    /**
     * Gboard outlines the emoji row (fast access bar) with a 1dp stroke whose
     * side edges are pushed off screen, leaving a line above and below the row.
     * Shifting that background up by the stroke width clips the top line away
     * and keeps the one between the emoji row and the toolbar.
     */
    private static void hideTopDivider(View emojiRow) {
        if (emojiRow == null || !emojiRow.isAttachedToWindow()) {
            return;
        }
        Drawable background = emojiRow.getBackground();
        if (background == null || background instanceof TopEdgeClippedDrawable) {
            return;
        }
        emojiRow.setBackground(new TopEdgeClippedDrawable(background, dp(emojiRow, 2)));
    }

    /** Marker wrapper so the emoji row background is only shifted once. */
    static final class TopEdgeClippedDrawable extends InsetDrawable {
        TopEdgeClippedDrawable(Drawable drawable, int clippedTop) {
            super(drawable, 0, -clippedTop, 0, 0);
        }
    }

    // ---- Toolbar spacing -----------------------------------------------------

    /**
     * Adds a top margin to the toolbar row while it is the first visible row,
     * so the sunburst keys sit clear of the rounded corners. Removed again when
     * the emoji row shows above it, gliding smoothly with a spring transition.
     */
    private static void spaceToolbarFromTop(View toolbarHolder, View menuKey, View micKey) {
        if (toolbarHolder == null || !toolbarHolder.isAttachedToWindow()
                || !(toolbarHolder.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                || !(toolbarHolder.getParent() instanceof ViewGroup)) {
            return;
        }
        // The emoji / GIF / sticker headers live in the same holder; spacing them
        // exposes a lighter strip of the keyboard background above the header.
        boolean hostsSunburstToolbar = isShownInside(menuKey, toolbarHolder)
                || isShownInside(micKey, toolbarHolder);
        int wanted = dp(toolbarHolder,
                toolbarTopSpacingDp(isFirstVisibleChild(toolbarHolder), hostsSunburstToolbar));
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) toolbarHolder.getLayoutParams();
        boolean initialized = Boolean.TRUE.equals(toolbarMarginInitialized.get(toolbarHolder));
        if (!initialized) {
            toolbarMarginInitialized.put(toolbarHolder, Boolean.TRUE);
        }
        if (params.topMargin != wanted) {
            int previousMargin = params.topMargin;
            params.topMargin = wanted;
            toolbarHolder.setLayoutParams(params);
            if (initialized && toolbarHolder.getHeight() > 0
                    && M3ExpressiveMotion.shouldAnimate(toolbarHolder.getContext())) {
                int delta = wanted - previousMargin;
                toolbarHolder.animate().cancel();
                toolbarHolder.setTranslationY(toolbarHolder.getTranslationY() - delta);
                toolbarHolder.animate()
                        .translationY(0f)
                        .setDuration(M3ExpressiveMotion.DURATION_SMOOTH_SPATIAL_MS)
                        .setInterpolator(M3ExpressiveMotion.spatialSmooth())
                        .start();
            }
        }
    }

    static int toolbarTopSpacingDp(boolean firstVisibleRow, boolean hostsSunburstToolbar) {
        return firstVisibleRow && hostsSunburstToolbar ? TOOLBAR_TOP_SPACING_DP : 0;
    }

    private static boolean isShownInside(View view, View ancestor) {
        if (view == null || !view.isShown()) {
            return false;
        }
        for (Object parent = view.getParent(); parent instanceof View;
                parent = ((View) parent).getParent()) {
            if (parent == ancestor) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFirstVisibleChild(View child) {
        ViewGroup parent = (ViewGroup) child.getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View sibling = parent.getChildAt(i);
            if (sibling == child) {
                return true;
            }
            if (sibling.getVisibility() == View.VISIBLE) {
                return false;
            }
        }
        return false;
    }

    private static View findViewByTag(View view, String tag) {
        if (tag.equals(view.getTag())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findViewByTag(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ---- Sunburst toolbar buttons -------------------------------------------

    /**
     * Puts the animated sunburst behind a toolbar key. Runs before every frame because
     * Gboard rebinds these keys (for example on ?123), so it only touches what
     * Gboard has reset; unchanged views are left alone to avoid redraw loops.
     */
    private static void styleSunburst(View button) {
        if (button == null || !button.isAttachedToWindow()
                || button.getVisibility() != View.VISIBLE) {
            return;
        }
        int[] colors = sunburstColors(button.getContext());
        if (!(button.getBackground() instanceof SunburstRipple)) {
            button.setBackground(new SunburstRipple(colors[0], button));
        }
        if (button instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) button;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearDecoration(group.getChildAt(i), colors[1]);
            }
        }
    }

    /** Drops the key's own circle so only the sunburst shows, and tints its icon. */
    private static void clearDecoration(View view, int iconColor) {
        if (view.getBackground() != null) {
            view.setBackground(null);
        }
        if (view instanceof ImageView) {
            ImageView icon = (ImageView) view;
            if (icon.getColorFilter() != null) {
                icon.clearColorFilter();
            }
            ColorStateList tint = icon.getImageTintList();
            if (tint == null || tint.isStateful() || tint.getDefaultColor() != iconColor) {
                icon.setImageTintList(ColorStateList.valueOf(iconColor));
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearDecoration(group.getChildAt(i), iconColor);
            }
        }
    }

    private static View findViewByIdName(View view, String idName) {
        int id = view.getId();
        if (id != View.NO_ID && (id >>> 24) != 0x01) {
            try {
                if (idName.equals(view.getResources().getResourceEntryName(id))) {
                    return view;
                }
            } catch (Resources.NotFoundException ignored) {
                // Generated ids have no entry name.
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findViewByIdName(group.getChildAt(i), idName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int[] sunburstColors(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                Resources resources = context.getResources();
                return new int[] {
                        resources.getColor(android.R.color.system_accent1_200, null),
                        resources.getColor(android.R.color.system_accent1_900, null),
                };
            } catch (Throwable ignored) {
                // Fall through to the fixed lavender pair.
            }
        }
        return new int[] {FALLBACK_SUNBURST_FILL, FALLBACK_SUNBURST_ICON};
    }

    /**
     * Sunburst fill with a ripple confined to the same shape. There is no
     * separate mask layer: the ripple is clipped to the animated fill itself,
     * so it squishes and morphs with the shape instead of showing a full-size
     * sunburst behind it.
     */
    static final class SunburstRipple extends RippleDrawable {
        SunburstRipple(int fillColor, View hostView) {
            super(ColorStateList.valueOf(RIPPLE_COLOR),
                    new SunburstDrawable(fillColor, hostView),
                    null);
        }
    }

    /** Wave depth the shape settles at: sunburst at rest, circle while active. */
    static float sunburstTargetDepth(boolean pressed, boolean activated) {
        if (activated) {
            return SUNBURST_ACTIVE_DEPTH;
        }
        return pressed ? SUNBURST_PRESSED_DEPTH : SUNBURST_DEPTH;
    }

    static float sunburstTargetScale(boolean pressed, boolean activated) {
        if (!pressed) {
            return 1.0f;
        }
        return activated ? SUNBURST_ACTIVE_PRESSED_SCALE : SUNBURST_PRESSED_SCALE;
    }

    /**
     * Filled 12-wave sunburst centred in the drawable bounds. Pressing squishes
     * it and softens the waves; while Gboard marks the key activated (features
     * menu open, dictation running) it springs into a full circle, and unfurls
     * back into a sunburst, turning one tooth, when that ends.
     */
    static final class SunburstDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final WeakReference<View> hostRef;

        private boolean pressed;
        private boolean activated;
        private float currentScale = 1.0f;
        private float currentDepth = SUNBURST_DEPTH;
        private float currentRotationDeg;
        private float lastBuiltDepth = -1f;
        private ValueAnimator activeAnimator;

        SunburstDrawable(int color, View hostView) {
            paint.setColor(color);
            this.hostRef = new WeakReference<View>(hostView);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] stateSet) {
            boolean nowPressed = hasState(stateSet, android.R.attr.state_pressed);
            boolean nowActivated = hasState(stateSet, android.R.attr.state_activated);
            if (nowPressed == pressed && nowActivated == activated) {
                return super.onStateChange(stateSet);
            }
            boolean pressChanged = nowPressed != pressed;
            boolean wasActivated = activated;
            pressed = nowPressed;
            activated = nowActivated;
            animateStateTransition(pressChanged, wasActivated);
            return true;
        }

        private void animateStateTransition(boolean pressChanged, boolean wasActivated) {
            View host = hostRef.get();
            Context context = host != null ? host.getContext() : null;
            float startScale = currentScale;
            float startDepth = currentDepth;
            float startRotation = currentRotationDeg;
            float targetScale = sunburstTargetScale(pressed, activated);
            float targetDepth = sunburstTargetDepth(pressed, activated);
            float targetRotation;
            if (pressed && pressChanged) {
                targetRotation = startRotation + (SUNBURST_STEP_DEGREES * 0.4f);
            } else if (activated) {
                // A circle looks the same at any angle; hold still.
                targetRotation = startRotation;
            } else {
                // Release, or unfurling from a circle: settle on the next tooth.
                targetRotation = nextAlignedRotation(startRotation);
            }
            boolean morphing = activated != wasActivated;

            if (activeAnimator != null) {
                activeAnimator.cancel();
                activeAnimator = null;
            }
            if (pressChanged) {
                animateHostIcons(host, pressed);
            }
            if (host == null || !M3ExpressiveMotion.shouldAnimate(context)) {
                currentScale = targetScale;
                currentDepth = targetDepth;
                currentRotationDeg = targetRotation % 360f;
                rebuildPathForCurrentBounds();
                invalidateSelf();
                return;
            }
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            if (pressed && pressChanged && !morphing) {
                animator.setDuration(M3ExpressiveMotion.DURATION_SUNBURST_PRESS_MS);
                animator.setInterpolator(M3ExpressiveMotion.pressIn());
            } else if (morphing) {
                animator.setDuration(M3ExpressiveMotion.DURATION_DEFAULT_SPATIAL_MS);
                animator.setInterpolator(M3ExpressiveMotion.spatialDefault());
            } else {
                animator.setDuration(M3ExpressiveMotion.DURATION_FAST_SPATIAL_MS);
                animator.setInterpolator(M3ExpressiveMotion.spatialFast());
            }
            animator.addUpdateListener(animation -> {
                float f = (Float) animation.getAnimatedValue();
                currentScale = startScale + (targetScale - startScale) * f;
                // Spring overshoot past a circle would invert the waves; clamp at flat.
                currentDepth = Math.max(0f, startDepth + (targetDepth - startDepth) * f);
                currentRotationDeg = startRotation + (targetRotation - startRotation) * f;
                rebuildPathForCurrentBounds();
                invalidateSelf();
            });
            activeAnimator = animator;
            animator.start();
        }

        private static void animateHostIcons(View host, boolean toPressed) {
            if (host == null || !host.isAttachedToWindow()) {
                return;
            }
            float target = toPressed ? 0.90f : 1.0f;
            if (host instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) host;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    animateIconView(child, target, toPressed);
                }
            }
        }

        private static void animateIconView(View view, float targetScale, boolean toPressed) {
            if (view instanceof ImageView) {
                view.animate().cancel();
                if (!M3ExpressiveMotion.shouldAnimate(view.getContext())) {
                    view.setScaleX(targetScale);
                    view.setScaleY(targetScale);
                    return;
                }
                view.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(toPressed
                                ? M3ExpressiveMotion.DURATION_SUNBURST_PRESS_MS
                                : M3ExpressiveMotion.DURATION_FAST_SPATIAL_MS)
                        .setInterpolator(toPressed
                                ? M3ExpressiveMotion.pressIn()
                                : M3ExpressiveMotion.spatialFast())
                        .start();
            } else if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    animateIconView(group.getChildAt(i), targetScale, toPressed);
                }
            }
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            lastBuiltDepth = -1f;
            rebuildPathForCurrentBounds();
        }

        private void rebuildPathForCurrentBounds() {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            if (Math.abs(currentDepth - lastBuiltDepth) < 0.0002f) {
                return;
            }
            lastBuiltDepth = currentDepth;
            buildSunburstPath(path, bounds.exactCenterX(), bounds.exactCenterY(),
                    Math.min(bounds.width(), bounds.height()) / 2f * SUNBURST_SCALE,
                    currentDepth);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            if (currentScale == 1.0f && currentRotationDeg == 0f) {
                canvas.drawPath(path, paint);
                return;
            }
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            canvas.save();
            if (currentRotationDeg != 0f) {
                canvas.rotate(currentRotationDeg, cx, cy);
            }
            if (currentScale != 1.0f) {
                canvas.scale(currentScale, currentScale, cx, cy);
            }
            canvas.drawPath(path, paint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static float nextAlignedRotation(float currentDeg) {
        return (float) (Math.ceil((currentDeg + 1.0f) / SUNBURST_STEP_DEGREES) * SUNBURST_STEP_DEGREES);
    }

    private static boolean hasState(int[] stateSet, int wanted) {
        if (stateSet == null) {
            return false;
        }
        for (int state : stateSet) {
            if (state == wanted) {
                return true;
            }
        }
        return false;
    }

    static void buildSunburstPath(Path path, float centerX, float centerY, float radius) {
        buildSunburstPath(path, centerX, centerY, radius, SUNBURST_DEPTH);
    }

    static void buildSunburstPath(Path path, float centerX, float centerY, float radius, float depth) {
        path.reset();
        for (int i = 0; i <= SUNBURST_PATH_STEPS; i++) {
            double theta = 2 * Math.PI * i / SUNBURST_PATH_STEPS;
            float r = sunburstRadius(radius, theta, depth);
            float x = centerX + (float) (r * Math.cos(theta - Math.PI / 2));
            float y = centerY + (float) (r * Math.sin(theta - Math.PI / 2));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    /** Outer radius at angle {@code theta}: peaks touch {@code radius}, valleys dip by 2 x depth. */
    static float sunburstRadius(float radius, double theta) {
        return sunburstRadius(radius, theta, SUNBURST_DEPTH);
    }

    static float sunburstRadius(float radius, double theta, float depth) {
        return (float) (radius * (1 - depth + depth * Math.cos(SUNBURST_WAVES * theta)));
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
