/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stickers are plain .png / .gif files in one private folder, newest first. The settings
 * provider serves them to other apps under {@link #URI_PATH_PREFIX}.
 */
public final class StickerStore {
    public static final String URI_PATH_PREFIX = "/stickers/";
    public static final String SEND_URI_PATH_PREFIX = "/sticker-send/";
    static final int MAX_DIMENSION_PX = 512;
    private static final String DIRECTORY = "custom_stickers";
    private static final String PREFERENCES = "pixelboard_custom_stickers";
    private static final String PREF_HEADER_BUTTON = "show_header_button";
    private static final String SEND_DIRECTORY = "send";
    // WhatsApp sticker limits: exactly 512x512, at most 100 KB, 16px recommended margin.
    private static final int WHATSAPP_MAX_BYTES = 100 * 1024;
    private static final int WHATSAPP_MARGIN_PX = 16;

    private StickerStore() {
    }

    /** Whether the Stickers button shows in the keyboard's top row (on by default). */
    static boolean isHeaderButtonEnabled(Context context) {
        return preferences(context).getBoolean(PREF_HEADER_BUTTON, true);
    }

    static void setHeaderButtonEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(PREF_HEADER_BUTTON, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public static List<File> list(Context context) {
        File[] files = directory(context).listFiles(StickerStore::isSticker);
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return Arrays.asList(files);
    }

    /** Resolves a provider URI path to a sticker (or its send copy), or null for anything else. */
    public static File fromUriPath(Context context, String path) {
        boolean sendCopy = path != null && path.startsWith(SEND_URI_PATH_PREFIX);
        if (path == null || (!sendCopy && !path.startsWith(URI_PATH_PREFIX))) {
            return null;
        }
        String name = path.substring(
                (sendCopy ? SEND_URI_PATH_PREFIX : URI_PATH_PREFIX).length());
        if (name.contains("/") || name.contains("\\")) {
            return null;
        }
        File file = new File(sendCopy ? sendDirectory(context) : directory(context), name);
        return file.isFile() && (sendCopy ? name.endsWith(".webp") : isSticker(file))
                ? file : null;
    }

    public static Uri uri(Context context, File sticker) {
        boolean sendCopy = sticker.getParentFile() != null
                && SEND_DIRECTORY.equals(sticker.getParentFile().getName());
        return Uri.parse("content://" + context.getPackageName()
                + GboardPatchesSettingsProvider.AUTHORITY_SUFFIX
                + (sendCopy ? SEND_URI_PATH_PREFIX : URI_PATH_PREFIX) + sticker.getName());
    }

    public static String mimeType(File sticker) {
        String name = sticker.getName();
        return name.endsWith(".gif") ? "image/gif"
                : name.endsWith(".webp") ? "image/webp" : "image/png";
    }

    /**
     * WebP copy of a PNG sticker for sending. Telegram and others treat WebP as a sticker while
     * PNG arrives as a photo; WhatsApp additionally needs 512x512 and at most 100 KB.
     */
    static File webpForSending(Context context, File png, boolean whatsApp) throws IOException {
        File out = new File(sendDirectory(context),
                png.getName().replace(".png", whatsApp ? "_wa.webp" : ".webp"));
        if (out.isFile() && out.lastModified() >= png.lastModified()) {
            return out;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(png.getPath());
        if (bitmap == null) {
            throw new IOException("Unreadable sticker " + png.getName());
        }
        if (whatsApp) {
            bitmap = padToSquare(bitmap);
        }
        byte[] data = encode(bitmap, Bitmap.CompressFormat.WEBP_LOSSLESS, 100);
        for (int quality = 90; whatsApp && data.length > WHATSAPP_MAX_BYTES && quality > 0;
                quality -= 15) {
            data = encode(bitmap, Bitmap.CompressFormat.WEBP_LOSSY, quality);
        }
        try (FileOutputStream stream = new FileOutputStream(out)) {
            stream.write(data);
        }
        return out;
    }

    private static Bitmap padToSquare(Bitmap bitmap) {
        int size = MAX_DIMENSION_PX;
        Bitmap square = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Bitmap fitted = fitWithin(bitmap, size - 2 * WHATSAPP_MARGIN_PX);
        new Canvas(square).drawBitmap(fitted, (size - fitted.getWidth()) / 2f,
                (size - fitted.getHeight()) / 2f, null);
        return square;
    }

    private static byte[] encode(Bitmap bitmap, Bitmap.CompressFormat format, int quality)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!bitmap.compress(format, quality, out)) {
            throw new IOException("WebP encoding failed");
        }
        return out.toByteArray();
    }

    private static File sendDirectory(Context context) {
        File directory = new File(directory(context), SEND_DIRECTORY);
        directory.mkdirs();
        return directory;
    }

    public static boolean isGif(File sticker) {
        return sticker.getName().endsWith(".gif");
    }

    static boolean isGifData(byte[] data) {
        return data != null && data.length > 4
                && data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8';
    }

    /** Writes a new PNG sticker, or overwrites {@code existing} when editing one. */
    static File savePng(Context context, Bitmap bitmap, File existing) throws IOException {
        File file = existing != null ? existing : newFile(context, ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("PNG encoding failed");
            }
        }
        return file;
    }

    static File saveGif(Context context, byte[] data) throws IOException {
        File file = newFile(context, ".gif");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
        return file;
    }

    /** Decodes image bytes scaled down to fit {@link #MAX_DIMENSION_PX}, or null if unreadable. */
    static Bitmap decodeForEditing(byte[] data) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2)
                >= MAX_DIMENSION_PX) {
            options.inSampleSize *= 2;
        }
        Bitmap decoded = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (decoded == null) {
            return null;
        }
        return fitWithin(decoded, MAX_DIMENSION_PX);
    }

    /** Small preview for lists and the keyboard grid; null if unreadable. */
    static Bitmap thumbnail(File sticker, int sizePx) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(sticker.getPath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2) >= sizePx) {
            options.inSampleSize *= 2;
        }
        Bitmap decoded = BitmapFactory.decodeFile(sticker.getPath(), options);
        return decoded == null ? null : fitWithin(decoded, sizePx);
    }

    static Bitmap fitWithin(Bitmap bitmap, int maxPx) {
        int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        Bitmap argb = bitmap.getConfig() == Bitmap.Config.ARGB_8888
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, false);
        if (largest <= maxPx) {
            return argb;
        }
        float scale = maxPx / (float) largest;
        return Bitmap.createScaledBitmap(argb,
                Math.max(1, Math.round(argb.getWidth() * scale)),
                Math.max(1, Math.round(argb.getHeight() * scale)),
                true);
    }

    private static File directory(Context context) {
        File directory = new File(context.getFilesDir(), DIRECTORY);
        directory.mkdirs();
        return directory;
    }

    private static File newFile(Context context, String extension) {
        return new File(directory(context), "sticker_" + System.currentTimeMillis() + extension);
    }

    private static boolean isSticker(File file) {
        String name = file.getName();
        return file.isFile() && (name.endsWith(".png") || name.endsWith(".gif"));
    }
}
