package com.akshaykadam.pixelboard.extension.settings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public final class GboardPatchesSettingsProvider extends ContentProvider {
    private static final String TAG = "PixelBoard";
    public static final String AUTHORITY_SUFFIX = ".gboard_patches";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        return new Bundle();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Context context = getContext();
        if (context != null) {
            File apk = new File(context.getApplicationInfo().sourceDir);
            if (apk.exists()) {
                return ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        }
        throw new FileNotFoundException("APK file not found");
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
