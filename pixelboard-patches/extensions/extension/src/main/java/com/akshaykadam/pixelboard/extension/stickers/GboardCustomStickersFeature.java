/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

/** Settings page to add, edit and delete custom stickers. */
public final class GboardCustomStickersFeature implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "PixelBoardStickers";
    private static final int THUMBNAIL_DP = 44;

    private final String entryTitle;
    private final String entrySummary;

    public GboardCustomStickersFeature(Context context) {
        entryTitle = GboardSettingsText.get(context, R.string.gboard_patches_stickers_title);
        entrySummary = GboardSettingsText.get(context, R.string.gboard_patches_stickers_summary);
    }

    @Override
    public String getEntryTitle() {
        return entryTitle;
    }

    @Override
    public String getEntrySummary() {
        return entrySummary;
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        Runnable refresh = () -> GboardPatchesSettingsContract.refresh(host);

        List<GboardPatchesSettingsContract.Row> addRows =
                new ArrayList<GboardPatchesSettingsContract.Row>();
        addRows.add(new GboardPatchesSettingsContract.PrimaryCommandRow(
                text(context, R.string.gboard_patches_stickers_add_file_title),
                text(context, R.string.gboard_patches_stickers_add_file_summary),
                true,
                () -> GboardPatchesSettingsContract.openBinaryDocument(host,
                        new String[] { "image/*" },
                        document -> importBytes(context, document.getData(), refresh))));
        addRows.add(new GboardPatchesSettingsContract.CommandRow(
                text(context, R.string.gboard_patches_stickers_paste_title),
                text(context, R.string.gboard_patches_stickers_paste_summary),
                true,
                () -> pasteFromClipboard(context, refresh)));

        List<File> stickers = StickerStore.list(context);
        List<GboardPatchesSettingsContract.Row> libraryRows =
                new ArrayList<GboardPatchesSettingsContract.Row>();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        int thumbnailPx = Math.round(THUMBNAIL_DP * context.getResources().getDisplayMetrics().density);
        for (File sticker : stickers) {
            libraryRows.add(stickerRow(context, sticker, dateFormat, thumbnailPx, refresh));
        }
        if (libraryRows.isEmpty()) {
            libraryRows.add(new GboardPatchesSettingsContract.DetailRow(
                    text(context, R.string.gboard_patches_stickers_empty), null, false));
        }

        List<GboardPatchesSettingsContract.Section> sections =
                new ArrayList<GboardPatchesSettingsContract.Section>();
        sections.add(new GboardPatchesSettingsContract.Section(
                text(context, R.string.gboard_patches_stickers_section_add), addRows));
        sections.add(new GboardPatchesSettingsContract.Section(
                text(context, R.string.gboard_patches_stickers_section_keyboard),
                Collections.singletonList(new GboardPatchesSettingsContract.ToggleRow(
                        text(context, R.string.gboard_patches_stickers_header_button_title),
                        text(context, R.string.gboard_patches_stickers_header_button_summary),
                        true,
                        StickerStore.isHeaderButtonEnabled(context),
                        enabled -> StickerStore.setHeaderButtonEnabled(context, enabled)))));
        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.format(context, R.string.gboard_patches_stickers_section_library,
                        stickers.size()),
                libraryRows));
        return new GboardPatchesSettingsContract.Screen(
                entryTitle,
                GboardSettingsText.get(context, R.string.gboard_patches_header_badge),
                entryTitle,
                text(context, R.string.gboard_patches_stickers_header_summary),
                Collections.emptyList(),
                sections,
                GboardPatchesSettingsContract.RefreshPolicy.none(),
                GboardPatchesSettingsContract.PanelStyle.FLAT);
    }

    private static GboardPatchesSettingsContract.Row stickerRow(Context context, File sticker,
            DateFormat dateFormat, int thumbnailPx, Runnable refresh) {
        Bitmap thumbnail = StickerStore.thumbnail(sticker, thumbnailPx);
        String summary;
        Runnable action;
        if (StickerStore.isGif(sticker)) {
            summary = text(context, R.string.gboard_patches_stickers_row_gif);
            action = () -> confirmDelete(context, sticker, refresh);
        } else {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(sticker.getPath(), bounds);
            summary = GboardSettingsText.format(context, R.string.gboard_patches_stickers_row_png,
                    bounds.outWidth, bounds.outHeight);
            action = () -> {
                Bitmap bitmap = BitmapFactory.decodeFile(sticker.getPath());
                if (bitmap != null && context instanceof Activity) {
                    StickerEditorDialog.show((Activity) context,
                            StickerStore.fitWithin(bitmap, StickerStore.MAX_DIMENSION_PX),
                            sticker, refresh);
                }
            };
        }
        return new GboardPatchesSettingsContract.CommandRow(
                dateFormat.format(new Date(sticker.lastModified())),
                summary,
                true,
                action,
                null,
                thumbnail == null ? null : new BitmapDrawable(context.getResources(), thumbnail));
    }

    private static void importBytes(Context context, byte[] data, Runnable refresh) {
        try {
            if (StickerStore.isGifData(data)) {
                StickerStore.saveGif(context, data);
                refresh.run();
                return;
            }
            Bitmap bitmap = StickerStore.decodeForEditing(data);
            if (bitmap == null || !(context instanceof Activity)) {
                toast(context, R.string.gboard_patches_stickers_read_failed);
                return;
            }
            StickerEditorDialog.show((Activity) context, bitmap, null, refresh);
        } catch (Exception exception) {
            Log.w(TAG, "Failed to import sticker", exception);
            toast(context, R.string.gboard_patches_stickers_read_failed);
        }
    }

    private static void pasteFromClipboard(Context context, Runnable refresh) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
        Uri uri = clip == null || clip.getItemCount() == 0 ? null : clip.getItemAt(0).getUri();
        if (uri == null) {
            toast(context, R.string.gboard_patches_stickers_no_clip_image);
            return;
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new java.io.FileNotFoundException(uri.toString());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            for (int read; (read = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, read);
            }
            importBytes(context, out.toByteArray(), refresh);
        } catch (Exception exception) {
            Log.w(TAG, "Failed to read clipboard image", exception);
            toast(context, R.string.gboard_patches_stickers_read_failed);
        }
    }

    private static void confirmDelete(Context context, File sticker, Runnable refresh) {
        StickerUi.confirm(context,
                text(context, R.string.gboard_patches_stickers_delete_title),
                text(context, R.string.gboard_patches_stickers_editor_cancel),
                text(context, R.string.gboard_patches_stickers_editor_delete),
                () -> {
                    sticker.delete();
                    refresh.run();
                });
    }

    private static void toast(Context context, int resId) {
        Toast.makeText(context, text(context, resId), Toast.LENGTH_SHORT).show();
    }

    private static String text(Context context, int resId) {
        return GboardSettingsText.get(context, resId);
    }
}
