// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Local app artwork inside one continuous rounded-rectangle mask. */
final class AppIconView extends View {
    private final Drawable icon;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path mask = new Path();
    /** Corner radius as a share of the tile side; 0.22 matches typical launcher masks. */
    private static final float CORNER_RATIO = .22f;

    AppIconView(Context context, Drawable drawable) {
        super(context); icon = drawable.mutate();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static Bitmap render(Context context, Drawable drawable, int size) {
        AppIconView view = new AppIconView(context, drawable);
        view.layout(0, 0, size, size);
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(result));
        return result;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        mask.reset();
        // One rounded rectangle for every tile: the same corner radius on real
        // app icons and on the bundled vector artwork, so the grid reads as a set.
        float radius = Math.min(w, h) * CORNER_RATIO;
        mask.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CW);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas target = new Canvas(bitmap); icon.setBounds(0, 0, w, h);
        if (icon.getClass() == AdaptiveIconDrawable.class) {
            // Preserve Android's expanded adaptive bounds so the artwork keeps
            // its launcher scale. Repair an empty layer without shrinking the
            // foreground into its resource's safe-zone a second time.
            AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable)icon;
            drawLayer(adaptive.getBackground(), target, w, h);
            drawLayer(adaptive.getForeground(), target, w, h);
        } else {
            // OEM subclasses may draw themed artwork in their own draw()
            // rather than in the public foreground/background layers.
            icon.draw(target);
        }
        paint.setShader(new BitmapShader(fill(bitmap, w, h), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
    }

    private static void drawLayer(Drawable layer, Canvas canvas, int w, int h) {
        if (layer == null) return;
        Rect previous = new Rect(layer.getBounds());
        if (previous.isEmpty()) layer.setBounds(0, 0, w, h);
        layer.draw(canvas); layer.setBounds(previous);
    }

    /**
     * Adaptive icons keep their artwork inside a safe zone, so on most phones the
     * drawn pixels are noticeably smaller than the tile while a few legacy bitmaps
     * fill it edge to edge. Stretching the drawn area back to the tile edges is what
     * makes every icon share one outline; the rounded mask then trims the corners.
     */
    private static Bitmap fill(Bitmap source, int w, int h) {
        int left = w, top = h, right = -1, bottom = -1;
        int[] pixels = new int[w * h];
        source.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                if ((pixels[row + x] >>> 24) > 8) {
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }
        if (right < left || bottom < top) return source;
        int drawnWidth = right - left + 1, drawnHeight = bottom - top + 1;
        if (drawnWidth >= w - 1 && drawnHeight >= h - 1) return source;
        float scale = Math.min((float) w / drawnWidth, (float) h / drawnHeight);
        float drawnTargetWidth = drawnWidth * scale, drawnTargetHeight = drawnHeight * scale;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint scaler = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, new Rect(left, top, right + 1, bottom + 1),
                new RectF((w - drawnTargetWidth) / 2f, (h - drawnTargetHeight) / 2f,
                        (w + drawnTargetWidth) / 2f, (h + drawnTargetHeight) / 2f), scaler);
        return out;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (paint.getShader() != null) canvas.drawPath(mask, paint);
    }
}
