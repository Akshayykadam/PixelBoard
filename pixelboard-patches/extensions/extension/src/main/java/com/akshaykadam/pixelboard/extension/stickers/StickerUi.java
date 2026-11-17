/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * The few Material 3 components the sticker screens need, built from plain views with the
 * published M3 measurements (the extension has no Material Components library).
 */
final class StickerUi {
    // State layer opacity for pressed (M3: 10%).
    private static final int PRESSED_ALPHA = 0x1A;

    private StickerUi() {
    }

    /** Small icon button: 40dp round container, 24dp icon, 48dp touch target. */
    static View iconButton(Context context, int glyph, int containerColor, int iconColor,
            StickerPalette palette, String description, View.OnClickListener listener) {
        FrameLayout target = new FrameLayout(context);
        target.setContentDescription(description);
        target.setOnClickListener(listener);
        View container = new View(context);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(containerColor);
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(0xFFFFFFFF);
        container.setBackground(new RippleDrawable(pressed(palette), circle, mask));
        container.setDuplicateParentStateEnabled(true);
        target.addView(container, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40),
                Gravity.CENTER));
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(new StickerIcons(glyph));
        icon.setImageTintList(ColorStateList.valueOf(iconColor));
        target.addView(icon, new FrameLayout.LayoutParams(dp(context, 24), dp(context, 24),
                Gravity.CENTER));
        target.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)));
        return target;
    }

    /**
     * Switch: 52x32dp track; off = surface container highest with a 2dp outline and a 16dp
     * outline-coloured handle, on = primary track with a 24dp on-primary handle, 28dp pressed.
     */
    static Switch m3Switch(Context context, StickerPalette palette) {
        Switch view = new Switch(context);
        int width = dp(context, 52);
        int height = dp(context, 32);
        GradientDrawable on = new GradientDrawable();
        on.setCornerRadius(height);
        on.setColor(palette.primary);
        on.setSize(width, height);
        GradientDrawable off = new GradientDrawable();
        off.setCornerRadius(height);
        off.setColor(palette.surfaceContainerHighest);
        off.setStroke(dp(context, 2), palette.outline);
        off.setSize(width, height);
        StateListDrawable track = new StateListDrawable();
        track.addState(new int[] { android.R.attr.state_checked }, on);
        track.addState(new int[0], off);
        view.setTrackDrawable(track);
        view.setThumbDrawable(new SwitchHandle(context, palette));
        view.setTrackTintList(null);
        view.setThumbTintList(null);
        view.setShowText(false);
        view.setSwitchMinWidth(width);
        return view;
    }

    /** Text button: label large in primary, 40dp tall, 12dp side padding. */
    static TextView textButton(Context context, String label, int color, StickerPalette palette,
            View.OnClickListener listener) {
        return button(context, label, color, 0, 0, dp(context, 12), palette, listener);
    }

    /** Tonal button: secondary container fill, 40dp tall, 16dp side padding, full corners. */
    static TextView tonalButton(Context context, String label, StickerPalette palette,
            View.OnClickListener listener) {
        return button(context, label, palette.onSecondaryContainer, palette.secondaryContainer, 0,
                dp(context, 16), palette, listener);
    }

    /** Outlined button: 1dp outline border, on surface variant label. */
    static TextView outlinedButton(Context context, String label, StickerPalette palette,
            View.OnClickListener listener) {
        return button(context, label, palette.onSurfaceVariant, 0, palette.outline,
                dp(context, 16), palette, listener);
    }

    private static TextView button(Context context, String label, int textColor, int fill,
            int stroke, int sidePadding, StickerPalette palette, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(label);
        labelLarge(button, textColor);
        button.setGravity(Gravity.CENTER);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setMinHeight(dp(context, 40));
        button.setPadding(sidePadding, 0, sidePadding, 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 100));
        shape.setColor(fill);
        if (stroke != 0) {
            shape.setStroke(dp(context, 1), stroke);
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(context, 100));
        mask.setColor(0xFFFFFFFF);
        button.setBackground(new RippleDrawable(pressed(palette), shape, mask));
        button.setOnClickListener(listener);
        return button;
    }

    /**
     * Basic dialog with a headline and two text buttons: surface container high, 28dp corners,
     * 24dp padding, confirming action at the trailing edge.
     */
    static void confirm(Context context, String headline, String dismissLabel,
            String confirmLabel, Runnable onConfirm) {
        StickerPalette palette = StickerPalette.of(context);
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 24);
        container.setPadding(padding, padding, padding, padding);
        container.setMinimumWidth(dp(context, 280));
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 28));
        shape.setColor(palette.surfaceContainerHigh);
        container.setBackground(shape);

        TextView title = new TextView(context);
        title.setText(headline);
        headlineSmall(title, palette.onSurface);
        container.addView(title);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.END);
        actions.addView(textButton(context, dismissLabel, palette.primary, palette,
                v -> dialog.dismiss()));
        TextView confirm = textButton(context, confirmLabel, palette.primary, palette, v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        confirmParams.setMarginStart(dp(context, 8));
        actions.addView(confirm, confirmParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(context, 24);
        container.addView(actions, actionsParams);

        dialog.setContentView(container);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        dialog.show();
    }

    // Gboard's theme centres text by default; M3 text here is start-aligned.
    private static void startAligned(TextView view) {
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    static void headlineSmall(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        view.setLineHeight(sp(view.getContext(), 32));
    }

    static void titleLarge(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        view.setLineHeight(sp(view.getContext(), 28));
    }

    static void titleMedium(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        view.setLineHeight(sp(view.getContext(), 24));
        view.setLetterSpacing(0.2f / 16f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    static void bodyLarge(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        view.setLineHeight(sp(view.getContext(), 24));
    }

    static void bodyMedium(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setLineHeight(sp(view.getContext(), 20));
        view.setLetterSpacing(0.2f / 14f);
    }

    static void labelLarge(TextView view, int color) {
        view.setTextColor(color);
        startAligned(view);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setLineHeight(sp(view.getContext(), 20));
        view.setLetterSpacing(0.1f / 14f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    /**
     * Round switch handle sized by state. Half the track wide, so the framework Switch keeps
     * the track at 52dp (it sizes the track to twice the handle).
     */
    private static final class SwitchHandle extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final StickerPalette palette;
        private final float density;
        private boolean checked;
        private boolean pressed;

        SwitchHandle(Context context, StickerPalette palette) {
            this.palette = palette;
            density = context.getResources().getDisplayMetrics().density;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float radius = (pressed ? 14 : checked ? 12 : 8) * density;
            paint.setColor(checked ? palette.onPrimary : palette.outline);
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean newChecked = false;
            boolean newPressed = false;
            for (int value : state) {
                newChecked |= value == android.R.attr.state_checked;
                newPressed |= value == android.R.attr.state_pressed;
            }
            boolean changed = newChecked != checked || newPressed != pressed;
            checked = newChecked;
            pressed = newPressed;
            if (changed) {
                invalidateSelf();
            }
            return changed;
        }

        @Override
        public int getIntrinsicWidth() {
            return Math.round(26 * density);
        }

        @Override
        public int getIntrinsicHeight() {
            return Math.round(32 * density);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static ColorStateList pressed(StickerPalette palette) {
        return ColorStateList.valueOf(StickerPalette.withAlpha(palette.onSurface, PRESSED_ALPHA));
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static int sp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                context.getResources().getDisplayMetrics()));
    }
}
