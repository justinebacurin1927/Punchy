package dev.jaycee.punchy.testsource;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.widget.TextView;
import android.widget.RemoteViews;

/** Separate emulator-only notification producer. It is not bundled in Punchy's APK. */
public final class TestActivity extends Activity {
    private static MediaSession music;
    private static int track = 1;
    @Override public void onCreate(Bundle state) { super.onCreate(state); publish(getIntent()); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); publish(intent); }
    private void publish(Intent intent) {
        if (intent.getBooleanExtra("open_only", false)) {
            TextView text = new TextView(this); text.setText("Download app opened"); text.setTextSize(24);
            setContentView(text); return;
        }
        String command = intent.getStringExtra("music");
        if (command != null) {
            if (command.equals("stop")) {
                if (music != null) { music.setActive(false); music.release(); music = null; }
            } else {
                if (music == null) {
                    music = new MediaSession(getApplicationContext(), "Punchy synthetic music");
                    music.setCallback(new MediaSession.Callback() {
                        @Override public void onPlay() { state(PlaybackState.STATE_PLAYING); }
                        @Override public void onPause() { state(PlaybackState.STATE_PAUSED); }
                        @Override public void onSkipToNext() { track++; metadata(); }
                        @Override public void onSkipToPrevious() { track = Math.max(1, track - 1); metadata(); }
                    });
                    music.setActive(true); metadata();
                }
                state(command.equals("pause") ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING);
            }
            finish(); return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("downloads", "Synthetic downloads", NotificationManager.IMPORTANCE_LOW));
        int id = intent.getIntExtra("id", 1);
        if (intent.getBooleanExtra("cancel", false)) manager.cancel(id);
        else {
            String style = intent.getStringExtra("style");
            int progress = intent.getIntExtra("progress", 0);
            Notification.Builder builder = new Notification.Builder(this, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Test download " + id)
            .setContentText("Synthetic progress for Punchy verification")
            .setContentIntent(PendingIntent.getActivity(this, 0,
                new Intent(this, TestActivity.class).putExtra("open_only", true),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(!"ordinary".equals(style)).setOnlyAlertOnce(true);
            if (style == null || style.equals("group"))
                builder.setProgress(100, progress, intent.getBooleanExtra("unknown", false));
            else if (style.equals("custom")) {
                RemoteViews views = new RemoteViews(getPackageName(), dev.jaycee.punchy.testsource.R.layout.download_progress);
                views.setTextViewText(dev.jaycee.punchy.testsource.R.id.filename, "Synthetic custom download");
                views.setProgressBar(dev.jaycee.punchy.testsource.R.id.progress, 100, progress, false);
                builder.setCustomContentView(views);
            } else builder.setContentText(progress + "% of 275 MB");
            if ("media".equals(style)) builder.setCategory(Notification.CATEGORY_TRANSPORT);
            if ("battery".equals(style)) builder.setContentText("Battery " + progress + "%");
            if ("group".equals(style)) builder.setGroup("synthetic_downloads")
                .setGroupSummary(intent.getBooleanExtra("summary", false));
            Notification notification = builder.build();
            if ("long".equals(style)) {
                notification.extras.putLong(Notification.EXTRA_PROGRESS, progress * 100000000L);
                notification.extras.putLong(Notification.EXTRA_PROGRESS_MAX, 10000000000L);
            }
            if ("malformed".equals(style)) notification.extras.putString(Notification.EXTRA_PROGRESS, "invalid");
            manager.notify(id, notification);
        }
        finish();
    }
    private static void metadata() {
        music.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Synthetic music " + track)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Punchy test artist").build());
    }
    private static void state(int state) {
        music.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(state, 0, 1f).build());
    }
}
