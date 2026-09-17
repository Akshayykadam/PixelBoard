package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Detects presence of native speech dictation payload libraries in installed target packages.
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardDictationPayloadDetector {
    private static final Pattern DICTATION_LIBRARY_ENTRY =
            Pattern.compile("^lib/[^/]+/libdictation_jni\\.so$");

    private GboardDictationPayloadDetector() {
    }

    public static Detection detect(Context context, List<String> targetPackageNames) {
        if (context == null || targetPackageNames == null || targetPackageNames.isEmpty()) {
            return Detection.unknown(null);
        }

        Context applicationContext = context.getApplicationContext();
        Context lookupContext = applicationContext != null ? applicationContext : context;
        PackageManager packageManager = lookupContext.getPackageManager();
        if (packageManager == null) {
            return Detection.unknown(null);
        }

        for (String packageName : targetPackageNames) {
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }
            try {
                ApplicationInfo applicationInfo =
                        packageManager.getApplicationInfo(packageName, 0);
                return inspectPackageArchives(
                        packageName,
                        applicationArchivePaths(applicationInfo));
            } catch (PackageManager.NameNotFoundException ignored) {
                // Continue to the next allowlisted target package.
            } catch (Throwable ignored) {
                return Detection.unknown(packageName);
            }
        }
        return Detection.unknown(null);
    }

    static Detection inspectPackageArchives(String packageName, List<String> apkPaths) {
        if (apkPaths == null || apkPaths.isEmpty()) {
            return Detection.unknown(packageName);
        }

        boolean inspectedArchive = false;
        boolean unreadableArchive = false;
        for (String apkPath : apkPaths) {
            if (apkPath == null || apkPath.isEmpty()) {
                unreadableArchive = true;
                continue;
            }
            try (ZipFile archive = new ZipFile(apkPath)) {
                inspectedArchive = true;
                Enumeration<? extends ZipEntry> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry != null
                            && DICTATION_LIBRARY_ENTRY.matcher(entry.getName()).matches()) {
                        return Detection.present(packageName);
                    }
                }
            } catch (Throwable ignored) {
                unreadableArchive = true;
            }
        }

        if (!inspectedArchive || unreadableArchive) {
            return Detection.unknown(packageName);
        }
        return Detection.absent(packageName);
    }

    private static List<String> applicationArchivePaths(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return Collections.emptyList();
        }
        List<String> apkPaths = new ArrayList<String>();
        if (applicationInfo.sourceDir != null && !applicationInfo.sourceDir.isEmpty()) {
            apkPaths.add(applicationInfo.sourceDir);
        }
        if (applicationInfo.splitSourceDirs != null) {
            for (String splitSourceDir : applicationInfo.splitSourceDirs) {
                if (splitSourceDir != null && !splitSourceDir.isEmpty()) {
                    apkPaths.add(splitSourceDir);
                }
            }
        }
        return apkPaths;
    }

    public enum Status {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    public static final class Detection {
        private final Status status;
        private final String packageName;

        private Detection(Status status, String packageName) {
            this.status = status;
            this.packageName = packageName;
        }

        public Status getStatus() {
            return status;
        }

        public String getPackageName() {
            return packageName;
        }

        private static Detection present(String packageName) {
            return new Detection(Status.PRESENT, packageName);
        }

        private static Detection absent(String packageName) {
            return new Detection(Status.ABSENT, packageName);
        }

        private static Detection unknown(String packageName) {
            return new Detection(Status.UNKNOWN, packageName);
        }
    }
}

