package dev.jaycee.punchy;

import android.app.Activity;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

final class Ui {
    static final int BACKGROUND = 0xFF090D0E;
    static final int SURFACE = 0xFF151C1D;
    static final int TEXT = 0xFFF4F8F5;
    static final int MUTED = 0xFF9CAFA8;
    static final int MINT = 0xFFD3FF5F;
    private Ui() {}
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static LinearLayout screen(Activity activity) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[] { BACKGROUND, 0xFF101919, BACKGROUND }));
        scroll.setClipToPadding(false);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 22), dp(activity, 22), dp(activity, 22), dp(activity, 32));
        scroll.addView(root);
        activity.setContentView(scroll);
        return root;
    }
    static LinearLayout card(LinearLayout parent) {
        Context c = parent.getContext();
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 14));
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[] { 0xFF1B2525, SURFACE, 0xFF111718 });
        background.setCornerRadius(dp(c, 26));
        background.setStroke(dp(c, 1), 0x20FFFFFF);
        card.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(c, 18);
        parent.addView(card, params);
        return card;
    }
    static TextView text(LinearLayout parent, String content, int sp, int color) {
        TextView text = new TextView(parent.getContext());
        text.setText(content);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setFontFeatureSettings("kern");
        text.setIncludeFontPadding(false);
        text.setLineSpacing(dp(parent.getContext(), 2), 1f);
        text.setPadding(0, dp(parent.getContext(), 3), 0, dp(parent.getContext(), 7));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }
    static void heading(LinearLayout parent, String content, int size) {
        TextView text = text(parent, content, size, TEXT);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setAccessibilityHeading(true);
    }
    static Button button(LinearLayout parent, String label, View.OnClickListener action) {
        return button(parent, label, action, false);
    }
    static Button primaryButton(LinearLayout parent, String label, View.OnClickListener action) {
        return button(parent, label, action, true);
    }
    private static Button button(LinearLayout parent, String label, View.OnClickListener action, boolean primary) {
        Button button = new Button(parent.getContext());
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setSingleLine(false); button.setMaxLines(3);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setIncludeFontPadding(false);
        button.setBackgroundTintList(null);
        int[][] states = { new int[] { android.R.attr.state_enabled }, new int[] {} };
        button.setTextColor(new ColorStateList(states, new int[] { primary ? 0xFF152008 : TEXT, 0xFF6C7B76 }));
        StateListDrawable fills = new StateListDrawable();
        fills.addState(new int[] { -android.R.attr.state_enabled }, rounded(parent.getContext(), 18, 0xFF202A28, 0x12FFFFFF));
        fills.addState(new int[] {}, rounded(parent.getContext(), 18, primary ? MINT : 0xFF24312F, primary ? 0 : 0x16FFFFFF));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(primary ? 0x22000000 : 0x20FFFFFF), fills, null));
        button.setMinHeight(dp(parent.getContext(), 52));
        button.setMinimumHeight(dp(parent.getContext(), 52));
        button.setPadding(dp(parent.getContext(), 16), dp(parent.getContext(), 12), dp(parent.getContext(), 16), dp(parent.getContext(), 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(parent.getContext(), 6);
        parent.addView(button, params);
        button.setOnClickListener(action);
        pressFeedback(button);
        return button;
    }
    static GradientDrawable rounded(Context c, int radius, int fill, int border) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(fill); shape.setCornerRadius(dp(c, radius));
        if (border != 0) shape.setStroke(dp(c, 1), border);
        return shape;
    }
    static Button actionRow(LinearLayout parent, String label, int icon, View.OnClickListener action) {
        Context c = parent.getContext();
        Button button = button(parent, label, action, false);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextSize(15);
        button.setMinHeight(dp(c, 56)); button.setMinimumHeight(dp(c, 56));
        button.setPadding(dp(c, 2), dp(c, 11), dp(c, 2), dp(c, 11));
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(new GlyphDrawable(icon, MUTED, dp(c, 21)), null,
            new GlyphDrawable(GlyphDrawable.CHEVRON, MUTED, dp(c, 14)), null);
        button.setCompoundDrawablePadding(dp(c, 12));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18FFFFFF),
            rounded(c, 12, 0x00000000, 0), null));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams(); params.topMargin = 0;
        return button;
    }
    static Button backRow(LinearLayout parent, String label, View.OnClickListener action) {
        Button button = actionRow(parent, label, GlyphDrawable.BACK, action);
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(new GlyphDrawable(GlyphDrawable.BACK, MUTED, dp(parent.getContext(), 20)), null, null, null);
        button.setTextColor(MUTED);
        return button;
    }
    static Switch toggle(LinearLayout parent, String label) {
        Context c = parent.getContext(); Switch toggle = new Switch(c);
        toggle.setText(label); toggle.setTextColor(TEXT); toggle.setTextSize(16);
        toggle.setSingleLine(false); toggle.setMaxLines(3);
        toggle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); toggle.setIncludeFontPadding(false);
        toggle.setMinHeight(dp(c, 56)); toggle.setSwitchPadding(dp(c, 12)); toggle.setSwitchMinWidth(dp(c, 48));
        toggle.setPadding(0, dp(c, 8), 0, dp(c, 8));
        int[][] states = { new int[] { android.R.attr.state_checked }, new int[] {} };
        toggle.setTrackTintList(new ColorStateList(states, new int[] { MINT, 0xFF3A4844 }));
        toggle.setThumbTintList(new ColorStateList(states, new int[] { MINT, 0xFFAAB8B1 }));
        parent.addView(toggle, new LinearLayout.LayoutParams(-1, -2)); return toggle;
    }
    static void divider(LinearLayout parent) {
        View line = new View(parent.getContext()); line.setBackgroundColor(0x12FFFFFF);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(parent.getContext(), 1)));
    }
    static void disclosure(LinearLayout parent, View details, boolean show) {
        if (ValueAnimator.areAnimatorsEnabled()) TransitionManager.beginDelayedTransition(parent, new AutoTransition().setDuration(170));
        details.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    static void pressFeedback(View view) {
        if (!ValueAnimator.areAnimatorsEnabled()) { view.setStateListAnimator(null); return; }
        StateListAnimator states = new StateListAnimator();
        AnimatorSet down = new AnimatorSet(); down.playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, .97f), ObjectAnimator.ofFloat(view, View.SCALE_Y, .97f)); down.setDuration(100);
        AnimatorSet up = new AnimatorSet(); up.playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, 1f), ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f)); up.setDuration(140);
        states.addState(new int[] { android.R.attr.state_pressed }, down); states.addState(new int[] {}, up); view.setStateListAnimator(states);
    }
}
