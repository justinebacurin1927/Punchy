package dev.jaycee.punchy.testsource;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

/** Separate emulator-only notification producer. It is not bundled in Punchy's APK. */
public final class TestActivity extends Activity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); publish(getIntent()); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); publish(intent); }
    private void publish(Intent intent) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("downloads", "Synthetic downloads", NotificationManager.IMPORTANCE_LOW));
        int id = intent.getIntExtra("id", 1);
        if (intent.getBooleanExtra("cancel", false)) manager.cancel(id);
        else manager.notify(id, new Notification.Builder(this, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Test download " + id)
            .setContentText("Synthetic progress for Punchy verification")
            .setProgress(100, intent.getIntExtra("progress", 0), intent.getBooleanExtra("unknown", false))
            .setOngoing(true).setOnlyAlertOnce(true).build());
        finish();
    }
}
