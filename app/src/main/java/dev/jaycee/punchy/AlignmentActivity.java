package dev.jaycee.punchy;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class AlignmentActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
    }
    private void buildScreen() {
        LinearLayout root = Ui.screen(this);
        Ui.backRow(root, "Back to Punchy", v -> finish());
        Ui.heading(root, "Shape & alignment", 26);
        Ui.text(root, "A precise fit for your camera. Your normal and music shapes keep their own sizes.", 14, Ui.MUTED);
        Ui.primaryButton(root, "Preview on phone · 6 seconds", v -> preview());
        Ui.actionRow(root, "Stop preview", GlyphDrawable.SCAN, v -> {
            if (PunchyRuntime.overlay != null) { PunchyRuntime.overlay.cancelTransient(); PunchyRuntime.publish(false); }
        });
        LinearLayout shape = Ui.card(root);
        Ui.heading(shape, "Shape", 18);
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton circle = choice("Circle"), capsule = choice("Capsule");
        circle.setId(android.view.View.generateViewId());
        capsule.setId(android.view.View.generateViewId());
        RadioGroup.LayoutParams left = new RadioGroup.LayoutParams(0, -2, 1);
        left.setMarginEnd(Ui.dp(this, 5));
        RadioGroup.LayoutParams right = new RadioGroup.LayoutParams(0, -2, 1);
        right.setMarginStart(Ui.dp(this, 5));
        group.addView(circle, left); group.addView(capsule, right);
        group.check(Prefs.capsule(this) ? capsule.getId() : circle.getId());
        shape.addView(group);
        group.setOnCheckedChangeListener((g, checked) -> {
            Prefs.shape(this, checked == capsule.getId());
            previewQuietly();
        });
        LinearLayout size = Ui.card(root);
        Ui.heading(size, "Size", 18);
        Ui.text(size, "Height sets the circle diameter. Capsule uses both dimensions.", 13, Ui.MUTED);
        slider(size, "Capsule width", "width", 40, 240, 116);
        slider(size, "Height / circle size", "height", 18, 90, 36);
        slider(size, "Border thickness", "stroke", 1, 8, 2);
        LinearLayout position = Ui.card(root);
        Ui.heading(position, "Position", 18);
        Ui.text(position, "Offsets from your camera in display units. Negative moves left or up.", 13, Ui.MUTED);
        slider(position, "Left / right", "x", -120, 120, 0);
        slider(position, "Up / down", "y", -80, 120, 0);
        LinearLayout music = Ui.card(root);
        Ui.heading(music, "Music capsule fit", 18);
        Ui.text(music, "Independent size during playback. Position and thickness stay shared; pausing returns to your normal shape.", 13, Ui.MUTED);
        slider(music, "Music capsule width", "music_width", 60, 240, 108);
        slider(music, "Music capsule height", "music_height", 24, 90, 40);
        Ui.actionRow(root, "Reset alignment", GlyphDrawable.REFRESH, v -> {
            Prefs.resetGeometry(this);
            buildScreen();
            previewQuietly();
        });
        Ui.text(root, "Tap the ring for download details and music controls. It hides in landscape, on the lock screen and while your screen is off.", 13, Ui.MUTED);
    }
    private RadioButton choice(String label) {
        RadioButton button = new RadioButton(this);
        button.setText(label);
        int[][] states = { new int[] { android.R.attr.state_checked }, new int[] {} };
        button.setTextColor(new ColorStateList(states, new int[] { 0xFF152008, Ui.TEXT }));
        button.setTextSize(15); button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setButtonDrawable(null); button.setGravity(Gravity.CENTER); button.setIncludeFontPadding(false);
        button.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] { android.R.attr.state_checked }, Ui.rounded(this, 14, Ui.MINT, 0));
        background.addState(new int[] {}, Ui.rounded(this, 14, 0xFF24312F, 0x18FFFFFF));
        button.setBackground(background);
        Ui.pressFeedback(button);
        button.setMinHeight(Ui.dp(this, 52));
        return button;
    }
    private void slider(LinearLayout parent, String title, String key, int min, int max, int fallback) {
        TextView label = Ui.text(parent, title + ": " + Prefs.value(this, key, fallback), 14, Ui.TEXT);
        label.setPadding(0, Ui.dp(this, 12), 0, 0);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, Prefs.value(this, key, fallback) - min)));
        seek.setMinHeight(Ui.dp(this, 48));
        seek.setThumbTintList(ColorStateList.valueOf(Ui.MINT));
        seek.setProgressTintList(ColorStateList.valueOf(Ui.MINT));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF36423E));
        seek.setContentDescription(title);
        seek.setStateDescription(String.valueOf(seek.getProgress() + min));
        parent.addView(seek, new LinearLayout.LayoutParams(-1, -2));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = progress + min;
                label.setText(getString(R.string.alignment_value, title, value));
                bar.setStateDescription(String.valueOf(value));
                Prefs.number(AlignmentActivity.this, key, value);
                previewQuietly();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { previewQuietly(); }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
    }
    private void previewQuietly() {
        if (PunchyRuntime.overlay != null && Prefs.enabled(this)) PunchyRuntime.overlay.preview();
    }
    private void preview() {
        if (PunchyRuntime.ready() && Prefs.enabled(this)) PunchyRuntime.overlay.preview();
        else Toast.makeText(this, "Complete setup and enable Punchy first.", Toast.LENGTH_LONG).show();
    }
}
