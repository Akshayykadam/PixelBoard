/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.updater;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates release metadata fetched from the GitHub Releases API.
 */
public final class ReleaseInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String version;
    private final String title;
    private final String changelog;
    private final String publishedAt;
    private final String htmlUrl;
    private final String apkDownloadUrl;
    private final String apkFileName;
    private final long apkSizeBytes;

    public ReleaseInfo(
            String version,
            String title,
            String changelog,
            String publishedAt,
            String htmlUrl,
            String apkDownloadUrl,
            String apkFileName,
            long apkSizeBytes) {
        this.version = version == null ? "" : cleanVersion(version);
        this.title = title == null ? "" : title.trim();
        this.changelog = changelog == null ? "" : changelog.trim();
        this.publishedAt = publishedAt == null ? "" : publishedAt.trim();
        this.htmlUrl = htmlUrl == null ? "" : htmlUrl.trim();
        this.apkDownloadUrl = apkDownloadUrl == null ? "" : apkDownloadUrl.trim();
        this.apkFileName = apkFileName == null ? "" : apkFileName.trim();
        this.apkSizeBytes = apkSizeBytes;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getApkDownloadUrl() {
        return apkDownloadUrl;
    }

    public String getApkFileName() {
        return apkFileName.isEmpty() ? ("PixelBoard-" + version + ".apk") : apkFileName;
    }

    public long getApkSizeBytes() {
        return apkSizeBytes;
    }

    public boolean hasApkAsset() {
        return apkDownloadUrl != null && !apkDownloadUrl.isEmpty();
    }

    /**
     * Compares this release version against a target version string (e.g. current installed version).
     *
     * @param currentVersion Installed version string (e.g. "1.3.0")
     * @return true if this release version is strictly newer than currentVersion
     */
    public boolean isNewerThan(String currentVersion) {
        return compareVersions(this.version, cleanVersion(currentVersion)) > 0;
    }

    /**
     * Formats the APK size into human-readable MB / KB.
     */
    public String getFormattedFileSize() {
        if (apkSizeBytes <= 0) {
            return "";
        }
        double megabytes = apkSizeBytes / (1024.0 * 1024.0);
        if (megabytes >= 1.0) {
            return String.format(Locale.US, "%.1f MB", megabytes);
        }
        double kilobytes = apkSizeBytes / 1024.0;
        return String.format(Locale.US, "%.0f KB", kilobytes);
    }

    /**
     * Formats ISO-8601 published date into human-readable date (e.g. "Oct 24, 2026").
     */
    public String getFormattedDate() {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date date = isoFormat.parse(publishedAt);
            if (date != null) {
                SimpleDateFormat readableFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                return readableFormat.format(date);
            }
        } catch (ParseException ignored) {
            try {
                if (publishedAt.length() >= 10) {
                    return publishedAt.substring(0, 10);
                }
            } catch (Throwable ignoredSecond) {
            }
        }
        return publishedAt;
    }

    public static class Highlight {
        private final String title;
        private final String description;

        public Highlight(String title, String description) {
            this.title = title != null ? title.trim() : "";
            this.description = description != null ? description.trim() : null;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public boolean hasDescription() {
            return description != null && !description.isEmpty();
        }
    }

    /**
     * Extracts structured highlights (title + description) from markdown changelog.
     */
    public List<Highlight> getParsedHighlights() {
        List<Highlight> items = new ArrayList<>();
        if (changelog == null || changelog.isEmpty()) {
            return items;
        }

        String[] lines = changelog.split("\n");
        for (String rawLine : lines) {
            // Skip indented lines (nested sub-bullets like duration lists, options, etc.)
            if (rawLine.startsWith("  ") || rawLine.startsWith("\t")) {
                continue;
            }
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Skip markdown headers, image tags, badges, horizontal rules
            if (trimmed.startsWith("#") || trimmed.startsWith("<") || trimmed.startsWith("![")) {
                continue;
            }
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                continue;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                String bulletText = trimmed.substring(2).trim();

                // Clean markdown links: [#65](https://...) -> #65, [text](url) -> text
                bulletText = bulletText.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1");
                // Clean code backticks: `code` -> code
                bulletText = bulletText.replace("`", "");
                // Clean italics
                bulletText = bulletText.replaceAll("(?<!\\w)_(.+?)_(?!\\w)", "$1");

                if (bulletText.isEmpty()) {
                    continue;
                }

                String itemTitle;
                String itemDesc = null;

                if (bulletText.startsWith("**")) {
                    int endBold = bulletText.indexOf("**", 2);
                    if (endBold > 2) {
                        itemTitle = bulletText.substring(2, endBold).trim();
                        String rest = bulletText.substring(endBold + 2).trim();
                        if (rest.startsWith(":")) {
                            rest = rest.substring(1).trim();
                        }
                        rest = rest.replace("**", "");
                        itemDesc = rest.isEmpty() ? null : rest;
                    } else {
                        itemTitle = bulletText.replace("**", "").trim();
                    }
                } else if (bulletText.contains(": ") && !bulletText.startsWith("http")) {
                    int colonIdx = bulletText.indexOf(": ");
                    itemTitle = bulletText.substring(0, colonIdx).replace("**", "").trim();
                    itemDesc = bulletText.substring(colonIdx + 2).replace("**", "").trim();
                } else {
                    itemTitle = bulletText.replace("**", "").trim();
                }

                // Skip generic headings that have no description (e.g. "Target Versions:")
                if (itemTitle.endsWith(":") && (itemDesc == null || itemDesc.isEmpty())) {
                    continue;
                }

                if (!itemTitle.isEmpty()) {
                    items.add(new Highlight(itemTitle, itemDesc));
                }
            }
        }
        return items;
    }

    /**
     * Extracts readable bullet points from the markdown changelog for display in settings.
     */
    public List<String> getChangelogHighlights() {
        List<String> items = new ArrayList<>();
        for (Highlight h : getParsedHighlights()) {
            if (h.hasDescription()) {
                items.add(h.getTitle() + ": " + h.getDescription());
            } else {
                items.add(h.getTitle());
            }
        }
        return items;
    }

    public static String cleanVersion(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1).trim();
        }
        return cleaned;
    }

    /**
     * Semantic version comparator.
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        String clean1 = cleanVersion(v1);
        String clean2 = cleanVersion(v2);

        String[] parts1 = clean1.split("[.-]");
        String[] parts2 = clean2.split("[.-]");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                num1 = parseIntegerSegment(parts1[i]);
            }
            if (i < parts2.length) {
                num2 = parseIntegerSegment(parts2[i]);
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static int parseIntegerSegment(String part) {
        if (part == null || part.isEmpty()) {
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
