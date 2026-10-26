package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.util.Log;

import com.akshaykadam.pixelboard.extension.settings.GboardEmojiSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardRoundedKeysSettings;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    // Stock default is "-" (no language) for both V2 suggestion lists.
    static final String FLAG_WRITING_TOOLS_V2_SMART_REPLY_P13N_LANGUAGE_TAGS =
            "writing_tools_v2_smart_reply_enabled_p13n_language_tags";
    static final String FLAG_WRITING_TOOLS_V2_SMART_REPLY_ZERO_STATE_LANGUAGE_TAGS =
            "writing_tools_v2_enabled_smart_reply_zero_state_suggestion_language_tags";
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
    static final String FLAG_ENABLE_WRITING_TOOLS_V2 =
            "enable_writing_tools_v2";
    static final String FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR =
            "enable_writing_tools_v2_on_toolbar";
    static final String FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE =
            "enable_writing_tools_suggest_style";
    static final String FLAG_WRITING_TOOLS_V2 =
            "writing_tools_v2";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS =
            "writing_tools_v2_enable_suggested_instructions";
    static final String FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT =
            "writing_tools_v2_enable_suggested_instructions_in_draft_response";
    static final String FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_ZERO_STATE =
            "writing_tools_v2_display_instruction_suggestions_in_zero_state";
    static final String FLAG_WRITING_TOOLS_V2_ALLOW_EMPTY_USER_INSTRUCTION =
            "writing_tools_v2_allow_empty_user_instruction";
    static final String FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS =
            "writing_tools_v2_show_predefined_prompts";
    static final String FLAG_ENABLE_WRITING_TOOLS_MY_STYLE =
            "enable_writing_tools_my_style";
    static final String FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS =
            "enable_writing_tools_my_style_plus";
    static final String FLAG_ENABLE_MODELESS_SMART_EDIT =
            "enable_nga_lab_modeless_smartedit";
    static final String FLAG_MODELESS_SMART_EDIT_REGEX_VERSION =
            "nga_lab_modeless_smartedit_regex_version";
    static final String FLAG_ENABLE_DEVICE_INTELLIGENCE =
            "enable_device_intelligence";
    static final String FLAG_EMOJI_SCALE_SUPPORTED =
            "emoji_scale_supported";
    static final String FLAG_ENABLE_ACCESS_POINTS_MENU_REDESIGN =
            "enable_access_points_menu_redesign";
    static final String FLAG_ENABLE_ACCESS_POINT_KEYBOARD =
            "enable_access_point_keyboard";
    static final String FLAG_ENABLE_AUTOFILL_IME_INTEGRATION =
            "enable_autofill_ime_integration";
    static final String FLAG_ENABLE_SPELLCHECKER_CHIPS_UI =
            "enable_spellchecker_chips_ui";
    static final String FLAG_ENABLE_SPELLCHECKER_CHIPS_UI_WEB_VIEW =
            "enable_spellchecker_chips_ui_web_view";
    static final String FLAG_ENABLE_SPELL_CHECKER_EXTENSION =
            "enable_spell_checker_extension";
    static final String FLAG_WRITING_HELPER_CHIP_IN_SPELLCHECKER =
            "writing_helper_chip_in_spellchecker";
    static final String FLAG_ENABLE_SPELL_CHECKER_HIGHLIGHT_ON_WEB_INPUT =
            "enable_spell_checker_highlight_on_web_input";
    static final String FLAG_ENABLE_SPELL_CHECKER_TRAINING_CACHE =
            "enable_spell_checker_training_cache";

    // Fast user history learning and prefix predictions
    static final String FLAG_INTENDED_OOV_MIN_COUNT =
            "intended_oov_min_count";
    static final Long ENFORCED_INTENDED_OOV_MIN_COUNT = 2L;
    static final String FLAG_MIN_CHAR_NUM_LIMIT_FOR_SINGLE_WORD_PREDICTIONS =
            "min_char_num_limit_for_single_word_predictions";
    static final Long ENFORCED_MIN_CHAR_NUM_LIMIT_FOR_SINGLE_WORD_PREDICTIONS = 2L;
    static final String FLAG_ENABLE_PK_AUTO_CORRECTION_LOCALES =
            "enable_pk_auto_correction_locales";
    static final String FLAG_ADD_PENDING_OOVS_ON_RESET_MAX_MS =
            "add_pending_oovs_on_reset_max_ms";
    static final String FLAG_ADD_PENDING_OOVS_ON_CREATE_MAX_MS =
            "add_pending_oovs_on_create_max_ms";
    static final Long ENFORCED_ADD_PENDING_OOVS_MAX_MS = 500L;
    static final String FLAG_THRESHOLD_FOR_CANDIDATES_FROM_USER_HISTORY =
            "threshold_for_candidates_from_user_history";
    static final Double ENFORCED_THRESHOLD_FOR_CANDIDATES_FROM_USER_HISTORY = -5.0d;
    static final String FLAG_FIX_FIRST_AUTO_CORRECTION_COMMIT =
            "fix_first_auto_correction_commit";
    static final String FLAG_LEARNING_FOR_EDITOR_WITHOUT_AUTO_CORRECTION =
            "learning_for_editor_without_auto_correction";
    static final String FLAG_ENABLE_PC_USER_HISTORY_NGRAM_COUNT =
            "enable_pc_user_history_ngram_count";
    static final String FLAG_POPULATE_NGRAM_COUNT_WITH_CONTEXT =
            "populate_ngram_count_with_context";
    static final String FLAG_POPULATE_NGRAM_TIMESTAMP_IN_USER_HISTORY =
            "populate_ngram_timestamp_in_user_history";
    static final String FLAG_ENABLE_MULTIWORD_PREDICTIONS_FROM_USER_HISTORY =
            "enable_multiword_predictions_from_user_history";
    static final String FLAG_ENABLE_MULTIWORD_SUGGESTIONS_FROM_USER_HISTORY =
            "enable_multiword_suggestions_from_user_history";
    static final String FLAG_ENABLE_DYNAMIC_LM_UNIGRAM_CONTINUATION_SCORING =
            "enable_dynamic_lm_unigram_continuation_scoring";
    static final String FLAG_PRESERVE_CONTEXT_IN_DYNAMIC_LM_COMPUTE_HISTORY =
            "preserve_context_in_dynamic_lm_compute_history";
    static final String FLAG_ENABLE_DYNAMIC_LM_NOISE_FILTER =
            "enable_dynamic_lm_noise_filter";
    static final String FLAG_ENABLE_OOV_DEDUPLICATION =
            "enable_oov_deduplication";

    // Emoji suggestions & shortcuts
    static final String FLAG_CONFIG_EXPRESSION =
            "config_expression";
    static final String FLAG_ENABLE_CONCEPT_PREDICTION_ENGINE =
            "enable_concept_prediction_engine";
    static final String FLAG_ENABLE_RULE_BASED_CONCEPT_PREDICTION =
            "enable_rule_based_concept_prediction";
    static final String FLAG_ENABLE_LAST_WORD_RULE_BASED_CONCEPT_PREDICTION =
            "enable_last_word_rule_based_concept_prediction";
    static final String FLAG_ENABLE_SUFFIX_RULE_BASED_CONCEPT_PREDICTION =
            "enable_suffix_rule_based_concept_prediction";
    static final String FLAG_ENABLE_EMOJI_SUGGESTION_FROM_TOP_CANDIDATE =
            "enable_emoji_suggestion_from_top_candidate";
    static final String FLAG_ENABLE_PREDICTIONS_FROM_EMOJI_SHORTCUTS =
            "enable_predictions_from_emoji_shortcuts";
    static final String FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_BY_SPANS =
            "enable_ngram_emoji_shortcuts_by_spans";
    static final String FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_FOR_PREDICTION =
            "enable_ngram_emoji_shortcuts_for_prediction";
    static final String FLAG_RETAIN_EMOJI_SHORTCUT_CANDIDATES_ON_SELECTION =
            "retain_emoji_shortcut_candidates_on_selection";

    // Autocorrect and glide typing consistency
    static final String FLAG_UPDATE_STREAMING_TOUCH_DATA_SET_IS_GESTURE =
            "update_streaming_touch_data_set_is_gesture";
    static final String FLAG_ENABLE_TWO_STAGE_DECODER =
            "enable_two_stage_decoder";
    static final String FLAG_ENABLE_SPACE_TRANSPOSITION_POST_CORRECTION =
            "enable_space_transposition_post_correction";
    static final String FLAG_ENABLE_PK_SPATIAL_MODEL_FEATURES =
            "enable_pk_spatial_model_features";
    static final String FLAG_ENABLE_PK_AUTO_CORRECTION_TRIGGER_PREDICTOR =
            "enable_pk_auto_correction_trigger_predictor";
    static final String FLAG_ENABLE_POST_CORRECTION_TRIGGER_PREDICTOR =
            "enable_post_correction_trigger_predictor";
    static final String FLAG_POPULATE_PREEMPTIVE_NEXT_KEY_PREDICTIONS =
            "populate_preemptive_next_key_predictions";
    static final String FLAG_AUTO_CORRECT_WITH_REPLACE_TEXT =
            "auto_correct_with_replace_text";
    static final String FLAG_ENABLE_REVERT_AUTO_CORRECT_ON_PK =
            "enable_revert_auto_correct_on_pk";

    // Decoder & Spacebar Auto-Correction Consistency
    static final String FLAG_DELAY_TO_WAIT_FOR_SUGGESTIONS_BEFORE_COMMIT_MS =
            "delay_to_wait_for_suggestions_before_commit_ms";
    static final Long ENFORCED_DELAY_TO_WAIT_FOR_SUGGESTIONS_BEFORE_COMMIT_MS = 150L;
    static final String FLAG_ENABLE_DECODER_RESPONSE_WAIT_WITHOUT_LOCK =
            "enable_decoder_response_wait_without_lock";
    static final String FLAG_TRY_INITIALIZE_DECODER_AFTER_LM_LOADS =
            "try_initialize_decoder_after_lm_loads";
    static final String FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_WEB_TEXT =
            "apps_to_respect_no_auto_correction_in_web_text";
    static final String FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION =
            "apps_to_respect_no_auto_correction";
    static final String FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_SPECIAL_TYPES =
            "apps_to_respect_no_auto_correction_in_special_types";
    static final String FLAG_PK_AUTO_CAPITALIZATION_USE_AUTO_CORRECTION =
            "pk_auto_capitalization_use_auto_correction";
    static final String FLAG_ENABLE_AUTO_CORRECTION_STATS =
            "enable_auto_correction_stats";
    static final String FLAG_ENABLE_CORRECTION_COMMIT_ANIMATION =
            "enable_correction_commit_animation";

    // Scan Text (OCR Camera Input)
    static final String FLAG_ENABLE_OCR =
            "enable_ocr";
    static final String FLAG_CONFIG_OCR =
            "config_ocr";
    static final String FLAG_ENABLE_CAMERA_PERMISSION_OVERLAY =
            "enable_camera_permission_overlay";
    static final String FLAG_OCR_PASSWORD_ALLOWLIST =
            "ocr_password_allowlist";
    static final String FLAG_ENABLED_OCR_LANGUAGE_TAGS =
            "enabled_ocr_language_tags";

    // Clipboard Screenshot & Image Floating Banner
    static final String FLAG_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE =
            "clipboard_screenshot_enabled_at_least_once";
    static final String FLAG_ENABLE_CLIPBOARD_IMAGE_SUGGESTION =
            "enable_clipboard_image_suggestion";
    static final String FLAG_ENABLE_SCREENSHOT_NOTIFICATION_FLOW =
            "enable_screenshot_notification_flow";
    static final String FLAG_ENABLE_SCREENSHOT_CHIP =
            "enable_screenshot_chip";
    static final String FLAG_ENABLE_CLIPBOARD_RECENT_SCREENSHOT =
            "enable_clipboard_recent_screenshot";
    static final String FLAG_ENABLE_EXTENDED_CLIPBOARD_HISTORY =
            "enable_extended_clipboard_history";
    static final String FLAG_ENABLE_SYSTEM_CLIPBOARD_DIRECT_PASTE_FOR_QUICK_PASTE =
            "enable_system_clipboard_direct_paste_for_quick_paste";

    // Emoji Picker Dedicated Recent/Frequent Tabs & Palette V2
    static final String FLAG_ENABLE_FREQUENT_EMOJI_RECENT_TAB =
            "enable_frequent_emoji_recent_Tab";
    static final String FLAG_ENABLE_EMOJI_FREQUENT_RECENT_SWITCH_OPTION =
            "enable_emoji_frequent_recent_switch_option";
    static final String FLAG_EMOJI_PALETTE_V2 =
            "emoji_palette_v2";

    // Super Insert / Quick Insert Hub
    static final String FLAG_SUPER_INSERT =
            "super_insert";
    static final String FLAG_SUPER_INSERT_VK =
            "super_insert_vk";
    static final String FLAG_SUPER_INSERT_ENABLE_DYNAMIC_MULTI_VIEW =
            "super_insert_enable_dynamic_multi_view";
    static final String FLAG_SUPER_INSERT_CONTACTS =
            "super_insert_contacts";
    static final String FLAG_SUPER_INSERT_CLIPBOARD_QUERY_RESULTS =
            "super_insert_clipboard_query_results";
    static final String FLAG_SUPER_INSERT_EXPRESSION_QUERY_RESULTS =
            "super_insert_expression_query_results";
    static final String FLAG_SUPER_INSERT_LOCAL_IMAGE_RESULTS =
            "super_insert_local_image_results";
    static final String FLAG_SUPER_INSERT_BROWSING_HISTORY_RESULTS =
            "super_insert_browsing_history_results";
    static final String FLAG_SUPER_INSERT_NESTA_RESULTS =
            "super_insert_nesta_results";
    static final String FLAG_SUPER_INSERT_BELOW_CURSOR =
            "super_insert_below_cursor";
    static final String FLAG_ENABLE_SI_SHORTCUT_KEYS =
            "enable_si_shortcut_keys";

    // Smart Compose (In-Line Ghost Autocompletion)
    static final String FLAG_ENABLE_SMART_COMPOSE =
            "enable_smart_compose";
    static final String FLAG_ENABLE_INLINE_SUGGESTIONS_ON_ALL_FIELDS =
            "enable_inline_suggestions_on_all_fields";
    static final String FLAG_ENABLE_INLINE_COMPLETION =
            "enable_inline_completion";
    static final String FLAG_ENABLE_DELIGHT5_SMART_COMPOSE =
            "enable_delight5_smart_compose";
    static final String FLAG_SMART_COMPOSE_STATUS =
            "smart_compose_status";

    // Manual Incognito Mode Button
    static final String FLAG_ENABLE_INCOGNITO_MODE_ENTRY_POINT =
            "enable_incognito_mode_entry_point";
    static final String FLAG_ALLOW_INCOGNITO_MODE_SWITCH =
            "allow_incognito_mode_switch";

    // Emogen (Generative AI Emoji Maker)
    static final String FLAG_ENABLE_EMOGEN =
            "enable_emogen";
    static final String FLAG_EMOGEN_ENTRY_POINT =
            "emogen_entry_point";
    static final String FLAG_EMOGEN_FEATURE_ENABLED =
            "emogen_feature_enabled";
    static final String FLAG_EMOGEN_IN_ACCESS_POINTS =
            "emogen_in_access_points";

    // Access Points immediate population (prevents blank shortcuts on first launch)
    static final String FLAG_STOP_DEFERRING_ACCESS_POINTS_TASKS =
            "stop_deferring_access_points_tasks";

    // Sign-to-Text (Signboard ASL Camera Translation)
    static final String FLAG_ENABLE_SIGNBOARD =
            "enable_signboard";
    static final String FLAG_SIGNBOARD_PROMPT_MANUAL_TRIGGER =
            "signboard_prompt_manual_trigger";
    static final String FLAG_ON_DEMAND_FEATURE_SPLIT =
            "on_demand_feature_split";
    static final String FLAG_AUTO_SHOW_SIGNBOARD =
            "auto_show_signboard";
    static final String FLAG_SIGNBOARD_AUTO_SHOW_PROMPT =
            "signboard_auto_show_prompt";
    static final String FLAG_SIGNBOARD_DOWNLOAD =
            "signboard_download";
    static final String FLAG_SIGNBOARD_DOWNLOAD_DIALOG =
            "signboard_download_dialog";
    static final String FLAG_SIGNBOARD_MOVE_AP_PROMPT =
            "signboard_move_ap_prompt";
    static final String FLAG_USE_4C_IN_SIGNBOARD =
            "use_4c_in_signboard";

    // Writing Helper & Imagen production backend hosts
    static final String FLAG_WRITING_HELPER_SERVER_TARGET =
            "writing_helper_server_target";
    static final String FLAG_GBOARD_IMAGEN_API_HOST =
            "gboard_imagen_api_host";
    static final String FLAG_WRITING_HELPER_VOICE_INPUT_MODEL_VERSION =
            "writing_helper_voice_input_model_version";
    static final String PROD_GBOARD_IMAGEN_SERVER_TARGET =
            "staging-gboard-imagen-pa.sandbox.googleapis.com";

    // Clipboard suggestion chips preference keys
    static final String PREF_KEY_ENABLE_AUTO_PASTE_CHIPS =
            "enable_auto_paste_chips";
    static final String PREF_KEY_ENABLE_SCREENSHOT_IN_CLIPBOARD =
            "enable_screenshot_in_clipboard";
    static final String PREF_KEY_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE =
            "clipboard_screenshot_enabled_at_least_once";
    static final String PREF_KEY_ENABLE_ENTITY_EXTRACTION =
            "enable_entity_extraction";

    // Writing Tools & Jarvis consent preference keys
    static final String PREF_KEY_JARVIS_CONSENT_HISTORY =
            "jarvis_consent_history";
    static final String PREF_KEY_WRITING_TOOLS_OPT_IN =
            "pref_key_writing_tools_opt_in";
    static final String PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN =
            "pref_key_writing_tools_screen_context_opt_in";
    static final String PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN_V1 =
            "pref_key_writing_tools_screen_context_opt_in_v1";
    static final String PREF_KEY_WRITING_TOOLS_ENABLE_PI_OVERRIDE =
            "pref_key_writing_tools_enable_pi_override";

    static final String ALL_LANGUAGES_ALLOWLIST_VALUE = "*";
    static final String MODELESS_SMART_EDIT_REGEX_VERSION = "v3";
    static final String RENAMED_GBOARD_PACKAGE =
            "com.akshaykadam.pixelboard";
    static final String PROOFREAD_MODEL_CONFIG =
            "202408051448_prod_sd_config";
    static final String TEXT_STYLIZATION_MODEL_CONFIG =
            "202504090000_writing_tools_config";

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

    private static final java.util.concurrent.atomic.AtomicBoolean DEFAULT_PREFERENCES_INITIALIZED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static volatile Context applicationContext;

    private GboardAiWritingToolsRuntime() {
    }

    static boolean isStockPhenotypeEnforcedFlag(String flagName) {
        return FLAG_ENABLE_DEVICE_INTELLIGENCE.equals(flagName)
                || FLAG_EMOJI_SCALE_SUPPORTED.equals(flagName)
                || FLAG_ENABLE_ACCESS_POINTS_MENU_REDESIGN.equals(flagName)
                || FLAG_ENABLE_ACCESS_POINT_KEYBOARD.equals(flagName)
                || FLAG_ENABLE_AUTOFILL_IME_INTEGRATION.equals(flagName)
                || FLAG_ENABLE_SPELL_CHECKER_EXTENSION.equals(flagName)
                || FLAG_WRITING_HELPER_CHIP_IN_SPELLCHECKER.equals(flagName)
                || FLAG_ENABLE_SPELL_CHECKER_HIGHLIGHT_ON_WEB_INPUT.equals(flagName)
                || FLAG_ENABLE_SPELL_CHECKER_TRAINING_CACHE.equals(flagName)
                || FLAG_ENABLE_PC_USER_HISTORY_NGRAM_COUNT.equals(flagName)
                || FLAG_POPULATE_NGRAM_COUNT_WITH_CONTEXT.equals(flagName)
                || FLAG_POPULATE_NGRAM_TIMESTAMP_IN_USER_HISTORY.equals(flagName)
                || FLAG_ENABLE_MULTIWORD_PREDICTIONS_FROM_USER_HISTORY.equals(flagName)
                || FLAG_ENABLE_MULTIWORD_SUGGESTIONS_FROM_USER_HISTORY.equals(flagName)
                || FLAG_ENABLE_DYNAMIC_LM_UNIGRAM_CONTINUATION_SCORING.equals(flagName)
                || FLAG_PRESERVE_CONTEXT_IN_DYNAMIC_LM_COMPUTE_HISTORY.equals(flagName)
                || FLAG_ENABLE_DYNAMIC_LM_NOISE_FILTER.equals(flagName)
                || FLAG_ENABLE_OOV_DEDUPLICATION.equals(flagName)
                || FLAG_CONFIG_EXPRESSION.equals(flagName)
                || FLAG_ENABLE_CONCEPT_PREDICTION_ENGINE.equals(flagName)
                || FLAG_ENABLE_RULE_BASED_CONCEPT_PREDICTION.equals(flagName)
                || FLAG_ENABLE_LAST_WORD_RULE_BASED_CONCEPT_PREDICTION.equals(flagName)
                || FLAG_ENABLE_SUFFIX_RULE_BASED_CONCEPT_PREDICTION.equals(flagName)
                || FLAG_ENABLE_EMOJI_SUGGESTION_FROM_TOP_CANDIDATE.equals(flagName)
                || FLAG_ENABLE_PREDICTIONS_FROM_EMOJI_SHORTCUTS.equals(flagName)
                || FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_BY_SPANS.equals(flagName)
                || FLAG_ENABLE_NGRAM_EMOJI_SHORTCUTS_FOR_PREDICTION.equals(flagName)
                || FLAG_RETAIN_EMOJI_SHORTCUT_CANDIDATES_ON_SELECTION.equals(flagName)
                || FLAG_UPDATE_STREAMING_TOUCH_DATA_SET_IS_GESTURE.equals(flagName)
                || FLAG_ENABLE_TWO_STAGE_DECODER.equals(flagName)
                || FLAG_ENABLE_SPACE_TRANSPOSITION_POST_CORRECTION.equals(flagName)
                || FLAG_ENABLE_PK_SPATIAL_MODEL_FEATURES.equals(flagName)
                || FLAG_ENABLE_PK_AUTO_CORRECTION_TRIGGER_PREDICTOR.equals(flagName)
                || FLAG_ENABLE_POST_CORRECTION_TRIGGER_PREDICTOR.equals(flagName)
                || FLAG_POPULATE_PREEMPTIVE_NEXT_KEY_PREDICTIONS.equals(flagName)
                || FLAG_ENABLE_OCR.equals(flagName)
                || FLAG_CONFIG_OCR.equals(flagName)
                || FLAG_ENABLE_CAMERA_PERMISSION_OVERLAY.equals(flagName)
                || FLAG_OCR_PASSWORD_ALLOWLIST.equals(flagName)
                || FLAG_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE.equals(flagName)
                || FLAG_ENABLE_CLIPBOARD_IMAGE_SUGGESTION.equals(flagName)
                || FLAG_ENABLE_SCREENSHOT_NOTIFICATION_FLOW.equals(flagName)
                || FLAG_ENABLE_SCREENSHOT_CHIP.equals(flagName)
                || FLAG_ENABLE_CLIPBOARD_RECENT_SCREENSHOT.equals(flagName)
                || FLAG_ENABLE_EXTENDED_CLIPBOARD_HISTORY.equals(flagName)
                || FLAG_ENABLE_SYSTEM_CLIPBOARD_DIRECT_PASTE_FOR_QUICK_PASTE.equals(flagName)
                || FLAG_EMOJI_PALETTE_V2.equals(flagName)
                || FLAG_SUPER_INSERT.equals(flagName)
                || FLAG_SUPER_INSERT_VK.equals(flagName)
                || FLAG_SUPER_INSERT_ENABLE_DYNAMIC_MULTI_VIEW.equals(flagName)
                || FLAG_SUPER_INSERT_CONTACTS.equals(flagName)
                || FLAG_SUPER_INSERT_CLIPBOARD_QUERY_RESULTS.equals(flagName)
                || FLAG_SUPER_INSERT_EXPRESSION_QUERY_RESULTS.equals(flagName)
                || FLAG_SUPER_INSERT_LOCAL_IMAGE_RESULTS.equals(flagName)
                || FLAG_SUPER_INSERT_BROWSING_HISTORY_RESULTS.equals(flagName)
                || FLAG_SUPER_INSERT_NESTA_RESULTS.equals(flagName)
                || FLAG_SUPER_INSERT_BELOW_CURSOR.equals(flagName)
                || FLAG_ENABLE_SI_SHORTCUT_KEYS.equals(flagName)
                || FLAG_ENABLE_SMART_COMPOSE.equals(flagName)
                || FLAG_ENABLE_INLINE_SUGGESTIONS_ON_ALL_FIELDS.equals(flagName)
                || FLAG_ENABLE_INLINE_COMPLETION.equals(flagName)
                || FLAG_ENABLE_DELIGHT5_SMART_COMPOSE.equals(flagName)
                || FLAG_SMART_COMPOSE_STATUS.equals(flagName)
                || FLAG_ENABLE_INCOGNITO_MODE_ENTRY_POINT.equals(flagName)
                || FLAG_ALLOW_INCOGNITO_MODE_SWITCH.equals(flagName)
                || FLAG_ENABLE_EMOGEN.equals(flagName)
                || FLAG_EMOGEN_ENTRY_POINT.equals(flagName)
                || FLAG_EMOGEN_FEATURE_ENABLED.equals(flagName)
                || FLAG_EMOGEN_IN_ACCESS_POINTS.equals(flagName)
                || FLAG_FIX_FIRST_AUTO_CORRECTION_COMMIT.equals(flagName)
                || FLAG_LEARNING_FOR_EDITOR_WITHOUT_AUTO_CORRECTION.equals(flagName)
                || FLAG_STOP_DEFERRING_ACCESS_POINTS_TASKS.equals(flagName)
                || FLAG_AUTO_CORRECT_WITH_REPLACE_TEXT.equals(flagName)
                || FLAG_ENABLE_REVERT_AUTO_CORRECT_ON_PK.equals(flagName)
                || FLAG_ENABLE_DECODER_RESPONSE_WAIT_WITHOUT_LOCK.equals(flagName)
                || FLAG_TRY_INITIALIZE_DECODER_AFTER_LM_LOADS.equals(flagName)
                || FLAG_PK_AUTO_CAPITALIZATION_USE_AUTO_CORRECTION.equals(flagName)
                || FLAG_ENABLE_AUTO_CORRECTION_STATS.equals(flagName)
                || FLAG_ENABLE_CORRECTION_COMMIT_ANIMATION.equals(flagName);
    }

    public static Object applyOverriddenFlagValue(Object receiver, Object originalResult) {
        try {
            if (receiver == null) {
                return originalResult;
            }
            String flagName = readFlagName(receiver);
            Context context = resolveContext();
            // The renamed package receives no server-side Phenotype config, so stock
            // features that default to false in decompiled code (such as Device Intelligence
            // autofill chips, emoji scaling, access points menu redesign, autofill quick settings
            // tile, spell checker suggestion chips, rapid user history learning, emoji suggestions,
            // two-stage decoding, gesture streaming fixes, OCR scan text, screenshot chips,
            // frequent emoji tabs, super insert, and smart compose) stay disabled unless overridden.
            if (FLAG_INTENDED_OOV_MIN_COUNT.equals(flagName)
                    && originalResult instanceof Long) {
                if (!ENFORCED_INTENDED_OOV_MIN_COUNT.equals(originalResult)) {
                    logLimited(FLAG_LOG_COUNT,
                            "flag=" + flagName
                                     + ", original=" + describe(originalResult)
                                     + ", enforced=" + describe(ENFORCED_INTENDED_OOV_MIN_COUNT));
                }
                return ENFORCED_INTENDED_OOV_MIN_COUNT;
            }
            if (FLAG_MIN_CHAR_NUM_LIMIT_FOR_SINGLE_WORD_PREDICTIONS.equals(flagName)
                    && originalResult instanceof Long) {
                return ENFORCED_MIN_CHAR_NUM_LIMIT_FOR_SINGLE_WORD_PREDICTIONS;
            }
            if (FLAG_ENABLE_PK_AUTO_CORRECTION_LOCALES.equals(flagName)
                    && originalResult instanceof String) {
                return ALL_LANGUAGES_ALLOWLIST_VALUE;
            }
            if ((FLAG_ADD_PENDING_OOVS_ON_RESET_MAX_MS.equals(flagName)
                    || FLAG_ADD_PENDING_OOVS_ON_CREATE_MAX_MS.equals(flagName))
                    && originalResult instanceof Long) {
                return ENFORCED_ADD_PENDING_OOVS_MAX_MS;
            }
            if (FLAG_THRESHOLD_FOR_CANDIDATES_FROM_USER_HISTORY.equals(flagName)
                    && originalResult instanceof Double) {
                return ENFORCED_THRESHOLD_FOR_CANDIDATES_FROM_USER_HISTORY;
            }
            if ((FLAG_WRITING_HELPER_SERVER_TARGET.equals(flagName)
                    || FLAG_GBOARD_IMAGEN_API_HOST.equals(flagName))
                    && originalResult instanceof String) {
                return PROD_GBOARD_IMAGEN_SERVER_TARGET;
            }
            if (FLAG_ENABLED_OCR_LANGUAGE_TAGS.equals(flagName)
                    && originalResult instanceof String) {
                return ALL_LANGUAGES_ALLOWLIST_VALUE;
            }
            if (FLAG_SMART_COMPOSE_STATUS.equals(flagName)
                    && originalResult instanceof Long) {
                return 1L;
            }
            if (FLAG_DELAY_TO_WAIT_FOR_SUGGESTIONS_BEFORE_COMMIT_MS.equals(flagName)
                    && originalResult instanceof Long) {
                return ENFORCED_DELAY_TO_WAIT_FOR_SUGGESTIONS_BEFORE_COMMIT_MS;
            }
            if ((FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_WEB_TEXT.equals(flagName)
                    || FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION.equals(flagName)
                    || FLAG_APPS_TO_RESPECT_NO_AUTO_CORRECTION_IN_SPECIAL_TYPES.equals(flagName))
                    && originalResult instanceof String) {
                return "";
            }
            if ((FLAG_ON_DEMAND_FEATURE_SPLIT.equals(flagName)
                    || FLAG_ENABLE_SIGNBOARD.equals(flagName)
                    || FLAG_SIGNBOARD_PROMPT_MANUAL_TRIGGER.equals(flagName)
                    || FLAG_AUTO_SHOW_SIGNBOARD.equals(flagName)
                    || FLAG_SIGNBOARD_AUTO_SHOW_PROMPT.equals(flagName)
                    || FLAG_SIGNBOARD_DOWNLOAD.equals(flagName)
                    || FLAG_SIGNBOARD_DOWNLOAD_DIALOG.equals(flagName)
                    || FLAG_SIGNBOARD_MOVE_AP_PROMPT.equals(flagName)
                    || FLAG_USE_4C_IN_SIGNBOARD.equals(flagName)
                    || FLAG_ENABLE_SPELLCHECKER_CHIPS_UI.equals(flagName)
                    || FLAG_ENABLE_SPELLCHECKER_CHIPS_UI_WEB_VIEW.equals(flagName)
                    || FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE.equals(flagName)
                    || FLAG_WRITING_HELPER_ON_SELECTED_TEXT.equals(flagName))
                    && originalResult instanceof Boolean) {
                return Boolean.FALSE;
            }
            if (isStockPhenotypeEnforcedFlag(flagName)
                    && originalResult instanceof Boolean) {
                if ((FLAG_SUPER_INSERT_BROWSING_HISTORY_RESULTS.equals(flagName)
                        || FLAG_SUPER_INSERT_NESTA_RESULTS.equals(flagName))
                        && !isPersonalContextHintSupported()) {
                    return Boolean.FALSE;
                }
                if (!Boolean.TRUE.equals(originalResult)) {
                    logLimited(FLAG_LOG_COUNT,
                            "flag=" + flagName
                                    + ", original=" + describe(originalResult)
                                    + ", enforced=" + describe(Boolean.TRUE));
                }
                return Boolean.TRUE;
            }
            if (FLAG_ENABLE_FREQUENT_EMOJI_RECENT_TAB.equals(flagName)
                    && originalResult instanceof Boolean) {
                boolean frequent = context != null
                        && GboardEmojiSettings.isFrequentEmojisEnabled(context);
                Boolean target = Boolean.valueOf(frequent);
                if (!target.equals(originalResult)) {
                    logLimited(FLAG_LOG_COUNT,
                            "flag=" + flagName
                                    + ", original=" + describe(originalResult)
                                    + ", enforced=" + describe(target));
                }
                return target;
            }
            if (FLAG_ENABLE_EMOJI_FREQUENT_RECENT_SWITCH_OPTION.equals(flagName)
                    && originalResult instanceof Boolean) {
                return Boolean.FALSE;
            }
            if (context == null) {
                return originalResult;
            }
            if (GboardRoundedKeysSettings.isRoundedKeyPhenotypeFlag(flagName)
                    && originalResult instanceof Boolean) {
                if (GboardRoundedKeysSettings.isEnabled(context)) {
                    if (!Boolean.TRUE.equals(originalResult)) {
                        logLimited(FLAG_LOG_COUNT,
                                "flag=" + flagName
                                        + ", original=" + describe(originalResult)
                                        + ", enforced=" + describe(Boolean.TRUE));
                    }
                    return Boolean.TRUE;
                }
            }
            GboardAiWritingToolsSettings.Snapshot settings =
                    GboardAiWritingToolsSettings.snapshot(context);
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
                                + ", backendType=" + settings.backendType
                                + ", v2Enabled=" + settings.v2Enabled);
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

        if (packageName != null
                && (packageName.equals(RENAMED_GBOARD_PACKAGE)
                        || packageName.startsWith(RENAMED_GBOARD_PACKAGE))
                && GboardAiWritingToolsSettings.BACKEND_GBOARD_SERVER.equals(
                        settings.backendType)
                && originalResult instanceof String) {
            if (FLAG_WRITING_HELPER_SERVER_TARGET.equals(flagName)
                    || FLAG_GBOARD_IMAGEN_API_HOST.equals(flagName)) {
                return PROD_GBOARD_IMAGEN_SERVER_TARGET;
            }
            if (FLAG_WRITING_HELPER_MODEL_VERSION.equals(flagName)
                    || FLAG_WRITING_HELPER_VOICE_INPUT_MODEL_VERSION.equals(flagName)) {
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
        if (FLAG_WRITING_TOOLS_V2_SMART_REPLY_P13N_LANGUAGE_TAGS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SMART_REPLY_ZERO_STATE_LANGUAGE_TAGS.equals(flagName)) {
            return settings.v2Enabled && settings.allKeyboardsEnabled
                            && originalResult instanceof String
                    ? ALL_LANGUAGES_ALLOWLIST_VALUE
                    : originalResult;
        }
        if (!(originalResult instanceof Boolean)) {
            return originalResult;
        }

        if (FLAG_ENABLE_WRITING_TOOLS_V2.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_V2_ON_TOOLBAR.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_SUGGEST_STYLE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ENABLE_SUGGESTED_INSTRUCTIONS_IN_DRAFT.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_DISPLAY_INSTRUCTION_SUGGESTIONS_ZERO_STATE.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_ALLOW_EMPTY_USER_INSTRUCTION.equals(flagName)
                || FLAG_WRITING_TOOLS_V2_SHOW_PREDEFINED_PROMPTS.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_MY_STYLE.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_MY_STYLE_PLUS.equals(flagName)) {
            return Boolean.valueOf(settings.v2Enabled);
        }

        if (FLAG_ENABLE_WRITING_TOOLS_COOPERATIVE_MODE.equals(flagName)
                || FLAG_WRITING_HELPER_ON_SELECTED_TEXT.equals(flagName)) {
            return Boolean.FALSE;
        }

        if (FLAG_CONFIG_PROOFREAD.equals(flagName)
                || FLAG_WRITING_TOOLS.equals(flagName)
                || FLAG_WRITING_HELPER_ENABLE_TEXT_STYLIZATION_INTERNAL.equals(flagName)
                || FLAG_WRITING_HELPER.equals(flagName)
                || FLAG_ENABLE_WRITING_TOOLS_FOR_MINORS.equals(flagName)
                || FLAG_ENABLE_MODELESS_SMART_EDIT.equals(flagName)) {
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

    private static volatile Boolean personalContextHintSupportedForTesting = null;

    static boolean isPersonalContextHintSupported() {
        Boolean override = personalContextHintSupportedForTesting;
        if (override != null) {
            return override.booleanValue();
        }
        try {
            Class.forName("android.service.personalcontext.hint.BundleHint$Builder");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void setPersonalContextHintSupportedForTesting(Boolean supported) {
        personalContextHintSupportedForTesting = supported;
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
            ensureDefaultPreferencesInitialized(cached);
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
            ensureDefaultPreferencesInitialized(reflected);
        }
        return reflected;
    }

    private static void ensureDefaultPreferencesInitialized(Context context) {
        if (context == null || !DEFAULT_PREFERENCES_INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        initializeDefaultPreferences(context);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                Context dpsContext = context.createDeviceProtectedStorageContext();
                if (dpsContext != null) {
                    initializeDefaultPreferences(dpsContext);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void initializeDefaultPreferences(Context context) {
        try {
            android.content.SharedPreferences prefs =
                    android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            android.content.SharedPreferences.Editor editor = null;
            if (!prefs.contains("pref_key_enable_emoji_suggestion")) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("pref_key_enable_emoji_suggestion", true);
            }
            if (!prefs.contains("spellchecker_enable_chips_ui")
                    || prefs.getBoolean("spellchecker_enable_chips_ui", false)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("spellchecker_enable_chips_ui", false);
            }
            if (!prefs.getBoolean("enable_mark_misspelled_words", false)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("enable_mark_misspelled_words", true);
            }
            if (!prefs.contains("pref_key_enable_grammar_checker")) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("pref_key_enable_grammar_checker", true);
            }
            if (!prefs.contains("pref_key_auto_correction")) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("pref_key_auto_correction", true);
            }
            if (!prefs.contains("show_suggestion_strip")) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean("show_suggestion_strip", true);
            }
            if (!prefs.contains(PREF_KEY_ENABLE_AUTO_PASTE_CHIPS)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_ENABLE_AUTO_PASTE_CHIPS, true);
            }
            if (!prefs.contains(PREF_KEY_ENABLE_SCREENSHOT_IN_CLIPBOARD)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_ENABLE_SCREENSHOT_IN_CLIPBOARD, true);
            }
            if (!prefs.contains(PREF_KEY_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_CLIPBOARD_SCREENSHOT_ENABLED_AT_LEAST_ONCE, true);
            }
            if (!prefs.contains(PREF_KEY_ENABLE_ENTITY_EXTRACTION)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_ENABLE_ENTITY_EXTRACTION, true);
            }
            if (!prefs.contains(PREF_KEY_JARVIS_CONSENT_HISTORY)) {
                if (editor == null) editor = prefs.edit();
                editor.putString(PREF_KEY_JARVIS_CONSENT_HISTORY, "Granted (Form)");
            }
            if (!prefs.contains(PREF_KEY_WRITING_TOOLS_OPT_IN)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_WRITING_TOOLS_OPT_IN, true);
            }
            if (!prefs.contains(PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN, true);
            }
            if (!prefs.contains(PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN_V1)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_WRITING_TOOLS_SCREEN_CONTEXT_OPT_IN_V1, true);
            }
            if (!prefs.contains(PREF_KEY_WRITING_TOOLS_ENABLE_PI_OVERRIDE)) {
                if (editor == null) editor = prefs.edit();
                editor.putBoolean(PREF_KEY_WRITING_TOOLS_ENABLE_PI_OVERRIDE, true);
            }
            if (editor != null) {
                editor.apply();
            }
        } catch (Throwable ignored) {
        }
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
