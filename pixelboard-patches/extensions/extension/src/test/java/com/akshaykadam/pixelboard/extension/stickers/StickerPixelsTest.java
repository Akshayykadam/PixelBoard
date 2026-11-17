package com.akshaykadam.pixelboard.extension.stickers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StickerPixelsTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFFFF0000;

    @Test
    public void clearsPlainBackgroundAndKeepsSubject() {
        int[] pixels = filled(5, 5, WHITE);
        pixels[12] = RED;
        // Near-white noise in the background still counts as background.
        pixels[1] = 0xFFF0F0F0;

        assertTrue(StickerPixels.removePlainBackground(pixels, 5, 5));

        for (int i = 0; i < pixels.length; i++) {
            assertEquals("pixel " + i, i == 12 ? RED : 0, pixels[i]);
        }
    }

    @Test
    public void keepsBackgroundColourEnclosedBySubject() {
        // A red ring around a white centre: the centre is not reachable from the border.
        int[] pixels = filled(5, 5, WHITE);
        for (int i : new int[] { 6, 7, 8, 11, 13, 16, 17, 18 }) pixels[i] = RED;

        assertTrue(StickerPixels.removePlainBackground(pixels, 5, 5));

        assertEquals(WHITE, pixels[12]);
        assertEquals(RED, pixels[6]);
        assertEquals(0, pixels[0]);
    }

    @Test
    public void leavesBusyBorderUntouched() {
        int[] pixels = new int[16];
        for (int i = 0; i < pixels.length; i++) pixels[i] = 0xFF000000 | (i * 0x101010 * 15);
        int[] before = pixels.clone();

        assertFalse(StickerPixels.removePlainBackground(pixels, 4, 4));
        assertArrayEquals(before, pixels);
    }

    @Test
    public void leavesExistingCutoutUntouched() {
        int[] pixels = filled(3, 3, 0);
        pixels[4] = 0xFF101010;

        assertFalse(StickerPixels.removePlainBackground(pixels, 3, 3));
        assertEquals(0xFF101010, pixels[4]);
    }

    @Test
    public void opaqueBoundsFindsSubjectOrNull() {
        int[] pixels = filled(4, 3, 0);
        pixels[5] = RED;
        pixels[7] = RED;
        assertArrayEquals(new int[] { 1, 1, 4, 2 }, StickerPixels.opaqueBounds(pixels, 4, 3));
        assertNull(StickerPixels.opaqueBounds(filled(2, 2, 0), 2, 2));
    }

    private static int[] filled(int width, int height, int color) {
        int[] pixels = new int[width * height];
        java.util.Arrays.fill(pixels, color);
        return pixels;
    }
}
