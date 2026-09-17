package com.akshaykadam.pixelboard.extension.advancedvoice;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GboardOfflineSpeechLanguageFormatter {
    private GboardOfflineSpeechLanguageFormatter() {
    }

    public static List<String> format(List<String> languageTags, Locale displayLocale) {
        if (languageTags == null || languageTags.isEmpty()) {
            return Collections.emptyList();
        }
        Locale safeDisplayLocale = displayLocale == null ? Locale.getDefault() : displayLocale;
        Set<String> displayNames = new LinkedHashSet<String>();
        for (String languageTag : languageTags) {
            if (languageTag == null || languageTag.trim().isEmpty()) {
                continue;
            }
            Locale locale = Locale.forLanguageTag(languageTag.trim());
            if (locale.getLanguage().isEmpty() || "und".equals(locale.getLanguage())) {
                continue;
            }
            String displayName = locale.getDisplayName(safeDisplayLocale);
            if (displayName != null && !displayName.trim().isEmpty()) {
                displayNames.add(displayName.trim());
            }
        }
        if (displayNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sortedDisplayNames = new ArrayList<String>(displayNames);
        Collections.sort(sortedDisplayNames, Collator.getInstance(safeDisplayLocale));
        return Collections.unmodifiableList(sortedDisplayNames);
    }
}
