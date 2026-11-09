/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public final class GboardRamblerDictationRuntimeTest {

    public static class FakeDiff {
        public int b;
        public String c; // prefix
        public String d; // to_replace
        public String e; // suffix
    }

    public static class FakeAction {
        public int b; // has_bits
        public int c; // intent
        public String d; // full_transcript
        public FakeDiff e; // rewrite diff proto
    }

    public static class FakeSpan {
        public String c; // committed text
        public FakeAction j; // action
    }

    public static class FakeProto {
        public List<FakeSpan> b;
    }

    public static class FakeSurroundingText {
        private final String textBefore;
        private final String selectedText;
        private final String textAfter;

        public FakeSurroundingText(String textBefore, String selectedText, String textAfter) {
            this.textBefore = textBefore;
            this.selectedText = selectedText;
            this.textAfter = textAfter;
        }

        public CharSequence n() {
            return textBefore;
        }

        public CharSequence l() {
            return selectedText;
        }

        public CharSequence m() {
            return textAfter;
        }
    }

    public static class FakeExtension {
        public FakeSurroundingText i;

        public FakeExtension(String textBefore, String selectedText, String textAfter) {
            this.i = new FakeSurroundingText(textBefore, selectedText, textAfter);
        }
    }

    @Test
    public void filtersAiPromptAnswerRestoringVerbatimTranscript() {
        FakeAction action = new FakeAction();
        action.c = 4; // INTENT_COMPOSE
        action.d = "What is the capital of France?";

        FakeSpan span = new FakeSpan();
        span.c = "Paris is the capital of France.";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(proto);

        Assert.assertEquals("What is the capital of France?", span.c);
        Assert.assertEquals(0, action.c);
    }

    @Test
    public void preservesNormalDictationVerbatimWhenFieldEmpty() {
        FakeAction action = new FakeAction();
        action.c = 0; // DEFAULT_INTENT_DICTATION
        action.d = "Hello world";

        FakeSpan span = new FakeSpan();
        span.c = "Hello world";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        FakeExtension ext = new FakeExtension("", "", "");
        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        Assert.assertEquals("Hello world", span.c);
        Assert.assertEquals(0, action.c);
    }

    @Test
    public void test1_repeatedPhraseIsAppendedRatherThanDropped() {
        // User previously dictated "Тридцать восемь попугаев пошли купаться в море. Ни один не выплыл."
        String existing = "Тридцать восемь попугаев пошли купаться в море. Ни один не выплыл.";
        FakeExtension ext = new FakeExtension(existing, "", "");

        // User speaks the exact same sentence into the mic:
        FakeAction action = new FakeAction();
        action.c = 0; // DEFAULT_INTENT_DICTATION
        action.d = existing;

        FakeSpan span = new FakeSpan();
        // Server deduped with context and sent span.c equal to existing:
        span.c = existing;
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        // Committed span must start with textBefore and include the repeated phrase with space:
        String expected = existing + " " + existing;
        Assert.assertEquals(expected, span.c);
        Assert.assertEquals(0, action.c);
    }

    @Test
    public void test2_similarPhraseDoesNotOverwritePrecedingText() {
        // User has in editor: "Я протестую, ваша честь, — сказал прокурор."
        String existing = "Я протестую, ваша честь, — сказал прокурор.";
        FakeExtension ext = new FakeExtension(existing, "", "");

        // User speaks: "Я протестую, ваша честь, — сказал адвокат."
        String spoken = "Я протестую, ваша честь, — сказал адвокат.";
        FakeAction action = new FakeAction();
        action.b = 0x10;
        action.c = 3; // Server mistook speech for INTENT_REWRITE
        action.d = spoken;

        FakeDiff diff = new FakeDiff();
        diff.c = "Я протестую, ваша честь, — сказал ";
        diff.d = "прокурор.";
        diff.e = "";
        action.e = diff;

        FakeSpan span = new FakeSpan();
        span.c = spoken;
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        // Diff must be cleared, intent reset to dictation (0), and span formatted with textBefore:
        Assert.assertNull(action.e);
        Assert.assertEquals(0, action.c);
        Assert.assertEquals(0, action.b & 0x10);

        String expected = existing + " " + spoken;
        Assert.assertEquals(expected, span.c);
    }

    @Test
    public void preservesContextualRewriteVoiceCommand() {
        FakeExtension ext = new FakeExtension("Hello world", "", "");

        FakeAction action = new FakeAction();
        action.c = 3; // INTENT_REWRITE (e.g. "Translate to Bengali")
        action.d = "Translate to Bengali";

        FakeDiff diff = new FakeDiff();
        diff.c = "";
        diff.d = "Hello world";
        diff.e = "";
        action.e = diff;

        FakeSpan span = new FakeSpan();
        span.c = "আজ আমি দেরি করব";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        // Committed text, diff, and rewrite intent must be preserved for genuine commands:
        Assert.assertEquals("আজ আমি দেরি করব", span.c);
        Assert.assertEquals(3, action.c);
        Assert.assertNotNull(action.e);
    }

    @Test
    public void preservesMakeItAllCapsVoiceCommand() {
        FakeExtension ext = new FakeExtension("this is a test", "", "");

        FakeAction action = new FakeAction();
        action.c = 3; // INTENT_REWRITE
        action.d = "make it all caps";

        FakeDiff diff = new FakeDiff();
        diff.c = "";
        diff.d = "this is a test";
        diff.e = "";
        action.e = diff;

        FakeSpan span = new FakeSpan();
        span.c = "THIS IS A TEST";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        Assert.assertEquals("THIS IS A TEST", span.c);
        Assert.assertEquals(3, action.c);
        Assert.assertNotNull(action.e);
    }

    @Test
    public void preservesVoiceEditCommands() {
        FakeAction action = new FakeAction();
        action.c = 1; // INTENT_EDIT (e.g. "delete that")
        action.d = "delete that";

        FakeSpan span = new FakeSpan();
        span.c = "";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(proto);

        Assert.assertEquals("", span.c);
        Assert.assertEquals(1, action.c);
    }

    @Test
    public void preservesVoiceUndoCommands() {
        FakeAction action = new FakeAction();
        action.c = 2; // INTENT_EDIT_HISTORY (e.g. "undo")
        action.d = "undo";

        FakeSpan span = new FakeSpan();
        span.c = "";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(proto);

        Assert.assertEquals("", span.c);
        Assert.assertEquals(2, action.c);
    }

    @Test
    public void preservesNormalDictationWithExistingPrefixAndDelta() {
        String existing = "Hello ";
        FakeExtension ext = new FakeExtension(existing, "", "");

        FakeAction action = new FakeAction();
        action.c = 0; // DEFAULT_INTENT_DICTATION
        action.d = "world";

        FakeSpan span = new FakeSpan();
        // Server already sent textBefore + delta:
        span.c = "Hello world";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(ext, proto);

        // Already has prefix and delta, left untouched:
        Assert.assertEquals("Hello world", span.c);
        Assert.assertEquals(0, action.c);
    }

    @Test
    public void handlesNullOrMalformedGracefully() {
        GboardRamblerDictationRuntime.filterVoiceText(null);
        GboardRamblerDictationRuntime.filterVoiceText("not_a_proto");
        GboardRamblerDictationRuntime.filterVoiceText(null, null);
        GboardRamblerDictationRuntime.filterVoiceText("not_an_ext", "not_a_proto");
    }
}
