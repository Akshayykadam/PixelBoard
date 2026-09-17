package com.akshaykadam.pixelboard.extension.rambler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Typed stock-route policy for Rambler in the formal Gboard 18.0.3 release. */
public final class GboardRambler1803StockPolicy {
    public static final String SHOW_RAMBLER_DICT_SETTINGS_FLAG =
            "show_rambler_dict_settings";
    public static final String FILTER_RAMBLER_CONTRIBUTED_INPUT_VIEW_SESSION_FLAG =
            "filter_rambler_contributed_input_view_session";
    public static final String ENABLE_RAMBLER_TOOLBAR_AT_CURSOR_POSITION_FLAG =
            "enable_rambler_toolbar_at_cursor_position";
    public static final String ENABLE_RAMBLER_AL_TOOLBAR_FLAG =
            "enable_rambler_al_toolbar";
    public static final String ENABLE_AGENTIC_DICTATION_FLAG =
            "enable_agentic_dictation";
    public static final String CONFIG_AGENTIC_DICTATION_FLAG =
            "config_agentic_dictation";
    public static final String ENABLE_JETSON_IN_TOOLBAR_FLAG =
            "enable_jetson_in_toolbar";
    public static final String ENABLE_RAMBLER_AL_TOOLBAR_OVERFLOW_MENU_FLAG =
            "enable_rambler_al_toolbar_overflow_menu";
    public static final String ENABLE_HIDE_RAMBLER_AL_IDLING_STATE_FLAG =
            "enable_hide_rambler_al_idling_state";
    public static final String ENABLE_RAMBLER_AL_SHOW_HIDE_NEW_ANIMATION_FLAG =
            "enable_rambler_al_show_hide_new_animation";
    public static final String ENABLE_RAMBLER_AL_TELEPORTING_ANIMATION_FLAG =
            "enable_rambler_al_teleporting_animation";
    public static final String RAMBLER_MOTION_WITH_GRADIENT_COLOR_FLAG =
            "rambler_motion_with_gradient_color";
    public static final String AGENTIC_DICTATION_SPOT_LIGHT2_FLAG =
            "agentic_dictation_spot_light2";
    public static final String AGENTIC_DICTATION_NEW_GI_MOTION_FLAG =
            "agentic_dictation_new_gi_motion";
    public static final String ENABLE_JETSON_S3_QUOTA_EXPERIMENT_OVERRIDE_FLAG =
            "enable_jetson_s3_quota_experiment_override";
    public static final String AGENTIC_DICTATION_DISABLE_PRE_SESSION_QUOTA_CHECK_FLAG =
            "agentic_dictation_disable_pre_session_quota_check";
    public static final String ENABLE_PCM_AUDIO_CACHING_FOR_OFFLINE_FALLBACK_FLAG =
            "enable_pcm_audio_caching_for_offline_fallback";
    public static final String AGENTIC_DICTATION_ENABLE_SEAMLESS_FALLBACK_FLAG =
            "agentic_dictation_enable_seamless_fallback";
    public static final String AGENTIC_DICTATION_ENABLE_PROACTIVE_PARALLEL_ASR_FLAG =
            "agentic_dictation_enable_proactive_parallel_asr";
    public static final String AD_ACTIVATION_TYPE_FLAG = "ad_activation_type";

    private static final Set<String> AGENTIC_FEATURE_FLAGS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    ENABLE_AGENTIC_DICTATION_FLAG,
                    CONFIG_AGENTIC_DICTATION_FLAG,
                    ENABLE_JETSON_IN_TOOLBAR_FLAG,
                    FILTER_RAMBLER_CONTRIBUTED_INPUT_VIEW_SESSION_FLAG,
                    ENABLE_RAMBLER_TOOLBAR_AT_CURSOR_POSITION_FLAG,
                    ENABLE_RAMBLER_AL_TOOLBAR_FLAG,
                    ENABLE_RAMBLER_AL_TOOLBAR_OVERFLOW_MENU_FLAG,
                    ENABLE_HIDE_RAMBLER_AL_IDLING_STATE_FLAG,
                    ENABLE_RAMBLER_AL_SHOW_HIDE_NEW_ANIMATION_FLAG,
                    ENABLE_RAMBLER_AL_TELEPORTING_ANIMATION_FLAG,
                    RAMBLER_MOTION_WITH_GRADIENT_COLOR_FLAG,
                    AGENTIC_DICTATION_SPOT_LIGHT2_FLAG,
                    AGENTIC_DICTATION_NEW_GI_MOTION_FLAG,
                    ENABLE_JETSON_S3_QUOTA_EXPERIMENT_OVERRIDE_FLAG,
                    AGENTIC_DICTATION_DISABLE_PRE_SESSION_QUOTA_CHECK_FLAG,
                    ENABLE_PCM_AUDIO_CACHING_FOR_OFFLINE_FALLBACK_FLAG,
                    AGENTIC_DICTATION_ENABLE_SEAMLESS_FALLBACK_FLAG,
                    AGENTIC_DICTATION_ENABLE_PROACTIVE_PARALLEL_ASR_FLAG)));

    private GboardRambler1803StockPolicy() {
    }

    public static Object maybeForceStockRouteValue(String flagName, Object originalResult) {
        if (SHOW_RAMBLER_DICT_SETTINGS_FLAG.equals(flagName)) {
            return originalResult instanceof Boolean ? Boolean.TRUE : originalResult;
        }
        if (AD_ACTIVATION_TYPE_FLAG.equals(flagName)) {
            return originalResult instanceof Long ? Long.valueOf(2L) : originalResult;
        }
        if (AGENTIC_FEATURE_FLAGS.contains(flagName)) {
            return originalResult instanceof Boolean
                            && GboardRambler1803OfficialSelectionRuntime
                                    .shouldEnableAgenticDictation()
                    ? Boolean.TRUE
                    : originalResult;
        }
        return originalResult;
    }

    public static boolean isTargetFlagName(String flagName) {
        return SHOW_RAMBLER_DICT_SETTINGS_FLAG.equals(flagName)
                || AGENTIC_FEATURE_FLAGS.contains(flagName)
                || AD_ACTIVATION_TYPE_FLAG.equals(flagName);
    }
}
