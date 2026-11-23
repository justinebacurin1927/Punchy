package dev.jaycee.punchy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Notification-driven state. No Android classes, wall clock, or inferred completion. */
public final class ProgressTracker {
    private record Download(int percent, boolean unknown, String title) {}
    public record Snapshot(int count, int percent, boolean indeterminate, String title, String key) {
        public boolean active() { return count > 0; }
    }
    private final Map<String, Download> downloads = new LinkedHashMap<>();

    /** Returns true only when an observed active final download explicitly reaches 100%. */
    public boolean update(String key, long progress, long maximum, boolean unknown, String title) {
        if (key == null || key.isEmpty()) return false;
        boolean wasActive = downloads.containsKey(key);
        if (!unknown && (progress < 0 || maximum <= 0)) {
            downloads.remove(key);
            return false;
        }
        int percent = unknown ? 0 : (int) Math.max(0, Math.min(100, (double) progress / maximum * 100));
        if (!unknown && percent >= 100) {
            downloads.remove(key);
            return wasActive && downloads.isEmpty();
        }
        downloads.remove(key);
        downloads.put(key, new Download(percent, unknown, title == null ? "Download" : title));
        return false;
    }

    public void remove(String key) { downloads.remove(key); }
    public void clear() { downloads.clear(); }

    public Snapshot snapshot() {
        if (downloads.isEmpty()) return new Snapshot(0, 0, false, "", "");
        long sum = 0;
        boolean unknown = false;
        String title = "Download";
        String key = "";
        for (Map.Entry<String, Download> entry : downloads.entrySet()) {
            Download d = entry.getValue();
            sum += d.percent();
            unknown |= d.unknown();
            title = d.title();
            key = entry.getKey();
        }
        return new Snapshot(downloads.size(), unknown ? 0 : (int) (sum / downloads.size()), unknown, title, key);
    }
}
