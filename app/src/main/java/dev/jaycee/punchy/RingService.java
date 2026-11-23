package dev.jaycee.punchy;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;

public final class RingService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable endTransient = () -> refresh(false);
    private WindowManager wm;
    private RingView ring;
    private boolean attached;
    private boolean receiverRegistered;
    private long previewUntil;
    private long completeUntil;
    String lastError;
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) cancelTransient();
            refresh(false);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = 0;
        info.flags = 0;
        setServiceInfo(info);
        wm = getSystemService(WindowManager.class);
        lastError = null;
        PunchyRuntime.overlay = this;
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(screen, filter, RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        PunchyRuntime.connectionChanged(this);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No screen events requested. */ }
    @Override public void onInterrupt() { detach(); }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); refresh(false); }
    @Override public boolean onUnbind(Intent intent) { cleanup(); return super.onUnbind(intent); }
    @Override public void onDestroy() { cleanup(); super.onDestroy(); }
    private void cleanup() {
        cancelTransient();
        detach();
        if (receiverRegistered) { unregisterReceiver(screen); receiverRegistered = false; }
        if (PunchyRuntime.overlay == this) {
            PunchyRuntime.overlay = null;
            PunchyRuntime.connectionChanged(this);
        }
    }
    void preview() {
        if (!Prefs.enabled(this) || !PunchyRuntime.ready()) return;
        previewUntil = SystemClock.uptimeMillis() + 6000;
        handler.removeCallbacks(endTransient);
        handler.postDelayed(endTransient, 6001);
        PunchyRuntime.publish(false);
    }
    void cancelTransient() {
        previewUntil = completeUntil = 0;
        handler.removeCallbacks(endTransient);
    }
    void refresh(boolean completed) {
        if (wm == null) return;
        long now = SystemClock.uptimeMillis();
        ProgressTracker.Snapshot snapshot = PunchyRuntime.tracker.snapshot();
        if (completed) {
            completeUntil = now + 900;
            handler.postDelayed(endTransient, 901);
        } else if (snapshot.active()) completeUntil = 0;
        boolean previewing = now < previewUntil;
        boolean finishing = now < completeUntil;
        PowerManager power = getSystemService(PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean visible = Prefs.enabled(this) && PunchyRuntime.ready()
            && (previewing || finishing || snapshot.active())
            && power != null && power.isInteractive()
            && keyguard != null && !keyguard.isKeyguardLocked()
            && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
        if (!visible) { detach(); return; }
        if (ring == null) ring = new RingView(this);
        WindowManager.LayoutParams params = parameters();
        try {
            if (!attached) { wm.addView(ring, params); attached = true; }
            else wm.updateViewLayout(ring, params);
            ring.progress(previewing ? 65 : finishing ? 100 : snapshot.percent(),
                !previewing && !finishing && snapshot.indeterminate(), Prefs.value(this, "stroke", 2));
            lastError = null;
        } catch (WindowManager.BadTokenException | SecurityException | IllegalArgumentException e) {
            lastError = getString(R.string.overlay_failed);
            detach();
        }
    }
    @android.annotation.SuppressLint("RtlHardcoded") // x is an absolute physical camera coordinate, independent of text direction.
    private WindowManager.LayoutParams parameters() {
        float density = getResources().getDisplayMetrics().density;
        WindowMetrics metrics = wm.getCurrentWindowMetrics();
        int screenWidth = metrics.getBounds().width();
        float centerX = screenWidth / 2f;
        float centerY = 24 * density;
        DisplayCutout cutout = metrics.getWindowInsets().getDisplayCutout();
        if (cutout != null) {
            for (Rect rect : cutout.getBoundingRects()) {
                if (rect.top < 100 * density && rect.width() < screenWidth / 2) {
                    centerX = rect.exactCenterX();
                    centerY = rect.exactCenterY();
                    break;
                }
            }
        }
        int height = Math.round(Prefs.value(this, "height", 36) * density);
        int width = Prefs.capsule(this) ? Math.round(Prefs.value(this, "width", 116) * density) : height;
        width = Math.max(height, Math.min(width, screenWidth - 4));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = Math.max(0, Math.min(screenWidth - width,
            Math.round(centerX - width / 2f + Prefs.value(this, "x", 0) * density)));
        params.y = Math.max(0, Math.round(centerY - height / 2f + Prefs.value(this, "y", 0) * density));
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setFitInsetsTypes(0);
        params.setTitle("Punchy download progress");
        return params;
    }
    private void detach() {
        if (attached && ring != null && wm != null) {
            try { wm.removeViewImmediate(ring); } catch (IllegalArgumentException ignored) { }
        }
        attached = false;
        ring = null;
    }
}
