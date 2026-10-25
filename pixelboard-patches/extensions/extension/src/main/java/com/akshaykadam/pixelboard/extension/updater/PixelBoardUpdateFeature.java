/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.updater;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.akshaykadam.pixelboard.extension.BuildConfig;
import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Settings feature exposing the in-app PixelBoard Update screen with What's New notes,
 * direct APK downloading from GitHub, and package installation.
 */
public final class PixelBoardUpdateFeature implements GboardPatchesSettingsContract.Feature {

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerTitle;
    private final String headerSummary;

    public PixelBoardUpdateFeature(Context context) {
        entryTitle = GboardSettingsText.get(context, R.string.gboard_patches_update_title);
        entrySummary = GboardSettingsText.get(context, R.string.gboard_patches_update_summary);
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        headerTitle = GboardSettingsText.get(context, R.string.gboard_patches_update_header_title);
        headerSummary = GboardSettingsText.get(context, R.string.gboard_patches_update_header_summary);
    }

    @Override
    public String getEntryTitle() {
        return entryTitle;
    }

    @Override
    public String getEntrySummary() {
        Context context = null;
        PixelBoardUpdateManager updateManager = PixelBoardUpdateManager.getInstance();
        ReleaseInfo releaseInfo = updateManager.getCurrentReleaseInfo();
        PixelBoardUpdateManager.Status status = updateManager.getCurrentStatus();

        if (status == PixelBoardUpdateManager.Status.UPDATE_AVAILABLE && releaseInfo != null) {
            String size = releaseInfo.getFormattedFileSize();
            return "Update available: v" + releaseInfo.getVersion() + (size.isEmpty() ? "" : (" (" + size + ")"));
        } else if (status == PixelBoardUpdateManager.Status.UP_TO_DATE && releaseInfo != null) {
            return "v" + releaseInfo.getVersion() + " • Up to date";
        }
        return entrySummary;
    }

    @Override
    public boolean isAvailable(Context context) {
        return true;
    }

    private static final long ONE_HOUR_MS = 3600000L;

    @Override
    public void onVisible(GboardPatchesSettingsContract.FeatureHost host) {
        if (host == null) {
            return;
        }
        Context context = host.getContext();
        PixelBoardUpdateManager updateManager = PixelBoardUpdateManager.getInstance();

        // Auto-check on first screen visit if enabled and hasn't checked in 1 hour
        if (PixelBoardUpdateManager.isAutoCheckEnabled(context)
                && updateManager.getCurrentStatus() == PixelBoardUpdateManager.Status.IDLE) {
            long lastCheck = PixelBoardUpdateManager.getLastCheckTime(context);
            long now = System.currentTimeMillis();
            if (now - lastCheck > ONE_HOUR_MS) {
                String installedVersion = resolveInstalledVersion(context);
                updateManager.checkForUpdates(context, installedVersion, (status, releaseInfo, msg) -> {
                    GboardPatchesSettingsContract.refresh(host);
                });
            }
        }
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        String installedVersion = resolveInstalledVersion(context);
        PixelBoardUpdateManager updateManager = PixelBoardUpdateManager.getInstance();
        ReleaseInfo releaseInfo = updateManager.getCurrentReleaseInfo();
        if (releaseInfo == null) {
            releaseInfo = updateManager.getCachedReleaseInfo(context);
        }
        PixelBoardUpdateManager.Status status = updateManager.getCurrentStatus();

        List<GboardPatchesSettingsContract.StatusBlock> statusBlocks = new ArrayList<>();
        List<GboardPatchesSettingsContract.Section> sections = new ArrayList<>();

        // 1. Build Status Block matching Material 3 tones
        switch (status) {
            case UPDATE_AVAILABLE:
                String updateTitle = releaseInfo != null
                        ? "Update Available: v" + releaseInfo.getVersion()
                        : GboardSettingsText.get(context, R.string.gboard_patches_update_status_available_title);
                String updateSummary = releaseInfo != null
                        ? "PixelBoard v" + releaseInfo.getVersion() + " is ready for download ("
                                + releaseInfo.getFormattedFileSize() + ").\nPublished: "
                                + releaseInfo.getFormattedDate()
                        : GboardSettingsText.get(context, R.string.gboard_patches_update_status_available_summary);
                statusBlocks.add(new GboardPatchesSettingsContract.StatusBlock(
                        updateTitle,
                        updateSummary,
                        GboardPatchesSettingsContract.StatusTone.INFO));
                break;
            case ERROR:
                String errorSummary = updateManager.getLastErrorMessage().isEmpty()
                        ? GboardSettingsText.get(context, R.string.gboard_patches_update_status_error_summary)
                        : (updateManager.getLastErrorMessage() + "\nYou can still download releases directly via browser.");
                statusBlocks.add(new GboardPatchesSettingsContract.StatusBlock(
                        GboardSettingsText.get(context, R.string.gboard_patches_update_status_error_title),
                        errorSummary,
                        GboardPatchesSettingsContract.StatusTone.WARNING));
                break;
            case DOWNLOADING:
            case DOWNLOAD_COMPLETED:
            case UP_TO_DATE:
            case CHECKING:
            case IDLE:
            default:
                // No top status box when downloading, downloaded, up to date, or idle to keep UI clean and uncluttered
                break;
        }

        // 2. Actions Section
        List<GboardPatchesSettingsContract.Row> actionRows = new ArrayList<>();
        final ReleaseInfo targetRelease = releaseInfo;
        if (status == PixelBoardUpdateManager.Status.UPDATE_AVAILABLE && targetRelease != null) {
            String downloadSummary = targetRelease.hasApkAsset()
                    ? ("Download " + targetRelease.getApkFileName() + " (" + targetRelease.getFormattedFileSize() + ") directly from Git")
                    : "Open release page on GitHub";

            actionRows.add(new GboardPatchesSettingsContract.PrimaryCommandRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_download),
                    downloadSummary,
                    true,
                    () -> {
                        if (context instanceof Activity activity) {
                            updateManager.downloadAndInstall(activity, targetRelease, () -> {
                                GboardPatchesSettingsContract.refresh(host);
                            });
                        } else {
                            PixelBoardUpdateManager.openReleasePage(context, targetRelease.getApkDownloadUrl());
                        }
                    }));

            actionRows.add(new GboardPatchesSettingsContract.NavigationRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github),
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github_summary),
                    true,
                    () -> PixelBoardUpdateManager.openReleasePage(context, targetRelease.getHtmlUrl())));
        } else if (status == PixelBoardUpdateManager.Status.DOWNLOADING) {
            actionRows.add(new GboardPatchesSettingsContract.PrimaryCommandRow(
                    "Downloading Update…",
                    "Download is in progress. Check notification panel for status.",
                    false,
                    null,
                    true));

            String githubUrl = targetRelease != null ? targetRelease.getHtmlUrl() : PixelBoardUpdateManager.GITHUB_REPO_RELEASES_WEB;
            actionRows.add(new GboardPatchesSettingsContract.NavigationRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github),
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github_summary),
                    true,
                    () -> PixelBoardUpdateManager.openReleasePage(context, githubUrl)));
        } else if (status == PixelBoardUpdateManager.Status.DOWNLOAD_COMPLETED) {
            actionRows.add(new GboardPatchesSettingsContract.PrimaryCommandRow(
                    "Install Update",
                    "Update package is downloaded. Tap to begin installation.",
                    true,
                    () -> {
                        Uri apkUri = updateManager.getLastDownloadedApkUri();
                        if (apkUri != null) {
                            PixelBoardUpdateManager.promptInstallApk(context, apkUri);
                        } else if (targetRelease != null) {
                            if (context instanceof Activity activity) {
                                updateManager.downloadAndInstall(activity, targetRelease, () -> {
                                    GboardPatchesSettingsContract.refresh(host);
                                });
                            }
                        }
                    }));

            String githubUrl = targetRelease != null ? targetRelease.getHtmlUrl() : PixelBoardUpdateManager.GITHUB_REPO_RELEASES_WEB;
            actionRows.add(new GboardPatchesSettingsContract.NavigationRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github),
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_github_summary),
                    true,
                    () -> PixelBoardUpdateManager.openReleasePage(context, githubUrl)));
        } else {
            long lastCheck = PixelBoardUpdateManager.getLastCheckTime(context);
            String checkSummary;
            if (status == PixelBoardUpdateManager.Status.CHECKING) {
                checkSummary = "Connecting to GitHub Releases repository…";
            } else if (status == PixelBoardUpdateManager.Status.UP_TO_DATE) {
                String relative = lastCheck > 0L ? formatRelativeTime(lastCheck) : "just now";
                checkSummary = "You're up to date • Checked " + relative;
            } else if (lastCheck > 0L) {
                checkSummary = "Last checked: " + formatRelativeTime(lastCheck);
            } else {
                checkSummary = "Check for newer versions and changelogs";
            }

            actionRows.add(new GboardPatchesSettingsContract.PrimaryCommandRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_check_now),
                    checkSummary,
                    status != PixelBoardUpdateManager.Status.CHECKING,
                    () -> {
                        updateManager.checkForUpdates(context, installedVersion, (resStatus, resInfo, msg) -> {
                            GboardPatchesSettingsContract.refresh(host);
                        });
                    },
                    status == PixelBoardUpdateManager.Status.CHECKING));

            actionRows.add(new GboardPatchesSettingsContract.NavigationRow(
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_all_releases),
                    GboardSettingsText.get(context, R.string.gboard_patches_update_action_view_all_releases_summary),
                    true,
                    () -> PixelBoardUpdateManager.openReleasePage(context, PixelBoardUpdateManager.GITHUB_REPO_RELEASES_WEB)));
        }

        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.get(context, R.string.gboard_patches_update_section_actions),
                actionRows));

        // 3. "What's New" Section (Changelog Highlights)
        ReleaseInfo changelogRelease;
        if (releaseInfo != null && releaseInfo.isNewerThan(installedVersion)) {
            changelogRelease = releaseInfo;
        } else if (releaseInfo != null && releaseInfo.getVersion().equals(installedVersion)) {
            changelogRelease = releaseInfo;
        } else {
            changelogRelease = new ReleaseInfo(
                    installedVersion,
                    "PixelBoard v" + installedVersion,
                    getDefaultChangelog(),
                    "",
                    PixelBoardUpdateManager.GITHUB_REPO_RELEASES_WEB,
                    "",
                    "",
                    0L);
        }

        List<ReleaseInfo.Highlight> highlights = changelogRelease.getParsedHighlights();
        List<GboardPatchesSettingsContract.Row> changelogRows = new ArrayList<>();

        int maxItems = 7;
        int count = 0;
        if (!highlights.isEmpty()) {
            for (ReleaseInfo.Highlight highlight : highlights) {
                if (count >= maxItems) {
                    break;
                }
                changelogRows.add(new GboardPatchesSettingsContract.DetailRow(
                        highlight.getTitle(),
                        highlight.getDescription(),
                        true));
                count++;
            }
        } else if (!changelogRelease.getTitle().isEmpty()) {
            changelogRows.add(new GboardPatchesSettingsContract.DetailRow(
                    changelogRelease.getTitle(),
                    changelogRelease.getChangelog().isEmpty() ? "See GitHub release notes for details." : changelogRelease.getChangelog(),
                    true));
        }

        final ReleaseInfo finalRel = changelogRelease;
        changelogRows.add(new GboardPatchesSettingsContract.CommandRow(
                "View full changelog on GitHub",
                "Open GitHub release page",
                true,
                () -> PixelBoardUpdateManager.openReleasePage(context, finalRel.getHtmlUrl())));

        if (!changelogRows.isEmpty()) {
            String sectionTitle = "PixelBoard v" + changelogRelease.getVersion();
            String sectionDesc = (status == PixelBoardUpdateManager.Status.UPDATE_AVAILABLE)
                    ? "Highlights and improvements in the upcoming update"
                    : "Highlights and key improvements in this release";
            sections.add(new GboardPatchesSettingsContract.Section(
                    sectionTitle,
                    sectionDesc,
                    GboardPatchesSettingsContract.SectionStyle.CHANGELOG,
                    changelogRows));
        }

        // 4. Update Preferences Section
        List<GboardPatchesSettingsContract.Row> prefRows = new ArrayList<>();
        prefRows.add(new GboardPatchesSettingsContract.ToggleRow(
                GboardSettingsText.get(context, R.string.gboard_patches_update_pref_autocheck_title),
                GboardSettingsText.get(context, R.string.gboard_patches_update_pref_autocheck_summary),
                true,
                PixelBoardUpdateManager.isAutoCheckEnabled(context),
                value -> {
                    PixelBoardUpdateManager.setAutoCheckEnabled(context, value);
                    GboardPatchesSettingsContract.refresh(host);
                }));

        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.get(context, R.string.gboard_patches_update_section_preferences),
                prefRows));

        // 5. Build Information Section
        List<GboardPatchesSettingsContract.Row> infoRows = new ArrayList<>();
        infoRows.add(new GboardPatchesSettingsContract.DetailRow(
                "Installed Version",
                "v" + installedVersion,
                true));
        infoRows.add(new GboardPatchesSettingsContract.DetailRow(
                "Base Gboard",
                "v18.5.3 (Release)",
                true));
        infoRows.add(new GboardPatchesSettingsContract.DetailRow(
                "Package Name",
                context != null ? context.getPackageName() : "com.akshaykadam.pixelboard",
                true));
        infoRows.add(new GboardPatchesSettingsContract.DetailRow(
                "Author",
                "Akshay Kadam (@Akshayykadam)",
                true));

        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.get(context, R.string.gboard_patches_update_section_info),
                infoRows));

        return new GboardPatchesSettingsContract.Screen(
                entryTitle,
                headerBadge,
                headerTitle,
                headerSummary,
                statusBlocks,
                sections,
                GboardPatchesSettingsContract.RefreshPolicy.none(),
                GboardPatchesSettingsContract.PanelStyle.FLAT,
                null,
                false);
    }

    public static String resolveInstalledVersion(Context context) {
        try {
            if (BuildConfig.PATCH_VERSION != null && !BuildConfig.PATCH_VERSION.trim().isEmpty()) {
                return BuildConfig.PATCH_VERSION.trim();
            }
        } catch (Throwable ignored) {
        }
        if (context != null) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (packageInfo != null && packageInfo.versionName != null) {
                    return packageInfo.versionName;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return "1.3.2";
    }

    public static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60000L) {
            return "Just now";
        } else if (diff < 3600000L) {
            long minutes = diff / 60000L;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < 86400000L) {
            long hours = diff / 3600000L;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            long days = diff / 86400000L;
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    private static String getDefaultChangelog() {
        return "* **In-App Updates**: Dedicated PixelBoard updates section directly in Gboard settings with GitHub release checks, formatted changelogs, and direct APK download\n"
                + "* **Extended Clipboard History**: Save copied text and images for 1 hour, 24 hours, 7 days, 30 days, or permanently (Never)\n"
                + "* **Upgraded Base to Gboard v18.5.3**: Rebased on Google's latest stable release with smoother typing performance\n"
                + "* **TalkBack Accessibility Overhaul**: Full screen reader optimization with clear labels, proper button roles, and audio cues\n"
                + "* **Arabic Language Support**: Added complete native Arabic (العربية) translation across all settings and AI features\n"
                + "* **Crash Fixes & Stability**: Resolved clipboard slider crashes, fixed settings layout issues, and improved battery efficiency";
    }
}
