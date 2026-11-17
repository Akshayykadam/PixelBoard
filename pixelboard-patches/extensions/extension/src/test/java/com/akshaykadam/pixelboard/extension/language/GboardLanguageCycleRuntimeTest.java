package com.akshaykadam.pixelboard.extension.language;

import android.content.SharedPreferences;

import com.akshaykadam.pixelboard.extension.settings.GboardLanguageCycleSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exercises {@link GboardLanguageCycleRuntime#nextEntry} against fakes shaped like Gboard
 * 18.6.3's input method entries (oyq: i(), k(), q()) and DynamicRotationList (qyf: entries
 * List, order int[], g(entry, wrap)), driven the way switchToNextInputMethodEntry drives them.
 */
@RunWith(RobolectricTestRunner.class)
public final class GboardLanguageCycleRuntimeTest {

    /** Stands in for Gboard's input method entry interface (oyq). */
    public interface Entry {
    }

    /** Stands in for a language tag (rzf); only toString() is used. */
    public static final class Tag {
        private final String tag;

        Tag(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    /** Stands in for an input method entry (oyz). */
    public static final class FakeEntry implements Entry {
        private final String name;
        private final Tag primary;
        private final String layout;
        private final List<Entry> secondaries = new ArrayList<>();

        FakeEntry(String name, String primary, String layout, FakeEntry... secondaries) {
            this.name = name;
            this.primary = new Tag(primary);
            this.layout = layout;
            this.secondaries.addAll(Arrays.asList(secondaries));
        }

        public Tag i() {
            return primary;
        }

        /** Language-only quirk seen on device (e.g. "de" for de-DE); must not be relied on. */
        public List<String> j() {
            List<String> languages = new ArrayList<>();
            for (Entry secondary : secondaries) {
                languages.add(((FakeEntry) secondary).primary.toString().split("-")[0]);
            }
            return languages;
        }

        public List<Entry> k() {
            return secondaries;
        }

        public String q() {
            return layout;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Stands in for DynamicRotationList (qyf), including R8-merged unrelated members. */
    public static final class FakeRotation {
        public static volatile FakeRotation sharedInstance;
        public final Object b;
        public final Object c;

        FakeRotation(Entry... entries) {
            b = Arrays.asList(entries);
            int[] order = new int[entries.length];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            c = order;
        }

        private int indexOf(Entry entry) {
            List<?> entries = (List<?>) b;
            int[] order = (int[]) c;
            for (int i = 0; i < order.length; i++) {
                if (entries.get(order[i]) == entry) {
                    return i;
                }
            }
            return -1;
        }

        public Entry g(Entry entry, boolean wrap) {
            int index = indexOf(entry);
            if (index < 0) {
                return null;
            }
            int next = index + 1;
            int[] order = (int[]) c;
            if (next == order.length) {
                if (!wrap) {
                    return null;
                }
                next = 0;
            }
            return (Entry) ((List<?>) b).get(order[next]);
        }

        public Object unrelated(Object value, boolean flag) {
            return value;
        }
    }

    private static final FakeEntry DE_SECONDARY = new FakeEntry("de", "de-DE", "qwerty");
    private static final FakeEntry EN_SECONDARY = new FakeEntry("en", "en-US", "qwerty");
    private static final FakeEntry EN_DE = new FakeEntry("EN+DE", "en-US", "qwerty", DE_SECONDARY);
    private static final FakeEntry DE_EN = new FakeEntry("DE+EN", "de-DE", "qwerty", EN_SECONDARY);
    private static final FakeEntry AR = new FakeEntry("AR", "ar-EG", "arabic");

    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = GboardPatchesSettings.preferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().commit();
    }

    @Test
    public void offByDefaultKeepsStockCycling() {
        FakeRotation rotation = new FakeRotation(EN_DE, DE_EN, AR);

        Assert.assertEquals("EN+DE DE+EN AR EN+DE", cycle(rotation, EN_DE, 3));
    }

    @Test
    public void skipsReversedPairWhenEnabled() {
        enable();
        FakeRotation rotation = new FakeRotation(EN_DE, DE_EN, AR);

        Assert.assertEquals("EN+DE AR EN+DE AR EN+DE", cycle(rotation, EN_DE, 4));
    }

    @Test
    public void leavingTheSkippedEntryStillWorks() {
        enable();
        FakeRotation rotation = new FakeRotation(EN_DE, DE_EN, AR);

        // DE+EN picked from the long-press picker: the globe key moves on and then skips it.
        Assert.assertEquals("DE+EN AR EN+DE AR", cycle(rotation, DE_EN, 3));
    }

    @Test
    public void listOrderDecidesWhichDuplicateIsKept() {
        enable();
        FakeRotation rotation = new FakeRotation(DE_EN, EN_DE, AR);

        Assert.assertEquals("DE+EN AR DE+EN AR", cycle(rotation, DE_EN, 3));
    }

    @Test
    public void collapsesThreeLanguageCombinations() {
        enable();
        FakeEntry fr = new FakeEntry("fr", "fr-FR", "qwerty");
        FakeEntry enDeFr = new FakeEntry("EN+DE+FR", "en-US", "qwerty", DE_SECONDARY, fr);
        FakeEntry deEnFr = new FakeEntry("DE+EN+FR", "de-DE", "qwerty", EN_SECONDARY, fr);
        FakeEntry frEnDe = new FakeEntry("FR+EN+DE", "fr-FR", "qwerty", EN_SECONDARY, DE_SECONDARY);
        FakeRotation rotation = new FakeRotation(enDeFr, deEnFr, frEnDe, AR);

        Assert.assertEquals("EN+DE+FR AR EN+DE+FR", cycle(rotation, enDeFr, 2));
    }

    @Test
    public void keepsDifferentLanguageSetsLayoutsAndRegions() {
        enable();
        FakeEntry fr = new FakeEntry("fr", "fr-FR", "qwerty");
        FakeEntry partialOverlap = new FakeEntry("DE+EN+FR", "de-DE", "qwerty", EN_SECONDARY, fr);
        FakeEntry otherLayout = new FakeEntry("DE+EN qwertz", "de-DE", "qwertz",
                new FakeEntry("en", "en-US", "qwertz"));
        FakeEntry otherRegion = new FakeEntry("EN-GB+DE", "en-GB", "qwerty", DE_SECONDARY);

        Assert.assertEquals("EN+DE DE+EN+FR AR EN+DE",
                cycle(new FakeRotation(EN_DE, partialOverlap, AR), EN_DE, 3));
        Assert.assertEquals("EN+DE DE+EN qwertz AR EN+DE",
                cycle(new FakeRotation(EN_DE, otherLayout, AR), EN_DE, 3));
        Assert.assertEquals("EN+DE EN-GB+DE AR EN+DE",
                cycle(new FakeRotation(EN_DE, otherRegion, AR), EN_DE, 3));
    }

    @Test
    public void entriesWithoutMultilingualTypingAreNeverSkipped() {
        enable();
        FakeEntry en = new FakeEntry("EN", "en-US", "qwerty");
        FakeEntry de = new FakeEntry("DE", "de-DE", "qwerty");
        FakeRotation rotation = new FakeRotation(en, de, AR);

        Assert.assertEquals("EN DE AR EN", cycle(rotation, en, 3));
    }

    @Test
    public void onlyDuplicatesLeftKeepsTheCurrentEntry() {
        enable();
        FakeRotation rotation = new FakeRotation(EN_DE, DE_EN);

        Assert.assertEquals("EN+DE EN+DE", cycle(rotation, EN_DE, 1));
    }

    @Test
    public void unknownRotationListFallsBackToNull() {
        enable();

        Assert.assertNull(GboardLanguageCycleRuntime.nextEntry(new Object(), EN_DE, true));
        Assert.assertNull(GboardLanguageCycleRuntime.nextEntry(null, EN_DE, true));
    }

    private void enable() {
        GboardLanguageCycleSettings.writeSkipDuplicatePairs(preferences, true);
    }

    /** Mirrors switchToNextInputMethodEntry: no-wrap lookup first, then the wrapping one. */
    private static Entry globe(FakeRotation rotation, Entry current) {
        Entry next = (Entry) GboardLanguageCycleRuntime.nextEntry(rotation, current, false);
        if (next == null) {
            next = (Entry) GboardLanguageCycleRuntime.nextEntry(rotation, current, true);
        }
        return next == null || next.equals(current) ? current : next;
    }

    private static String cycle(FakeRotation rotation, Entry start, int presses) {
        StringBuilder out = new StringBuilder(start.toString());
        Entry current = start;
        for (int i = 0; i < presses; i++) {
            current = globe(rotation, current);
            out.append(' ').append(current);
        }
        return out.toString();
    }
}
