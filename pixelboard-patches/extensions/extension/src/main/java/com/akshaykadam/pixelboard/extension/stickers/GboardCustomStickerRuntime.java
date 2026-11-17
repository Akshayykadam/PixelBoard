/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Adds a Stickers button next to Gboard's header access points (Sticker, GIF, Clipboard).
 * It opens a sticker panel over the keys, like the GIF keyboard; tapping a sticker commits it.
 */
public final class GboardCustomStickerRuntime {
    private static final String TAG = "PixelBoardStickers";
    private static final String BUTTON_TAG = "pixelboard_stickers_button";
    private static final String PANEL_TAG = "pixelboard_stickers_panel";
    private static final int COLUMNS = 4;
    // How long "Save a Gboard GIF" waits for the user to pick one.
    private static final long CAPTURE_WINDOW_MS = 120_000;

    private static volatile long captureUntil;
    private static volatile Context appContext;
    private static volatile WeakReference<View> captureAnchor = new WeakReference<View>(null);
    private static final String WHATSAPP_STICKER_TYPE = "image/webp.wasticker";
    private static final long MESSAGE_DURATION_MS = 3000;

    private GboardCustomStickerRuntime() {
    }

    /** Called when Gboard's header access points bar inflates or changes visibility. */
    public static void onAccessPointsBarChanged(View bar) {
        // Inflation runs before the bar is attached to its parent, so wait for the next frame.
        bar.post(() -> {
            try {
                addHeaderButton(bar);
            } catch (Throwable throwable) {
                Log.w(TAG, "Failed to add Stickers header button", throwable);
            }
        });
    }

    /**
     * Called (on a background thread) for every GIF or sticker Gboard is about to send. While a
     * capture is armed, the next one is saved as a custom sticker instead: returns null so Gboard
     * treats it as handled. Otherwise returns the content unchanged so it is sent.
     */
    public static InputContentInfo interceptCommitContent(InputContentInfo content) {
        if (content == null || System.currentTimeMillis() > captureUntil) {
            return content;
        }
        captureUntil = 0;
        Context context = appContext;
        boolean saved = false;
        try {
            String mimeType = content.getDescription().getMimeTypeCount() > 0
                    ? content.getDescription().getMimeType(0) : "";
            byte[] data;
            try (InputStream in = context.getContentResolver()
                    .openInputStream(content.getContentUri())) {
                data = readAll(in);
            }
            if (StickerStore.isGifData(data) || "image/gif".equals(mimeType)) {
                StickerStore.saveGif(context, data);
                saved = true;
            } else {
                Bitmap bitmap = StickerStore.decodeForEditing(data);
                if (bitmap != null) {
                    StickerStore.savePng(context, bitmap, null);
                    saved = true;
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to save Gboard content as a sticker", throwable);
        }
        int message = saved ? R.string.gboard_patches_stickers_capture_saved
                : R.string.gboard_patches_stickers_capture_failed;
        new Handler(Looper.getMainLooper()).post(() -> {
            View anchor = captureAnchor.get();
            if (anchor != null && anchor.isAttachedToWindow()) {
                showMessage(anchor, text(context, message));
            } else {
                Toast.makeText(context, text(context, message), Toast.LENGTH_SHORT).show();
            }
        });
        return null;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null) {
            throw new IOException("Content not readable");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        for (int read; (read = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /** Called when the features menu (four squares) opens or closes: it replaces our panel. */
    public static void onFeaturesMenuChanged(View panel) {
        if (panel.isShown()) {
            closePanel(panel);
        }
    }

    // The bar sits in a FrameLayout inside the header row; our button goes right after it,
    // before the microphone key.
    private static void addHeaderButton(View bar) {
        if (!(bar.getParent() instanceof View)
                || !(((View) bar.getParent()).getParent() instanceof LinearLayout)) {
            return;
        }
        View barSlot = (View) bar.getParent();
        LinearLayout row = (LinearLayout) barSlot.getParent();
        View existing = row.findViewWithTag(BUTTON_TAG);
        boolean enabled = StickerStore.isHeaderButtonEnabled(row.getContext());
        if (existing != null && !enabled) {
            row.removeView(existing);
        }
        if (existing != null || !enabled) {
            return;
        }
        Context context = row.getContext();
        View nativeItem = bar instanceof ViewGroup && ((ViewGroup) bar).getChildCount() > 0
                ? ((ViewGroup) bar).getChildAt(0) : null;
        int itemWidth = nativeItem != null && nativeItem.getWidth() > 0
                ? nativeItem.getWidth() : dp(context, 56);
        ImageView nativeIcon = nativeItem != null ? firstImageView(nativeItem) : null;
        int iconSize = nativeIcon != null && nativeIcon.getWidth() > 0
                ? nativeIcon.getWidth() : dp(context, 24);
        StickerPalette palette = StickerPalette.of(context);
        ColorStateList iconTint = nativeIcon != null && nativeIcon.getImageTintList() != null
                ? nativeIcon.getImageTintList()
                : ColorStateList.valueOf(palette.onSurfaceVariant);

        FrameLayout button = new FrameLayout(context);
        button.setTag(BUTTON_TAG);
        button.setContentDescription(text(context, R.string.gboard_patches_stickers_keyboard_button));
        // Pill behind the icon, sized like Gboard's own: the press ripple stays inside it, and it
        // fills with secondary container while the panel is open (like a selected item).
        View indicator = new View(context);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(context, 100));
        pill.setColor(0);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(context, 100));
        mask.setColor(0xFFFFFFFF);
        indicator.setBackground(new RippleDrawable(
                StickerUi.pressed(palette), pill, mask));
        indicator.setDuplicateParentStateEnabled(true);
        button.addView(indicator, new FrameLayout.LayoutParams(
                Math.round(iconSize * 2.4f), Math.round(iconSize * 1.33f), Gravity.CENTER));
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(new StickerIcons(StickerIcons.STICKER));
        icon.setImageTintList(iconTint);
        button.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        button.setOnClickListener(v -> {
            if (!closePanel(v)) {
                openPanel(row, pill, icon, iconTint, palette);
            }
        });
        row.addView(button, row.indexOfChild(barSlot) + 1,
                new LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static boolean closePanel(View anyKeyboardView) {
        View panel = anyKeyboardView.getRootView().findViewWithTag(PANEL_TAG);
        if (panel == null) {
            return false;
        }
        ((ViewGroup) panel.getParent()).removeView(panel);
        return true;
    }

    /** Covers the keyboard body below the header, the way the GIF keyboard replaces the keys. */
    private static void openPanel(View headerRow, GradientDrawable pill, ImageView icon,
            ColorStateList iconTint, StickerPalette palette) {
        Context context = headerRow.getContext();
        View root = headerRow.getRootView();
        View header = findById(root, "keyboard_header_view_holder");
        View holder = findById(root, "keyboard_holder");
        FrameLayout host = keyboardHost(headerRow);
        if (header == null || host == null) {
            Log.w(TAG, "Keyboard layout not recognised, cannot open stickers");
            return;
        }
        int[] hostLocation = new int[2];
        int[] headerLocation = new int[2];
        host.getLocationInWindow(hostLocation);
        header.getLocationInWindow(headerLocation);
        int top = headerLocation[1] + header.getHeight() - hostLocation[1];

        // The keyboard body (keys, GIF keyboard, ...) is the holder child after the header.
        ViewGroup body = holder instanceof ViewGroup ? (ViewGroup) holder : null;
        View bodyContent = null;
        if (body != null) {
            int headerIndex = body.indexOfChild(header);
            View bodySlot = headerIndex >= 0 ? body.getChildAt(headerIndex + 1) : null;
            if (bodySlot instanceof ViewGroup && ((ViewGroup) bodySlot).getChildCount() > 0) {
                bodyContent = ((ViewGroup) bodySlot).getChildAt(0);
            }
        }

        StickerPanel panel = new StickerPanel(context, pill, icon, iconTint, palette,
                bodyContent);
        panel.setBackground(keyboardBackground(host));
        panel.addView(buildPanelContent(context, palette, host));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.topMargin = top;
        params.bottomMargin = host.getHeight() - (holder.getHeight()
                + ((View) holder.getParent()).getTop() + holder.getTop());
        host.addView(panel, params);
    }

    /** The FrameLayout spanning the whole keyboard (header and body), or null. */
    private static FrameLayout keyboardHost(View anyKeyboardView) {
        View holder = findById(anyKeyboardView.getRootView(), "keyboard_holder");
        if (holder == null || !(holder.getParent() instanceof View)
                || !(((View) holder.getParent()).getParent() instanceof FrameLayout)) {
            return null;
        }
        return (FrameLayout) ((View) holder.getParent()).getParent();
    }

    /**
     * Snackbar inside the keyboard: Android hides toasts from the keyboard process. Colours are
     * the inverse pair (dark container in light theme), corner 4dp, at least 48dp tall.
     */
    private static void showMessage(View anyKeyboardView, String message) {
        FrameLayout host = keyboardHost(anyKeyboardView);
        Context context = anyKeyboardView.getContext();
        if (host == null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            return;
        }
        StickerPalette palette = StickerPalette.of(context);
        TextView snackbar = new TextView(context);
        snackbar.setText(message);
        StickerUi.bodyMedium(snackbar, palette.surfaceContainer);
        snackbar.setMinHeight(dp(context, 48));
        snackbar.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 4));
        shape.setColor(palette.onSurface);
        snackbar.setBackground(shape);
        snackbar.setElevation(dp(context, 6));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        int margin = dp(context, 12);
        params.setMargins(margin, margin, margin, margin);
        host.addView(snackbar, params);
        snackbar.postDelayed(() -> host.removeView(snackbar), MESSAGE_DURATION_MS);
    }

    private static View buildPanelContent(Context context, StickerPalette palette, View host) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Header: title (Title Medium, on surface), add (tonal icon button), close (standard).
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(context, 16), dp(context, 4), dp(context, 4), dp(context, 4));
        TextView title = new TextView(context);
        title.setText(text(context, R.string.gboard_patches_stickers_keyboard_button));
        StickerUi.titleMedium(title, palette.onSurface);
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(StickerUi.iconButton(context, StickerIcons.ADD, palette.secondaryContainer,
                palette.onSecondaryContainer, palette,
                text(context, R.string.gboard_patches_stickers_keyboard_add),
                v -> openSettings(context)));
        header.addView(StickerUi.iconButton(context, StickerIcons.CLOSE, 0,
                palette.onSurfaceVariant, palette,
                text(context, R.string.gboard_patches_stickers_keyboard_close),
                v -> closePanel(host)));
        content.addView(header);

        // Arms a one-shot capture: the next GIF or sticker picked in Gboard is saved, not sent.
        LinearLayout captureRow = new LinearLayout(context);
        captureRow.setPadding(dp(context, 12), 0, dp(context, 12), dp(context, 8));
        captureRow.addView(StickerUi.outlinedButton(context,
                text(context, R.string.gboard_patches_stickers_capture_button), palette, v -> {
                    appContext = context.getApplicationContext();
                    captureAnchor = new WeakReference<View>(host);
                    captureUntil = System.currentTimeMillis() + CAPTURE_WINDOW_MS;
                    closePanel(host);
                    showMessage(host, text(context, R.string.gboard_patches_stickers_capture_hint));
                }));
        content.addView(captureRow);

        List<File> stickers = StickerStore.list(context);
        if (stickers.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(text(context, R.string.gboard_patches_stickers_keyboard_empty));
            StickerUi.bodyMedium(empty, palette.onSurfaceVariant);
            empty.setGravity(Gravity.CENTER);
            empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            empty.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 24));
            content.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            return content;
        }

        int gap = dp(context, 8);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(context, 12), 0, dp(context, 12), dp(context, 12));
        LinearLayout row = null;
        for (int i = 0; i < stickers.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(context);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = i == 0 ? 0 : gap;
                rows.addView(row, rowParams);
            }
            LinearLayout.LayoutParams tileParams =
                    new LinearLayout.LayoutParams(0, dp(context, 80), 1f);
            tileParams.setMarginStart(i % COLUMNS == 0 ? 0 : gap);
            row.addView(stickerTile(context, stickers.get(i), palette), tileParams);
        }
        // Keep the last row's tiles the same width as full rows.
        for (int i = stickers.size() % COLUMNS; i > 0 && i < COLUMNS; i++) {
            LinearLayout.LayoutParams fillerParams = new LinearLayout.LayoutParams(0, 0, 1f);
            fillerParams.setMarginStart(gap);
            row.addView(new View(context), fillerParams);
        }
        ScrollView scroll = new ScrollView(context);
        scroll.addView(rows);
        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return content;
    }

    private static View stickerTile(Context context, File sticker, StickerPalette palette) {
        ImageView tile = new ImageView(context);
        tile.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = dp(context, 8);
        tile.setPadding(pad, pad, pad, pad);
        tile.setContentDescription(sticker.getName());
        Drawable animated = StickerStore.isGif(sticker) ? animatedGif(sticker) : null;
        if (animated != null) {
            tile.setImageDrawable(animated);
        } else {
            Bitmap thumbnail = StickerStore.thumbnail(sticker, dp(context, 72));
            if (thumbnail != null) {
                tile.setImageBitmap(thumbnail);
            }
        }
        // Filled card: surface container high, CornerLarge (16dp), 10% pressed state layer.
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 16));
        shape.setColor(palette.surfaceContainerHigh);
        tile.setBackground(new RippleDrawable(
                StickerUi.pressed(palette), shape, null));
        tile.setOnClickListener(v -> send(v, sticker));
        return tile;
    }

    private static Drawable animatedGif(File sticker) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        try {
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(sticker));
            if (drawable instanceof AnimatedImageDrawable) {
                ((AnimatedImageDrawable) drawable).start();
            }
            return drawable;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Commits the sticker to the focused editor when it accepts the type; otherwise copies it to
     * the clipboard so it can be pasted.
     */
    static void send(View view, File sticker) {
        Context context = view.getContext();
        InputMethodService ims = findInputMethodService(context);
        InputConnection connection = ims == null ? null : ims.getCurrentInputConnection();
        EditorInfo editorInfo = ims == null ? null : ims.getCurrentInputEditorInfo();
        File payload = sticker;
        String mimeType = StickerStore.mimeType(sticker);
        if (!StickerStore.isGif(sticker)) {
            // Chats treat WebP as a sticker (PNG becomes a photo); WhatsApp asks for its own type.
            try {
                if (acceptsExactly(editorInfo, WHATSAPP_STICKER_TYPE)) {
                    payload = StickerStore.webpForSending(context, sticker, true);
                    mimeType = WHATSAPP_STICKER_TYPE;
                } else if (accepts(editorInfo, "image/webp")) {
                    payload = StickerStore.webpForSending(context, sticker, false);
                    mimeType = "image/webp";
                }
            } catch (IOException exception) {
                Log.w(TAG, "WebP conversion failed, sending PNG", exception);
            }
        }
        Uri uri = StickerStore.uri(context, payload);
        boolean committed = false;
        if (connection != null && accepts(editorInfo, mimeType)) {
            try {
                committed = connection.commitContent(
                        new InputContentInfo(uri,
                                new ClipDescription(sticker.getName(), new String[] { mimeType })),
                        InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                        null);
            } catch (Throwable throwable) {
                Log.w(TAG, "commitContent failed", throwable);
            }
        }
        if (committed) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newUri(context.getContentResolver(), sticker.getName(), uri));
        }
        showMessage(view, text(context, R.string.gboard_patches_stickers_keyboard_copied));
    }

    private static boolean acceptsExactly(EditorInfo editorInfo, String mimeType) {
        String[] accepted = editorInfo == null ? null : editorInfo.contentMimeTypes;
        return accepted != null && Arrays.asList(accepted).contains(mimeType);
    }

    private static boolean accepts(EditorInfo editorInfo, String mimeType) {
        String[] accepted = editorInfo == null ? null : editorInfo.contentMimeTypes;
        if (accepted == null) {
            return false;
        }
        for (String type : accepted) {
            if (ClipDescription.compareMimeTypes(mimeType, type)) {
                return true;
            }
        }
        return false;
    }

    private static InputMethodService findInputMethodService(Context context) {
        for (int i = 0; i < 10 && context != null; i++) {
            if (context instanceof InputMethodService) {
                return (InputMethodService) context;
            }
            if (!(context instanceof ContextWrapper)) {
                return null;
            }
            Context base = ((ContextWrapper) context).getBaseContext();
            context = base == context ? null : base;
        }
        return null;
    }

    private static void openSettings(Context context) {
        try {
            ArrayList<String> path = new ArrayList<String>(Collections.singletonList(
                    GboardCustomStickersFeature.class.getName()));
            context.startActivity(new Intent(context, GboardPatchesSettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putStringArrayListExtra(GboardPatchesSettingsActivity.EXTRA_NAVIGATION_PATH,
                            path));
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to open sticker settings", throwable);
        }
    }

    // Gboard keeps a few view ids unobfuscated; the renamed package owns them.
    private static View findById(View root, String name) {
        Context context = root.getContext();
        for (String pkg : new String[] { context.getPackageName(),
                "com.google.android.inputmethod.latin" }) {
            int id = context.getResources().getIdentifier(name, "id", pkg);
            View view = id == 0 ? null : root.findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    // Reuses the keyboard's own background so the panel matches the keyboard theme.
    private static Drawable keyboardBackground(View host) {
        for (View view = host; view != null;
                view = view.getParent() instanceof View ? (View) view.getParent() : null) {
            Drawable background = view.getBackground();
            if (background != null && background.getConstantState() != null) {
                return background.getConstantState().newDrawable().mutate();
            }
        }
        GradientDrawable fallback = new GradientDrawable();
        fallback.setColor(StickerPalette.of(host.getContext()).surfaceContainer);
        return fallback;
    }

    private static ImageView firstImageView(View view) {
        if (view instanceof ImageView) {
            return (ImageView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView found = firstImageView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int dp(Context context, float value) {
        return StickerUi.dp(context, value);
    }

    private static String text(Context context, int resId) {
        return GboardSettingsText.get(context, resId);
    }

    /**
     * The sticker panel. Closes itself when the keyboard hides or when Gboard swaps the keyboard
     * body (e.g. the user opens the GIF keyboard), and lights up the header button while open.
     */
    private static final class StickerPanel extends FrameLayout {
        private final GradientDrawable pill;
        private final ImageView icon;
        private final ColorStateList iconTint;
        private final View bodyContent;
        private final ViewTreeObserver.OnGlobalLayoutListener bodyWatcher;

        StickerPanel(Context context, GradientDrawable pill, ImageView icon,
                ColorStateList iconTint, StickerPalette palette, View bodyContent) {
            super(context);
            setTag(PANEL_TAG);
            setClickable(true);
            this.pill = pill;
            this.icon = icon;
            this.iconTint = iconTint;
            this.bodyContent = bodyContent;
            pill.setColor(palette.secondaryContainer);
            icon.setImageTintList(ColorStateList.valueOf(palette.onSecondaryContainer));
            bodyWatcher = () -> {
                if (this.bodyContent != null && this.bodyContent.getParent() == null) {
                    close();
                }
            };
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            getViewTreeObserver().addOnGlobalLayoutListener(bodyWatcher);
        }

        @Override
        protected void onDetachedFromWindow() {
            getViewTreeObserver().removeOnGlobalLayoutListener(bodyWatcher);
            pill.setColor(0);
            icon.setImageTintList(iconTint);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility != VISIBLE) {
                post(this::close);
            }
        }

        private void close() {
            if (getParent() instanceof ViewGroup) {
                ((ViewGroup) getParent()).removeView(this);
            }
        }
    }
}
