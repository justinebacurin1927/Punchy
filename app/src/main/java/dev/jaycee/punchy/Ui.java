package dev.jaycee.punchy;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int BACKGROUND = 0xFF111512;
    static final int SURFACE = 0xFF1E2721;
    static final int TEXT = 0xFFE7F1E9;
    static final int MUTED = 0xFFB4C3B8;
    static final int MINT = 0xFFB7E5CA;
    private Ui() {}
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static LinearLayout screen(Activity activity) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 22), dp(activity, 28), dp(activity, 22), dp(activity, 36));
        scroll.addView(root);
        activity.setContentView(scroll);
        return root;
    }
    static LinearLayout card(LinearLayout parent) {
        Context c = parent.getContext();
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(SURFACE);
        background.setCornerRadius(dp(c, 20));
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
        text.setPadding(0, dp(parent.getContext(), 4), 0, dp(parent.getContext(), 8));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }
    static void heading(LinearLayout parent, String content, int size) {
        TextView text = text(parent, content, size, TEXT);
        text.setTypeface(null, Typeface.BOLD);
        text.setAccessibilityHeading(true);
    }
    static Button button(LinearLayout parent, String label, View.OnClickListener action) {
        Button button = new Button(parent.getContext());
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        int[][] states = { new int[] { android.R.attr.state_enabled }, new int[] {} };
        button.setTextColor(new ColorStateList(states, new int[] { MINT, 0xFF83998B }));
        button.setBackgroundTintList(new ColorStateList(states, new int[] { 0xFF303F35, 0xFF253129 }));
        button.setMinHeight(dp(parent.getContext(), 52));
        button.setPadding(dp(parent.getContext(), 12), dp(parent.getContext(), 8), dp(parent.getContext(), 12), dp(parent.getContext(), 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(parent.getContext(), 6);
        parent.addView(button, params);
        button.setOnClickListener(action);
        return button;
    }
}
