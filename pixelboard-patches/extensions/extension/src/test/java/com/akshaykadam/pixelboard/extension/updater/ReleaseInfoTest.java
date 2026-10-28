/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.updater;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ReleaseInfoTest {

    @Test
    public void versionCleaningHandlesVPrefixAndWhitespace() {
        Assert.assertEquals("1.3.0", ReleaseInfo.cleanVersion("v1.3.0"));
        Assert.assertEquals("1.3.0", ReleaseInfo.cleanVersion("V1.3.0"));
        Assert.assertEquals("1.3.0", ReleaseInfo.cleanVersion(" 1.3.0 "));
        Assert.assertEquals("1.3.0", ReleaseInfo.cleanVersion("  v1.3.0  "));
        Assert.assertEquals("", ReleaseInfo.cleanVersion(null));
    }

    @Test
    public void versionComparisonDetectsNewerReleases() {
        ReleaseInfo v140 = new ReleaseInfo(
                "v1.4.0",
                "PixelBoard 1.4.0",
                "Notes",
                "2026-10-24T12:00:00Z",
                "https://github.com/Akshayykadam/PixelBoard/releases/tag/1.4.0",
                "https://github.com/Akshayykadam/PixelBoard/releases/download/1.4.0/PixelBoard-1.4.0.apk",
                "PixelBoard-1.4.0.apk",
                82000000L);

        Assert.assertTrue(v140.isNewerThan("1.3.0"));
        Assert.assertTrue(v140.isNewerThan("v1.3.0"));
        Assert.assertTrue(v140.isNewerThan("1.3.9"));
        Assert.assertFalse(v140.isNewerThan("1.4.0"));
        Assert.assertFalse(v140.isNewerThan("1.4.1"));
        Assert.assertFalse(v140.isNewerThan("2.0.0"));
    }

    @Test
    public void versionComparisonHandlesPatchReleases() {
        ReleaseInfo patchRelease = new ReleaseInfo(
                "1.3.1",
                "PixelBoard 1.3.1 Patch",
                "Patch notes",
                "2026-10-24T12:00:00Z",
                "",
                "",
                "",
                0L);

        Assert.assertTrue(patchRelease.isNewerThan("1.3.0"));
        Assert.assertFalse(patchRelease.isNewerThan("1.3.1"));
        Assert.assertFalse(patchRelease.isNewerThan("1.3.2"));
        Assert.assertFalse(patchRelease.isNewerThan("1.3.3"));
    }

    @Test
    public void fileSizeFormattingReturnsReadableUnits() {
        ReleaseInfo release = new ReleaseInfo(
                "1.4.0",
                "PixelBoard 1.4.0",
                "",
                "",
                "",
                "",
                "",
                82837504L); // ~79.0 MB

        Assert.assertEquals("79.0 MB", release.getFormattedFileSize());
    }

    @Test
    public void changelogExtractorPullsCleanBulletPoints() {
        String markdown = "# Release 1.4.0\n"
                + "\n"
                + "[![Badge](https://img.shields.io)](https://example.com)\n"
                + "\n"
                + "--- \n"
                + "\n"
                + "### Highlights\n"
                + "- **Rambler Voice Typing**: Ultra-fast voice dictation\n"
                + "* **AI Writing Tools**: In-line proofreading support\n"
                + "• **Modern Spell Checker**: Suggestion chips for Android 12+\n"
                + "- Added in-app updater directly from Git\n";

        ReleaseInfo release = new ReleaseInfo(
                "1.4.0",
                "PixelBoard 1.4.0",
                markdown,
                "2026-10-24T12:00:00Z",
                "",
                "",
                "",
                0L);

        List<String> highlights = release.getChangelogHighlights();
        Assert.assertEquals(4, highlights.size());
        Assert.assertEquals("Rambler Voice Typing: Ultra-fast voice dictation", highlights.get(0));
        Assert.assertEquals("AI Writing Tools: In-line proofreading support", highlights.get(1));
        Assert.assertEquals("Modern Spell Checker: Suggestion chips for Android 12+", highlights.get(2));
        Assert.assertEquals("Added in-app updater directly from Git", highlights.get(3));

        List<ReleaseInfo.Highlight> parsed = release.getParsedHighlights();
        Assert.assertEquals(4, parsed.size());
        Assert.assertEquals("Rambler Voice Typing", parsed.get(0).getTitle());
        Assert.assertEquals("Ultra-fast voice dictation", parsed.get(0).getDescription());
        Assert.assertEquals("AI Writing Tools", parsed.get(1).getTitle());
        Assert.assertEquals("In-line proofreading support", parsed.get(1).getDescription());
    }
}
