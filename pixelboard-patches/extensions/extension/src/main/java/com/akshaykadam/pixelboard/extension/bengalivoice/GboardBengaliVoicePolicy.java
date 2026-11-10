package com.akshaykadam.pixelboard.extension.bengalivoice;

import java.util.IllformedLocaleException;
import java.util.Locale;

/** Locale and value rules shared by the Bengali settings row and dictation routing. */
public final class GboardBengaliVoicePolicy {
    public static final String STANDARD = "standard";
    public static final String RAMBLER = "rambler";
    public static final String DEFAULT_MODE = STANDARD;

    private GboardBengaliVoicePolicy() {
    }

    public static boolean isBengali(String languageTag) {
        return normalizeBengaliTag(languageTag) != null;
    }

    /** All layouts/scripts for one Bengali region share its voice setting. */
    public static String normalizeBengaliTag(String languageTag) {
        if (languageTag == null || languageTag.trim().isEmpty()) {
            return null;
        }
        try {
            Locale locale = new Locale.Builder()
                    .setLanguageTag(languageTag.trim().replace('_', '-'))
                    .build();
            if (!"bn".equals(locale.getLanguage())) {
                return null;
            }
            String region = locale.getCountry();
            return region.isEmpty() ? "bn" : "bn-" + region;
        } catch (IllformedLocaleException invalidTag) {
            return null;
        }
    }

    public static boolean isValidMode(String mode) {
        return STANDARD.equals(mode) || RAMBLER.equals(mode);
    }

    public static String modeOrDefault(Object value) {
        return RAMBLER.equals(value) ? RAMBLER : DEFAULT_MODE;
    }
}
