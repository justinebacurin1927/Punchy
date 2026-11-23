package dev.jaycee.punchy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Static, decorative 65% illustration. Real download state is shown separately in text. */
final class IslandPreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(), segment = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final RectF pill = new RectF();
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final GlyphDrawable arrow;
    private LinearGradient fill;
    private float radius, labelX, labelBaseline;
    IslandPreviewView(Context c) {
        super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        arrow = new GlyphDrawable(GlyphDrawable.DOWNLOAD, Ui.MINT, Ui.dp(c, 22));
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 18, getResources().getDisplayMetrics()));
        paint.getFontMetrics(metrics);
    }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float w = Math.min(Ui.dp(getContext(), 188), getWidth() - Ui.dp(getContext(), 8));
        float h = Ui.dp(getContext(), 58), left = (getWidth() - w) / 2f, top = (getHeight() - h) / 2f;
        pill.set(left, top, left + w, top + h); radius = h / 2;
        fill = new LinearGradient(left, top, left + w, top + h,
            new int[] { 0xFF26342F, 0xFF111A18, 0xFF080E0C }, null, Shader.TileMode.CLAMP);
        path.reset(); path.addRoundRect(pill, radius, radius, Path.Direction.CW);
        measure.setPath(path, true); segment.reset();
        measure.getSegment(0, measure.getLength() * .65f, segment, true);
        int centerY = (int) (top + h / 2), glyphSize = Ui.dp(getContext(), 22);
        int glyphX = (int) left + Ui.dp(getContext(), 24);
        arrow.setBounds(glyphX, centerY - glyphSize / 2, glyphX + glyphSize, centerY + glyphSize / 2);
        labelX = left + Ui.dp(getContext(), 87);
        labelBaseline = centerY - (metrics.ascent + metrics.descent) / 2;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(fill);
        canvas.drawRoundRect(pill, radius, radius, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Ui.dp(getContext(), 1)); paint.setColor(0x30FFFFFF);
        canvas.drawPath(path, paint);
        paint.setColor(0x22D3FF5F); paint.setStrokeWidth(Ui.dp(getContext(), 5)); canvas.drawPath(segment, paint);
        paint.setColor(Ui.MINT); paint.setStrokeWidth(Ui.dp(getContext(), 2)); paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawPath(segment, paint);
        arrow.draw(canvas);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Ui.TEXT);
        canvas.drawText("65%", labelX, labelBaseline, paint);
    }
}
