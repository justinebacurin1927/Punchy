package dev.jaycee.punchy;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class DownloadListener extends NotificationListenerService {
    private MusicMonitor musicMonitor;
    @Override public void onListenerConnected() {
        PunchyRuntime.listener = this;
        if (musicMonitor != null) musicMonitor.stop();
        musicMonitor = new MusicMonitor(this);
        rescan();
        PunchyRuntime.connectionChanged(this);
    }
    @Override public void onListenerDisconnected() {
        disconnected();
        requestRebind(new ComponentName(this, DownloadListener.class));
    }
    @Override public void onDestroy() {
        disconnected();
        super.onDestroy();
    }
    private void disconnected() {
        if (PunchyRuntime.listener == this) {
            PunchyRuntime.listener = null;
            PunchyRuntime.tracker.clear();
            stopMusic();
            if (PunchyRuntime.overlay != null) PunchyRuntime.overlay.cancelTransient();
            PunchyRuntime.connectionChanged(this);
        }
    }
    void rescan() {
        PunchyRuntime.tracker.clear();
        if (Prefs.enabled(this)) {
            if (musicMonitor != null) musicMonitor.start();
            try {
                StatusBarNotification[] active = getActiveNotifications();
                if (active != null) for (StatusBarNotification sbn : active) accept(sbn, false);
            } catch (SecurityException ignored) {
                // Access was revoked while reconnecting. The setup screen reflects the disconnection.
            }
        }
        PunchyRuntime.publish(false);
    }
    void stopMusic() {
        if (musicMonitor != null) musicMonitor.stop();
        PunchyRuntime.music = null;
    }
    android.app.PendingIntent contentIntent(String key) {
        try {
            StatusBarNotification[] notifications = getActiveNotifications(new String[] { key });
            if (notifications != null && notifications.length > 0) return notifications[0].getNotification().contentIntent;
        } catch (SecurityException ignored) { }
        return null;
    }
    String sourcePackage(String key) {
        try {
            StatusBarNotification[] notifications = getActiveNotifications(new String[] { key });
            if (notifications != null && notifications.length > 0) return notifications[0].getPackageName();
        } catch (SecurityException ignored) { }
        return null;
    }
    @Override public void onNotificationPosted(StatusBarNotification sbn) { accept(sbn, true); }
    private void accept(StatusBarNotification sbn, boolean publish) {
        if (sbn == null || !Prefs.enabled(this)) return;
        if (getPackageName().equals(sbn.getPackageName()) || !Prefs.tracks(this, sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Bundle extras = n.extras;
        if (extras == null) return;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        boolean completed = PunchyRuntime.tracker.update(sbn.getKey(),
            extras.getInt(Notification.EXTRA_PROGRESS, -1),
            extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
            title == null ? "Download" : title.toString());
        if (publish) PunchyRuntime.publish(completed);
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        PunchyRuntime.tracker.remove(sbn.getKey());
        PunchyRuntime.publish(false);
    }
}
