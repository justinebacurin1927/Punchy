package dev.jaycee.punchy;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Public notification data only. No screen reads, hidden RemoteViews fields or guessed completion. */
final class NotificationProgress {
    record Result(long progress, long maximum, boolean unknown, String source) {}
    record Inspection(Result result, String fields, String reason) {}
    private NotificationProgress() {}

    @SuppressWarnings("deprecation")
    static Inspection inspect(Context context, Notification notification) {
        Bundle extras = notification.extras == null ? Bundle.EMPTY : notification.extras;
        Object progress = extras.get(Notification.EXTRA_PROGRESS);
        Object maximum = extras.get(Notification.EXTRA_PROGRESS_MAX);
        Object unknown = extras.get(Notification.EXTRA_PROGRESS_INDETERMINATE);
        String fields = "progress=" + numericField(progress) + ", max=" + numericField(maximum)
            + ", indeterminate=" + (unknown instanceof Boolean ? unknown : "absent");
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category) || extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                || Notification.CATEGORY_MESSAGE.equals(notification.category) || Notification.CATEGORY_CALL.equals(notification.category))
            return new Inspection(null, fields, "Music, message or call notification");
        if (Boolean.TRUE.equals(unknown))
            return found(new Result(0, 0, true, "Notification fields"), fields);
        if (progress instanceof Number p && maximum instanceof Number m && p.longValue() >= 0 && m.longValue() > 0)
            return found(new Result(p.longValue(), m.longValue(), false, "Notification fields"), fields);

        List<CharSequence> texts = new ArrayList<>();
        for (String key : new String[] { Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_SUB_TEXT, Notification.EXTRA_INFO_TEXT, Notification.EXTRA_TITLE }) {
            Object value = extras.get(key);
            if (value instanceof CharSequence text) texts.add(text);
        }
        Object lines = extras.get(Notification.EXTRA_TEXT_LINES);
        if (lines instanceof CharSequence[] array) java.util.Collections.addAll(texts, array);
        boolean customFailed = false;
        for (RemoteViews views : new RemoteViews[] { notification.bigContentView, notification.contentView, notification.headsUpContentView }) {
            if (views == null) continue;
            try {
                Result result = inspectView(views.apply(context, null), texts, new int[] { 0 }, 0);
                if (result != null) return found(result, fields);
            } catch (RuntimeException ignored) { customFailed = true; }
        }
        // Text fallbacks need a task in progress. Exclude common non-download percentages.
        boolean ongoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        boolean task = ongoing || Notification.CATEGORY_PROGRESS.equals(notification.category);
        String combined = texts.toString().toLowerCase(Locale.ROOT);
        boolean excluded = combined.contains("battery") || combined.contains("charging") || combined.contains("volume");
        if (task && !excluded) for (CharSequence text : texts) {
            int percent = ProgressText.percent(text);
            if (percent >= 0) return found(new Result(percent, 100, false, "Text percentage"), fields);
        }
        return new Inspection(null, fields, customFailed ? "Custom notification could not be read" : "No readable progress found");
    }
    private static Inspection found(Result result, String fields) { return new Inspection(result, fields, ""); }
    private static String numericField(Object value) {
        return value == null ? "absent" : value instanceof Number ? value.getClass().getSimpleName() + ":" + value
            : value.getClass().getSimpleName();
    }
    private static Result inspectView(View view, List<CharSequence> texts, int[] visited, int depth) {
        if (view == null || depth > 16 || ++visited[0] > 128 || view.getVisibility() != View.VISIBLE) return null;
        if (view instanceof ProgressBar bar) {
            if (bar.isIndeterminate()) return new Result(0, 0, true, "Custom progress bar");
            if (bar.getMax() > 0) return new Result(bar.getProgress(), bar.getMax(), false, "Custom progress bar");
        }
        if (view instanceof TextView text) texts.add(text.getText());
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            Result result = inspectView(group.getChildAt(i), texts, visited, depth + 1);
            if (result != null) return result;
        }
        return null;
    }
}
