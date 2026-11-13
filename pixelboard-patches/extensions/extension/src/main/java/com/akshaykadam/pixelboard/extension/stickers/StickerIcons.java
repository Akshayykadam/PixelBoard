/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.stickers;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * 24dp icons drawn in code, since the extension ships no drawables. Add and close use the
 * Material icon outlines; the sticker is a rounded square with a peeled corner.
 */
final class StickerIcons extends Drawable {
    static final int ADD = 0;
    static final int CLOSE = 1;
    static final int STICKER = 2;

    // Material "add" and "close" icons, as polygons on the 24x24 grid.
    private static final float[] ADD_POINTS = { 19, 13, 13, 13, 13, 19, 11, 19, 11, 13, 5, 13,
            5, 11, 11, 11, 11, 5, 13, 5, 13, 11, 19, 11 };
    private static final float[] CLOSE_POINTS = { 19, 6.41f, 17.59f, 5, 12, 10.59f, 6.41f, 5,
            5, 6.41f, 10.59f, 12, 5, 17.59f, 6.41f, 19, 12, 13.41f, 17.59f, 19, 19, 17.59f,
            13.41f, 12 };

    private final int glyph;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private ColorStateList tint = ColorStateList.valueOf(0xFF000000);

    StickerIcons(int glyph) {
        this.glyph = glyph;
        if (glyph == STICKER) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            // Outline with the bottom-right corner peeled off, plus the fold.
            path.moveTo(7, 4);
            path.lineTo(17, 4);
            path.quadTo(20, 4, 20, 7);
            path.lineTo(20, 13);
            path.lineTo(13, 20);
            path.lineTo(7, 20);
            path.quadTo(4, 20, 4, 17);
            path.lineTo(4, 7);
            path.quadTo(4, 4, 7, 4);
            path.close();
            path.moveTo(13, 20);
            path.lineTo(13, 16);
            path.quadTo(13, 13, 16, 13);
            path.lineTo(20, 13);
        } else {
            float[] points = glyph == ADD ? ADD_POINTS : CLOSE_POINTS;
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            path.close();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        paint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(save);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint != null) {
            this.tint = tint;
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return tint.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        invalidateSelf();
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        return 24;
    }

    @Override
    public int getIntrinsicHeight() {
        return 24;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
