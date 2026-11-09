package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Keeps Gboard's clipboard texts honest about how long clips last.
 *
 * <p>Google's texts say clips are deleted after 1 hour, so they are shown unchanged only while
 * the retention is 1 hour. At any other retention, including PixelBoard's 24-hour default, the
 * pin tip names the chosen duration instead, and the other texts are swapped for Google's own
 * translated wording that names no duration.</p>
 */
public final class GboardClipboardRetentionTextRuntime {
    // Gboard 18.5.3 resource ids.
    /** "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour." */
    static final int PIN_TIP = 0x7f1401a6;
    /** "Gboard's clipboard lets you copy text and images, keeping them for 1 hour, ..." */
    static final int INTRO_CARD = 0x7f140192;
    /** "No items copied within the last hour" */
    static final int EMPTY_SUBTITLE = 0x7f14019e;
    /** "Clipboard allows you to access your recent copy and paste history" */
    static final int DESCRIPTION = 0x7f1401a1;

    // Gboard 18.6.3 resource ids.
    static final int PIN_TIP_1863 = 0x7f1401a5;
    static final int INTRO_CARD_1863 = 0x7f140191;
    static final int EMPTY_SUBTITLE_1863 = 0x7f14019d;
    static final int DESCRIPTION_1863 = 0x7f1401a0;
    /** The intro card's body TextView in layouts 0x7f0e0061 and 0x7f0e0062. */
    static final int INTRO_CARD_BODY_ID = 0x7f0b01ac;
    /** "Touch and hold a clip to pin it.", added to every locale by the resource patch. */
    static final String PIN_TIP_SHORT_NAME = "pixelboard_clipboard_pin_tip_short";
    /** The pin tip saying "... deleted after 24 hours.", "7 days." or "30 days.", per locale. */
    static final String PIN_TIP_24_HOURS_NAME = "pixelboard_clipboard_pin_tip_24h";
    static final String PIN_TIP_7_DAYS_NAME = "pixelboard_clipboard_pin_tip_7d";
    static final String PIN_TIP_30_DAYS_NAME = "pixelboard_clipboard_pin_tip_30d";

    private GboardClipboardRetentionTextRuntime() {
    }

    /** Returns the string to show in place of one of Gboard's clipboard strings. */
    public static int resolveText(int resId) {
        try {
            Context context = GboardClipboardHistorySettings.applicationContext();
            if (context == null || GboardClipboardHistorySettings.isOneHourRetention(context)) {
                return resId;
            }
            return replacementFor(context, resId);
        } catch (Throwable ignored) {
            return resId;
        }
    }

    /** Updates the "Copy more for faster pasting" card after Gboard inflates it. */
    public static void applyIntroCardText(View card) {
        try {
            if (card == null
                    || GboardClipboardHistorySettings.isOneHourRetention(card.getContext())) {
                return;
            }
            View body = card.findViewById(INTRO_CARD_BODY_ID);
            if (body instanceof TextView) {
                int descriptionRes = resolveDescriptionRes(card.getContext());
                if (descriptionRes != 0) {
                    ((TextView) body).setText(descriptionRes);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static int replacementFor(Context context, int resId) {
        if (resId == PIN_TIP || resId == PIN_TIP_1863) {
            int tip = string(context, pinTipName(
                    GboardClipboardHistorySettings.readGboardDuration(context)));
            if (tip == 0) {
                tip = string(context, PIN_TIP_SHORT_NAME);
            }
            return tip != 0 ? tip : resId;
        }
        if (resId == INTRO_CARD || resId == EMPTY_SUBTITLE) {
            return DESCRIPTION;
        }
        if (resId == INTRO_CARD_1863 || resId == EMPTY_SUBTITLE_1863) {
            return DESCRIPTION_1863;
        }
        return resId;
    }

    static int resolveDescriptionRes(Context context) {
        try {
            if (context != null) {
                context.getResources().getString(DESCRIPTION_1863);
                return DESCRIPTION_1863;
            }
        } catch (Throwable ignored) {
        }
        return DESCRIPTION;
    }

    /** The pin tip naming {@code duration}, or the short tip when clips are never deleted. */
    static String pinTipName(String duration) {
        switch (duration) {
            case GboardClipboardHistorySettings.DURATION_24_HOURS: return PIN_TIP_24_HOURS_NAME;
            case GboardClipboardHistorySettings.DURATION_7_DAYS:   return PIN_TIP_7_DAYS_NAME;
            case GboardClipboardHistorySettings.DURATION_30_DAYS:  return PIN_TIP_30_DAYS_NAME;
            default:                                               return PIN_TIP_SHORT_NAME;
        }
    }

    private static int string(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }
}
