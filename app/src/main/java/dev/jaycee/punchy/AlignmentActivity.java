package dev.jaycee.punchy;

import android.app.Activity;
import android.os.Bundle;
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
        Ui.button(root, "Back to Punchy", v -> finish());
        Ui.heading(root, "Shape & alignment", 28);
        Ui.text(root, "Start with automatic camera alignment. Music automatically uses a separate capsule fit.", 16, Ui.MUTED);
        Ui.button(root, "Preview on phone · 6 seconds", v -> preview());
        Ui.button(root, "Stop preview", v -> {
            if (PunchyRuntime.overlay != null) { PunchyRuntime.overlay.cancelTransient(); PunchyRuntime.publish(false); }
        });
        LinearLayout shape = Ui.card(root);
        Ui.heading(shape, "Shape", 20);
        RadioGroup group = new RadioGroup(this);
        RadioButton circle = choice("Circle"), capsule = choice("Capsule");
        circle.setId(android.view.View.generateViewId());
        capsule.setId(android.view.View.generateViewId());
        group.addView(circle); group.addView(capsule);
        group.check(Prefs.capsule(this) ? capsule.getId() : circle.getId());
        shape.addView(group);
        group.setOnCheckedChangeListener((g, checked) -> {
            Prefs.shape(this, checked == capsule.getId());
            previewQuietly();
        });
        LinearLayout size = Ui.card(root);
        Ui.heading(size, "Size", 20);
        Ui.text(size, "Circle uses Height for its diameter. Capsule uses both Width and Height.", 14, Ui.MUTED);
        slider(size, "Capsule width", "width", 40, 240, 116);
        slider(size, "Height / circle size", "height", 18, 90, 36);
        slider(size, "Border thickness", "stroke", 1, 8, 2);
        LinearLayout position = Ui.card(root);
        Ui.heading(position, "Position", 20);
        Ui.text(position, "Offsets from the detected camera. Negative moves left or up; positive moves right or down. Values use Android display units.", 14, Ui.MUTED);
        slider(position, "Left / right", "x", -120, 120, 0);
        slider(position, "Up / down", "y", -80, 120, 0);
        LinearLayout music = Ui.card(root);
        Ui.heading(music, "Music capsule fit", 20);
        Ui.text(music, "When music is active or paused, Punchy uses these dimensions and leaves the native artwork visible. The position and border thickness above apply to both shapes.", 14, Ui.MUTED);
        slider(music, "Music capsule width", "music_width", 60, 240, 108);
        slider(music, "Music capsule height", "music_height", 24, 90, 40);
        Ui.button(root, "Reset alignment", v -> {
            Prefs.resetGeometry(this);
            buildScreen();
            previewQuietly();
        });
        Ui.text(root, "Tap the ring to open download details. With music active, the card also includes music controls. The indicator hides in landscape, while locked, and when the screen is off.", 14, Ui.MUTED);
    }
    private RadioButton choice(String label) {
        RadioButton button = new RadioButton(this);
        button.setText(label);
        button.setTextColor(Ui.TEXT);
        button.setTextSize(18);
        button.setMinHeight(Ui.dp(this, 52));
        return button;
    }
    private void slider(LinearLayout parent, String title, String key, int min, int max, int fallback) {
        TextView label = Ui.text(parent, title + ": " + Prefs.value(this, key, fallback), 16, Ui.TEXT);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, Prefs.value(this, key, fallback) - min)));
        seek.setMinHeight(Ui.dp(this, 48));
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
