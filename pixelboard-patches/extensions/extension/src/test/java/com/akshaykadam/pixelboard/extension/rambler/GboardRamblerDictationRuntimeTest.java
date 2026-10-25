package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public final class GboardRamblerDictationRuntimeTest {

    public static class FakeAction {
        public int c; // intent
        public String d; // full_transcript
    }

    public static class FakeSpan {
        public String c; // committed text
        public FakeAction j; // action
    }

    public static class FakeProto {
        public List<FakeSpan> b;
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
    public void preservesNormalDictationVerbatim() {
        FakeAction action = new FakeAction();
        action.c = 0; // DEFAULT_INTENT_DICTATION
        action.d = "Hello world";

        FakeSpan span = new FakeSpan();
        span.c = "Hello world";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(proto);

        Assert.assertEquals("Hello world", span.c);
        Assert.assertEquals(0, action.c);
    }

    @Test
    public void preservesContextualRewriteVoiceCommand() {
        FakeAction action = new FakeAction();
        action.c = 3; // INTENT_REWRITE (e.g. "Translate to Bengali", "Fix grammar")
        action.d = "Translate to Bengali";

        FakeSpan span = new FakeSpan();
        span.c = "আজ আমি দেরি করব";
        span.j = action;

        FakeProto proto = new FakeProto();
        proto.b = Collections.singletonList(span);

        GboardRamblerDictationRuntime.filterVoiceText(proto);

        // Committed text and rewrite intent must be preserved so Gboard executes the command
        Assert.assertEquals("আজ আমি দেরি করব", span.c);
        Assert.assertEquals(3, action.c);
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
    public void handlesNullOrMalformedGracefully() {
        GboardRamblerDictationRuntime.filterVoiceText(null);
        GboardRamblerDictationRuntime.filterVoiceText("not_a_proto");
    }
}
