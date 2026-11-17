/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import java.io.File;
import java.util.ArrayDeque;
import java.util.function.IntConsumer;

/**
 * Full-screen sticker editor (M3 full-screen dialog): optional plain-background removal, a
 * manual eraser, rotation and cropping. Nothing is generated, only existing pixels are kept or
 * cleared.
 */
final class StickerEditorDialog {
    private static final String TAG = "PixelBoardStickers";
    private static final int MAX_UNDO = 20;
    private static final int DEFAULT_BRUSH_DP = 14;
    private static final int MAX_BRUSH_DP = 48;

    private StickerEditorDialog() {
    }

    /** Opens the editor for a new sticker ({@code existing} null) or an existing PNG sticker. */
    static void show(Activity activity, Bitmap source, File existing, Runnable onChanged) {
        StickerPalette palette = StickerPalette.of(activity);
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context context = dialog.getContext();
        int side = StickerUi.dp(context, 24);

        EditorView editor = new EditorView(context, source, palette);
        editor.brushRadiusPx = StickerUi.dp(context, DEFAULT_BRUSH_DP);
        Runnable close = () -> {
            if (editor.changed) {
                StickerUi.confirm(activity,
                        text(context, R.string.gboard_patches_stickers_discard_title),
                        text(context, R.string.gboard_patches_stickers_editor_cancel),
                        text(context, R.string.gboard_patches_stickers_discard),
                        dialog::dismiss);
            } else {
                dialog.dismiss();
            }
        };

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(palette.surfaceContainerHigh);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });

        // Header (56dp): close at the start, headline, Save as a text button at the end.
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(StickerUi.dp(context, 4), 0, StickerUi.dp(context, 12), 0);
        header.addView(StickerUi.iconButton(context, StickerIcons.CLOSE, 0, palette.onSurface,
                palette, text(context, R.string.gboard_patches_stickers_editor_cancel),
                v -> close.run()));
        TextView title = new TextView(context);
        title.setText(text(context, existing == null
                ? R.string.gboard_patches_stickers_editor_title_new
                : R.string.gboard_patches_stickers_editor_title_edit));
        StickerUi.titleLarge(title, palette.onSurface);
        title.setPadding(StickerUi.dp(context, 4), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(StickerUi.textButton(context,
                text(context, R.string.gboard_patches_stickers_editor_save), palette.primary,
                palette, v -> save(activity, editor, existing, dialog, onChanged)));
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                StickerUi.dp(context, 56)));

        TextView hint = new TextView(context);
        hint.setText(text(context, R.string.gboard_patches_stickers_editor_hint));
        StickerUi.bodyMedium(hint, palette.onSurfaceVariant);
        hint.setPadding(side, 0, side, StickerUi.dp(context, 8));
        root.addView(hint);

        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        editorParams.setMargins(side, 0, side, 0);
        editor.setBackgroundColor(palette.surfaceContainerHighest);
        root.addView(editor, editorParams);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(side, StickerUi.dp(context, 16), side, StickerUi.dp(context, 16));

        LinearLayout switchRow = new LinearLayout(context);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView switchLabel = new TextView(context);
        switchLabel.setText(text(context, R.string.gboard_patches_stickers_editor_remove_bg));
        StickerUi.bodyLarge(switchLabel, palette.onSurface);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch removeBackground = StickerUi.m3Switch(context, palette);
        removeBackground.setContentDescription(switchLabel.getText());
        removeBackground.setOnCheckedChangeListener((button, checked) -> {
            if (checked != editor.backgroundRemoved && !editor.setBackgroundRemoved(checked)) {
                button.setChecked(false);
                Toast.makeText(context,
                        text(context, R.string.gboard_patches_stickers_editor_no_plain_bg),
                        Toast.LENGTH_LONG).show();
            }
        });
        switchRow.addView(removeBackground);
        controls.addView(switchRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, StickerUi.dp(context, 56)));

        TextView brushLabel = new TextView(context);
        brushLabel.setText(text(context, R.string.gboard_patches_stickers_editor_brush));
        StickerUi.bodyMedium(brushLabel, palette.onSurfaceVariant);
        controls.addView(brushLabel);
        SeekBar brush = slider(context, palette);
        brush.setMax(MAX_BRUSH_DP - 2);
        brush.setProgress(DEFAULT_BRUSH_DP - 2);
        brush.setContentDescription(brushLabel.getText());
        onProgress(brush, progress -> editor.brushRadiusPx = StickerUi.dp(context, progress + 2));
        controls.addView(brush, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, StickerUi.dp(context, 48)));

        LinearLayout buttons = new LinearLayout(context);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.addView(StickerUi.tonalButton(context,
                text(context, R.string.gboard_patches_stickers_editor_undo), palette,
                v -> editor.undo()));
        buttons.addView(StickerUi.tonalButton(context,
                text(context, R.string.gboard_patches_stickers_editor_rotate), palette,
                v -> editor.rotate()), spaced(context));
        TextView crop = StickerUi.tonalButton(context,
                text(context, R.string.gboard_patches_stickers_editor_crop), palette, v -> {
                    if (editor.cropping) {
                        editor.applyCrop();
                    } else {
                        editor.startCrop();
                    }
                });
        buttons.addView(crop, spaced(context));
        buttons.addView(new View(context), new LinearLayout.LayoutParams(0, 0, 1f));
        if (existing != null) {
            buttons.addView(StickerUi.textButton(context,
                    text(context, R.string.gboard_patches_stickers_editor_delete), palette.error,
                    palette, v -> StickerUi.confirm(activity,
                            text(context, R.string.gboard_patches_stickers_delete_title),
                            text(context, R.string.gboard_patches_stickers_editor_cancel),
                            text(context, R.string.gboard_patches_stickers_editor_delete),
                            () -> {
                                existing.delete();
                                dialog.dismiss();
                                onChanged.run();
                            })));
        }
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsParams.topMargin = StickerUi.dp(context, 8);
        controls.addView(buttons, buttonsParams);
        root.addView(controls);

        // Keep labels and the switch in step with crop mode, undo and lost backgrounds.
        editor.onStateChanged = () -> {
            crop.setText(text(context, editor.cropping
                    ? R.string.gboard_patches_stickers_editor_apply_crop
                    : R.string.gboard_patches_stickers_editor_crop));
            hint.setText(text(context, editor.cropping
                    ? R.string.gboard_patches_stickers_editor_crop_hint
                    : R.string.gboard_patches_stickers_editor_hint));
            removeBackground.setChecked(editor.backgroundRemoved);
        };

        // Closing with unsaved changes asks first, so back must not dismiss directly.
        dialog.setCancelable(false);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                close.run();
                return true;
            }
            return false;
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            dialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, close::run);
        }

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(palette.surfaceContainerHigh));
            // Draw behind the status and navigation bars; root pads itself by their insets.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                boolean dark = (context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                window.getInsetsController().setSystemBarsAppearance(dark ? 0 : lightBars,
                        lightBars);
            }
        }
        dialog.show();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private static LinearLayout.LayoutParams spaced(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(StickerUi.dp(context, 8));
        return params;
    }

    private static void onProgress(SeekBar slider, IntConsumer listener) {
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                listener.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private static void save(Activity activity, EditorView editor, File existing, Dialog dialog,
            Runnable onChanged) {
        if (editor.cropping) {
            editor.applyCrop();
        }
        Bitmap result = editor.compose();
        if (result == null) {
            Toast.makeText(activity, text(activity, R.string.gboard_patches_stickers_editor_empty),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            StickerStore.savePng(activity, result, existing);
            dialog.dismiss();
            onChanged.run();
        } catch (Exception exception) {
            Log.w(TAG, "Failed to save sticker", exception);
            Toast.makeText(activity, String.valueOf(exception.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * M3 Expressive slider look: 16dp rounded track (primary active, secondary container
     * inactive) and a 4dp x 44dp handle.
     */
    private static SeekBar slider(Context context, StickerPalette palette) {
        SeekBar slider = new SeekBar(context);
        int track = StickerUi.dp(context, 16);
        GradientDrawable inactive = new GradientDrawable();
        inactive.setCornerRadius(track);
        inactive.setColor(palette.secondaryContainer);
        GradientDrawable active = new GradientDrawable();
        active.setCornerRadius(track);
        active.setColor(palette.primary);
        LayerDrawable progress = new LayerDrawable(new Drawable[] {
                inactive, new ClipDrawable(active, Gravity.START, ClipDrawable.HORIZONTAL) });
        progress.setId(0, android.R.id.background);
        progress.setId(1, android.R.id.progress);
        slider.setProgressDrawable(progress);
        GradientDrawable handle = new GradientDrawable();
        handle.setCornerRadius(StickerUi.dp(context, 2));
        handle.setColor(palette.primary);
        handle.setSize(StickerUi.dp(context, 4), StickerUi.dp(context, 44));
        slider.setThumb(handle);
        slider.setSplitTrack(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            slider.setMinHeight(track);
            slider.setMaxHeight(track);
        }
        return slider;
    }

    private static String text(Context context, int resId) {
        return GboardSettingsText.get(context, resId);
    }

    /** One undo step: the picture and the eraser mask. Pictures are never edited in place. */
    private static final class Snapshot {
        final Bitmap picture;
        final Bitmap mask;

        Snapshot(Bitmap picture, Bitmap mask) {
            this.picture = picture;
            this.mask = mask;
        }
    }

    /**
     * Shows the sticker over a checkerboard. Dragging erases into a mask; in crop mode it moves
     * the crop frame or its corners instead.
     */
    private static final class EditorView extends View {
        private static final int NO_CORNER = -1;
        private static final int MOVE_FRAME = 4;

        private final ArrayDeque<Snapshot> undo = new ArrayDeque<Snapshot>();
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint erase = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint image = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint checker = new Paint();
        private final Paint scrim = new Paint();
        private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix toView = new Matrix();
        private final Matrix toImage = new Matrix();
        private final RectF imageRect = new RectF();
        private final RectF cropRect = new RectF();
        private final Path clip = new Path();
        private final float density;
        private Bitmap picture;
        private Bitmap base;
        private Bitmap mask;
        private Canvas maskCanvas;
        private float lastX;
        private float lastY;
        private int dragging = NO_CORNER;
        float brushRadiusPx;
        boolean changed;
        boolean cropping;
        boolean backgroundRemoved;
        Runnable onStateChanged = () -> { };

        EditorView(Context context, Bitmap source, StickerPalette palette) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            picture = source;
            base = source;
            mask = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ALPHA_8);
            maskCanvas = new Canvas(mask);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            scrim.setColor(0x99000000);
            frame.setStyle(Paint.Style.STROKE);
            frame.setStrokeWidth(2 * density);
            frame.setColor(Color.WHITE);
            handle.setColor(palette.primary);

            Bitmap tile = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            tile.setPixel(0, 0, Color.LTGRAY);
            tile.setPixel(1, 1, Color.LTGRAY);
            tile.setPixel(0, 1, Color.WHITE);
            tile.setPixel(1, 0, Color.WHITE);
            BitmapShader shader = new BitmapShader(tile, Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT);
            Matrix cell = new Matrix();
            cell.setScale(8 * density, 8 * density);
            shader.setLocalMatrix(cell);
            checker.setShader(shader);
        }

        /** Returns false when removal was asked for but the image has no plain background. */
        boolean setBackgroundRemoved(boolean removed) {
            boolean ok = deriveBase(removed);
            changed = true;
            invalidate();
            return ok;
        }

        // The background-removed image is always derived from the picture, never undone itself.
        private boolean deriveBase(boolean removed) {
            backgroundRemoved = false;
            base = picture;
            if (!removed) {
                return true;
            }
            int width = picture.getWidth();
            int height = picture.getHeight();
            int[] pixels = new int[width * height];
            picture.getPixels(pixels, 0, width, 0, 0, width, height);
            if (!StickerPixels.removePlainBackground(pixels, width, height)) {
                return false;
            }
            base = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            backgroundRemoved = true;
            return true;
        }

        void rotate() {
            if (cropping) {
                return;
            }
            pushUndo();
            Matrix turn = new Matrix();
            turn.setRotate(90);
            setPicture(Bitmap.createBitmap(picture, 0, 0, picture.getWidth(),
                    picture.getHeight(), turn, true),
                    Bitmap.createBitmap(mask, 0, 0, mask.getWidth(), mask.getHeight(), turn, false));
        }

        void startCrop() {
            cropping = true;
            cropRect.set(0, 0, picture.getWidth(), picture.getHeight());
            onStateChanged.run();
            invalidate();
        }

        void applyCrop() {
            cropping = false;
            int left = Math.round(cropRect.left);
            int top = Math.round(cropRect.top);
            int width = Math.round(cropRect.right) - left;
            int height = Math.round(cropRect.bottom) - top;
            if (width < picture.getWidth() || height < picture.getHeight()) {
                pushUndo();
                setPicture(Bitmap.createBitmap(picture, left, top, width, height),
                        Bitmap.createBitmap(mask, left, top, width, height));
            } else {
                onStateChanged.run();
                invalidate();
            }
        }

        void undo() {
            if (cropping) {
                // Undo while cropping just leaves crop mode.
                cropping = false;
                onStateChanged.run();
                invalidate();
                return;
            }
            if (undo.isEmpty()) {
                return;
            }
            Snapshot step = undo.pop();
            picture = step.picture;
            mask = step.mask;
            maskCanvas = new Canvas(mask);
            deriveBase(backgroundRemoved);
            fitImage(getWidth(), getHeight());
            onStateChanged.run();
            invalidate();
        }

        private void pushUndo() {
            if (undo.size() == MAX_UNDO) {
                undo.removeLast();
            }
            undo.push(new Snapshot(picture, mask.copy(Bitmap.Config.ALPHA_8, true)));
            changed = true;
        }

        private void setPicture(Bitmap newPicture, Bitmap newMask) {
            picture = newPicture;
            mask = newMask.isMutable() && newMask.getConfig() == Bitmap.Config.ALPHA_8
                    ? newMask : newMask.copy(Bitmap.Config.ALPHA_8, true);
            maskCanvas = new Canvas(mask);
            deriveBase(backgroundRemoved);
            fitImage(getWidth(), getHeight());
            onStateChanged.run();
            invalidate();
        }

        /** The edited sticker trimmed to its visible pixels, or null if nothing is left. */
        Bitmap compose() {
            int width = base.getWidth();
            int height = base.getHeight();
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawBitmap(base, 0, 0, null);
            canvas.drawBitmap(mask, 0, 0, erase);
            int[] pixels = new int[width * height];
            out.getPixels(pixels, 0, width, 0, 0, width, height);
            int[] bounds = StickerPixels.opaqueBounds(pixels, width, height);
            if (bounds == null) {
                return null;
            }
            return Bitmap.createBitmap(out, bounds[0], bounds[1],
                    bounds[2] - bounds[0], bounds[3] - bounds[1]);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            fitImage(width, height);
        }

        private void fitImage(int width, int height) {
            int inset = Math.round(16 * density);
            toView.setRectToRect(new RectF(0, 0, picture.getWidth(), picture.getHeight()),
                    new RectF(inset, inset, width - inset, height - inset),
                    Matrix.ScaleToFit.CENTER);
            toView.invert(toImage);
            imageRect.set(0, 0, picture.getWidth(), picture.getHeight());
            toView.mapRect(imageRect);
            // Image area: CornerLarge (16dp) like the rest of the M3 containers here.
            float radius = 16 * density;
            clip.reset();
            clip.addRoundRect(0, 0, width, height, radius, radius, Path.Direction.CW);
        }

        @Override
        public void draw(Canvas canvas) {
            int save = canvas.save();
            canvas.clipPath(clip);
            super.draw(canvas);
            canvas.restoreToCount(save);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawRect(imageRect, checker);
            int layer = canvas.saveLayer(imageRect, null);
            canvas.drawBitmap(base, toView, image);
            canvas.drawBitmap(mask, toView, erase);
            canvas.restoreToCount(layer);
            if (!cropping) {
                return;
            }
            RectF frameRect = new RectF(cropRect);
            toView.mapRect(frameRect);
            // Dim everything outside the frame, then outline it with corner handles.
            canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, frameRect.top, scrim);
            canvas.drawRect(imageRect.left, frameRect.bottom, imageRect.right, imageRect.bottom,
                    scrim);
            canvas.drawRect(imageRect.left, frameRect.top, frameRect.left, frameRect.bottom, scrim);
            canvas.drawRect(frameRect.right, frameRect.top, imageRect.right, frameRect.bottom,
                    scrim);
            canvas.drawRect(frameRect, frame);
            float radius = 8 * density;
            for (int corner = 0; corner < 4; corner++) {
                float[] point = corner(frameRect, corner);
                canvas.drawCircle(point[0], point[1], radius, handle);
                canvas.drawCircle(point[0], point[1], radius, frame);
            }
        }

        // Corners in order: top-left, top-right, bottom-right, bottom-left.
        private static float[] corner(RectF rect, int corner) {
            return new float[] {
                    corner == 0 || corner == 3 ? rect.left : rect.right,
                    corner < 2 ? rect.top : rect.bottom };
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return cropping ? onCropTouch(event) : onEraseTouch(event);
        }

        private boolean onCropTouch(MotionEvent event) {
            float[] point = { event.getX(), event.getY() };
            toImage.mapPoints(point);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    dragging = NO_CORNER;
                    RectF frameRect = new RectF(cropRect);
                    toView.mapRect(frameRect);
                    float reach = 32 * density;
                    for (int corner = 0; corner < 4; corner++) {
                        float[] at = corner(frameRect, corner);
                        if (Math.hypot(at[0] - event.getX(), at[1] - event.getY()) <= reach) {
                            dragging = corner;
                        }
                    }
                    if (dragging == NO_CORNER && cropRect.contains(point[0], point[1])) {
                        dragging = MOVE_FRAME;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    dragFrame(point[0], point[1]);
                    invalidate();
                    break;
                default:
                    break;
            }
            lastX = point[0];
            lastY = point[1];
            return true;
        }

        private void dragFrame(float x, float y) {
            float width = picture.getWidth();
            float height = picture.getHeight();
            float min = Math.min(16, Math.min(width, height));
            if (dragging == MOVE_FRAME) {
                float dx = Math.max(-cropRect.left, Math.min(width - cropRect.right, x - lastX));
                float dy = Math.max(-cropRect.top, Math.min(height - cropRect.bottom, y - lastY));
                cropRect.offset(dx, dy);
                return;
            }
            if (dragging == NO_CORNER) {
                return;
            }
            x = Math.max(0, Math.min(width, x));
            y = Math.max(0, Math.min(height, y));
            if (dragging == 0 || dragging == 3) {
                cropRect.left = Math.min(x, cropRect.right - min);
            } else {
                cropRect.right = Math.max(x, cropRect.left + min);
            }
            if (dragging < 2) {
                cropRect.top = Math.min(y, cropRect.bottom - min);
            } else {
                cropRect.bottom = Math.max(y, cropRect.top + min);
            }
        }

        private boolean onEraseTouch(MotionEvent event) {
            float[] point = { event.getX(), event.getY() };
            toImage.mapPoints(point);
            float radius = toImage.mapRadius(brushRadiusPx);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    pushUndo();
                    maskCanvas.drawCircle(point[0], point[1], radius, dot);
                    break;
                case MotionEvent.ACTION_MOVE:
                    stroke.setStrokeWidth(radius * 2);
                    maskCanvas.drawLine(lastX, lastY, point[0], point[1], stroke);
                    break;
                default:
                    return true;
            }
            lastX = point[0];
            lastY = point[1];
            invalidate();
            return true;
        }
    }
}
