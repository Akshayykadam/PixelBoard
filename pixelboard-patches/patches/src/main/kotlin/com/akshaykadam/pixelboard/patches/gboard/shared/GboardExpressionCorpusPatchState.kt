package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags

internal enum class GboardExpressionCorpusPatchState {
    STOCK,
    CUSTOM_ONLY,
    TAB_ONLY,
    COMPOSED,
}

internal fun MutableMethod.requireGboardExpressionCorpusPatchState():
    GboardExpressionCorpusPatchState {
    check(
        definingClass == EXPRESSION_CORPUS_MANAGER_CLASS &&
            name == EXPRESSION_CORPUS_MANAGER_METHOD &&
            returnType == EXPRESSION_CORPUS_MANAGER_RETURN_TYPE &&
            parameterTypes == EXPRESSION_CORPUS_MANAGER_PARAMETERS,
    ) {
        "Refusing non-target expression corpus method " +
            "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"
    }
    check(accessFlags == EXPECTED_ACCESS_FLAGS) {
        "Unexpected access flags in $EXPRESSION_CORPUS_MANAGER_DESCRIPTOR: $accessFlags"
    }
    val fingerprint = gboardStructuralFingerprint()
    return when (fingerprint) {
        STOCK_FINGERPRINT -> GboardExpressionCorpusPatchState.STOCK
        CUSTOM_ONLY_FINGERPRINT -> GboardExpressionCorpusPatchState.CUSTOM_ONLY
        TAB_ONLY_FINGERPRINT -> GboardExpressionCorpusPatchState.TAB_ONLY
        COMPOSED_FINGERPRINT -> GboardExpressionCorpusPatchState.COMPOSED
        else -> error(
            "Unexpected expression corpus structure in $EXPRESSION_CORPUS_MANAGER_DESCRIPTOR: " +
                fingerprint,
        )
    }
}

private const val EXPRESSION_CORPUS_MANAGER_CLASS = "Lgri;"
private const val EXPRESSION_CORPUS_MANAGER_METHOD = "a"
private const val EXPRESSION_CORPUS_MANAGER_RETURN_TYPE = "Lvvw;"
private val EXPRESSION_CORPUS_MANAGER_PARAMETERS =
    listOf("Landroid/view/inputmethod/EditorInfo;", "Z")
private const val EXPRESSION_CORPUS_MANAGER_DESCRIPTOR =
    "Lgri;->a(Landroid/view/inputmethod/EditorInfo;Z)Lvvw;"
private val EXPECTED_ACCESS_FLAGS = AccessFlags.PUBLIC.value or AccessFlags.FINAL.value
private const val STOCK_FINGERPRINT =
    "16e28c427db28420cefee20eb9540514abeb052611f217d573d162b51bd448bd"
private const val CUSTOM_ONLY_FINGERPRINT =
    "eb76629f175f17385fbdea910d71428d3146b51c21b8d4a206c2763458374069"
private const val TAB_ONLY_FINGERPRINT =
    "84e94bd83ed9d56aa5efac05b5044d50ba14d6a8ec2b8f4df0409de4a93b50ab"
private const val COMPOSED_FINGERPRINT =
    "ca4083859038f75ddadd281d4a389dff842c6a7399764d9f5dfcaeb17b292c61"
