/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Keeps Gboard's own Rambler minimize key next to the finish key instead of in the close slot.
 *
 * <p>With the AL toolbar off, stock Gboard swaps the Rambler close key for a minimize key that
 * collapses the keyboard into the floating voice toolbar. The patch keeps the close key and moves
 * the minimize key into the header's end extra key slot, the same slot the stock voice typing
 * minimize key uses.</p>
 */
public final class GboardRamblerMinimizeKeyRuntime {
    private static final String END_EXTRA_KEY_NAME = "key_pos_header_end_extra_key";

    private static int endExtraKeyId;
    private static boolean minimizeKeyPlaced;

    private GboardRamblerMinimizeKeyRuntime() {
    }

    /** Returns the slot for the minimize key, or 0 to keep Gboard's stock placement. */
    public static int placeMinimizeKey() {
        int id = resolveEndExtraKeyId(null);
        minimizeKeyPlaced = id != 0;
        return id;
    }

    /** Returns the slot to clear when the Rambler header is torn down, or 0 if nothing was placed. */
    public static int clearMinimizeKey() {
        if (!minimizeKeyPlaced) {
            return 0;
        }
        minimizeKeyPlaced = false;
        return resolveEndExtraKeyId(null);
    }

    /** Undoes the Rambler header's hiding of non-Rambler slots for the minimize key slot. */
    public static void revealMinimizeKey(ViewGroup header) {
        if (!minimizeKeyPlaced || header == null) {
            return;
        }
        int id = resolveEndExtraKeyId(header.getContext());
        if (id == 0) {
            return;
        }
        View key = header.findViewById(id);
        if (key == null) {
            return;
        }
        key.animate().cancel();
        key.setAlpha(1.0f);
        key.setVisibility(View.VISIBLE);
    }

    private static int resolveEndExtraKeyId(Context context) {
        if (endExtraKeyId != 0) {
            return endExtraKeyId;
        }
        Context target = context != null ? context : GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (target == null) {
            return 0;
        }
        try {
            endExtraKeyId = target.getResources().getIdentifier(
                    END_EXTRA_KEY_NAME, "id", target.getPackageName());
        } catch (Throwable ignored) {
            endExtraKeyId = 0;
        }
        return endExtraKeyId;
    }

    static void resetForTests() {
        endExtraKeyId = 0;
        minimizeKeyPlaced = false;
    }

    static void seedEndExtraKeyIdForTests(int id) {
        endExtraKeyId = id;
    }
}
