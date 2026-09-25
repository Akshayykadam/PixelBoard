package com.akshaykadam.pixelboard.extension.writingtools;

import org.junit.Assert;
import org.junit.Test;

public final class GboardAiWritingToolsRuntimeTest {
    @Test
    public void masterOffPreservesEveryStockResultIdentity() {
        Object original = Long.valueOf(3L);

        Assert.assertSame(original, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                original,
                settings(false, true,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void targetBooleanFlagsUseTheFormalStockCapabilityTuple() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(false);
        GboardAiWritingToolsOfficialPreferences.Snapshot proofreadOff = official(false, true);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                Boolean.TRUE,
                settings,
                proofreadOff));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                Boolean.FALSE,
                settings,
                proofreadOff));

        GboardAiWritingToolsOfficialPreferences.Snapshot writingToolsOff = official(true, false);
        String[] editingFlags = new String[]{
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_ON_SELECTED_TEXT,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_HELPER_ENABLE_TEXT_STYLIZATION_INTERNAL
        };
        for (String flag : editingFlags) {
            Assert.assertSame(flag, Boolean.TRUE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            flag,
                            Boolean.TRUE,
                            settings,
                            writingToolsOff));
        }
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER,
                Boolean.FALSE,
                settings,
                writingToolsOff));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_FOR_MINORS,
                Boolean.FALSE,
                settings,
                writingToolsOff));
    }

    @Test
    public void voiceCommandRolloutFlagIsEnabledWithoutChangingWrongTypes() {
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS,
                Boolean.FALSE,
                serverSettings(false),
                official(false, false)));

        Object wrongType = "false";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS,
                wrongType,
                serverSettings(false),
                official(true, true)));
    }

    @Test
    public void unknownOfficialValuesStayEnabledForReachability() {
        GboardAiWritingToolsOfficialPreferences.Snapshot unknown = official(null, null);

        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                Boolean.FALSE,
                serverSettings(false),
                unknown));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                Boolean.FALSE,
                serverSettings(false),
                unknown));
    }

    @Test
    public void allKeyboardsChangesOnlyTwoStringAllowlists() {
        GboardAiWritingToolsSettings.Snapshot enabled = serverSettings(true);
        Assert.assertEquals("*", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS,
                "en-US",
                enabled,
                official(true, true)));
        Assert.assertEquals("*", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_LLM_PC_SUPPORTED_LANGUAGE_TAGS,
                "en-US",
                enabled,
                official(true, true)));

        Object wrongType = Integer.valueOf(7);
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS,
                wrongType,
                enabled,
                official(true, true)));
    }

    @Test
    public void renamedServerPackageUsesModelConfigs() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(false);

        String proofread = "202406101250_prod_sd_config";
        Assert.assertEquals("202408051448_prod_sd_config",
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                        proofread,
                        settings,
                        official(true, true),
                        "com.akshaykadam.pixelboard"));
        String stylization = "composer_stylization_base";
        Assert.assertEquals("composer_stylization_base",
                GboardAiWritingToolsRuntime.computeOverrideValue(
                        GboardAiWritingToolsRuntime
                                .FLAG_WRITING_HELPER_TEXT_STYLIZATION_MODEL_VERSION,
                        stylization,
                        settings,
                        official(true, true),
                        "com.akshaykadam.pixelboard"));
    }

    @Test
    public void officialServerPackagePreservesItsRemoteModelConfigs() {
        String stockModel = "official-remote-model";
        Assert.assertSame(stockModel, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                stockModel,
                serverSettings(false),
                official(true, true),
                "com.google.android.inputmethod.latin"));
    }

    @Test
    public void serverModelConfigsPreserveWrongTypeIdentity() {
        Object wrongType = Integer.valueOf(7);

        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                wrongType,
                serverSettings(false),
                official(true, true),
                "com.akshaykadam.pixelboard"));
    }

    @Test
    public void privateBackendsPreserveTheirStockModelConfigs() {
        String stockModel = "private-stock-model";

        Assert.assertSame(stockModel, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_MODEL_VERSION,
                stockModel,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_AICORE),
                official(true, true)));
    }

    @Test
    public void unrelatedAndWrongTypesStayUnchanged() {
        Object unrelated = new Object();
        Assert.assertSame(unrelated, GboardAiWritingToolsRuntime.computeOverrideValue(
                "unrelated_flag",
                unrelated,
                serverSettings(false),
                official(true, true)));

        Object wrongType = "false";
        Assert.assertSame(wrongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                wrongType,
                serverSettings(false),
                official(false, false)));

    }

    @Test
    public void distinctFalseOfficialBooleanIsOverridden() throws Exception {
        Boolean distinctFalse = distinctBoolean(false);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_CONFIG_PROOFREAD,
                distinctFalse,
                serverSettings(false),
                official(true, true)));
    }

    @Test
    public void distinctTrueOfficialBooleanIsOverridden() throws Exception {
        Boolean distinctTrue = distinctBoolean(true);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS,
                distinctTrue,
                serverSettings(false),
                official(true, false)));
    }

    @Test
    public void modelessRegexPromotesOnlyAnEmptyString() {
        Assert.assertEquals("v3", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_MODELESS_SMART_EDIT_REGEX_VERSION,
                "",
                serverSettings(false),
                official(false, false)));
        String stock = "v4";
        Assert.assertSame(stock, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_MODELESS_SMART_EDIT_REGEX_VERSION,
                stock,
                serverSettings(false),
                official(false, false)));
    }

    @Test
    public void backendDecisionReturnsOneAtomicThreeValueTuple() {
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER,
                1L,
                false,
                false);
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_AICORE,
                2L,
                true,
                true);
        assertBackend(
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA,
                3L,
                true,
                false);
        assertBackend("INVALID", 1L, false, false);
    }

    @Test
    public void backendFlagsPreserveWrongTypeIdentity() {
        GboardAiWritingToolsSettings.Snapshot astrea = settings(
                true,
                false,
                GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA);
        Assert.assertEquals(Long.valueOf(3L), GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                Long.valueOf(1L),
                astrea,
                official(true, true)));
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_HYBRID,
                Boolean.FALSE,
                astrea,
                official(true, true)));
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ON_DEVICE_PROOFREAD,
                Boolean.FALSE,
                astrea,
                official(true, true)));

        Integer wrongLongType = Integer.valueOf(1);
        Assert.assertSame(wrongLongType, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_BACKEND_TYPE,
                wrongLongType,
                astrea,
                official(true, true)));

        Assert.assertEquals(Long.valueOf(3L), GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_PI_STYLIZATION_CANDIDATE_COUNT,
                Long.valueOf(1L),
                astrea,
                official(true, true)));
        Assert.assertEquals(Long.valueOf(3L), GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_TOP_K,
                Long.valueOf(1L),
                astrea,
                official(true, true)));
        Assert.assertEquals(Long.valueOf(1L), GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_RETRY_MAX_ATTEMPTS,
                Long.valueOf(0L),
                astrea,
                official(true, true)));
    }

    @Test
    public void distinctFalseBackendBooleanIsOverridden() throws Exception {
        Boolean distinctFalse = distinctBoolean(false);
        Assert.assertSame(Boolean.TRUE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_HYBRID,
                distinctFalse,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void distinctTrueBackendBooleanIsOverridden() throws Exception {
        Boolean distinctTrue = distinctBoolean(true);
        Assert.assertSame(Boolean.FALSE, GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_ON_DEVICE_PROOFREAD,
                distinctTrue,
                settings(
                        true,
                        false,
                        GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA),
                official(true, true)));
    }

    @Test
    public void writingToolsV2FlagsAreEnabledWithFullSuite() {
        GboardAiWritingToolsSettings.Snapshot settings = serverSettings(true);
        String[] v2Flags = new String[]{
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_V2_TUTORIAL_PROMPTS,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_USE_V2_LANDSCAPE_LAYOUT,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_ENABLE_STREAMING_UI,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_REPLACE_BUTTON,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_MY_STYLE,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_SUPER_ICON_IN_DYNAMIC_WRITING_TOOLS,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_DECORATION_ANIM,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_ITEM_VIEW_EXPAND_ANIM,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_SCROLL_HINT_ANIM,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEW_SELECT_ANIM,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEWS_FADE_IN_ANIM,
                GboardAiWritingToolsRuntime.FLAG_ENABLE_WRITING_TOOLS_THUMB_UP_AND_DOWN,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_ENABLE_STABLE_ENTRANCE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_USER_PROFILE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_P13N,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_SHOW_P13N_TAG,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_SHOW_PI_LABEL,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_SCREENSHOT_CONTEXT,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_SMART_REPLY_FOR_SELF_REPLY,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_SMART_REPLY_ENABLE_MODULAR_PROMPT_TEMPLATE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_HELPER_ENABLE_ACCESS_POINT_ANIMATION,
        };
        for (String flag : v2Flags) {
            Assert.assertSame(flag, Boolean.TRUE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            flag,
                            Boolean.FALSE,
                            settings,
                            official(true, true)));
        }

        String[] v2EnabledSuggestionFlags = new String[]{
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_IN_ZERO_STATE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT_RESPONSE,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_TOAST,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_LOADING_STATUS,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_ANIMATED_LOADING_STATUS,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_MULTI_STATUS_ITEM,
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_CANCEL_ZERO_STATE_INSTRUCTION_SUGGESTION_ON_TYPING
        };
        for (String flag : v2EnabledSuggestionFlags) {
            Assert.assertSame(flag, Boolean.TRUE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            flag,
                            Boolean.FALSE,
                            settings,
                            official(true, true)));
        }

        String[] v2DisabledFlags = new String[]{
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_PREPARE_PI_ON_ACCESS_POINT,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_PREPARE_PI_ON_COOPERATIVE_MODE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_PREPARE_PI_ON_PROOFREAD_CHIP,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_PROMPT_DOWNLOAD,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_ENABLE_PROMPT_ROLE,
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_ENABLE_MULTI_ROLE_PROMPT
        };
        for (String flag : v2DisabledFlags) {
            Assert.assertSame(flag, Boolean.FALSE,
                    GboardAiWritingToolsRuntime.computeOverrideValue(
                            flag,
                            Boolean.TRUE,
                            settings,
                            official(true, true)));
        }

        Assert.assertEquals("*", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLED_SMART_REPLY_ZERO_STATE_SUGGESTION_LANGUAGE_TAGS,
                "en",
                settings,
                official(true, true)));

        GboardAiWritingToolsSettings.Snapshot settingsNoAll = serverSettings(false);
        Assert.assertEquals("en", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_ENABLED_SMART_REPLY_ZERO_STATE_SUGGESTION_LANGUAGE_TAGS,
                "en",
                settingsNoAll,
                official(true, true)));

        Assert.assertEquals("", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_PROOFREAD_SUPPORTED_APPS,
                "-com.google.android.gm",
                settings,
                official(true, true)));

        Assert.assertEquals("v3", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_PROMPT_TEMPLATE_VERSION,
                "",
                settings,
                official(true, true)));

        Assert.assertEquals("v3", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime
                        .FLAG_WRITING_TOOLS_V2_FALLBACK_PROMPT_TEMPLATE_VERSION,
                "-",
                settings,
                official(true, true)));

        Assert.assertEquals("p3", GboardAiWritingToolsRuntime.computeOverrideValue(
                GboardAiWritingToolsRuntime.FLAG_WRITING_TOOLS_V2_P13N_PROMPT_TEMPLATE_VERSION,
                "",
                settings,
                official(true, true)));
    }

    @Test
    public void adaptPromptMessagesSafelyHandlesSmallOrNonIterableInputs() {
        Assert.assertNull(GboardAiWritingToolsRuntime.adaptPromptMessages(null));
        Object nonIterable = "not-a-list";
        Assert.assertSame(nonIterable, GboardAiWritingToolsRuntime.adaptPromptMessages(nonIterable));
        java.util.List<Object> empty = java.util.Collections.emptyList();
        Assert.assertSame(empty, GboardAiWritingToolsRuntime.adaptPromptMessages(empty));
        java.util.List<Object> single = java.util.Collections.singletonList(new Object());
        Assert.assertSame(single, GboardAiWritingToolsRuntime.adaptPromptMessages(single));
    }

    @Test
    public void resolveDescribeDraftReturnsProvidedDraftAndUpdatesLastKnown() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting(null);
        String resolved = GboardAiWritingToolsRuntime.resolveDescribeDraft("My initial text", null);
        Assert.assertEquals("My initial text", resolved);
        Assert.assertEquals("My initial text", GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void resolveDescribeDraftFallsBackToLastKnownDraftWhenBlankAndNoTrigger() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Captured editor text from active session");
        String resolved = GboardAiWritingToolsRuntime.resolveDescribeDraft("", null);
        Assert.assertEquals("Captured editor text from active session", resolved);
        Assert.assertEquals("Captured editor text from active session", GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());

        String resolvedNull = GboardAiWritingToolsRuntime.resolveDescribeDraft(null, null);
        Assert.assertEquals("Captured editor text from active session", resolvedNull);
        Assert.assertEquals("Captured editor text from active session", GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void observeEditorInfoCapturesSelectedText() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting(null);
        Object mockEditorInfo = new Object() {
            public String packageName = "com.google.android.apps.messaging";
            public CharSequence getInitialSelectedText(int flags) {
                return "Selected text in editor";
            }
        };
        GboardAiWritingToolsRuntime.observeEditorInfo(mockEditorInfo);
        Assert.assertEquals("Selected text in editor", GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void observeEditorInfoCapturesSurroundingTextWhenNoSelection() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting(null);
        Object mockEditorInfo = new Object() {
            public String packageName = "ai.perplexity.app.android";
            public CharSequence getInitialTextBeforeCursor(int n, int flags) {
                return "Your lighting is warm, ";
            }
            public CharSequence getInitialTextAfterCursor(int n, int flags) {
                return "but even accounting for that...";
            }
        };
        GboardAiWritingToolsRuntime.observeEditorInfo(mockEditorInfo);
        Assert.assertEquals("Your lighting is warm, but even accounting for that...",
                GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void observeEditorInfoIgnoresImePackages() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Original draft");
        Object mockEditorInfo = new Object() {
            public String packageName = "com.google.android.inputmethod.latin";
            public CharSequence getInitialTextBeforeCursor(int n, int flags) {
                return "Prompt query typed in keyboard";
            }
        };
        GboardAiWritingToolsRuntime.observeEditorInfo(mockEditorInfo);
        Assert.assertEquals("Original draft", GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void resolveDescribeDraftIgnoresErrorStrings() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting(null);
        String resolvedError = GboardAiWritingToolsRuntime.resolveDescribeDraft("Something went wrong. Please try again.", null);
        Assert.assertEquals("", resolvedError);
        Assert.assertNull(GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());

        final String errorText = "Couldn't load suggestion";
        Object mockOvi = new Object() {
            public CharSequence b = errorText;
        };
        Object mockHsj = new Object() {
            public Object r = mockOvi;
        };
        String resolvedTriggerError = GboardAiWritingToolsRuntime.resolveDescribeDraft("", mockHsj);
        Assert.assertEquals("", resolvedTriggerError);
        Assert.assertNull(GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void sanitizePromptInjectsMissingDraftWhenAvailable() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Let us schedule the sync on Friday.");
        String rawPrompt = "You are a writing assistant.\n\n"
                + "## User's Current Request\n"
                + "User instruction: <INSTRUCTION>make it professional</INSTRUCTION>\n"
                + "---\n"
                + "Now, begin processing the request based on the protocol above and provide your response.";

        String sanitized = GboardAiWritingToolsRuntime.sanitizePrompt(rawPrompt);
        Assert.assertTrue(sanitized.contains("Context: <CURRENT_DRAFT>Let us schedule the sync on Friday.</CURRENT_DRAFT>"));
        Assert.assertFalse(sanitized.contains("Output: <DRAFT>"));
    }

    @Test
    public void sanitizePromptDoesNotDuplicateExistingDraftOrOutputTag() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Existing draft.");
        String alreadyCompletePrompt = "## User's Current Request\n"
                + "User instruction: <INSTRUCTION>polish</INSTRUCTION>\n"
                + "Context: <CURRENT_DRAFT>Already has draft</CURRENT_DRAFT>\n";

        String sanitized = GboardAiWritingToolsRuntime.sanitizePrompt(alreadyCompletePrompt);
        Assert.assertEquals(alreadyCompletePrompt, sanitized);
    }

    @Test
    public void sanitizePromptReplacesEmptyDraftTagWithLastKnownDraft() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Recovered draft text.");
        String promptWithEmptyDraft = "## Context Information\n"
                + "<CURRENT_DRAFT>\n\n</CURRENT_DRAFT>\n"
                + "## User's Current Request\n"
                + "User instruction: <INSTRUCTION>make it concise</INSTRUCTION>\n";

        String sanitized = GboardAiWritingToolsRuntime.sanitizePrompt(promptWithEmptyDraft);
        Assert.assertTrue(sanitized.contains("<CURRENT_DRAFT>\nRecovered draft text.\n</CURRENT_DRAFT>"));
    }

    @Test
    public void resolveDescribeDraftRecoversDraftFromHsjFields() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting(null);
        final String expectedDraft = "Draft inside mock hsj";
        Object mockOvi = new Object() {
            public CharSequence b = expectedDraft;
        };
        Object mockHsj = new Object() {
            public Object r = mockOvi;
        };

        String resolved = GboardAiWritingToolsRuntime.resolveDescribeDraft("", mockHsj);
        Assert.assertEquals(expectedDraft, resolved);
        Assert.assertEquals(expectedDraft, GboardAiWritingToolsRuntime.getLastKnownDraftForTesting());
    }

    @Test
    public void sanitizePromptDoesNotDuplicateWhenContextSectionHasDraft() {
        GboardAiWritingToolsRuntime.setLastKnownDraftForTesting("Recovered pink casing draft");
        String nativePromptWithContextDraft = "## Context Information\n"
                + "<CONTEXT>\n"
                + "<CURRENT_DRAFT>Your lighting is warm...</CURRENT_DRAFT>\n"
                + "</CONTEXT>\n"
                + "## User's Current Request\n"
                + "<INSTRUCTION>Email</INSTRUCTION>\n"
                + "---\n";

        String sanitized = GboardAiWritingToolsRuntime.sanitizePrompt(nativePromptWithContextDraft);
        Assert.assertEquals(nativePromptWithContextDraft, sanitized);
    }

    @Test
    public void repairAiResponseWrapsRawTextWithoutTags() {
        String rawServerResponse = "Subject: Pink Casing Question\n\nHi there,\n\nBest,";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(rawServerResponse);

        Assert.assertTrue(repaired.contains("<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>"));
        Assert.assertTrue(repaired.contains("<DRAFT>" + rawServerResponse + "</DRAFT>"));
    }

    @Test
    public void repairAiResponseStripsMarkdownCodeFences() {
        String markdownResponse = "```xml\n"
                + "<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>\n"
                + "<DRAFT>Here is the revised draft</DRAFT>\n"
                + "```";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(markdownResponse);

        Assert.assertFalse(repaired.contains("```"));
        Assert.assertTrue(repaired.startsWith("<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>"));
        Assert.assertTrue(repaired.endsWith("<DRAFT>Here is the revised draft</DRAFT>"));
    }

    @Test
    public void repairAiResponseClosesUnclosedDraftTag() {
        String unclosed = "<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>\n"
                + "<DRAFT>Draft text here without closing tag";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(unclosed);

        Assert.assertTrue(repaired.endsWith("</DRAFT>"));
    }

    @Test
    public void repairAiResponseAddsInstructionClassWhenMissing() {
        String onlyDraft = "<DRAFT>Just a draft text</DRAFT>";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(onlyDraft);

        Assert.assertTrue(repaired.startsWith("<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>"));
        Assert.assertTrue(repaired.contains(onlyDraft));
    }

    @Test
    public void repairAiResponseRemovesThinkingTags() {
        String withThinking = "<think>Let me compose an email</think>\n"
                + "Subject: Meeting\n\nLet us meet tomorrow.";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(withThinking);

        Assert.assertFalse(repaired.contains("<think>"));
        Assert.assertFalse(repaired.contains("</think>"));
        Assert.assertTrue(repaired.contains("<DRAFT>Subject: Meeting\n\nLet us meet tomorrow.</DRAFT>"));
    }

    @Test
    public void repairAiResponsePreservesAlreadyValidResponse() {
        String valid = "<INSTRUCTION_CLASS>COMPOSITION</INSTRUCTION_CLASS>\n"
                + "<DRAFT>Valid composition output</DRAFT>";
        String repaired = GboardAiWritingToolsRuntime.repairAiResponse(valid);

        Assert.assertEquals(valid, repaired);
    }

    @Test
    public void adaptAiResponseUpdatesFieldOnListItems() {
        class MockOkq {
            public String b = "Raw text reply from model";
        }
        MockOkq item = new MockOkq();
        java.util.List<MockOkq> list = java.util.Collections.singletonList(item);

        Object adapted = GboardAiWritingToolsRuntime.adaptAiResponse(list);
        Assert.assertSame(list, adapted);
        Assert.assertTrue(item.b.contains("<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>"));
        Assert.assertTrue(item.b.contains("<DRAFT>Raw text reply from model</DRAFT>"));
    }

    private static Boolean distinctBoolean(boolean value) throws Exception {
        return Boolean.class.getDeclaredConstructor(boolean.class).newInstance(value);
    }

    private static void assertBackend(String backend, long type, boolean hybrid,
            boolean onDeviceProofread) {
        GboardAiWritingToolsRuntime.BackendDecision decision =
                GboardAiWritingToolsRuntime.backendDecision(backend);
        Assert.assertEquals(Long.valueOf(type), decision.backendType);
        Assert.assertEquals(Boolean.valueOf(hybrid), decision.hybridEnabled);
        Assert.assertEquals(Boolean.valueOf(onDeviceProofread),
                decision.onDeviceProofreadEnabled);
    }

    private static GboardAiWritingToolsSettings.Snapshot serverSettings(
            boolean allKeyboardsEnabled) {
        return settings(
                true,
                allKeyboardsEnabled,
                GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER);
    }

    private static GboardAiWritingToolsSettings.Snapshot settings(boolean featureEnabled,
            boolean allKeyboardsEnabled, String backendType) {
        return new GboardAiWritingToolsSettings.Snapshot(
                featureEnabled,
                allKeyboardsEnabled,
                backendType);
    }

    private static GboardAiWritingToolsOfficialPreferences.Snapshot official(
            Boolean proofreadEnabled, Boolean writingToolsEnabled) {
        return new GboardAiWritingToolsOfficialPreferences.Snapshot(
                proofreadEnabled,
                writingToolsEnabled);
    }
}
