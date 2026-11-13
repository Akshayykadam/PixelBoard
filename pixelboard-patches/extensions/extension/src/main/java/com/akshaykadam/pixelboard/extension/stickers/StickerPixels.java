/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import java.util.HashMap;
import java.util.Map;

/**
 * Pixel operations on ARGB int arrays. Free of Android types so they run in plain unit tests.
 */
final class StickerPixels {
    // Max per-channel difference from the background colour that still counts as background.
    static final int TOLERANCE = 40;

    private StickerPixels() {
    }

    /**
     * Clears the background connected to the image border, but only when at least half of the
     * border shares one colour. Photos rarely have that, so they are left untouched instead of
     * being eaten away.
     *
     * @return false when no plain background was found and nothing changed
     */
    static boolean removePlainBackground(int[] pixels, int width, int height) {
        int[] border = borderIndices(width, height);
        Map<Integer, Integer> buckets = new HashMap<Integer, Integer>();
        int bestBucket = 0;
        int bestCount = 0;
        for (int index : border) {
            int bucket = bucket(pixels[index]);
            Integer previous = buckets.get(bucket);
            int count = previous == null ? 1 : previous + 1;
            buckets.put(bucket, count);
            if (count > bestCount) {
                bestCount = count;
                bestBucket = bucket;
            }
        }
        // A transparent border means the image is already a cutout.
        if (bestBucket == -1 || bestCount * 2 < border.length) {
            return false;
        }

        long r = 0;
        long g = 0;
        long b = 0;
        for (int index : border) {
            int color = pixels[index];
            if (bucket(color) == bestBucket) {
                r += (color >> 16) & 0xFF;
                g += (color >> 8) & 0xFF;
                b += color & 0xFF;
            }
        }
        int background = (int) (r / bestCount) << 16 | (int) (g / bestCount) << 8
                | (int) (b / bestCount);

        boolean[] cleared = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int tail = 0;
        for (int index : border) {
            if (!cleared[index] && isBackground(pixels[index], background)) {
                cleared[index] = true;
                queue[tail++] = index;
            }
        }
        for (int head = 0; head < tail; head++) {
            int index = queue[head];
            pixels[index] = 0;
            int x = index % width;
            int y = index / width;
            if (x > 0) tail = visit(pixels, cleared, queue, tail, index - 1, background);
            if (x < width - 1) tail = visit(pixels, cleared, queue, tail, index + 1, background);
            if (y > 0) tail = visit(pixels, cleared, queue, tail, index - width, background);
            if (y < height - 1) tail = visit(pixels, cleared, queue, tail, index + width, background);
        }
        return true;
    }

    /**
     * Returns {left, top, right, bottom} (right and bottom exclusive) of the non-transparent
     * pixels, or null when the image is fully transparent.
     */
    static int[] opaqueBounds(int[] pixels, int width, int height) {
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) != 0) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return right < 0 ? null : new int[] { left, top, right + 1, bottom + 1 };
    }

    private static int visit(int[] pixels, boolean[] cleared, int[] queue, int tail, int index,
            int background) {
        if (!cleared[index] && isBackground(pixels[index], background)) {
            cleared[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }

    private static boolean isBackground(int color, int background) {
        if ((color >>> 24) == 0) {
            return true;
        }
        return Math.abs(((color >> 16) & 0xFF) - ((background >> 16) & 0xFF)) <= TOLERANCE
                && Math.abs(((color >> 8) & 0xFF) - ((background >> 8) & 0xFF)) <= TOLERANCE
                && Math.abs((color & 0xFF) - (background & 0xFF)) <= TOLERANCE;
    }

    // Groups similar colours so slight noise along the border still counts as one colour.
    private static int bucket(int color) {
        if ((color >>> 24) == 0) {
            return -1;
        }
        return ((color >> 20) & 0xF) << 8 | ((color >> 12) & 0xF) << 4 | ((color >> 4) & 0xF);
    }

    private static int[] borderIndices(int width, int height) {
        if (height == 1) {
            int[] row = new int[width];
            for (int x = 0; x < width; x++) row[x] = x;
            return row;
        }
        int sides = Math.max(0, height - 2);
        int[] indices = new int[width * 2 + (width > 1 ? sides * 2 : sides)];
        int n = 0;
        for (int x = 0; x < width; x++) {
            indices[n++] = x;
            indices[n++] = (height - 1) * width + x;
        }
        for (int y = 1; y < height - 1; y++) {
            indices[n++] = y * width;
            if (width > 1) indices[n++] = y * width + width - 1;
        }
        return indices;
    }
}
