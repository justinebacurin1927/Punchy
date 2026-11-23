package dev.jaycee.punchy;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Small, consistent line icons for the settings UI. The launcher uses the original Punchy mark. */
final class GlyphDrawable extends Drawable {
    static final int CHEVRON = 0, BACK = 1, SLIDERS = 2, NOTIFICATION = 3,
        LAYERS = 4, HELP = 5, TILE = 6, APPS = 7, SCAN = 8, REFRESH = 9, COPY = 10, DOWNLOAD = 11;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int kind;
    private final int size;

    GlyphDrawable(int kind, int color, int size) {
        this.kind = kind; this.size = size;
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
    }
    private void line(float x, float y, float x2, float y2) { path.moveTo(x, y); path.lineTo(x2, y2); }
    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        path.reset();
        switch (kind) {
            case CHEVRON -> { path.moveTo(9, 6); path.lineTo(15, 12); path.lineTo(9, 18); }
            case BACK -> { line(19, 12, 5, 12); path.moveTo(11, 6); path.lineTo(5, 12); path.lineTo(11, 18); }
            case SLIDERS -> {
                line(4, 7, 10, 7); line(14, 7, 20, 7); line(4, 17, 14, 17); line(18, 17, 20, 17);
                path.addCircle(12, 7, 2, Path.Direction.CW); path.addCircle(16, 17, 2, Path.Direction.CW);
            }
            case NOTIFICATION -> {
                path.moveTo(6, 10); path.cubicTo(6, 2, 18, 2, 18, 10); path.lineTo(18, 15);
                path.lineTo(20, 18); path.lineTo(4, 18); path.lineTo(6, 15); path.close();
                path.moveTo(10, 21); path.quadTo(12, 23, 14, 21);
            }
            case LAYERS -> {
                path.moveTo(12, 3); path.lineTo(21, 8); path.lineTo(12, 13); path.lineTo(3, 8); path.close();
                path.moveTo(3, 12); path.lineTo(12, 17); path.lineTo(21, 12);
                path.moveTo(3, 16); path.lineTo(12, 21); path.lineTo(21, 16);
            }
            case HELP -> {
                path.addCircle(12, 12, 9, Path.Direction.CW); path.moveTo(9, 9);
                path.cubicTo(9, 5, 16, 5, 15, 10); path.cubicTo(15, 12, 12, 12, 12, 14); line(12, 17, 12, 17.1f);
            }
            case TILE, APPS -> {
                path.addRoundRect(4, 4, 10, 10, 1.5f, 1.5f, Path.Direction.CW);
                path.addRoundRect(14, 4, 20, 10, 1.5f, 1.5f, Path.Direction.CW);
                path.addRoundRect(4, 14, 10, 20, 1.5f, 1.5f, Path.Direction.CW);
                path.addRoundRect(14, 14, 20, 20, 1.5f, 1.5f, Path.Direction.CW);
            }
            case SCAN -> {
                path.moveTo(8, 3); path.lineTo(3, 3); path.lineTo(3, 8);
                path.moveTo(16, 3); path.lineTo(21, 3); path.lineTo(21, 8);
                path.moveTo(3, 16); path.lineTo(3, 21); path.lineTo(8, 21);
                path.moveTo(21, 16); path.lineTo(21, 21); path.lineTo(16, 21);
                line(7, 12, 10, 12); line(10, 12, 12, 8); line(12, 8, 14, 16); line(14, 16, 16, 12); line(16, 12, 18, 12);
            }
            case REFRESH -> {
                path.moveTo(19, 8); path.cubicTo(12, -2, 1, 8, 6, 16); path.cubicTo(11, 24, 22, 18, 20, 12);
                path.moveTo(19, 3); path.lineTo(19, 8); path.lineTo(14, 8);
            }
            case COPY -> {
                path.addRoundRect(8, 8, 21, 21, 2, 2, Path.Direction.CW);
                path.moveTo(16, 4); path.lineTo(5, 4); path.quadTo(3, 4, 3, 6); path.lineTo(3, 16);
            }
            default -> {
                line(12, 4, 12, 16); path.moveTo(7, 11); path.lineTo(12, 16); path.lineTo(17, 11);
                path.moveTo(4, 18); path.lineTo(4, 21); path.lineTo(20, 21); path.lineTo(20, 18);
            }
        }
        canvas.drawPath(path, paint); canvas.restoreToCount(save);
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
