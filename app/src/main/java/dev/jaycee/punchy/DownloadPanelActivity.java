package dev.jaycee.punchy;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** A translucent Activity supplies native Back/outside dismissal and foreground app actions. */
public final class DownloadPanelActivity extends Activity {
    private final Runnable changed = this::render;
    private boolean includeMusic;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        includeMusic = PunchyRuntime.musicPlaying();
        getWindow().setDecorFitsSystemWindows(false);
    }
    @Override public void onResume() {
        super.onResume();
        PunchyRuntime.panelVisible = true;
        PunchyRuntime.observers.add(changed);
        PunchyRuntime.publish(false);
        hideStatusBar();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) hideStatusBar();
    }
    private void hideStatusBar() {
        var controller = getWindow().getInsetsController();
        if (controller != null) controller.hide(WindowInsets.Type.statusBars());
    }
    @Override public void onPause() {
        PunchyRuntime.observers.remove(changed);
        PunchyRuntime.panelVisible = false;
        PunchyRuntime.publish(false);
        super.onPause();
    }
    private void render() {
        if (!Prefs.enabled(this)) { finish(); return; }
        ProgressTracker.Snapshot snapshot = PunchyRuntime.tracker.snapshot();
        includeMusic |= PunchyRuntime.musicPlaying();
        MediaController music = includeMusic ? PunchyRuntime.music : null;
        FrameLayout backdrop = new FrameLayout(this);
        backdrop.setOnClickListener(v -> finish());
        ScrollView scroll = new ScrollView(this);
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        position.setMargins(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 24));
        backdrop.addView(scroll, position);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        int cutoutBottom = 0;
        var cutout = getWindowManager().getCurrentWindowMetrics().getWindowInsets().getDisplayCutout();
        if (cutout != null) for (var rect : cutout.getBoundingRects()) {
            if (rect.top < Ui.dp(this, 100)) cutoutBottom = Math.max(cutoutBottom, rect.bottom);
        }
        card.setPadding(Ui.dp(this, 16), Math.max(Ui.dp(this, 12), cutoutBottom), Ui.dp(this, 16), Ui.dp(this, 16));
        GradientDrawable black = new GradientDrawable();
        black.setColor(Color.BLACK); black.setCornerRadius(Ui.dp(this, 24));
        card.setBackground(black);
        scroll.addView(card);
        LinearLayout header = row(card);
        TextView heading = Ui.text(header, music == null ? "Download details" : "Music & downloads", 20, Ui.TEXT);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        heading.setAccessibilityHeading(true);
        icon(header, android.R.drawable.ic_menu_close_clear_cancel, "Close details", this::finish, true);

        if (music != null) musicSection(card, music);
        boolean sample = !snapshot.active() && PunchyRuntime.overlay != null && PunchyRuntime.overlay.previewing();
        Ui.text(card, sample ? "Sample download" : snapshot.active() ? snapshot.title() : "No active downloads", 18, Ui.TEXT);
        if (snapshot.active() || sample) {
            boolean unknown = !sample && snapshot.indeterminate();
            int percent = sample ? 65 : snapshot.percent();
            String label = sample ? "Preview · 65%" : unknown ? "Total unknown"
                : getString(R.string.percent_value, percent);
            if (snapshot.count() > 1) label = getResources().getQuantityString(R.plurals.download_count, snapshot.count(), snapshot.count()) + " · " + label;
            Ui.text(card, label, 16, Ui.MUTED);
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setIndeterminate(unknown); bar.setMax(100); bar.setProgress(percent);
            bar.setProgressTintList(ColorStateList.valueOf(Ui.MINT));
            bar.setIndeterminateTintList(ColorStateList.valueOf(Ui.MINT));
            bar.setContentDescription("Download progress");
            card.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 12)));
        }
        Button open = Ui.button(card, "Open app", v -> openDownload(snapshot.key()));
        open.setEnabled(snapshot.active() && PunchyRuntime.listener != null);
        setContentView(backdrop);
    }
    private void musicSection(LinearLayout card, MediaController controller) {
        MediaMetadata metadata = controller.getMetadata();
        LinearLayout info = row(card);
        Bitmap artwork = metadata == null ? null : metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (artwork == null && metadata != null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (artwork == null && metadata != null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (artwork != null) {
            ImageView cover = new ImageView(this);
            cover.setImageBitmap(artwork); cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams image = new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56));
            image.setMarginEnd(Ui.dp(this, 12)); info.addView(cover, image);
        }
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        info.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        String title = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        TextView song = Ui.text(labels, title == null || title.isBlank() ? "Music" : title, 18, Ui.TEXT);
        song.setMaxLines(2); song.setEllipsize(TextUtils.TruncateAt.END);
        String artist = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (artist != null && !artist.isBlank()) Ui.text(labels, artist, 14, Ui.MUTED);
        PlaybackState state = controller.getPlaybackState();
        long actions = state == null ? 0 : state.getActions();
        boolean playing = state != null && (state.getState() == PlaybackState.STATE_PLAYING
            || state.getState() == PlaybackState.STATE_BUFFERING || state.getState() == PlaybackState.STATE_CONNECTING);
        LinearLayout controls = row(card); controls.setGravity(Gravity.CENTER);
        icon(controls, android.R.drawable.ic_media_previous, "Previous track", () -> controller.getTransportControls().skipToPrevious(),
            (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        icon(controls, playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
            playing ? "Pause music" : "Play music", () -> {
                if (playing) controller.getTransportControls().pause(); else controller.getTransportControls().play();
            }, (actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0);
        icon(controls, android.R.drawable.ic_media_next, "Next track", () -> controller.getTransportControls().skipToNext(),
            (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
        android.view.View divider = new android.view.View(this); divider.setBackgroundColor(0xFF303A33);
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, Ui.dp(this, 1));
        line.topMargin = line.bottomMargin = Ui.dp(this, 12); card.addView(divider, line);
    }
    private LinearLayout row(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL); parent.addView(row, new LinearLayout.LayoutParams(-1, -2)); return row;
    }
    private void icon(LinearLayout parent, int drawable, String label, Runnable action, boolean enabled) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawable); button.setImageTintList(ColorStateList.valueOf(Ui.TEXT));
        button.setContentDescription(label); button.setEnabled(enabled); button.setAlpha(enabled ? 1f : .35f);
        button.setBackgroundResource(android.R.drawable.list_selector_background);
        parent.addView(button, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 52)));
        button.setOnClickListener(v -> action.run());
    }
    private void openDownload(String key) {
        if (PunchyRuntime.listener == null || key.isEmpty()) return;
        PendingIntent target = PunchyRuntime.listener.contentIntent(key);
        try {
            if (target != null) {
                // Android 14 requires sender opt-in for this explicit foreground user action.
                var options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                target.send(this, 0, null, null, null, null, options.toBundle());
                finish(); return;
            }
            String pkg = PunchyRuntime.listener.sourcePackage(key);
            Intent intent = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null && "com.android.providers.downloads".equals(pkg)) intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            if (intent != null) { startActivity(intent); finish(); return; }
        } catch (PendingIntent.CanceledException | ActivityNotFoundException | SecurityException ignored) { }
        Toast.makeText(this, "The download app is no longer available. Open it from your home screen.", Toast.LENGTH_LONG).show();
    }
}
