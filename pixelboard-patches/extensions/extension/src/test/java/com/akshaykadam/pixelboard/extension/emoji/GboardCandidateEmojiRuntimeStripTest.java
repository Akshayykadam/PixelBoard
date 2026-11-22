package com.akshaykadam.pixelboard.extension.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GboardCandidateEmojiRuntimeStripTest {

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

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(3, result.size());
        assertEquals("funny", result.get(0).a.toString());
        assertEquals(0, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(0)));

        assertEquals("runny", result.get(1).a.toString());
        assertEquals(1, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(1)));

        assertEquals("😂", result.get(2).a.toString());
        assertEquals(2, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(2)));
    }

    @Test
    public void testReorderCandidatesForStrip_fourCandidates_emojiPlacedInSlot2() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("fun", 0));
        candidates.add(new TestCandidate("😂", 1));
        candidates.add(new TestCandidate("funny", 2));
        candidates.add(new TestCandidate("function", 3));

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(4, result.size());
        // Slot 0 (left): raw text
        assertEquals("fun", result.get(0).a.toString());
        // Slot 1 (center): prediction autocompletion
        assertEquals("funny", result.get(1).a.toString());
        // Slot 2 (right): suggested emoji!
        assertEquals("😂", result.get(2).a.toString());
        // Slot 3 (overflow): next text candidate
        assertEquals("function", result.get(3).a.toString());

        for (int i = 0; i < result.size(); i++) {
            assertEquals(i, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(i)));
        }
    }

    @Test
    public void testReorderCandidatesForStrip_immutableList_success() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> original = new java.util.ArrayList<>();
        original.add(new TestCandidate("fun", 0));
        original.add(new TestCandidate("😂", 1));
        original.add(new TestCandidate("funny", 2));
        java.util.List<TestCandidate> immutable = java.util.Collections.unmodifiableList(original);

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, immutable);

        assertEquals(3, result.size());
        assertEquals("fun", result.get(0).a.toString());
        assertEquals("funny", result.get(1).a.toString());
        assertEquals("😂", result.get(2).a.toString());
    }

    @Test
    public void testReorderCandidatesForStrip_twoCandidates_emojiOnRight() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("funny", 0));
        candidates.add(new TestCandidate("😂", 1));

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(2, result.size());
        assertEquals("funny", result.get(0).a.toString());
        assertEquals(0, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(0)));

        assertEquals("😂", result.get(1).a.toString());
        assertEquals(1, GboardCandidateEmojiRuntime.getCandidateSlot(result.get(1)));
    }

    @Test
    public void testReorderCandidatesForStrip_noEmoji_untouched() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("funny", 0));
        candidates.add(new TestCandidate("fun", 1));
        candidates.add(new TestCandidate("runny", 2));

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(3, result.size());
        assertEquals("funny", result.get(0).a.toString());
        assertEquals(0, result.get(0).l);
        assertEquals("fun", result.get(1).a.toString());
        assertEquals(1, result.get(1).l);
        assertEquals("runny", result.get(2).a.toString());
        assertEquals(2, result.get(2).l);
    }

    @Test
    public void testReorderCandidatesForStrip_allEmojis_untouched() {
        TestHolderView holderView = new TestHolderView();
        java.util.List<TestCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(new TestCandidate("😂", 0));
        candidates.add(new TestCandidate("🤣", 1));

        @SuppressWarnings("unchecked")
        java.util.List<TestCandidate> result =
                (java.util.List<TestCandidate>) GboardCandidateEmojiRuntime.reorderCandidatesForStrip(holderView, candidates);

        assertEquals(2, result.size());
        assertEquals("😂", result.get(0).a.toString());
        assertEquals("🤣", result.get(1).a.toString());
    }
}
