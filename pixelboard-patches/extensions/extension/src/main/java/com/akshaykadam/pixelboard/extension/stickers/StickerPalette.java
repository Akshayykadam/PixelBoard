/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

/** Material You colour roles from the system palette (Android 14+), else the M3 baseline. */
final class StickerPalette {
    final int primary;
    final int onPrimary;
    final int secondaryContainer;
    final int onSecondaryContainer;
    final int surfaceContainer;
    final int surfaceContainerHigh;
    final int surfaceContainerHighest;
    final int onSurface;
    final int onSurfaceVariant;
    final int outline;
    final int error;

    private StickerPalette(int[] roles) {
        primary = roles[0];
        onPrimary = roles[1];
        secondaryContainer = roles[2];
        onSecondaryContainer = roles[3];
        surfaceContainer = roles[4];
        surfaceContainerHigh = roles[5];
        surfaceContainerHighest = roles[6];
        onSurface = roles[7];
        onSurfaceVariant = roles[8];
        outline = roles[9];
        error = roles[10];
    }

    static StickerPalette of(Context context) {
        boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int[] ids = dark ? new int[] {
                    android.R.color.system_primary_dark,
                    android.R.color.system_on_primary_dark,
                    android.R.color.system_secondary_container_dark,
                    android.R.color.system_on_secondary_container_dark,
                    android.R.color.system_surface_container_dark,
                    android.R.color.system_surface_container_high_dark,
                    android.R.color.system_surface_container_highest_dark,
                    android.R.color.system_on_surface_dark,
                    android.R.color.system_on_surface_variant_dark,
                    android.R.color.system_outline_dark,
                    android.R.color.system_error_dark,
            } : new int[] {
                    android.R.color.system_primary_light,
                    android.R.color.system_on_primary_light,
                    android.R.color.system_secondary_container_light,
                    android.R.color.system_on_secondary_container_light,
                    android.R.color.system_surface_container_light,
                    android.R.color.system_surface_container_high_light,
                    android.R.color.system_surface_container_highest_light,
                    android.R.color.system_on_surface_light,
                    android.R.color.system_on_surface_variant_light,
                    android.R.color.system_outline_light,
                    android.R.color.system_error_light,
            };
            int[] roles = new int[ids.length];
            for (int i = 0; i < ids.length; i++) {
                roles[i] = context.getResources().getColor(ids[i], null);
            }
            return new StickerPalette(roles);
        }
        // M3 baseline scheme.
        return new StickerPalette(dark
                ? new int[] { 0xFFD0BCFF, 0xFF381E72, 0xFF4A4458, 0xFFE8DEF8, 0xFF211F26,
                        0xFF2B2930, 0xFF36343B, 0xFFE6E0E9, 0xFFCAC4D0, 0xFF938F99, 0xFFF2B8B5 }
                : new int[] { 0xFF6750A4, 0xFFFFFFFF, 0xFFE8DEF8, 0xFF1D192B, 0xFFF3EDF7,
                        0xFFECE6F0, 0xFFE6E0E9, 0xFF1D1B20, 0xFF49454F, 0xFF79747E, 0xFFB3261E });
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
