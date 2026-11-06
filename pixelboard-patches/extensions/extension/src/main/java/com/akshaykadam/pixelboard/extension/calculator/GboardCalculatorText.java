package com.akshaykadam.pixelboard.extension.calculator;

import java.util.Locale;

/** Locale selection for calculator labels and announcements. */
final class GboardCalculatorText {
    private GboardCalculatorText() {
    }

    static String localized(String chinese, String english) {
        return "zh".equalsIgnoreCase(Locale.getDefault().getLanguage()) ? chinese : english;
    }
}
