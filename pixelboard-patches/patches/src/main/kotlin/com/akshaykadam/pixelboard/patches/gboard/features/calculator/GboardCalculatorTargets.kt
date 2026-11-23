/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Simple Calculator ported from Gboard-patches by Jason Wu (@jasonwu1994)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.calculator

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardFieldTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget

/**
 * Lifecycle targets for Simple Calculator, parameterised by the IME service
 * class name so the patch is not pinned to a single Gboard build.
 *
 * The three hooked methods are Android framework overrides on Gboard's IME
 * service (onStartInputView / onUpdateSelection / onWindowHidden), so their
 * names and signatures are stable across Gboard versions; only the obfuscated
 * class name and the InputView field name can move.
 */
internal class GboardCalculatorTargets(
    imeClassType: String,
    inputViewFieldName: String,
) {
    val inputView = GboardFieldTarget(
        classType = imeClassType,
        name = inputViewFieldName,
        type = INPUT_VIEW_TYPE,
    )

    val onStartInputView = GboardMethodTarget(
        classType = imeClassType,
        name = "onStartInputView",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val onUpdateSelection = GboardMethodTarget(
        classType = imeClassType,
        name = "onUpdateSelection",
        parameterTypes = listOf("I", "I", "I", "I", "I", "I"),
        returnType = "V",
    )

    val onWindowHidden = GboardMethodTarget(
        classType = imeClassType,
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    companion object {
        const val INPUT_VIEW_TYPE =
            "Lcom/google/android/libraries/inputmethod/inputview/InputView;"

        /** IME service class verified against Gboard 18.5.4 / 18.5.3. */
        const val KNOWN_IME_CLASS_TYPE = "Lphr;"

        /** InputView field name verified against Gboard 18.5.4 / 18.5.3. */
        const val KNOWN_INPUT_VIEW_FIELD_NAME = "i"
    }
}
