package dev.jaycee.punchy;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DownloadListener extends NotificationListenerService {
    private MusicMonitor musicMonitor;
    private record Entry(StatusBarNotification notification, NotificationProgress.Inspection inspection) {}
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private int received;
    private long lastReceived;
    private String scanError = "";
    private long lastChecked;
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
        entries.clear();
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
        if (Prefs.enabled(this) && musicMonitor != null) musicMonitor.start();
        scan();
        PunchyRuntime.publish(false);
    }
    /** Manual diagnostic refresh also works when Punchy's master switch is off. */
    void scan() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) { scanError = "Android did not return active notifications. Reconnect notification access."; return; }
            entries.clear();
            for (StatusBarNotification sbn : active) remember(sbn);
            scanError = "";
            lastChecked = System.currentTimeMillis();
            reconcile(false);
        } catch (RuntimeException ignored) {
            scanError = "Notification access could not be read. Turn it off and on in Android Settings.";
        }
    }
    void stopMusic() {
        if (musicMonitor != null) musicMonitor.stop();
        PunchyRuntime.music = null;
    }
    void refreshMusic() { if (musicMonitor != null) musicMonitor.update(); }
    java.util.Set<String> mediaPackages() {
        return musicMonitor == null ? java.util.Set.of() : musicMonitor.packages();
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
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        received++;
        lastReceived = System.currentTimeMillis();
        remember(sbn);
        reconcile(true);
    }
    private void remember(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null || getPackageName().equals(sbn.getPackageName())) return;
        NotificationProgress.Inspection inspection;
        try { inspection = NotificationProgress.inspect(this, sbn.getNotification()); }
        catch (RuntimeException ignored) {
            inspection = new NotificationProgress.Inspection(null, "Progress fields unavailable", "Notification data could not be read");
        }
        entries.put(sbn.getKey(), new Entry(sbn, inspection));
    }
    private Set<String> selectedKeys() {
        Set<String> childGroups = new HashSet<>(), selected = new HashSet<>();
        for (Entry entry : entries.values()) {
            StatusBarNotification sbn = entry.notification();
            if (entry.inspection().result() != null && Prefs.tracks(this, sbn.getPackageName()) && !summary(sbn))
                childGroups.add(sbn.getGroupKey());
        }
        for (Entry entry : entries.values()) {
            StatusBarNotification sbn = entry.notification();
            if (entry.inspection().result() != null && Prefs.tracks(this, sbn.getPackageName())
                    && !(summary(sbn) && childGroups.contains(sbn.getGroupKey()))) selected.add(sbn.getKey());
        }
        return selected;
    }
    private static boolean summary(StatusBarNotification sbn) {
        return (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0;
    }
    private void reconcile(boolean publish) {
        boolean completed = false;
        if (!Prefs.enabled(this)) PunchyRuntime.tracker.clear();
        else {
            Set<String> selected = selectedKeys();
            for (String key : PunchyRuntime.tracker.keys()) if (!selected.contains(key)) PunchyRuntime.tracker.remove(key);
            // Add/refresh active items before processing terminal updates, avoiding false final completion.
            for (boolean terminal : new boolean[] { false, true }) for (String key : selected) {
                Entry entry = entries.get(key);
                NotificationProgress.Result result = entry.inspection().result();
                boolean finished = !result.unknown() && result.progress() >= result.maximum();
                if (finished != terminal) continue;
                Bundle extras = entry.notification().getNotification().extras;
                CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
                completed |= PunchyRuntime.tracker.update(key, result.progress(), result.maximum(), result.unknown(),
                    title == null ? "Download" : title.toString());
            }
        }
        if (publish) PunchyRuntime.publish(completed && !PunchyRuntime.tracker.snapshot().active());
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        entries.remove(sbn.getKey());
        reconcile(true);
    }

    String detectionReport() {
        StringBuilder report = new StringBuilder("Punchy " + BuildConfig.VERSION_NAME + " · Android " + android.os.Build.VERSION.RELEASE);
        report.append("\nNotification access connected\nPunchy ").append(Prefs.enabled(this) ? "on" : "off")
            .append(" · ").append(Prefs.allApps(this) ? "All progress sources" : "Selected sources only")
            .append("\nOverlay ").append(PunchyRuntime.overlay == null ? "disconnected" : "connected")
            .append(" · Tracked downloads: ").append(PunchyRuntime.tracker.snapshot().count())
            .append("\nActive notifications received: ").append(entries.size())
            .append("\nLive updates received: ").append(received);
        if (PunchyRuntime.overlay != null && PunchyRuntime.overlay.lastError != null)
            report.append("\n").append(PunchyRuntime.overlay.lastError);
        if (lastReceived > 0) report.append(" · last at ").append(java.text.DateFormat.getTimeInstance().format(new java.util.Date(lastReceived)));
        if (!scanError.isEmpty()) report.append("\n").append(scanError);
        Set<String> selected = selectedKeys();
        int shown = 0;
        // Progress sources first, then other apps. Never include titles, messages or notification IDs.
        for (boolean recognized : new boolean[] { true, false }) for (Map.Entry<String, Entry> item : entries.entrySet()) {
            Entry entry = item.getValue();
            NotificationProgress.Inspection inspection = entry.inspection();
            if ((inspection.result() != null) != recognized || shown++ >= 40) continue;
            StatusBarNotification sbn = entry.notification();
            report.append("\n\n").append(sbn.getPackageName()).append("\n");
            if (!Prefs.tracks(this, sbn.getPackageName())) report.append("Excluded by Download apps filter");
            else if (inspection.result() == null) report.append(inspection.reason());
            else if (!selected.contains(item.getKey())) report.append("Group summary · using child progress");
            else {
                NotificationProgress.Result result = inspection.result();
                report.append(result.source()).append(" · ");
                if (result.unknown()) report.append("Total unknown");
                else report.append((int) Math.min(100, (double) result.progress() / result.maximum() * 100)).append('%');
                if (!Prefs.enabled(this)) report.append(" · Punchy is off");
            }
            report.append("\n").append(inspection.fields()).append("\ncategory=")
                .append(sbn.getNotification().category).append(", ongoing=").append(sbn.isOngoing())
                .append(", summary=").append(summary(sbn));
        }
        if (entries.isEmpty()) report.append("\n\nNo app notifications were delivered to Punchy. Keep the download running, refresh, and check notification access in Android Settings.");
        return report.toString();
    }
    record DetectionStatus(boolean trouble, boolean excluded, long checkedAt) {}
    DetectionStatus detectionStatus() {
        boolean excluded = false;
        for (Entry entry : entries.values()) {
            NotificationProgress.Result result = entry.inspection().result();
            if (result != null && (result.unknown() || result.progress() < result.maximum())
                    && !Prefs.tracks(this, entry.notification().getPackageName())) excluded = true;
        }
        return new DetectionStatus(!scanError.isEmpty(), excluded, Math.max(lastChecked, lastReceived));
    }
}
