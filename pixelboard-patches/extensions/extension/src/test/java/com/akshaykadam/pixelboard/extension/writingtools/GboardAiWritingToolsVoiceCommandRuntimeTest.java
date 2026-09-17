package com.akshaykadam.pixelboard.extension.writingtools;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public final class GboardAiWritingToolsVoiceCommandRuntimeTest {
    @Test
    public void protoLanguagesAreCopiedFinalizedCachedAndFailOpen() {
        FakeProto original = new FakeProto();
        original.b.addAll(Arrays.asList("en", "fr"));
        FakeProtoFlag flag = new FakeProtoFlag(
                "writing_tools_voice_commands_i18n_languages");

        Object merged = GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(true, flag, original);

        Assert.assertTrue(merged instanceof FakeProto);
        Assert.assertNotSame(original, merged);
        Assert.assertEquals(
                Arrays.asList("en", "fr", "de", "es", "it", "ja"),
                ((FakeProto) merged).b);
        Assert.assertTrue(((FakeProtoBase) merged).finalized);
        Assert.assertSame(merged, GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(true, flag, original));
        Assert.assertSame(original, GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(false, flag, original));
        Assert.assertSame(original, GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(
                        true,
                        new FakeProtoFlag("unrelated"),
                        original));
        Assert.assertSame(original, GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(true, new BrokenFlag(), original));
    }

    @Test
    public void completeProtoPreservesOriginalIdentity() {
        FakeProto original = new FakeProto();
        original.b.addAll(Arrays.asList("en", "de", "es", "fr", "it", "ja"));

        Assert.assertSame(original, GboardAiWritingToolsVoiceCommandRuntime
                .maybeMergeVoiceCommandLanguages(
                        true,
                        new FakeProtoFlag("writing_tools_voice_commands_i18n_languages"),
                        original));
    }

    @Test
    public void runtimeAdaptersPromoteOnlyExactTransientStates() throws Exception {
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandRuntime
                .applyUnknownClientTypeGate(
                        true,
                        FakeClientType.CLIENT_TYPE_UNKNOWN,
                        false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .applyUnknownClientTypeGate(
                        true,
                        FakeClientType.CLIENT_TYPE_REGULAR_DICTATION,
                        false));

        FakeBooleanFlag modeless = new FakeBooleanFlag(
                "enable_nga_lab_modeless_smartedit");
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandRuntime
                .shouldEnableSmartEditModule(true, modeless));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .shouldEnableSmartEditModule(
                        true,
                        new FakeBooleanFlag("unrelated")));

        FakeAmbiguity ambiguity = new FakeAmbiguity();
        GboardAiWritingToolsVoiceCommandRuntime
                .applyCommandAmbiguityCapability(true, ambiguity);
        Assert.assertTrue(ambiguity.b);

        Assert.assertEquals(Boolean.TRUE, GboardAiWritingToolsVoiceCommandRuntime
                .applyClassifierModelessFlagValue(true, Boolean.FALSE));
        Assert.assertEquals("v3", GboardAiWritingToolsVoiceCommandRuntime
                .applyClassificationRegexVersion(true, ""));
    }

    @Test
    public void publicInjectedEntrypointsFailOpenWithoutAHostContext() {
        Object stockObject = new Object();

        Assert.assertTrue(GboardAiWritingToolsVoiceCommandRuntime
                .applyGenAiInitClientType(new BrokenFlag(), true));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .applySmartEditInitClientType(new BrokenFlag(), false));
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .shouldEnableSmartEditModule(new BrokenFlag()));
        GboardAiWritingToolsVoiceCommandRuntime
                .afterCommandAmbiguityConstructed(new BrokenFlag());
        Assert.assertSame(stockObject, GboardAiWritingToolsVoiceCommandRuntime
                .applyClassifierModelessFlagValue(stockObject));
        Assert.assertSame(stockObject, GboardAiWritingToolsVoiceCommandRuntime
                .applyClassificationRegexVersion(stockObject));
        Assert.assertSame(stockObject, GboardAiWritingToolsVoiceCommandRuntime
                .applyVoiceCommandLanguages(new BrokenFlag(), stockObject));
        Assert.assertNull(GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        null,
                        new BrokenSmartEditModuleFixture.Manager(),
                        BrokenSmartEditModuleFixture.hhf.class));
    }

    @Test
    public void smartRefreshReplaysRememberedGenAiInitOnlyOncePerActivation() {
        FakeManager manager = new FakeManager();
        FakeHandler handler = new FakeHandler();
        Object module = new Object();

        GboardAiWritingToolsVoiceCommandRuntime.rememberGenAiInitCall(
                handler, manager, module);
        GboardAiWritingToolsVoiceCommandRuntime.beginGenAiRefresh(manager);
        GboardAiWritingToolsVoiceCommandRuntime.observeGenAiRefreshClientType(
                manager, FakeClientType.CLIENT_TYPE_SMART_DICTATION);
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandRuntime
                .finishGenAiRefresh(true, manager));
        Assert.assertSame(module, manager.genAiVoiceEditManager);
        Assert.assertEquals(1, handler.initCount);

        manager.genAiVoiceEditManager = null;
        GboardAiWritingToolsVoiceCommandRuntime.beginGenAiRefresh(manager);
        GboardAiWritingToolsVoiceCommandRuntime.observeGenAiRefreshClientType(
                manager, FakeClientType.CLIENT_TYPE_SMART_DICTATION);
        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .finishGenAiRefresh(true, manager));
        Assert.assertNull(manager.genAiVoiceEditManager);
        Assert.assertEquals(1, handler.initCount);

        GboardAiWritingToolsVoiceCommandRuntime.rememberGenAiInitCall(
                handler, manager, module);
        GboardAiWritingToolsVoiceCommandRuntime.beginGenAiRefresh(manager);
        GboardAiWritingToolsVoiceCommandRuntime.observeGenAiRefreshClientType(
                manager, FakeClientType.CLIENT_TYPE_SMART_DICTATION);
        Assert.assertTrue(GboardAiWritingToolsVoiceCommandRuntime
                .finishGenAiRefresh(true, manager));
        Assert.assertSame(module, manager.genAiVoiceEditManager);
        Assert.assertEquals(2, handler.initCount);
    }

    @Test
    public void regularDictationRefreshDoesNotReplayGenAiInit() {
        FakeManager manager = new FakeManager();
        FakeHandler handler = new FakeHandler();

        GboardAiWritingToolsVoiceCommandRuntime.rememberGenAiInitCall(
                handler, manager, new Object());
        GboardAiWritingToolsVoiceCommandRuntime.beginGenAiRefresh(manager);
        GboardAiWritingToolsVoiceCommandRuntime.observeGenAiRefreshClientType(
                manager, FakeClientType.CLIENT_TYPE_REGULAR_DICTATION);

        Assert.assertFalse(GboardAiWritingToolsVoiceCommandRuntime
                .finishGenAiRefresh(true, manager));
        Assert.assertEquals(0, handler.initCount);
    }

    @Test
    public void smartEditGeneratorPreservesStockAndUsesOfficialLoaderBeforeFallback()
            throws Throwable {
        SmartEditModuleFixture.Manager manager = new SmartEditModuleFixture.Manager();
        SmartEditModuleFixture.hhf stock = new SmartEditModuleFixture.hhi();
        SmartEditModuleFixture.hhf loaded = new SmartEditModuleFixture.hhi();

        Assert.assertSame(stock, GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        stock,
                        manager,
                        SmartEditModuleFixture.hhf.class));

        manager.loaded = loaded;
        Assert.assertSame(loaded, GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        null,
                        manager,
                        SmartEditModuleFixture.hhf.class));
        Assert.assertEquals(1, manager.loadCount);
    }

    @Test
    public void smartEditGeneratorCreatesInitializesAndCachesOfficialFallback()
            throws Throwable {
        MissingSmartEditModuleFixture.Manager manager =
                new MissingSmartEditModuleFixture.Manager();

        Object first = GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        null,
                        manager,
                        MissingSmartEditModuleFixture.hhf.class);
        Object second = GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        null,
                        manager,
                        MissingSmartEditModuleFixture.hhf.class);

        Assert.assertTrue(first instanceof MissingSmartEditModuleFixture.hhi);
        Assert.assertSame(first, second);
        Assert.assertSame(manager.b,
                ((MissingSmartEditModuleFixture.hhi) first).context);
        Assert.assertEquals(1,
                ((MissingSmartEditModuleFixture.hhi) first).initCount);
        Assert.assertEquals(1, manager.loadCount);
    }

    @Test
    public void smartEditGeneratorFallbackIsExactScopedAndFailsOpen() {
        BrokenSmartEditModuleFixture.Manager manager =
                new BrokenSmartEditModuleFixture.Manager();

        Assert.assertNull(GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        false,
                        null,
                        manager,
                        BrokenSmartEditModuleFixture.hhf.class));
        Assert.assertNull(GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        null,
                        manager,
                        String.class));
        Assert.assertNull(GboardAiWritingToolsVoiceCommandRuntime
                .restoreSmartEditGeneratorModule(
                        true,
                        null,
                        manager,
                        BrokenSmartEditModuleFixture.hhf.class));
    }

    private enum FakeClientType {
        CLIENT_TYPE_UNKNOWN,
        CLIENT_TYPE_REGULAR_DICTATION,
        CLIENT_TYPE_SMART_DICTATION
    }

    private static final class FakeManager {
        private Object genAiVoiceEditManager;
    }

    private static final class FakeHandler {
        private int initCount;

        private void c(FakeManager manager, Object module) {
            initCount++;
            manager.genAiVoiceEditManager = module;
        }
    }

    private static final class FakeBooleanFlag {
        private final String a;

        private FakeBooleanFlag(String name) {
            a = name;
        }
    }

    private static final class FakeAmbiguity {
        private boolean b;
    }

    private static final class FakeProtoFlag {
        private final String name;

        private FakeProtoFlag(String name) {
            this.name = name;
        }

        private String h() {
            return name;
        }
    }

    private static final class BrokenFlag {
    }

    private static class FakeProtoBase {
        private boolean finalized;

        protected final void ca() {
            finalized = true;
        }
    }

    private static final class FakeProto extends FakeProtoBase {
        private FakeLanguageList b = new FakeLanguageList();
    }

    private static final class FakeLanguageList extends ArrayList<String> {
    }

    private static final class SmartEditModuleFixture {
        private interface hhf {
        }

        private static final class pab {
        }

        private static final class hhi implements hhf {
            private void fw(FakeContext context, pab definition) {
            }
        }

        private static final class Manager {
            private final FakeContext b = new FakeContext();
            private Object loaded;
            private int loadCount;

            private Object b(Class<?> requestedClass) {
                loadCount++;
                return loaded;
            }
        }
    }

    private static final class MissingSmartEditModuleFixture {
        private interface hhf {
        }

        private static final class pab {
        }

        private static final class hhi implements hhf {
            private FakeContext context;
            private int initCount;

            private void fw(FakeContext value, pab definition) {
                context = value;
                initCount++;
            }
        }

        private static final class Manager {
            private final FakeContext b = new FakeContext();
            private int loadCount;

            private Object b(Class<?> requestedClass) {
                loadCount++;
                return null;
            }
        }
    }

    private static final class BrokenSmartEditModuleFixture {
        private interface hhf {
        }

        private static final class Manager {
            private Object b(Class<?> requestedClass) {
                return null;
            }
        }
    }

    private static final class FakeContext {
    }
}
