package dev.jaycee.punchy;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.function.Consumer;

/** A small native floating window: actual backdrop blur, never a captured screen image. */
public final class DownloadPanelActivity extends Activity {
    private final Runnable changed = this::render;
    private final Consumer<Boolean> blurChanged = this::updateGlass;
    private boolean includeMusic;
    private boolean listeningForBlur;
    private static final int SECONDARY = 0xFFDDE7E1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        includeMusic = state != null ? state.getBoolean("include_music") : PunchyRuntime.musicPlaying();
        setFinishOnTouchOutside(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        if (!ValueAnimator.areAnimatorsEnabled()) getWindow().setWindowAnimations(0);
        updateGlass(false);
    }
    @Override protected void onStart() {
        super.onStart();
        try {
            updateGlass(getWindowManager().isCrossWindowBlurEnabled());
            getWindowManager().addCrossWindowBlurEnabledListener(blurChanged);
            listeningForBlur = true;
        } catch (RuntimeException unsupported) { updateGlass(false); }
        sizeWindow();
    }
    @Override protected void onStop() {
        if (listeningForBlur) {
            getWindowManager().removeCrossWindowBlurEnabledListener(blurChanged);
            listeningForBlur = false;
        }
        super.onStop();
    }
    @Override public void onSaveInstanceState(Bundle state) {
        state.putBoolean("include_music", includeMusic);
        super.onSaveInstanceState(state);
    }
    private void updateGlass(boolean blur) {
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            blur ? new int[] { 0xC12A3835, 0xBA141E1D, 0xC1142421 }
                 : new int[] { 0xF02A3532, 0xF016201F, 0xF0182823 });
        glass.setCornerRadius(Ui.dp(this, 28));
        glass.setStroke(Math.max(1, Ui.dp(this, .6f)), 0x42FFFFFF);
        getWindow().setBackgroundDrawable(glass);
        getWindow().setBackgroundBlurRadius(blur ? Math.min(96, Ui.dp(this, 32)) : 0);
    }
    private void sizeWindow() {
        int available = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        getWindow().setLayout(Math.min(available - Ui.dp(this, 24), Ui.dp(this, 420)), -2);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.y = Ui.dp(this, 10);
        params.setFitInsetsTypes(android.view.WindowInsets.Type.systemBars());
        getWindow().setAttributes(params);
    }
    @Override public void onResume() {
        super.onResume();
        PunchyRuntime.panelVisible = true;
        PunchyRuntime.observers.add(changed);
        PunchyRuntime.publish(false);
        sizeWindow();
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
        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 20));
        scroll.addView(card);
        LinearLayout header = row(card);
        TextView heading = label(header, music == null ? "Download details" : "Music & downloads", 16, Ui.TEXT, true);
        heading.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        heading.setAccessibilityHeading(true);
        icon(header, "close", "Close details", this::finish, true, false);

        if (music != null) musicSection(card, music);
        boolean sample = !snapshot.active() && PunchyRuntime.overlay != null && PunchyRuntime.overlay.previewing();
        String filename = sample ? "Sample download" : snapshot.active() ? snapshot.title() : "No active downloads";
        LinearLayout file = row(card);
        LinearLayout.LayoutParams fileSpace = (LinearLayout.LayoutParams) file.getLayoutParams();
        fileSpace.topMargin = Ui.dp(this, music == null ? 10 : 0);
        TextView title = label(file, filename, 16, Ui.TEXT, true);
        title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        title.setContentDescription(filename);
        title.setOnLongClickListener(v -> { Toast.makeText(this, filename, Toast.LENGTH_LONG).show(); return true; });
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        if (snapshot.active() || sample) {
            boolean unknown = !sample && snapshot.indeterminate();
            int percent = sample ? 65 : snapshot.percent();
            String value = sample ? "65%" : unknown ? "Total unknown" : getString(R.string.percent_value, percent);
            TextView amount = label(file, value, unknown ? 13 : 24, Ui.MINT, true);
            amount.setPadding(Ui.dp(this, 12), 0, 0, 0);
            if (sample || snapshot.count() > 1)
                label(card, sample ? "Preview" : getResources().getQuantityString(R.plurals.download_count, snapshot.count(), snapshot.count()), 12, SECONDARY, false);
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setIndeterminate(unknown); bar.setMax(100); bar.setProgress(percent);
            bar.setProgressTintList(ColorStateList.valueOf(Ui.MINT));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
            bar.setIndeterminateTintList(ColorStateList.valueOf(Ui.MINT));
            bar.setContentDescription("Download progress");
            LinearLayout.LayoutParams progress = new LinearLayout.LayoutParams(-1, Ui.dp(this, 8));
            progress.topMargin = Ui.dp(this, 12); progress.bottomMargin = Ui.dp(this, 16);
            card.addView(bar, progress);
        } else title.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
        Button open = new Button(this) {
            @Override public void setPressed(boolean pressed) { super.setPressed(pressed); press(this, pressed); }
        };
        open.setText(R.string.open_download_app); open.setAllCaps(false); open.setTextSize(15);
        open.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        open.setTextColor(0xFF10170A);
        open.setBackgroundTintList(null);
        open.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), shape(Ui.MINT, 24), null));
        open.setMinHeight(Ui.dp(this, 48));
        open.setEnabled(snapshot.active() && PunchyRuntime.listener != null);
        open.setAlpha(open.isEnabled() ? 1 : .4f);
        open.setOnClickListener(v -> openDownload(snapshot.key()));
        card.addView(open, new LinearLayout.LayoutParams(-1, -2));
        setContentView(scroll);
    }
    private void musicSection(LinearLayout card, MediaController controller) {
        MediaMetadata metadata = controller.getMetadata();
        LinearLayout info = row(card);
        LinearLayout.LayoutParams infoSpace = (LinearLayout.LayoutParams) info.getLayoutParams();
        infoSpace.topMargin = Ui.dp(this, 8);
        Bitmap artwork = metadata == null ? null : metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (artwork == null && metadata != null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (artwork == null && metadata != null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        ImageView cover = new ImageView(this);
        cover.setBackground(shape(0x332AF0A2, 12)); cover.setClipToOutline(true);
        if (artwork != null) { cover.setImageBitmap(artwork); cover.setScaleType(ImageView.ScaleType.CENTER_CROP); }
        else { cover.setImageDrawable(new Glyph("music", Ui.MINT)); cover.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14)); }
        cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams image = new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56));
        image.setMarginEnd(Ui.dp(this, 12)); info.addView(cover, image);
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        info.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        String title = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        TextView song = label(labels, title == null || title.isBlank() ? "Music" : title, 17, Ui.TEXT, true);
        song.setMaxLines(2); song.setEllipsize(TextUtils.TruncateAt.END);
        String artist = metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (artist != null && !artist.isBlank()) {
            TextView name = label(labels, artist, 13, SECONDARY, false);
            name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(0, Ui.dp(this, 5), 0, 0);
        }
        PlaybackState state = controller.getPlaybackState();
        long actions = state == null ? 0 : state.getActions();
        boolean playing = PunchyRuntime.musicPlaying();
        LinearLayout controls = row(card); controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controlsSpace = (LinearLayout.LayoutParams) controls.getLayoutParams();
        controlsSpace.topMargin = Ui.dp(this, 12);
        icon(controls, "previous", "Previous track", () -> controller.getTransportControls().skipToPrevious(),
            (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0, false);
        icon(controls, playing ? "pause" : "play", playing ? "Pause music" : "Play music", () -> {
            if (playing) controller.getTransportControls().pause(); else controller.getTransportControls().play();
        }, (actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0, true);
        icon(controls, "next", "Next track", () -> controller.getTransportControls().skipToNext(),
            (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0, false);
        View divider = new View(this); divider.setBackgroundColor(0x24FFFFFF);
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, Ui.dp(this, 1));
        line.topMargin = line.bottomMargin = Ui.dp(this, 16); card.addView(divider, line);
    }
    private LinearLayout row(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL); parent.addView(row, new LinearLayout.LayoutParams(-1, -2)); return row;
    }
    private TextView label(LinearLayout parent, String value, int size, int color, boolean medium) {
        TextView text = new TextView(this);
        text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setIncludeFontPadding(false);
        text.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        parent.addView(text, new LinearLayout.LayoutParams(-2, -2)); return text;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(Ui.dp(this, radius)); return shape;
    }
    private void icon(LinearLayout parent, String glyph, String label, Runnable action, boolean enabled, boolean primary) {
        ImageButton button = new ImageButton(this) {
            @Override public void setPressed(boolean pressed) { super.setPressed(pressed); press(this, pressed); }
        };
        button.setImageDrawable(new Glyph(glyph, primary ? 0xFF10170A : Ui.TEXT));
        button.setContentDescription(label); button.setEnabled(enabled); button.setAlpha(enabled ? 1 : .3f);
        button.setPadding(Ui.dp(this, 13), Ui.dp(this, 13), Ui.dp(this, 13), Ui.dp(this, 13));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF), shape(primary ? Ui.MINT : 0x13FFFFFF, 24), null));
        LinearLayout.LayoutParams target = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48));
        if (!glyph.equals("close")) { target.setMarginStart(Ui.dp(this, 8)); target.setMarginEnd(Ui.dp(this, 8)); }
        parent.addView(button, target);
        button.setOnClickListener(v -> action.run());
    }
    private void press(View view, boolean pressed) {
        if (ValueAnimator.areAnimatorsEnabled()) view.animate().scaleX(pressed ? .97f : 1).scaleY(pressed ? .97f : 1).setDuration(100).start();
        else { view.setScaleX(1); view.setScaleY(1); }
    }
    /** Consistent vector control weight, without the platform's dated bitmap media buttons. */
    private static final class Glyph extends Drawable {
        private final String name;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        Glyph(String name, int color) { this.name = name; paint.setColor(color); paint.setStrokeWidth(2); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); }
        @Override public void draw(Canvas canvas) {
            canvas.save(); canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            paint.setStyle(Paint.Style.STROKE);
            path.reset();
            switch (name) {
                case "close" -> { path.moveTo(6, 6); path.lineTo(18, 18); path.moveTo(18, 6); path.lineTo(6, 18); }
                case "pause" -> { canvas.drawLine(9, 6, 9, 18, paint); canvas.drawLine(15, 6, 15, 18, paint); }
                case "play" -> { path.moveTo(8, 5); path.lineTo(19, 12); path.lineTo(8, 19); path.close(); }
                case "next" -> { path.moveTo(5, 5); path.lineTo(15, 12); path.lineTo(5, 19); path.close(); canvas.drawLine(19, 5, 19, 19, paint); }
                case "previous" -> { path.moveTo(19, 5); path.lineTo(9, 12); path.lineTo(19, 19); path.close(); canvas.drawLine(5, 5, 5, 19, paint); }
                case "music" -> { path.moveTo(9, 17); path.lineTo(9, 6); path.lineTo(19, 4); path.lineTo(19, 15); canvas.drawOval(3, 15, 9, 20, paint); canvas.drawOval(13, 13, 19, 18, paint); }
                default -> { }
            }
            canvas.drawPath(path, paint); canvas.restore();
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
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
