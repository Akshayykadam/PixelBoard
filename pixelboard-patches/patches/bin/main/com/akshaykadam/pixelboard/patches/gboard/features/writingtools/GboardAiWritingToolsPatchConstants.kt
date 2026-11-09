package com.akshaykadam.pixelboard.patches.gboard.features.writingtools

import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId

internal const val AI_WRITING_TOOLS_FEATURE_MARKER_NAME =
    "com.akshaykadam.pixelboard.feature.ai_writing_tools"
internal val AI_WRITING_TOOLS_RUNTIME_CLASS = RuntimeAbiCatalog.abi(
    RuntimeCallId.AI_WRITING_TOOLS_RUNTIME_APPLY_OVERRIDDEN_FLAG_VALUE,
).owner
internal const val SIGNAL_GATE_CLASS = "Lpel;"
internal const val SIGNAL_MARKER_CLASS = "Lpei;"
internal const val PREFERENCE_MANAGER_CLASS = "Lpnp;"
internal const val SETTINGS_CONTROLLER_CLASS = "Leqn;"
internal const val PREFERENCE_WRAPPER_CLASS = "Lptc;"
internal const val BACKEND_FACTORY_CLASS = "Lgyc;"
