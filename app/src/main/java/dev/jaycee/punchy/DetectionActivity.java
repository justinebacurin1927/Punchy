package dev.jaycee.punchy;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** On-device diagnostics, only copied when requested. No notification bodies or persistent logs. */
public final class DetectionActivity extends Activity {
    private TextView report;
    private TextView summaryTitle;
    private TextView checkedAt;
    private String technicalReport = "";
    private final Runnable changed = this::render;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.screen(this);
        Ui.backRow(root, "Back to Punchy", v -> finish());
        Ui.heading(root, "Download detection", 26);
        Ui.text(root, "Start a download, then check whether Punchy can see it.", 14, Ui.MUTED);
        Ui.primaryButton(root, "Refresh notifications", v -> refresh());
        LinearLayout actions = Ui.card(root);
        Ui.actionRow(actions, "Allow all progress sources", GlyphDrawable.APPS, v -> {
            Prefs.filter(this, true, Prefs.apps(this));
            render();
        });
        Ui.divider(actions);
        Ui.actionRow(actions, "Copy detection report", GlyphDrawable.COPY, v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Punchy download detection", technicalReport));
            Toast.makeText(this, "Detection report copied.", Toast.LENGTH_SHORT).show();
        });
        Ui.actionRow(root, "Notification access settings", GlyphDrawable.NOTIFICATION,
            v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        Ui.text(root, "Need help? Copy the report and share it when asked.", 13, Ui.MUTED);
        LinearLayout summary = Ui.card(root);
        summaryTitle = Ui.text(summary, "", 20, Ui.TEXT);
        summaryTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        summaryTitle.setAccessibilityHeading(true);
        report = Ui.text(summary, "", 16, Ui.TEXT);
        checkedAt = Ui.text(summary, "", 13, Ui.MUTED);
    }
    @Override public void onResume() {
        super.onResume();
        PunchyRuntime.observers.add(changed);
        refresh();
    }
    @Override public void onPause() { PunchyRuntime.observers.remove(changed); super.onPause(); }
    private void refresh() {
        if (PunchyRuntime.listener != null) PunchyRuntime.listener.scan();
        else NotificationListenerService.requestRebind(new ComponentName(this, DownloadListener.class));
        PunchyRuntime.publish(false);
    }
    private void render() {
        DownloadListener listener = PunchyRuntime.listener;
        technicalReport = listener == null ? "Punchy " + BuildConfig.VERSION_NAME + "\nNotification access disconnected" : listener.detectionReport();
        DownloadListener.DetectionStatus status = listener == null ? null : listener.detectionStatus();
        ProgressTracker.Snapshot downloads = PunchyRuntime.tracker.snapshot();
        if (listener == null || status.trouble()) {
            summaryTitle.setText(R.string.detection_access_title);
            report.setText(R.string.detection_access_help);
        } else if (PunchyRuntime.overlay == null) {
            summaryTitle.setText(R.string.detection_ring_title);
            report.setText(R.string.detection_ring_help);
        } else if (PunchyRuntime.overlay.lastError != null) {
            summaryTitle.setText(R.string.detection_restart_title);
            report.setText(R.string.detection_restart_help);
        } else if (!Prefs.enabled(this)) {
            summaryTitle.setText(R.string.detection_off_title);
            report.setText(R.string.detection_off_help);
        } else if (downloads.active()) {
            summaryTitle.setText(R.string.detection_found_title);
            String count = getResources().getQuantityString(R.plurals.download_count, downloads.count(), downloads.count());
            String progress = downloads.indeterminate() ? getString(R.string.detection_downloading) : getString(R.string.percent_value, downloads.percent());
            report.setText(getString(R.string.detection_found_body, count, progress));
        } else if (status.excluded()) {
            summaryTitle.setText(R.string.detection_excluded_title);
            report.setText(R.string.detection_excluded_help);
        } else {
            summaryTitle.setText(R.string.detection_ready_title);
            report.setText(R.string.detection_ready_help);
        }
        checkedAt.setText(status != null && status.checkedAt() > 0
            ? getString(R.string.detection_checked_at, android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(status.checkedAt()))) : "");
    }
}
