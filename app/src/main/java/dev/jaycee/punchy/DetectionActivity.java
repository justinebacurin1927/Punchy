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
    private final Runnable changed = this::render;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.screen(this);
        Ui.button(root, "Back to Punchy", v -> finish());
        Ui.heading(root, "Download detection", 28);
        Ui.text(root, "Keep a download running, then refresh. Look for its app below to see what Punchy receives.", 16, Ui.MUTED);
        Ui.button(root, "Refresh notifications", v -> refresh());
        Ui.button(root, "Allow all progress sources", v -> {
            Prefs.filter(this, true, Prefs.apps(this));
            render();
        });
        Ui.button(root, "Notification access settings", v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        Ui.button(root, "Copy detection report", v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Punchy download detection", report.getText()));
            Toast.makeText(this, "Detection report copied.", Toast.LENGTH_SHORT).show();
        });
        Ui.text(root, "The report contains app package names and progress fields. Message text and filenames are excluded. It stays on your phone unless you copy and share it.", 14, Ui.MUTED);
        report = Ui.text(Ui.card(root), "", 16, Ui.TEXT);
        report.setTextIsSelectable(true);
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
        report.setText(PunchyRuntime.listener == null
            ? "Notification access is disconnected. Allow Punchy in notification access settings. If already allowed, turn that setting off and on, return here and refresh."
            : PunchyRuntime.listener.detectionReport());
    }
}
