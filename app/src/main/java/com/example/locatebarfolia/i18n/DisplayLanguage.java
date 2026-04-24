package com.example.locatebarfolia.i18n;

import java.util.Locale;

public enum DisplayLanguage {
    ENGLISH,
    KOREAN;

    public static DisplayLanguage fromLocaleTag(final String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return ENGLISH;
        }

        final String normalized = localeTag.replace('-', '_').toLowerCase(Locale.ROOT);
        return normalized.startsWith("ko") ? KOREAN : ENGLISH;
    }
}
