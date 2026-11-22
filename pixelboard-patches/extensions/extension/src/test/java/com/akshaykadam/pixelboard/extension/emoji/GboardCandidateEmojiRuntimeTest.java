package com.akshaykadam.pixelboard.extension.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.TextPaint;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GboardCandidateEmojiRuntimeTest {

    @Test
    public void testContainsEmoji_emptyAndNull() {
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji(null));
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji(""));
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji("   "));
    }

    @Test
    public void testContainsEmoji_plainText() {
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji("Hello world"));
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji("1234567890"));
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji("What is this?"));
        assertFalse(GboardCandidateEmojiRuntime.containsEmoji("test@example.com"));
    }

    @Test
    public void testContainsEmoji_surrogatePairs() {
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("😊"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("🚀"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("🔥"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("Good morning 😊"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("👍🏻"));
    }

    @Test
    public void testContainsEmoji_bmpSymbolsAndPresentationSelectors() {
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("❤️"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("☕️"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("✨"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("⭐"));
        assertTrue(GboardCandidateEmojiRuntime.containsEmoji("⚡"));
    }

    @Test
    public void testAdjustCandidatePaint_forcesFullAlphaForEmoji() {
        Context context = RuntimeEnvironment.getApplication();
        TextView textView = new TextView(context);
        TextPaint paint = new TextPaint();

        // 60% translucent secondary text color (alpha ~ 0x99)
        int initialColor = 0x99444444;
        paint.setColor(initialColor);
        textView.setText("😊");

        GboardCandidateEmojiRuntime.adjustCandidatePaint(textView, paint);

        assertEquals(255, paint.getAlpha());
        assertEquals(0xFF444444, paint.getColor());
    }

    @Test
    public void testAdjustCandidatePaint_preservesNormalTextPaint() {
        Context context = RuntimeEnvironment.getApplication();
        TextView textView = new TextView(context);
        TextPaint paint = new TextPaint();

        int initialColor = 0x99444444;
        paint.setColor(initialColor);
        textView.setText("suggested");

        GboardCandidateEmojiRuntime.adjustCandidatePaint(textView, paint);

        assertEquals(initialColor, paint.getColor());
    }

    public static class TestCandidate {
        public final CharSequence a;
        public int l;

        public TestCandidate(CharSequence a, int l) {
            this.a = a;
            this.l = l;
        }
    }

    public static class TestHolderView {
        public int d = 3;
    }

    @Test
    public void testReorderCandidatesForStrip_movesMiddleEmojiToRightSlot() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("funny", 0));
        candidates.add(new TestCandidate("😂", 1));
        candidates.add(new TestCandidate("runny", 2));

        GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(3, candidates.size());
        assertEquals("funny", candidates.get(0).a.toString());
        assertEquals(0, GboardCandidateEmojiRuntime.getCandidateSlot(candidates.get(0)));

        assertEquals("runny", candidates.get(1).a.toString());
        assertEquals(1, GboardCandidateEmojiRuntime.getCandidateSlot(candidates.get(1)));

        assertEquals("😂", candidates.get(2).a.toString());
        assertEquals(2, GboardCandidateEmojiRuntime.getCandidateSlot(candidates.get(2)));
    }

    @Test
    public void testReorderCandidatesForStrip_twoCandidates_emojiOnRight() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("funny", 0));
        candidates.add(new TestCandidate("😂", 1));

        GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(2, candidates.size());
        assertEquals("funny", candidates.get(0).a.toString());
        assertEquals(0, GboardCandidateEmojiRuntime.getCandidateSlot(candidates.get(0)));

        assertEquals("😂", candidates.get(1).a.toString());
        assertEquals(2, GboardCandidateEmojiRuntime.getCandidateSlot(candidates.get(1)));
    }

    @Test
    public void testReorderCandidatesForStrip_noEmoji_untouched() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("funny", 0));
        candidates.add(new TestCandidate("fun", 1));
        candidates.add(new TestCandidate("runny", 2));

        GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(3, candidates.size());
        assertEquals("funny", candidates.get(0).a.toString());
        assertEquals(0, candidates.get(0).l);
        assertEquals("fun", candidates.get(1).a.toString());
        assertEquals(1, candidates.get(1).l);
        assertEquals("runny", candidates.get(2).a.toString());
        assertEquals(2, candidates.get(2).l);
    }

    @Test
    public void testReorderCandidatesForStrip_allEmojis_untouched() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("😂", 0));
        candidates.add(new TestCandidate("🤣", 1));

        GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(2, candidates.size());
        assertEquals("😂", candidates.get(0).a.toString());
        assertEquals("🤣", candidates.get(1).a.toString());
    }
}
