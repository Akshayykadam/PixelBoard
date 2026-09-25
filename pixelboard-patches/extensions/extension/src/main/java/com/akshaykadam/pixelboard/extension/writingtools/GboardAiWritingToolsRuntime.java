package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PixelBoard runtime engine for Gboard 18.0.3 AI Writing Tools and Proofread capabilities.
 * Controls feature flags, model versions, and text stylization hooks dynamically.
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardAiWritingToolsRuntime {
    static final String FLAG_WRITING_HELPER = "writing_helper";
    static final String FLAG_CONFIG_PROOFREAD = "config_proofread";
    static final String FLAG_WRITING_HELPER_ON_SELECTED_TEXT =
            "writing_helper_on_selected_text";
    static final String FLAG_WRITING_HELPER_ENABLE_TEXT_STYLIZATION_INTERNAL =
            "writing_helper_enable_text_stylization_internal";
    static final String FLAG_WRITING_TOOLS = "writing_tools";
    static final String FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE =
            "enable_writing_tools_cooperative_mode";
    static final String FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS =
            "writing_helper_supported_language_tags";
    static final String FLAG_LLM_PC_SUPPORTED_LANGUAGE_TAGS =
            "llm_pc_supported_language_tags";
    static final String FLAG_WRITING_HELPER_MODEL_VERSION =
            "writing_helper_model_version";
    static final String FLAG_WRITING_HELPER_TEXT_STYLIZATION_MODEL_VERSION =
            "writing_helper_text_stylization_model_version";
    static final String FLAG_ENABLE_WRITING_TOOLS_FOR_MINORS =
            "enable_writing_tools_for_minors";
    static final String FLAG_BACKEND_TYPE = "writing_tools_v2_backend_type";
    static final String FLAG_HYBRID = "writing_tools_enable_hybrid";
    static final String FLAG_ON_DEVICE_PROOFREAD = "enable_on_device_proofread";
    static final String FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS =
            "enable_writing_tools_voice_commands";
    static final String FLAG_ENABLE_MODELESS_SMART_EDIT =
            "enable_nga_lab_modeless_smartedit";
    static final String FLAG_MODELESS_SMART_EDIT_REGEX_VERSION =
            "nga_lab_modeless_smartedit_regex_version";
    static final String FLAG_ENABLE_WRITING_TOOLS_V2 = "enable_writing_tools_v2";
    static final String FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR =
            "enable_writing_tools_v2_on_toolbar";
    static final String FLAG_ENABLE_WRITING_TOOLS_V2_TUTORIAL_PROMPTS =
            "enable_writing_tools_v2_tutorial_prompts";
    static final String FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_IN_ZERO_STATE =
            "writing_tools_v2_display_instruction_suggestions_in_zero_state";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS =
            "writing_tools_v2_enable_suggested_instructions";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT_RESPONSE =
            "writing_tools_v2_enable_suggested_instructions_in_draft_response";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_TOAST =
            "writing_tools_v2_enable_suggested_instructions_toast";
    static final String FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS =
            "writing_tools_v2_show_predefined_prompts";
    static final String FLAG_WRITING_TOOLS_USE_V2_LANDSCAPE_LAYOUT =
            "writing_tools_use_v2_landscape_layout";
    static final String FLAG_WRITING_TOOLS_ENABLE_STREAMING_UI =
            "writing_tools_enable_streaming_ui";
    static final String FLAG_ENABLE_WRITING_TOOLS_REPLACE_BUTTON =
            "enable_writing_tools_replace_button";
    static final String FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE =
            "enable_writing_tools_suggest_style";
    static final String FLAG_ENABLE_WRITING_TOOLS_MY_STYLE =
            "enable_writing_tools_my_style";
    static final String FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS =
            "enable_writing_tools_my_style_plus";
    static final String FLAG_ENABLE_SUPER_ICON_IN_DYNAMIC_WRITING_TOOLS =
            "enable_super_icon_in_dynamic_writing_tools";
    static final String FLAG_ENABLE_WRITING_TOOLS_DECORATION_ANIM =
            "enable_writing_tools_decoration_anim";
    static final String FLAG_ENABLE_WRITING_TOOLS_ITEM_VIEW_EXPAND_ANIM =
            "enable_writing_tools_item_view_expand_anim";
    static final String FLAG_ENABLE_WRITING_TOOLS_SCROLL_HINT_ANIM =
            "enable_writing_tools_scroll_hint_anim";
    static final String FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEW_SELECT_ANIM =
            "enable_writing_tools_style_view_select_anim";
    static final String FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEWS_FADE_IN_ANIM =
            "enable_writing_tools_style_views_fade_in_anim";
    static final String FLAG_ENABLE_WRITING_TOOLS_THUMB_UP_AND_DOWN =
            "enable_writing_tools_thumb_up_and_down";
    static final String FLAG_WRITING_TOOLS_ENABLE_STABLE_ENTRANCE =
            "writing_tools_enable_stable_entrance";
    static final String FLAG_WRITING_TOOLS_ENABLE_PROMPT_ROLE =
            "writing_tools_enable_prompt_role";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_MULTI_ROLE_PROMPT =
            "writing_tools_v2_enable_multi_role_prompt";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_USER_PROFILE =
            "writing_tools_v2_enable_user_profile";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_P13N =
            "writing_tools_v2_enable_p13n";
    static final String FLAG_WRITING_TOOLS_V2_SHOW_P13N_TAG =
            "writing_tools_v2_show_p13n_tag";
    static final String FLAG_WRITING_TOOLS_V2_SHOW_PI_LABEL =
            "writing_tools_v2_show_pi_label";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_LOADING_STATUS =
            "writing_tools_v2_enable_zero_state_instruction_suggestion_loading_status";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_ANIMATED_LOADING_STATUS =
            "writing_tools_v2_enable_zero_state_instruction_suggestion_animated_loading_status";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_MULTI_STATUS_ITEM =
            "writing_tools_v2_enable_zero_state_instruction_suggestion_multi_status_item";
    static final String FLAG_WRITING_TOOLS_V2_CANCEL_ZERO_STATE_INSTRUCTION_SUGGESTION_ON_TYPING =
            "writing_tools_v2_cancel_zero_state_instruction_suggestion_on_typing";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SCREENSHOT_CONTEXT =
            "writing_tools_v2_enable_screenshot_context";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SMART_REPLY_FOR_SELF_REPLY =
            "writing_tools_v2_enable_smart_reply_for_self_reply";
    static final String FLAG_WRITING_TOOLS_V2_SMART_REPLY_ENABLE_MODULAR_PROMPT_TEMPLATE =
            "writing_tools_v2_smart_rely_enable_modular_prompt_template";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_PROMPT_DOWNLOAD =
            "writing_tools_v2_enable_prompt_download";
    static final String FLAG_WRITING_HELPER_ENABLE_ACCESS_POINT_ANIMATION =
            "writing_helper_enable_access_point_animation";
    static final String FLAG_WRITING_TOOLS_PREPARE_PI_ON_ACCESS_POINT =
            "writing_tools_prepare_pi_on_access_point";
    static final String FLAG_WRITING_TOOLS_PREPARE_PI_ON_COOPERATIVE_MODE =
            "writing_tools_prepare_pi_on_cooperative_mode";
    static final String FLAG_WRITING_TOOLS_PREPARE_PI_ON_PROOFREAD_CHIP =
            "writing_tools_prepare_pi_on_proofread_chip";
    static final String FLAG_PROOFREAD_SUPPORTED_APPS =
            "proofread_supported_apps";
    static final String FLAG_WRITING_TOOLS_V2_ENABLED_SMART_REPLY_ZERO_STATE_SUGGESTION_LANGUAGE_TAGS =
            "writing_tools_v2_enabled_smart_reply_zero_state_suggestion_language_tags";
    static final String FLAG_WRITING_TOOLS_V2_FALLBACK_PROMPT_TEMPLATE_VERSION =
            "writing_tools_v2_fallback_prompt_template_version";
    static final String FLAG_WRITING_TOOLS_V2_PROMPT_TEMPLATE_VERSION =
            "writing_tools_v2_prompt_template_version";
    static final String FLAG_WRITING_TOOLS_V2_P13N_PROMPT_TEMPLATE_VERSION =
            "writing_tools_v2_p13n_prompt_template_version";
    static final String FLAG_WRITING_TOOLS_PI_STYLIZATION_CANDIDATE_COUNT =
            "writing_tools_pi_stylization_candidate_count";
    static final String FLAG_WRITING_TOOLS_V2_TOP_K =
            "writing_tools_v2_top_k";
    static final String FLAG_WRITING_TOOLS_V2_RETRY_MAX_ATTEMPTS =
            "writing_tools_v2_retry_max_attempts";
    static final String ALL_LANGUAGES_ALLOWLIST_VALUE = "*";
    static final String MODELESS_SMART_EDIT_REGEX_VERSION = "v3";
    static final String RENAMED_GBOARD_PACKAGE =
            "com.akshaykadam.pixelboard";
    static final String PROOFREAD_MODEL_CONFIG =
            "202408051448_prod_sd_config";
    static final String TEXT_STYLIZATION_MODEL_CONFIG =
            "composer_stylization_base";

    private static final String TAG = "GboardPatches";
    private static final String LOG_PREFIX = "[gboard-writing-tools-18.0.3] ";
    private static final String[] FORCED_SIGNAL_TARGET_SPECS = new String[]{
            "gvu#a",
            "lvi#b",
            "gqd#a"
    };
    private static final ConcurrentHashMap<Class<?>, Field> FLAG_NAME_FIELDS =
            new ConcurrentHashMap<Class<?>, Field>();
    private static final ConcurrentHashMap<ClassLoader, Set<Object>> FORCED_SIGNALS =
            new ConcurrentHashMap<ClassLoader, Set<Object>>();
    private static final AtomicInteger FLAG_LOG_COUNT = new AtomicInteger(0);
    private static final AtomicInteger FLAG_FAILURE_LOG_COUNT = new AtomicInteger(0);
    private static final AtomicInteger SIGNAL_LOG_COUNT = new AtomicInteger(0);
    private static final AtomicInteger SIGNAL_FAILURE_LOG_COUNT = new AtomicInteger(0);
    private static final AtomicInteger SETTINGS_REMOVAL_LOG_COUNT = new AtomicInteger(0);
    private static final ThreadLocal<Integer> SETTINGS_CONTROLLER_SCOPE_DEPTH =
            new ThreadLocal<Integer>();

    private static volatile Context applicationContext;
    private static volatile String sLastKnownDraft = null;

    private GboardAiWritingToolsRuntime() {
    }

    public static Object applyOverriddenFlagValue(Object receiver, Object originalResult) {
        try {
            Context context = resolveContext();
            if (context == null || receiver == null) {
                return originalResult;
            }
            GboardAiWritingToolsSettings.Snapshot settings =
                    GboardAiWritingToolsSettings.snapshot(context);
            String flagName = readFlagName(receiver);
            Object enforced = computeOverrideValue(
                    flagName,
                    originalResult,
                    settings,
                    null,
                    context.getPackageName());
            if (enforced != originalResult) {
                logLimited(FLAG_LOG_COUNT,
                        "flag=" + flagName
                                + ", original=" + describe(originalResult)
                                + ", enforced=" + describe(enforced)
                                + ", featureEnabled=" + settings.featureEnabled
                                + ", allKeyboards=" + settings.allKeyboardsEnabled
                                + ", backendType=" + settings.backendType);
            }
            return enforced;
        } catch (Throwable throwable) {
            logLimited(FLAG_FAILURE_LOG_COUNT,
                    "failed to apply flag override", throwable);
            return originalResult;
        }
    }

    static Object computeOverrideValue(String flagName, Object originalResult,
            GboardAiWritingToolsSettings.Snapshot settings,
            GboardAiWritingToolsOfficialPreferences.Snapshot officialPreferences) {
        return computeOverrideValue(
                flagName,
                originalResult,
                settings,
                officialPreferences,
                null);
    }

    static Object computeOverrideValue(String flagName, Object originalResult,
            GboardAiWritingToolsSettings.Snapshot settings,
            GboardAiWritingToolsOfficialPreferences.Snapshot officialPreferences,
            String packageName) {
        if (flagName == null || settings == null || !settings.featureEnabled) {
            return originalResult;
        }

        if (RENAMED_GBOARD_PACKAGE.equals(packageName)
                && GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER.equals(
                        settings.backendType)
                && originalResult instanceof String) {
            if (FLAG_WRITING_HELPER_MODEL_VERSION.equals(flagName)) {
                return PROOFREAD_MODEL_CONFIG;
            }
            if (FLAG_WRITING_HELPER_TEXT_STYLIZATION_MODEL_VERSION.equals(flagName)) {
                return TEXT_STYLIZATION_MODEL_CONFIG;
            }
        }

        BackendDecision backend = backendDecision(settings.backendType);
        if (FLAG_BACKEND_TYPE.equals(flagName)) {
            return originalResult instanceof Long ? backend.backendType : originalResult;
        }
        if (FLAG_HYBRID.equals(flagName)) {
            return originalResult instanceof Boolean ? backend.hybridEnabled : originalResult;
        }
        if (FLAG_ON_DEVICE_PROOFREAD.equals(flagName)) {
            return originalResult instanceof Boolean
                    ? backend.onDeviceProofreadEnabled
                    : originalResult;
        }
        if (FLAG_ENABLE_WRITING_TOOLS_VOICE_COMMANDS.equals(flagName)) {
            return originalResult instanceof Boolean ? Boolean.TRUE : originalResult;
        }
        if (FLAG_MODELESS_SMART_EDIT_REGEX_VERSION.equals(flagName)) {
            return originalResult instanceof String && ((String) originalResult).isEmpty()
                    ? MODELESS_SMART_EDIT_REGEX_VERSION
                    : originalResult;
        }

        if (FLAG_WRITING_HELPER_SUPPORTED_LANGUAGE_TAGS.equals(flagName)
                || FLAG_LLM_PC_SUPPORTED_LANGUAGE_TAGS.equals(flagName)) {
            return settings.allKeyboardsEnabled && originalResult instanceof String
                    ? ALL_LANGUAGES_ALLOWLIST_VALUE
                    : originalResult;
        }
        if (FLAG_WRITING_TOOLS_V2_ENABLED_SMART_REPLY_ZERO_STATE_SUGGESTION_LANGUAGE_TAGS
                .equals(flagName)) {
            if (!(originalResult instanceof String)) {
                return originalResult;
            }
            if (settings.allKeyboardsEnabled) {
                return ALL_LANGUAGES_ALLOWLIST_VALUE;
            }
            String stock = (String) originalResult;
            return stock.isEmpty() ? "en" : stock;
        }
        if (FLAG_PROOFREAD_SUPPORTED_APPS.equals(flagName)) {
            return originalResult instanceof String ? "" : originalResult;
        }
        if (FLAG_WRITING_TOOLS_V2_FALLBACK_PROMPT_TEMPLATE_VERSION.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_PROMPT_TEMPLATE_VERSION.equals(flagName)) {
            return originalResult instanceof String
                    && ("-".equals(originalResult) || ((String) originalResult).isEmpty())
                    ? "v3"
                    : originalResult;
        }
        if (FLAG_WRITING_TOOLS_V2_P13N_PROMPT_TEMPLATE_VERSION.equals(flagName)) {
            return originalResult instanceof String
                    && ("-".equals(originalResult) || ((String) originalResult).isEmpty())
                    ? "p3"
                    : originalResult;
        }
        if (FLAG_WRITING_TOOLS_PI_STYLIZATION_CANDIDATE_COUNT.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_TOP_K.equals(flagName)) {
            return originalResult instanceof Long ? Long.valueOf(3L) : originalResult;
        }
        if (FLAG_WRITING_TOOLS_V2_RETRY_MAX_ATTEMPTS.equals(flagName)) {
            return originalResult instanceof Long ? Long.valueOf(1L) : originalResult;
        }
        if (!(originalResult instanceof Boolean)) {
            return originalResult;
        }

        if (FLAG_WRITING_TOOLS_PREPARE_PI_ON_ACCESS_POINT.equals(flagName)
                || FLAG_WRITING_TOOLS_PREPARE_PI_ON_COOPERATIVE_MODE.equals(flagName)
                || FLAG_WRITING_TOOLS_PREPARE_PI_ON_PROOFREAD_CHIP.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_PROMPT_DOWNLOAD.equals(flagName)
                || FLAG_WRITING_TOOLS_ENABLE_PROMPT_ROLE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_MULTI_ROLE_PROMPT.equals(flagName)) {
            return Boolean.FALSE;
        }

        if (FLAG_CONFIG_PROOFREAD.equals(flagName)
                || FLAG_WRITING_TOOLS.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE.equals(flagName)
                || FLAG_WRITING_HELPER_ON_SELECTED_TEXT.equals(flagName)
                || FLAG_WRITING_HELPER_ENABLE_TEXT_STYLIZATION_INTERNAL.equals(flagName)
                || FLAG_WRITING_HELPER.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_FOR_MINORS.equals(flagName)
                || FLAG_ENABLE_MODELESS_SMART_EDIT.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_V2.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_V2_TUTORIAL_PROMPTS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS.equals(flagName)
                || FLAG_WRITING_TOOLS_USE_V2_LANDSCAPE_LAYOUT.equals(flagName)
                || FLAG_WRITING_TOOLS_ENABLE_STREAMING_UI.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_REPLACE_BUTTON.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_MY_STYLE.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS.equals(flagName)
                || FLAG_ENABLE_SUPER_ICON_IN_DYNAMIC_WRITING_TOOLS.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_DECORATION_ANIM.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_ITEM_VIEW_EXPAND_ANIM.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_SCROLL_HINT_ANIM.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEW_SELECT_ANIM.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_STYLE_VIEWS_FADE_IN_ANIM.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_THUMB_UP_AND_DOWN.equals(flagName)
                || FLAG_WRITING_TOOLS_ENABLE_STABLE_ENTRANCE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_USER_PROFILE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_P13N.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SHOW_P13N_TAG.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SHOW_PI_LABEL.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SCREENSHOT_CONTEXT.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SMART_REPLY_FOR_SELF_REPLY.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SMART_REPLY_ENABLE_MODULAR_PROMPT_TEMPLATE
                        .equals(flagName)
                || FLAG_WRITING_HELPER_ENABLE_ACCESS_POINT_ANIMATION.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_IN_ZERO_STATE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT_RESPONSE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_TOAST.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_LOADING_STATUS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_ANIMATED_LOADING_STATUS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_ZERO_STATE_INSTRUCTION_SUGGESTION_MULTI_STATUS_ITEM.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_CANCEL_ZERO_STATE_INSTRUCTION_SUGGESTION_ON_TYPING.equals(flagName)) {
            return Boolean.TRUE;
        }
        return originalResult;
    }

    static BackendDecision backendDecision(String backendType) {
        if (GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_AICORE.equals(backendType)) {
            return new BackendDecision(Long.valueOf(2L), Boolean.TRUE, Boolean.TRUE);
        }
        if (GboardAiWritingToolsSettings.BACKEND_PRIVATE_INFERENCE_ASTREA.equals(backendType)) {
            return new BackendDecision(Long.valueOf(3L), Boolean.TRUE, Boolean.FALSE);
        }
        return new BackendDecision(Long.valueOf(1L), Boolean.FALSE, Boolean.FALSE);
    }

    public static boolean applySignalResult(Object signal, boolean originalResult) {
        if (originalResult) {
            return true;
        }
        try {
            Context context = resolveContext();
            if (context == null
                    || signal == null
                    || !GboardAiWritingToolsSettings.snapshot(context).featureEnabled) {
                return false;
            }
            boolean forced = forcedSignals(signal.getClass().getClassLoader()).contains(signal);
            if (forced) {
                logLimited(SIGNAL_LOG_COUNT,
                        "signal=" + describe(signal) + ", original=false, enforced=true");
            }
            return forced;
        } catch (Throwable throwable) {
            logLimited(SIGNAL_FAILURE_LOG_COUNT,
                    "failed to evaluate signal", throwable);
            return originalResult;
        }
    }

    public static void enterSettingsControllerScope() {
        SETTINGS_CONTROLLER_SCOPE_DEPTH.set(Integer.valueOf(
                currentSettingsControllerScopeDepth() + 1));
    }

    public static void exitSettingsControllerScope() {
        int nextDepth = currentSettingsControllerScopeDepth() - 1;
        if (nextDepth <= 0) {
            SETTINGS_CONTROLLER_SCOPE_DEPTH.remove();
        } else {
            SETTINGS_CONTROLLER_SCOPE_DEPTH.set(Integer.valueOf(nextDepth));
        }
    }

    static int currentSettingsControllerScopeDepth() {
        Integer depth = SETTINGS_CONTROLLER_SCOPE_DEPTH.get();
        return depth == null ? 0 : depth.intValue();
    }

    public static boolean shouldBypassSettingsRemoval(int keyResId) {
        try {
            Context context = resolveContext();
            if (context == null
                    || currentSettingsControllerScopeDepth() <= 0
                    || !GboardAiWritingToolsSettings.snapshot(context).featureEnabled) {
                return false;
            }
            return keyResId == GboardAiWritingToolsOfficialPreferences.PROOFREAD_PREF_KEY
                    || keyResId == GboardAiWritingToolsOfficialPreferences.WRITING_TOOLS_PREF_KEY
                    || keyResId == GboardAiWritingToolsOfficialPreferences
                    .WRITING_TOOLS_CATEGORY_KEY;
        } catch (Throwable throwable) {
            logLimited(SETTINGS_REMOVAL_LOG_COUNT,
                    "failed to evaluate settings removal", throwable);
            return false;
        }
    }

    private static String readFlagName(Object receiver) throws Throwable {
        Field field = FLAG_NAME_FIELDS.get(receiver.getClass());
        if (field == null) {
            Field resolved = receiver.getClass().getDeclaredField("a");
            if (resolved.getType() != String.class) {
                return null;
            }
            resolved.setAccessible(true);
            Field existing = FLAG_NAME_FIELDS.putIfAbsent(receiver.getClass(), resolved);
            field = existing == null ? resolved : existing;
        }
        return (String) field.get(receiver);
    }

    private static Set<Object> forcedSignals(ClassLoader classLoader) throws Throwable {
        Set<Object> cached = FORCED_SIGNALS.get(classLoader);
        if (cached != null) {
            return cached;
        }
        Set<Object> resolved = Collections.newSetFromMap(
                new IdentityHashMap<Object, Boolean>());
        for (String spec : FORCED_SIGNAL_TARGET_SPECS) {
            int separator = spec.indexOf('#');
            Class<?> owner = Class.forName(spec.substring(0, separator), false, classLoader);
            Field field = owner.getDeclaredField(spec.substring(separator + 1));
            field.setAccessible(true);
            Object signal = field.get(null);
            if (signal != null) {
                resolved.add(signal);
            }
        }
        Set<Object> immutable = Collections.unmodifiableSet(resolved);
        Set<Object> existing = FORCED_SIGNALS.putIfAbsent(classLoader, immutable);
        return existing == null ? immutable : existing;
    }

    static Context resolveContext() {
        Context cached = applicationContext;
        if (cached != null) {
            return cached;
        }
        Context reflected = reflectedApplicationContext(
                "android.app.ActivityThread", "currentApplication");
        if (reflected == null) {
            reflected = reflectedApplicationContext(
                    "android.app.AppGlobals", "getInitialApplication");
        }
        if (reflected != null) {
            applicationContext = reflected;
        }
        return reflected;
    }

    static Context reflectedApplicationContext(String className, String methodName) {
        try {
            Object application = Class.forName(className).getMethod(methodName).invoke(null);
            if (!(application instanceof Context)) {
                return null;
            }
            Context context = (Context) application;
            Context applicationContext = context.getApplicationContext();
            return applicationContext == null ? context : applicationContext;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String[] forcedSignalTargetSpecsForTesting() {
        return FORCED_SIGNAL_TARGET_SPECS.clone();
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof CharSequence) {
            return value.getClass().getSimpleName() + "(" + value + ")";
        }
        return value.getClass().getName();
    }

    private static void logLimited(AtomicInteger counter, String message) {
        logLimited(counter, message, null);
    }

    private static void logLimited(AtomicInteger counter, String message, Throwable throwable) {
        if (counter.incrementAndGet() <= 80) {
            safeLog(message, throwable);
        }
    }

    private static boolean isInvalidDraft(String text) {
        if (text == null) {
            return true;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        return lower.startsWith("something went wrong")
                || lower.startsWith("couldn't load")
                || lower.startsWith("could not load")
                || lower.startsWith("failed to get")
                || lower.startsWith("please try again")
                || lower.equals("try again")
                || lower.equals("cancel")
                || lower.equals("describe")
                || lower.equals("clear")
                || lower.equals("undo");
    }

    public static void observeEditorInfo(Object editorInfoObj) {
        if (editorInfoObj == null) {
            return;
        }
        try {
            // Ignore IME internal editor infos to prevent overwriting with user prompt queries
            try {
                Field pkgField = editorInfoObj.getClass().getField("packageName");
                Object pkgVal = pkgField.get(editorInfoObj);
                if (pkgVal instanceof String) {
                    String pkg = ((String) pkgVal).toLowerCase();
                    if (pkg.contains("inputmethod") || pkg.contains("pixelboard")) {
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }

            // 1. Try getInitialSelectedText(0)
            try {
                Method selMethod = editorInfoObj.getClass().getMethod("getInitialSelectedText", int.class);
                Object sel = selMethod.invoke(editorInfoObj, 0);
                if (sel != null) {
                    String selStr = sel.toString();
                    if (!isInvalidDraft(selStr)) {
                        sLastKnownDraft = selStr.trim();
                        safeLogInfo("observeEditorInfo: captured selected text: " + sLastKnownDraft);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }

            // 2. Try getInitialTextBeforeCursor + getInitialTextAfterCursor
            CharSequence before = null;
            CharSequence after = null;
            try {
                Method beforeMethod = editorInfoObj.getClass().getMethod("getInitialTextBeforeCursor", int.class, int.class);
                Object b = beforeMethod.invoke(editorInfoObj, 10000, 0);
                if (b instanceof CharSequence) {
                    before = (CharSequence) b;
                }
            } catch (Throwable ignored) {
            }
            try {
                Method afterMethod = editorInfoObj.getClass().getMethod("getInitialTextAfterCursor", int.class, int.class);
                Object a = afterMethod.invoke(editorInfoObj, 10000, 0);
                if (a instanceof CharSequence) {
                    after = (CharSequence) a;
                }
            } catch (Throwable ignored) {
            }

            String full = (before != null ? before.toString() : "")
                    + (after != null ? after.toString() : "");
            if (!isInvalidDraft(full)) {
                sLastKnownDraft = full.trim();
                safeLogInfo("observeEditorInfo: captured editor text: " + sLastKnownDraft);
            }
        } catch (Throwable t) {
            safeLog("observeEditorInfo failed", t);
        }
    }

    public static String resolveDescribeDraft(String draft, Object hsj) {
        if (draft != null && !isInvalidDraft(draft)) {
            sLastKnownDraft = draft.trim();
            return draft.trim();
        }
        if (hsj != null) {
            String recovered = extractTriggerDraft(hsj);
            if (recovered != null && !isInvalidDraft(recovered)) {
                sLastKnownDraft = recovered.trim();
                safeLogInfo("resolveDescribeDraft: recovered draft from trigger: " + recovered);
                return recovered.trim();
            }
        }
        if (sLastKnownDraft != null && !isInvalidDraft(sLastKnownDraft)) {
            safeLogInfo("resolveDescribeDraft: recovered draft from sLastKnownDraft: " + sLastKnownDraft);
            return sLastKnownDraft;
        }
        return "";
    }

    private static String extractTriggerDraft(Object hsj) {
        if (hsj == null) {
            return null;
        }
        try {
            // 1. Check hsj.r (Lovi) -> field "b" (CharSequence)
            try {
                Field rf = hsj.getClass().getDeclaredField("r");
                rf.setAccessible(true);
                Object ovi = rf.get(hsj);
                if (ovi != null) {
                    Field bf = ovi.getClass().getDeclaredField("b");
                    bf.setAccessible(true);
                    Object bVal = bf.get(ovi);
                    if (bVal instanceof CharSequence && !isInvalidDraft(bVal.toString())) {
                        return bVal.toString();
                    }
                }
            } catch (Throwable ignored) {
            }

            // 2. Check hsj.a() -> Lhsm -> field "b" (CharSequence)
            try {
                Method am = hsj.getClass().getDeclaredMethod("a");
                am.setAccessible(true);
                Object hsm = am.invoke(hsj);
                if (hsm != null) {
                    for (Field f : hsm.getClass().getDeclaredFields()) {
                        if (CharSequence.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            Object bVal = f.get(hsm);
                            if (bVal instanceof CharSequence && !isInvalidDraft(bVal.toString())) {
                                return bVal.toString();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 3. Check hsj.w (Lhsq) -> list "j" -> item "b"
            try {
                Field wf = hsj.getClass().getDeclaredField("w");
                wf.setAccessible(true);
                Object hsq = wf.get(hsj);
                if (hsq != null) {
                    Field jf = hsq.getClass().getDeclaredField("j");
                    Field kf = hsq.getClass().getDeclaredField("k");
                    jf.setAccessible(true);
                    kf.setAccessible(true);
                    List<?> list = (List<?>) jf.get(hsq);
                    int k = kf.getInt(hsq);
                    if (list != null) {
                        List<Integer> indices = new ArrayList<>();
                        if (k >= 0 && k < list.size()) {
                            indices.add(k);
                        }
                        for (int i = list.size() - 1; i >= 0; i--) {
                            if (i != k) {
                                indices.add(i);
                            }
                        }
                        for (int idx : indices) {
                            Object item = list.get(idx);
                            if (item != null) {
                                for (Field f : item.getClass().getDeclaredFields()) {
                                    if (CharSequence.class.isAssignableFrom(f.getType())) {
                                        f.setAccessible(true);
                                        Object bVal = f.get(item);
                                        if (bVal instanceof CharSequence && !isInvalidDraft(bVal.toString())) {
                                            return bVal.toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 4. Check hsj.U (CharSequence)
            try {
                Field uf = hsj.getClass().getDeclaredField("U");
                uf.setAccessible(true);
                Object uVal = uf.get(hsj);
                if (uVal instanceof CharSequence && !isInvalidDraft(uVal.toString())) {
                    return uVal.toString();
                }
            } catch (Throwable ignored) {
            }

            // 5. Existing fallback: scan all objects for AtomicReference
            for (Field f : hsj.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object obj = f.get(hsj);
                if (obj == null || obj == hsj || obj.getClass().getName().startsWith("android.")
                        || obj.getClass().getName().startsWith("java.")) {
                    continue;
                }
                for (Field hf : obj.getClass().getDeclaredFields()) {
                    if (AtomicReference.class.isAssignableFrom(hf.getType())) {
                        hf.setAccessible(true);
                        AtomicReference<?> ref = (AtomicReference<?>) hf.get(obj);
                        if (ref != null && ref.get() != null) {
                            Object trigger = ref.get();
                            for (Field tf : trigger.getClass().getDeclaredFields()) {
                                tf.setAccessible(true);
                                Object val = tf.get(trigger);
                                if (val != null && !val.getClass().isPrimitive()
                                        && !(val instanceof Boolean) && !(val instanceof Number)) {
                                    String text = val.toString();
                                    if (text != null && !isInvalidDraft(text)
                                            && !text.startsWith("Optional") && !text.contains("@")) {
                                        return text;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            safeLog("extractTriggerDraft error", t);
        }
        return null;
    }

    static String sanitizePrompt(String text) {
        if (text == null) {
            return null;
        }
        String result = text;

        // If <CURRENT_DRAFT> is present but contains only whitespace or is empty, populate from sLastKnownDraft
        if (sLastKnownDraft != null && !sLastKnownDraft.trim().isEmpty()) {
            Pattern emptyDraftPattern = Pattern.compile("(?s)<CURRENT_DRAFT>\\s*</CURRENT_DRAFT>");
            if (emptyDraftPattern.matcher(result).find()) {
                result = emptyDraftPattern.matcher(result).replaceAll(
                        "<CURRENT_DRAFT>\n" + Matcher.quoteReplacement(sLastKnownDraft) + "\n</CURRENT_DRAFT>");
            }
        }

        if (!result.contains("<CURRENT_DRAFT>") && sLastKnownDraft != null
                && !sLastKnownDraft.trim().isEmpty()) {
            if (result.contains("## User's Current Request")) {
                int userReqIdx = result.lastIndexOf("## User's Current Request");
                int endInstr = result.indexOf("</INSTRUCTION>", userReqIdx);
                if (endInstr != -1) {
                    int insertPos = endInstr + "</INSTRUCTION>".length();
                    result = result.substring(0, insertPos) + "\nContext: <CURRENT_DRAFT>"
                            + sLastKnownDraft + "</CURRENT_DRAFT>" + result.substring(insertPos);
                }
            } else if (result.contains("</INSTRUCTION>")) {
                int endInstr = result.lastIndexOf("</INSTRUCTION>");
                int insertPos = endInstr + "</INSTRUCTION>".length();
                result = result.substring(0, insertPos) + "\nContext: <CURRENT_DRAFT>"
                        + sLastKnownDraft + "</CURRENT_DRAFT>" + result.substring(insertPos);
            } else {
                result = result + "\nContext: <CURRENT_DRAFT>" + sLastKnownDraft + "</CURRENT_DRAFT>";
            }
        }
        return result;
    }

    static void setLastKnownDraftForTesting(String draft) {
        sLastKnownDraft = draft;
    }

    static String getLastKnownDraftForTesting() {
        return sLastKnownDraft;
    }

    public static Object adaptPromptMessages(Object messages) {
        if (messages == null) {
            return null;
        }
        if (!(messages instanceof Iterable)) {
            return messages;
        }
        List<Object> items = new ArrayList<>();
        for (Object item : (Iterable<?>) messages) {
            if (item != null) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return messages;
        }

        List<String> texts = new ArrayList<>();
        List<Object> nonTextItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            String text = null;
            try {
                Method cMethod = item.getClass().getMethod("c");
                Object res = cMethod.invoke(item);
                if (res instanceof String) {
                    text = (String) res;
                }
            } catch (Throwable ignored) {
            }
            if (text != null && !text.isEmpty()) {
                texts.add(text);
                safeLogInfo("adaptPromptMessages: msg[" + i + "]=" + text);
            } else {
                nonTextItems.add(item);
            }
        }

        if (texts.isEmpty()) {
            return messages;
        }

        String combinedText;
        if (texts.size() == 1) {
            combinedText = texts.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String t : texts) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(t);
            }
            combinedText = sb.toString();
        }

        String sanitizedText = sanitizePrompt(combinedText);
        if (items.size() == 1 && sanitizedText.equals(texts.get(0))) {
            return messages;
        }

        safeLogInfo("adaptPromptMessages: adapting " + items.size()
                + " messages into 1 text message: " + sanitizedText);

        try {
            ClassLoader cl = null;
            if (!items.isEmpty()) {
                cl = items.get(0).getClass().getClassLoader();
            }
            if (cl == null) {
                cl = messages.getClass().getClassLoader();
            }
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }

            Class<?> oidClass = null;
            if (!items.isEmpty()) {
                oidClass = items.get(0).getClass();
            } else {
                oidClass = Class.forName("oid", true, cl);
            }

            Method bMethod = oidClass.getMethod("b", String.class);
            Object singleOid = bMethod.invoke(null, sanitizedText);

            Class<?> wcoClass = null;
            try {
                wcoClass = Class.forName("wco", true, oidClass.getClassLoader());
            } catch (Throwable ignored) {
            }
            if (wcoClass == null && cl != null) {
                try {
                    wcoClass = Class.forName("wco", true, cl);
                } catch (Throwable ignored) {
                }
            }
            if (wcoClass == null) {
                Class<?> curr = messages.getClass();
                while (curr != null && curr != Object.class) {
                    for (Method m : curr.getDeclaredMethods()) {
                        if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("r")
                                && m.getParameterTypes().length == 1) {
                            wcoClass = curr;
                            break;
                        }
                    }
                    if (wcoClass != null) {
                        break;
                    }
                    curr = curr.getSuperclass();
                }
            }

            if (wcoClass == null) {
                safeLog("adaptPromptMessages: could not resolve wco class", null);
                return messages;
            }

            if (nonTextItems.isEmpty()) {
                Method rMethod = wcoClass.getMethod("r", Object.class);
                return rMethod.invoke(null, singleOid);
            } else {
                List<Object> combinedList = new ArrayList<>();
                combinedList.add(singleOid);
                combinedList.addAll(nonTextItems);
                Method oMethod = wcoClass.getMethod("o", Collection.class);
                return oMethod.invoke(null, combinedList);
            }
        } catch (Throwable t) {
            safeLog("Failed to create single adapted prompt message", t);
            return messages;
        }
    }

    public static Object adaptAiResponse(Object response) {
        if (response == null) {
            return null;
        }
        if (!(response instanceof Iterable)) {
            return response;
        }
        for (Object item : (Iterable<?>) response) {
            if (item == null) {
                continue;
            }
            try {
                Field bField = null;
                try {
                    bField = item.getClass().getDeclaredField("b");
                } catch (NoSuchFieldException e) {
                    for (Field f : item.getClass().getDeclaredFields()) {
                        if (CharSequence.class.isAssignableFrom(f.getType())) {
                            bField = f;
                            break;
                        }
                    }
                }
                if (bField != null) {
                    bField.setAccessible(true);
                    Object val = bField.get(item);
                    if (val instanceof String) {
                        String raw = (String) val;
                        safeLogInfo("adaptAiResponse: raw response: " + raw);
                        String repaired = repairAiResponse(raw);
                        if (!raw.equals(repaired)) {
                            bField.set(item, repaired);
                            safeLogInfo("adaptAiResponse: repaired response: " + repaired);
                        }
                    }
                }
            } catch (Throwable t) {
                safeLog("adaptAiResponse: failed to adapt item", t);
            }
        }
        return response;
    }

    static String repairAiResponse(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return raw;
        }

        // 1. Remove markdown code fences if wrapped: ```xml ... ``` or ```markdown ... ``` or ``` ... ```
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            } else {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        // 2. Remove reasoning/thinking tags if model outputs <think> ... </think> or <thought> ... </thought>
        text = text.replaceAll("(?s)<(think|thought)>.*?</\\1>", "").trim();

        boolean hasDraftOpen = text.contains("<DRAFT>");
        boolean hasDraftClose = text.contains("</DRAFT>");
        boolean hasInstrClass = text.contains("<INSTRUCTION_CLASS>");

        // Case A: Missing <DRAFT> completely
        if (!hasDraftOpen && !hasDraftClose) {
            StringBuilder sb = new StringBuilder();
            if (!hasInstrClass) {
                sb.append("<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>\n");
            }
            sb.append("<DRAFT>").append(text).append("</DRAFT>");
            return sb.toString();
        }

        // Case B: Has <DRAFT> but missing </DRAFT>
        if (hasDraftOpen && !hasDraftClose) {
            text = text + "</DRAFT>";
        }

        // Case C: Has </DRAFT> but missing <DRAFT>
        if (!hasDraftOpen && hasDraftClose) {
            text = "<DRAFT>" + text;
        }

        // Case D: Missing <INSTRUCTION_CLASS>
        if (!hasInstrClass) {
            text = "<INSTRUCTION_CLASS>MODIFICATION</INSTRUCTION_CLASS>\n" + text;
        }

        return text;
    }

    private static void safeLogInfo(String message) {
        try {
            Log.i(TAG, LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Host-side unit tests do not provide android.util.Log.
        }
    }

    private static void safeLog(String message, Throwable throwable) {
        try {
            if (throwable == null) {
                Log.w(TAG, LOG_PREFIX + message);
            } else {
                Log.w(TAG, LOG_PREFIX + message, throwable);
            }
        } catch (Throwable ignored) {
            // Host-side unit tests do not provide android.util.Log.
        }
    }

    static final class BackendDecision {
        final Long backendType;
        final Boolean hybridEnabled;
        final Boolean onDeviceProofreadEnabled;

        private BackendDecision(Long backendType, Boolean hybridEnabled,
                Boolean onDeviceProofreadEnabled) {
            this.backendType = backendType;
            this.hybridEnabled = hybridEnabled;
            this.onDeviceProofreadEnabled = onDeviceProofreadEnabled;
        }
    }
}
