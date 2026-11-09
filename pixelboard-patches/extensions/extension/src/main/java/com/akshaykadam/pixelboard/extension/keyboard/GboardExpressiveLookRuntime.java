/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.keyboard;

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
 * Material 3 Expressive look (issue #133): sunburst-shaped toolbar buttons and
 * no divider line above the emoji row at the rounded keyboard top.
 *
 * <p>Called from Gboard's top-rounded keyboard module for every keyboard view
 * it outlines. Every step is best effort and leaves the stock look in place
 * when the expected views are not found.</p>
 */
public final class GboardExpressiveLookRuntime {
    private static final String TAG = "GboardPatches";

    static final int SUNBURST_WAVES = 12;
    static final float SUNBURST_DEPTH = 0.04f;
    /** Sunburst diameter relative to the key; matches Gboard's own 34dp icon circle. */
    static final float SUNBURST_SCALE = 0.78f;
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
                spaceToolbarFromTop(toolbarHolder.get());
            } catch (Throwable throwable) {
                Log.w(TAG, "[expressive] pre-draw failed", throwable);
            }
            return true;
        }
    }

    private static final Map<View, Targets> targetsByRoot =
            Collections.synchronizedMap(new WeakHashMap<View, Targets>());
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
     * the emoji row shows above it. Only changes the layout when needed.
     */
    private static void spaceToolbarFromTop(View toolbarHolder) {
        if (toolbarHolder == null || !toolbarHolder.isAttachedToWindow()
                || !(toolbarHolder.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                || !(toolbarHolder.getParent() instanceof ViewGroup)) {
            return;
        }
        int wanted = isFirstVisibleChild(toolbarHolder) ? dp(toolbarHolder, TOOLBAR_TOP_SPACING_DP) : 0;
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) toolbarHolder.getLayoutParams();
        if (params.topMargin != wanted) {
            params.topMargin = wanted;
            toolbarHolder.setLayoutParams(params);
        }
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
     * Puts the sunburst behind a toolbar key. Runs before every frame because
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
            button.setBackground(new SunburstRipple(colors[0]));
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

    /** Sunburst fill with a ripple confined to the same shape. */
    static final class SunburstRipple extends RippleDrawable {
        SunburstRipple(int fillColor) {
            super(ColorStateList.valueOf(RIPPLE_COLOR),
                    new SunburstDrawable(fillColor),
                    new SunburstDrawable(0xFFFFFFFF));
        }
    }

    /** Filled 12-wave sunburst centred in the drawable bounds. */
    static final class SunburstDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        SunburstDrawable(int color) {
            paint.setColor(color);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            buildSunburstPath(path, bounds.exactCenterX(), bounds.exactCenterY(),
                    Math.min(bounds.width(), bounds.height()) / 2f * SUNBURST_SCALE);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawPath(path, paint);
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

    static void buildSunburstPath(Path path, float centerX, float centerY, float radius) {
        path.reset();
        for (int i = 0; i <= SUNBURST_PATH_STEPS; i++) {
            double theta = 2 * Math.PI * i / SUNBURST_PATH_STEPS;
            float r = sunburstRadius(radius, theta);
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
        return (float) (radius * (1 - SUNBURST_DEPTH + SUNBURST_DEPTH * Math.cos(SUNBURST_WAVES * theta)));
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
