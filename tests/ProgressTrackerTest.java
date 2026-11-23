package dev.jaycee.punchy;

public final class ProgressTrackerTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ProgressTracker t = new ProgressTracker();
        check(!t.snapshot().active(), "A fresh tracker must be idle");
        check(!t.update("a", 0, 100, false, "File A"), "Starting a download cannot complete it");
        check(t.snapshot().count() == 1 && t.snapshot().percent() == 0, "Zero is valid progress");
        t.update("a", 42, 100, false, "File A");
        check(t.snapshot().percent() == 42, "Track a real in-flight percentage");
        t.update("b", 10, 20, false, "File B");
        check(t.snapshot().count() == 2 && t.snapshot().percent() == 46, "Aggregate simultaneous downloads in different units");
        check(!t.update("a", 100, 100, false, "File A"), "One completed download must not replace remaining progress with success");
        check(t.snapshot().count() == 1 && t.snapshot().percent() == 50, "The other download remains active");
        check(t.update("b", 20, 20, false, "File B"), "Explicit 100% on final active item completes");
        check(!t.snapshot().active(), "Completed downloads leave active state");
        check(!t.update("b", 20, 20, false, "File B"), "Repeated 100% cannot replay completion");
        check(!t.update("old", 100, 100, false, "Old item"), "An already-complete notification on reconnect is not a new success");
        t.update("cancelled", 60, 100, false, "Cancelled");
        t.remove("cancelled");
        check(!t.snapshot().active(), "Cancellation/removal clears progress without a completion event");
        t.update("unknown", -1, 0, true, "Unknown total");
        check(t.snapshot().indeterminate() && t.snapshot().count() == 1, "Unknown totals stay indeterminate");
        t.update("known", 90, 100, false, "Known total");
        check(t.snapshot().indeterminate(), "A combined total must not pretend unknown progress is measurable");
        t.update("unknown", 25, 100, false, "Now known");
        check(!t.snapshot().indeterminate() && t.snapshot().percent() == 57, "Indeterminate item can transition to real progress");
        t.clear();
        check(!t.snapshot().active(), "Disable/disconnect clears all downloads");
        t.update("overflow", Long.MAX_VALUE / 2, Long.MAX_VALUE, false, "Large");
        check(t.snapshot().percent() >= 49 && t.snapshot().percent() <= 50, "Large counters must not overflow multiplication");
        check(t.update("overflow", Long.MAX_VALUE, Long.MAX_VALUE, false, "Large"), "Large explicit final counters complete");
        t.update("reset", 90, 100, false, "Earlier file");
        t.update("reset", 2, 100, false, "New file");
        check(t.snapshot().percent() == 2, "Reused notification key can restart at lower progress");
        check(!t.update("reset", -1, 0, false, "No progress"), "Losing progress fields cannot prove success");
        check(!t.snapshot().active(), "A non-progress replacement must clear its previous state");
        t.update("broken", -1, 100, false, null);
        check(!t.snapshot().active(), "Negative determinate counters are ignored");
        t.update(null, 1, 100, false, "Bad key");
        t.update("", 1, 100, false, "Bad key");
        check(!t.snapshot().active(), "Missing notification keys cannot create stuck items");
        t.update("over", 101, 100, false, "Already done");
        check(!t.snapshot().active(), "Out-of-range completed counters never create active progress");
        t.update("null-title", 1, 10, false, null);
        check(t.snapshot().title().equals("Download"), "Missing notification titles have a harmless label");
        System.out.println(checks + " meaningful progress-state checks passed.");
    }
}
