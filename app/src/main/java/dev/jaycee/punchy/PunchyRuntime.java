package dev.jaycee.punchy;

import android.content.ComponentName;
import android.content.Context;
import android.service.quicksettings.TileService;
import java.util.LinkedHashSet;
import java.util.Set;

/** All entry points are delivered on Android's main thread, in one process. */
final class PunchyRuntime {
    static final ProgressTracker tracker = new ProgressTracker();
    static final Set<Runnable> observers = new LinkedHashSet<>();
    static DownloadListener listener;
    static RingService overlay;

    static boolean ready() { return listener != null && overlay != null; }
    static boolean setEnabled(Context c, boolean enabled) {
        if (enabled && !ready()) return false;
        Prefs.get(c).edit().putBoolean("enabled", enabled).apply();
        if (!enabled) {
            tracker.clear();
            if (overlay != null) overlay.cancelTransient();
        } else if (listener != null) {
            listener.rescan();
        }
        publish(false);
        refreshTile(c);
        return true;
    }
    static void publish(boolean completed) {
        if (overlay != null) overlay.refresh(completed);
        for (Runnable observer : Set.copyOf(observers)) observer.run();
    }
    static void connectionChanged(Context c) {
        publish(false);
        refreshTile(c);
    }
    static void refreshTile(Context c) {
        TileService.requestListeningState(c, new ComponentName(c, PunchyTile.class));
    }
}
