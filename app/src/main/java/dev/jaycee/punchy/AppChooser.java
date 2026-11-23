package dev.jaycee.punchy;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Shared app picker with a draft selection: only Save writes preferences. */
final class AppChooser extends Dialog {
    private final Activity activity;
    private final Map<String, String> labels;
    private final Set<String> draft;
    private final List<String> ordered;
    private final List<String> visible = new ArrayList<>();
    private final Set<String> duplicateLabels = new HashSet<>();
    private final Map<String, Drawable> icons = new HashMap<>();
    private final TextView count;
    private final EditText search;
    private final PickerAdapter adapter = new PickerAdapter();

    static AppChooser show(Activity activity, String title, Map<String, String> labels, Set<String> selected,
            Consumer<Set<String>> save) {
        AppChooser chooser = new AppChooser(activity, title, labels, selected, save);
        chooser.show();return chooser;
    }
    private AppChooser(Activity activity, String title, Map<String, String> labels, Set<String> selected,
            Consumer<Set<String>> save) {
        super(activity, R.style.Theme_Punchy_Popup);
        this.activity = activity;
        this.labels = new HashMap<>(labels);
        draft = new HashSet<>(selected);
        ordered = new ArrayList<>(labels.keySet());
        ordered.sort(Comparator.<String, Boolean>comparing(pkg -> !draft.contains(pkg))
            .thenComparing(labels::get, String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
        Set<String> names = new HashSet<>();
        for (String label : labels.values()) if (!names.add(label)) duplicateLabels.add(label);
        setCanceledOnTouchOutside(true);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(activity, 20), Ui.dp(activity, 20), Ui.dp(activity, 20), Ui.dp(activity, 16));
        Ui.heading(root, title, 22);
        Ui.text(root, "Choose apps, then tap Save.", 14, Ui.MUTED);
        search = new EditText(activity);
        search.setSingleLine(true); search.setTextSize(16); search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED); search.setHint("Search apps");
        search.setContentDescription("Search apps");
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        search.setOnEditorActionListener((text, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            activity.getSystemService(android.view.inputmethod.InputMethodManager.class)
                .hideSoftInputFromWindow(search.getWindowToken(), 0);
            search.clearFocus();return true;
        });
        search.setBackgroundTintList(null);
        search.setBackground(Ui.rounded(activity, 16, 0xFF23302D, 0x20FFFFFF));
        search.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 12), Ui.dp(activity, 14), Ui.dp(activity, 12));
        search.setMinHeight(Ui.dp(activity, 48));
        root.addView(search, new LinearLayout.LayoutParams(-1, -2));
        count = Ui.text(root, "", 13, Ui.MUTED);
        count.setPadding(0, Ui.dp(activity, 12), 0, Ui.dp(activity, 8));
        android.widget.FrameLayout listArea = new android.widget.FrameLayout(activity);
        root.addView(listArea, new LinearLayout.LayoutParams(-1, 0, 1));
        ListView list = new ListView(activity);
        list.setDivider(null); list.setSelector(android.R.color.transparent);
        list.setAdapter(adapter); list.setClipToPadding(false);
        listArea.addView(list, new android.widget.FrameLayout.LayoutParams(-1, -1));
        TextView empty = new TextView(activity);
        empty.setText(R.string.chooser_empty); empty.setTextSize(15); empty.setTextColor(Ui.MUTED);
        empty.setGravity(Gravity.CENTER); empty.setPadding(0, Ui.dp(activity, 24), 0, Ui.dp(activity, 24));
        listArea.addView(empty, new android.widget.FrameLayout.LayoutParams(-1, -1));
        list.setEmptyView(empty);
        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL); buttons.setGravity(Gravity.TOP);
        buttons.setBaselineAligned(false);
        root.addView(buttons, new LinearLayout.LayoutParams(-1, -2));
        Button cancel = Ui.button(buttons, "Cancel", v -> dismiss());
        Button apply = Ui.primaryButton(buttons, "Save", v -> { save.accept(new HashSet<>(draft)); dismiss(); });
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        left.setMarginEnd(Ui.dp(activity, 6)); left.topMargin = Ui.dp(activity, 12);cancel.setLayoutParams(left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
        right.setMarginStart(Ui.dp(activity, 6)); right.topMargin = Ui.dp(activity, 12);apply.setLayoutParams(right);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(Ui.rounded(activity, 26, 0xFF151F1D, 0x30FFFFFF));
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int length, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int length) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable value) { }
        });
        filter("");
    }
    @Override protected void onStart() {
        super.onStart();
        var metrics = activity.getWindowManager().getCurrentWindowMetrics();
        int width = Math.min(metrics.getBounds().width() - Ui.dp(activity, 24), Ui.dp(activity, 440));
        // Let Android measure against the currently available height, including the keyboard.
        // A fixed pixel height can leave the action row behind the IME on floating windows.
        if (getWindow() != null) getWindow().setLayout(width, WindowManager.LayoutParams.MATCH_PARENT);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN && getWindow() != null) {
            View decor = getWindow().getDecorView();
            if (event.getX() < 0 || event.getY() < 0 || event.getX() >= decor.getWidth() || event.getY() >= decor.getHeight()) {
                cancel();return true;
            }
        }
        return super.onTouchEvent(event);
    }
    private void filter(String query) {
        String text = query.strip().toLowerCase(Locale.ROOT);
        visible.clear();
        for (String pkg : ordered) if (labels.get(pkg).toLowerCase(Locale.ROOT).contains(text)
            || pkg.toLowerCase(Locale.ROOT).contains(text)) visible.add(pkg);
        adapter.notifyDataSetChanged();updateCount();
    }
    private void updateCount() {
        int selected = 0;
        for (String pkg : ordered) if (draft.contains(pkg)) selected++;
        count.setText(activity.getResources().getQuantityString(R.plurals.selected_apps, selected, selected));
    }
    private final class PickerAdapter extends BaseAdapter {
        @Override public int getCount() { return visible.size(); }
        @Override public String getItem(int index) { return visible.get(index); }
        @Override public long getItemId(int index) { return ordered.indexOf(getItem(index)); }
        @Override public View getView(int index, View recycled, android.view.ViewGroup parent) {
            PickerRow row = recycled instanceof PickerRow item ? item : new PickerRow(activity);
            row.bind(getItem(index));return row;
        }
    }
    private final class PickerRow extends LinearLayout {
        private final ImageView icon;
        private final TextView name, detail;
        private final ImageView indicator;
        private String pkg;
        PickerRow(Context context) {
            super(context);
            setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
            setPadding(Ui.dp(context, 10), Ui.dp(context, 12), Ui.dp(context, 10), Ui.dp(context, 12));
            setMinimumHeight(Ui.dp(context, 64));setClickable(true);setFocusable(true);
            setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            icon = new ImageView(context);icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            LayoutParams art = new LayoutParams(Ui.dp(context, 36), Ui.dp(context, 36));art.setMarginEnd(Ui.dp(context, 12));addView(icon, art);
            LinearLayout text = new LinearLayout(context);text.setOrientation(VERTICAL);
            text.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(text, new LayoutParams(0, -2, 1));
            name = Ui.text(text, "", 16, Ui.TEXT);name.setPadding(0, 0, 0, 0);name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            detail = Ui.text(text, "", 12, Ui.MUTED);detail.setPadding(0, Ui.dp(context, 4), 0, 0);
            indicator = new ImageView(context);indicator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            LayoutParams mark = new LayoutParams(Ui.dp(context, 24), Ui.dp(context, 24));mark.setMarginStart(Ui.dp(context, 12));addView(indicator, mark);
            Ui.pressFeedback(this);
            setOnClickListener(v -> {
                if (!draft.add(pkg)) draft.remove(pkg);
                selected();updateCount();
                sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            });
        }
        void bind(String pkg) {
            this.pkg = pkg;
            String title = labels.get(pkg);name.setText(title);
            boolean duplicate = duplicateLabels.contains(title);
            detail.setText(duplicate ? pkg : "");detail.setVisibility(duplicate ? VISIBLE : GONE);
            setContentDescription(duplicate ? title + ", " + pkg : title);
            if (!icons.containsKey(pkg)) {
                try { icons.put(pkg, activity.getPackageManager().getApplicationIcon(pkg)); }
                catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                    icons.put(pkg, new GlyphDrawable(GlyphDrawable.APPS, Ui.MUTED, Ui.dp(activity, 36)));
                }
            }
            icon.setImageDrawable(icons.get(pkg));selected();
        }
        private void selected() {
            boolean checked = draft.contains(pkg);
            setBackground(new RippleDrawable(ColorStateList.valueOf(0x24FFFFFF),
                Ui.rounded(activity, 14, checked ? 0x163D5515 : 0, checked ? 0x46D3FF5F : 0), null));
            indicator.setImageDrawable(new CheckMark(checked));
            setSelected(checked);
        }
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(CheckBox.class.getName());info.setCheckable(true);info.setChecked(draft.contains(pkg));
        }
    }
    private static final class CheckMark extends Drawable {
        private final boolean checked;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF(2, 2, 22, 22);
        private final Path tick = new Path();
        CheckMark(boolean checked) { this.checked = checked;tick.moveTo(7, 12);tick.lineTo(10, 15);tick.lineTo(17, 8); }
        @Override public void draw(Canvas canvas) {
            int save = canvas.save();canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            paint.setStyle(checked ? Paint.Style.FILL : Paint.Style.STROKE);paint.setStrokeWidth(1.5f);
            paint.setColor(checked ? Ui.MINT : Ui.MUTED);canvas.drawRoundRect(rect, 6, 6, paint);
            if (checked) {
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.2f);paint.setColor(0xFF152008);
                paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(tick, paint);
            }
            canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha);invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter);invalidateSelf(); }
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
