package dev.jaycee.punchy;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.View;

/** Transparent, non-interactive Canvas border. No polling when determinate or idle. */
final class RingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path segment = new Path();
    private final PathMeasure measure = new PathMeasure();
    private float fraction = .65f;
    private float phase;
    private float stroke = 2f;
    private boolean unknown;
    private ValueAnimator animator;

    RingView(Context c) {
        super(c);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }
    void progress(int percent, boolean indeterminate, int thicknessDp) {
        fraction = Math.max(0, Math.min(1, percent / 100f));
        unknown = indeterminate;
        stroke = thicknessDp * getResources().getDisplayMetrics().density;
        rebuildPath();
        updateMotion();
        invalidate();
    }
    private void updateMotion() {
        if (unknown && ValueAnimator.areAnimatorsEnabled() && isAttachedToWindow()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0, 1);
                animator.setDuration(1500);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setInterpolator(new android.view.animation.LinearInterpolator());
                animator.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
                animator.start();
            }
        } else stopAnimation();
    }
    private void stopAnimation() {
        if (animator != null) { animator.cancel(); animator = null; }
        phase = 0;
    }
    @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); updateMotion(); }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { rebuildPath(); }
    private void rebuildPath() {
        float inset = stroke / 2f + 2f;
        float left = inset, top = inset, right = getWidth() - inset, bottom = getHeight() - inset;
        if (right <= left || bottom <= top) return;
        float radius = Math.min(right - left, bottom - top) / 2f;
        float center = (left + right) / 2f;
        path.reset();
        path.moveTo(center, top);
        path.lineTo(right - radius, top);
        path.arcTo(new RectF(right - radius * 2, top, right, top + radius * 2), -90, 90, false);
        path.lineTo(right, bottom - radius);
        path.arcTo(new RectF(right - radius * 2, bottom - radius * 2, right, bottom), 0, 90, false);
        path.lineTo(left + radius, bottom);
        path.arcTo(new RectF(left, bottom - radius * 2, left + radius * 2, bottom), 90, 90, false);
        path.lineTo(left, top + radius);
        path.arcTo(new RectF(left, top, left + radius * 2, top + radius * 2), 180, 90, false);
        path.close();
        measure.setPath(path, true);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStrokeWidth(stroke + 2);
        paint.setColor(0xDD0B100D);
        canvas.drawPath(path, paint);
        paint.setStrokeWidth(stroke);
        paint.setColor(0x885B7665);
        canvas.drawPath(path, paint);
        float length = measure.getLength();
        segment.reset();
        if (unknown) {
            float start = phase * length, end = start + length * .24f;
            measure.getSegment(start, Math.min(length, end), segment, true);
            if (end > length) measure.getSegment(0, end - length, segment, true);
        } else measure.getSegment(0, length * fraction, segment, true);
        paint.setColor(0xFFB7E5CA);
        canvas.drawPath(segment, paint);
    }
}
