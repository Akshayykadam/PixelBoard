/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages GitHub release update checking, release notes parsing, and APK download/install flows.
 */
public final class PixelBoardUpdateManager {
    private static final String TAG = "PixelBoardUpdate";

    public static final String GITHUB_REPO_API =
            "https://api.github.com/repos/Akshayykadam/PixelBoard/releases/latest";
    public static final String GITHUB_REPO_RELEASES_WEB =
            "https://github.com/Akshayykadam/PixelBoard/releases";

    private static final String PREF_NAME = "pixelboard_updates";
    private static final String PREF_KEY_AUTO_CHECK = "auto_check_updates";
    private static final String PREF_KEY_LAST_CHECK_TIME = "last_check_timestamp";
    private static final String PREF_KEY_CACHED_VERSION = "cached_version";
    private static final String PREF_KEY_CACHED_TITLE = "cached_title";
    private static final String PREF_KEY_CACHED_CHANGELOG = "cached_changelog";
    private static final String PREF_KEY_CACHED_APK_URL = "cached_apk_url";
    private static final String PREF_KEY_CACHED_APK_NAME = "cached_apk_name";
    private static final String PREF_KEY_CACHED_APK_SIZE = "cached_apk_size";
    private static final String PREF_KEY_CACHED_HTML_URL = "cached_html_url";
    private static final String PREF_KEY_CACHED_PUBLISHED_AT = "cached_published_at";
    private static final String PREF_KEY_ACTIVE_DOWNLOAD_ID = "active_download_id";

    public enum Status {
        IDLE,
        CHECKING,
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        DOWNLOADING,
        DOWNLOAD_COMPLETED,
        ERROR
    }

    public interface UpdateCallback {
        void onUpdateResult(Status status, ReleaseInfo releaseInfo, String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "PixelBoardUpdateChecker");
        thread.setDaemon(true);
        return thread;
    });

    private static void postToMain(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        try {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                new Handler(mainLooper).post(runnable);
                return;
            }
        } catch (Throwable ignored) {
        }
        runnable.run();
    }

    private static volatile PixelBoardUpdateManager instance;

    private Status currentStatus = Status.IDLE;
    private ReleaseInfo currentReleaseInfo;
    private String lastErrorMessage = "";
    private BroadcastReceiver downloadReceiver;

    private PixelBoardUpdateManager() {
    }

    public static PixelBoardUpdateManager getInstance() {
        if (instance == null) {
            synchronized (PixelBoardUpdateManager.class) {
                if (instance == null) {
                    instance = new PixelBoardUpdateManager();
                }
            }
        }
        return instance;
    }

    public Status getCurrentStatus() {
        return currentStatus;
    }

    void setCurrentStatusForTest(Status status) {
        this.currentStatus = status;
    }

    public ReleaseInfo getCurrentReleaseInfo() {
        return currentReleaseInfo;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private Uri lastDownloadedApkUri;

    public Uri getLastDownloadedApkUri() {
        return lastDownloadedApkUri;
    }

    public void setLastDownloadedApkUri(Uri uri) {
        this.lastDownloadedApkUri = uri;
    }

    public static SharedPreferences preferences(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAutoCheckEnabled(Context context) {
        SharedPreferences prefs = preferences(context);
        return prefs == null || prefs.getBoolean(PREF_KEY_AUTO_CHECK, true);
    }

    public static void setAutoCheckEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = preferences(context);
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_KEY_AUTO_CHECK, enabled).apply();
        }
    }

    public static long getLastCheckTime(Context context) {
        SharedPreferences prefs = preferences(context);
        return prefs != null ? prefs.getLong(PREF_KEY_LAST_CHECK_TIME, 0L) : 0L;
    }

    /**
     * Loads the last cached release info from disk so the UI displays instantly without waiting.
     */
    public ReleaseInfo getCachedReleaseInfo(Context context) {
        if (currentReleaseInfo != null) {
            return currentReleaseInfo;
        }
        SharedPreferences prefs = preferences(context);
        if (prefs == null || !prefs.contains(PREF_KEY_CACHED_VERSION)) {
            return null;
        }
        String version = prefs.getString(PREF_KEY_CACHED_VERSION, "");
        if (version.isEmpty()) {
            return null;
        }
        ReleaseInfo cached = new ReleaseInfo(
                version,
                prefs.getString(PREF_KEY_CACHED_TITLE, ""),
                prefs.getString(PREF_KEY_CACHED_CHANGELOG, ""),
                prefs.getString(PREF_KEY_CACHED_PUBLISHED_AT, ""),
                prefs.getString(PREF_KEY_CACHED_HTML_URL, ""),
                prefs.getString(PREF_KEY_CACHED_APK_URL, ""),
                prefs.getString(PREF_KEY_CACHED_APK_NAME, ""),
                prefs.getLong(PREF_KEY_CACHED_APK_SIZE, 0L));
        currentReleaseInfo = cached;
        return cached;
    }

    /**
     * Checks GitHub for the latest release asynchronously.
     */
    public void checkForUpdates(Context context, String installedVersion, UpdateCallback callback) {
        currentStatus = Status.CHECKING;
        lastErrorMessage = "";
        if (callback != null) {
            callback.onUpdateResult(Status.CHECKING, currentReleaseInfo, null);
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(GITHUB_REPO_API);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "PixelBoard-Android-App");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("GitHub API returned HTTP " + responseCode);
                }

                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append('\n');
                }
                reader.close();

                JSONObject releaseJson = new JSONObject(response.toString());
                String tagName = releaseJson.optString("tag_name", "");
                String name = releaseJson.optString("name", "");
                String body = releaseJson.optString("body", "");
                String publishedAt = releaseJson.optString("published_at", "");
                String htmlUrl = releaseJson.optString("html_url", GITHUB_REPO_RELEASES_WEB);

                String apkDownloadUrl = "";
                String apkFileName = "";
                long apkSizeBytes = 0L;

                JSONArray assets = releaseJson.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) {
                            continue;
                        }
                        String assetName = asset.optString("name", "");
                        if (assetName.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", "");
                            apkFileName = assetName;
                            apkSizeBytes = asset.optLong("size", 0L);
                            break;
                        }
                    }
                }

                ReleaseInfo releaseInfo = new ReleaseInfo(
                        tagName,
                        name,
                        body,
                        publishedAt,
                        htmlUrl,
                        apkDownloadUrl,
                        apkFileName,
                        apkSizeBytes);

                currentReleaseInfo = releaseInfo;
                saveToCache(context, releaseInfo);

                boolean isNewer = releaseInfo.isNewerThan(installedVersion);
                currentStatus = isNewer ? Status.UPDATE_AVAILABLE : Status.UP_TO_DATE;

                postToMain(() -> {
                    if (callback != null) {
                        callback.onUpdateResult(currentStatus, releaseInfo, null);
                    }
                });

            } catch (Throwable throwable) {
                Log.w(TAG, "Update check failed", throwable);
                currentStatus = Status.ERROR;
                lastErrorMessage = throwable.getMessage() != null
                        ? throwable.getMessage()
                        : GboardSettingsText.get(context, R.string.gboard_patches_update_network_error);
                postToMain(() -> {
                    if (callback != null) {
                        callback.onUpdateResult(Status.ERROR, currentReleaseInfo, lastErrorMessage);
                    }
                });
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private void saveToCache(Context context, ReleaseInfo releaseInfo) {
        SharedPreferences prefs = preferences(context);
        if (prefs == null || releaseInfo == null) {
            return;
        }
        prefs.edit()
                .putLong(PREF_KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                .putString(PREF_KEY_CACHED_VERSION, releaseInfo.getVersion())
                .putString(PREF_KEY_CACHED_TITLE, releaseInfo.getTitle())
                .putString(PREF_KEY_CACHED_CHANGELOG, releaseInfo.getChangelog())
                .putString(PREF_KEY_CACHED_HTML_URL, releaseInfo.getHtmlUrl())
                .putString(PREF_KEY_CACHED_APK_URL, releaseInfo.getApkDownloadUrl())
                .putString(PREF_KEY_CACHED_APK_NAME, releaseInfo.getApkFileName())
                .putLong(PREF_KEY_CACHED_APK_SIZE, releaseInfo.getApkSizeBytes())
                .putString(PREF_KEY_CACHED_PUBLISHED_AT, releaseInfo.getPublishedAt())
                .apply();
    }

    /**
     * Downloads the release APK using Android DownloadManager and initiates package installation on completion.
     */
    public void downloadAndInstall(Activity activity, ReleaseInfo releaseInfo, Runnable onStateChanged) {
        if (activity == null || releaseInfo == null) {
            return;
        }
        if (!releaseInfo.hasApkAsset()) {
            openReleasePage(activity, releaseInfo.getHtmlUrl());
            return;
        }

        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            openReleasePage(activity, releaseInfo.getApkDownloadUrl());
            return;
        }

        try {
            Uri downloadUri = Uri.parse(releaseInfo.getApkDownloadUrl());
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setTitle("PixelBoard " + releaseInfo.getVersion());
            request.setDescription(GboardSettingsText.get(activity,
                    R.string.gboard_patches_update_download_description));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, releaseInfo.getApkFileName());
            request.setMimeType("application/vnd.android.package-archive");

            long downloadId = downloadManager.enqueue(request);

            SharedPreferences prefs = preferences(activity);
            if (prefs != null) {
                prefs.edit().putLong(PREF_KEY_ACTIVE_DOWNLOAD_ID, downloadId).apply();
            }

            currentStatus = Status.DOWNLOADING;
            Toast.makeText(activity, GboardSettingsText.format(activity,
                    R.string.gboard_patches_update_download_started, releaseInfo.getVersion()),
                    Toast.LENGTH_SHORT).show();
            if (onStateChanged != null) {
                onStateChanged.run();
            }

            registerDownloadReceiver(activity.getApplicationContext(), downloadId, onStateChanged);

        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to enqueue APK download", throwable);
            Toast.makeText(activity, GboardSettingsText.get(activity,
                    R.string.gboard_patches_update_download_start_failed), Toast.LENGTH_LONG).show();
            openReleasePage(activity, releaseInfo.getApkDownloadUrl());
        }
    }

    private void registerDownloadReceiver(Context appContext, long targetDownloadId, Runnable onStateChanged) {
        if (downloadReceiver != null) {
            try {
                appContext.unregisterReceiver(downloadReceiver);
            } catch (Throwable ignored) {
            }
            downloadReceiver = null;
        }

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    return;
                }
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (completedId != targetDownloadId) {
                    return;
                }

                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager == null) {
                    return;
                }

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(targetDownloadId);
                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(statusIndex);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Uri apkUri = downloadManager.getUriForDownloadedFile(targetDownloadId);
                            currentStatus = Status.DOWNLOAD_COMPLETED;
                            lastDownloadedApkUri = apkUri;
                            if (onStateChanged != null) {
                                postToMain(onStateChanged);
                            }
                            if (apkUri != null) {
                                promptInstallApk(context, apkUri);
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            currentStatus = Status.ERROR;
                            lastErrorMessage = GboardSettingsText.get(context,
                                    R.string.gboard_patches_update_download_failed);
                            if (onStateChanged != null) {
                                postToMain(onStateChanged);
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Failed to inspect completed download", t);
                } finally {
                    try {
                        appContext.unregisterReceiver(this);
                    } catch (Throwable ignored) {
                    }
                    downloadReceiver = null;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(
                    downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED);
        } else {
            appContext.registerReceiver(
                    downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    /**
     * Launches package installer intent for the downloaded APK file URI.
     */
    public static void promptInstallApk(Context context, Uri apkUri) {
        if (context == null || apkUri == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                if (context instanceof Activity activity && !activity.isFinishing()) {
                    new AlertDialog.Builder(activity)
                            .setTitle(GboardSettingsText.get(activity,
                                    R.string.gboard_patches_update_install_permission_title))
                            .setMessage(GboardSettingsText.get(activity,
                                    R.string.gboard_patches_update_install_permission_message))
                            .setPositiveButton(GboardSettingsText.get(activity,
                                    R.string.gboard_patches_update_install_permission_action),
                                    (dialog, which) -> {
                                Intent settingsIntent = new Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + context.getPackageName()));
                                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(settingsIntent);
                            })
                            .setNegativeButton(GboardSettingsText.get(activity,
                                    R.string.gboard_patches_dialog_cancel), null)
                            .show();
                    return;
                }
            }
        }

        try {
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installIntent);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to trigger APK install intent", throwable);
            Toast.makeText(context, GboardSettingsText.get(context,
                    R.string.gboard_patches_update_installer_failed), Toast.LENGTH_LONG).show();
        }
    }

    public static void openReleasePage(Context context, String url) {
        if (context == null) {
            return;
        }
        String targetUrl = (url != null && !url.isEmpty()) ? url : GITHUB_REPO_RELEASES_WEB;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to open release URL: " + targetUrl, throwable);
        }
    }
}
