package com.akshaykadam.pixelboard.extension.advancedvoice;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets Gboard request the download of a language pack only once per language and source in
 * each process.
 *
 * <p>The eligibility checker requests a download every time a locale has no offline speech
 * pack. The speech service refuses silent downloads from the renamed package but still
 * completes the request, so the download counts as a success without installing anything; the
 * completion re-runs the eligibility check, which requests the same download again, every few
 * milliseconds for as long as an interaction is open.
 */
public final class GboardLanguageDownloadGuard {
    private static final ConcurrentHashMap<String, Boolean> REQUESTED =
            new ConcurrentHashMap<String, Boolean>();

    private GboardLanguageDownloadGuard() {
    }

    /** Returns true when an identical request was already let through; the caller drops it. */
    public static boolean shouldSkip(String languageTag, Object source) {
        try {
            return languageTag != null
                    && REQUESTED.putIfAbsent(languageTag + '|' + source, Boolean.TRUE) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void resetForTest() {
        REQUESTED.clear();
    }
}
