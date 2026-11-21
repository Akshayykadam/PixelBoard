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
 * Keeps Gboard's own Rambler minimize key next to the close key on the left side instead of replacing it.
 *
 * <p>With the AL toolbar off, stock Gboard swaps the Rambler close key for a minimize key that
 * collapses the keyboard into the floating voice toolbar. The patch keeps the close key and moves
 * the minimize key into the header's start extra key slot (or end extra key slot as fallback),
 * keeping both Close and Minimise accessible on the left side.</p>
 */
public final class GboardRamblerMinimizeKeyRuntime {
    private static final String START_EXTRA_KEY_NAME = "key_pos_header_start_extra_key";
    private static final String END_EXTRA_KEY_NAME = "key_pos_header_end_extra_key";

    private static int minimizeKeySlotId;
    private static boolean minimizeKeyPlaced;

    private GboardRamblerMinimizeKeyRuntime() {
    }

    /** Returns the slot for the minimize key, or 0 to keep Gboard's stock placement. */
    public static int placeMinimizeKey() {
        int id = resolveMinimizeKeySlotId(null);
        minimizeKeyPlaced = id != 0;
        return id;
    }

    /** Returns the slot to clear when the Rambler header is torn down, or 0 if nothing was placed. */
    public static int clearMinimizeKey() {
        if (!minimizeKeyPlaced) {
            return 0;
        }
        minimizeKeyPlaced = false;
        return resolveMinimizeKeySlotId(null);
    }

    /** Undoes the Rambler header's hiding of non-Rambler slots for the minimize key slot. */
    public static void revealMinimizeKey(ViewGroup header) {
        if (!minimizeKeyPlaced || header == null) {
            return;
        }
        int id = resolveMinimizeKeySlotId(header.getContext());
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

    private static int resolveMinimizeKeySlotId(Context context) {
        if (minimizeKeySlotId != 0) {
            return minimizeKeySlotId;
        }
        Context target = context != null ? context : GboardRambler1803OfficialSelectionRuntime.applicationContext();
        if (target == null) {
            return 0;
        }
        try {
            // Prefer left side next to close key: key_pos_header_start_extra_key
            minimizeKeySlotId = target.getResources().getIdentifier(
                    START_EXTRA_KEY_NAME, "id", target.getPackageName());
            if (minimizeKeySlotId == 0) {
                // Fallback to right side if start extra key is missing
                minimizeKeySlotId = target.getResources().getIdentifier(
                        END_EXTRA_KEY_NAME, "id", target.getPackageName());
            }
        } catch (Throwable ignored) {
            minimizeKeySlotId = 0;
        }
        return minimizeKeySlotId;
    }

    static void resetForTests() {
        minimizeKeySlotId = 0;
        minimizeKeyPlaced = false;
    }

    static void seedSlotKeyIdForTests(int id) {
        minimizeKeySlotId = id;
    }

    static void seedEndExtraKeyIdForTests(int id) {
        seedSlotKeyIdForTests(id);
    }
}
