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
        Context context = getContext();
        if (context != null) {
            try {
                android.content.pm.PackageManager pm = context.getPackageManager();
                android.content.ComponentName launcher = new android.content.ComponentName(
                    context,
                    "com.google.android.libraries.inputmethod.launcher.LauncherActivity"
                );
                int state = pm.getComponentEnabledSetting(launcher);
                if (state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                    pm.setComponentEnabledSetting(
                        launcher,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    );
                }
            } catch (Throwable ignored) {
            }
        }
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
