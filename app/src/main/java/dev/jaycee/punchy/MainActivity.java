package dev.jaycee.punchy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity {
    private Switch enabled;
    private TextView status;
    private TextView live;
    private Button notificationAccess;
    private Button overlayAccess;
    private Button preview;
    private Button apps;
    private Switch allApps;
    private boolean updating;
    private final Runnable stateChanged = this::updateState;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        LinearLayout root = Ui.screen(this);
        Ui.heading(root, "Punchy", 32);
        Ui.text(root, "Download progress, around your camera.", 16, Ui.MUTED);

        LinearLayout main = Ui.card(root);
        enabled = toggle(main, "Enable Punchy");
        status = Ui.text(main, "Off", 16, Ui.MUTED);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            if (!PunchyRuntime.setEnabled(this, checked)) toast("Complete notification access and overlay setup below first.");
            updateState();
        });
        live = Ui.text(main, "", 16, Ui.TEXT);
        live.setMaxLines(3);
        live.setEllipsize(android.text.TextUtils.TruncateAt.END);

        FrameLayout sample = new FrameLayout(this);
        sample.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        main.addView(sample, new LinearLayout.LayoutParams(-1, Ui.dp(this, 66)));
        FrameLayout pill = new FrameLayout(this);
        GradientDrawable black = new GradientDrawable();
        black.setColor(Color.BLACK);
        black.setCornerRadius(Ui.dp(this, 24));
        pill.setBackground(black);
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(Ui.dp(this, 130), Ui.dp(this, 40), Gravity.CENTER);
        sample.addView(pill, pillParams);
        RingView illustration = new RingView(this);
        pill.addView(illustration, new FrameLayout.LayoutParams(-1, -1));
        illustration.progress(65, false, 2);
        Ui.text(main, "Sample progress: 65%", 14, Ui.MUTED).setGravity(Gravity.CENTER);
        preview = Ui.button(main, "Preview on phone · 6 seconds", v -> {
            if (PunchyRuntime.overlay != null) { PunchyRuntime.overlay.preview(); toast("Sample ring shown for 6 seconds."); }
        });
        Ui.button(main, "Adjust shape & alignment", v -> startActivity(new Intent(this, AlignmentActivity.class)));

        LinearLayout access = Ui.card(root);
        Ui.heading(access, "Setup", 20);
        Ui.text(access, "Two Android settings let Punchy read download progress and draw above the status bar.", 16, Ui.MUTED);
        notificationAccess = Ui.button(access, "Allow notification access", v -> openNotificationAccess());
        overlayAccess = Ui.button(access, "Enable overlay service", v -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Ui.text(access, "In Accessibility, choose Punchy progress ring. It draws only; it does not read your screen or control gestures.", 14, Ui.MUTED);
        Ui.button(access, "Android blocking access?", v -> new AlertDialog.Builder(this)
            .setTitle("Allow restricted settings")
            .setMessage("For a sideloaded app, Android may block notification or accessibility access.\n\nOpen Settings → Apps → Punchy, tap the menu at the top, then Allow restricted settings. Return here and try setup again. MagicOS may use slightly different names.")
            .setPositiveButton("App settings", (dialog, which) -> openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:" + getPackageName()))))
            .setNegativeButton("Close", null).show());
        Ui.button(access, "Add Quick Settings tile", v -> addTile());

        LinearLayout sources = Ui.card(root);
        Ui.heading(sources, "Download apps", 20);
        Ui.text(sources, "Browsers and system downloads are selected by default. Apps must publish a progress notification.", 16, Ui.MUTED);
        allApps = toggle(sources, "All progress notifications");
        allApps.setChecked(Prefs.allApps(this));
        Ui.text(sources, "Also includes file transfers and app updates.", 14, Ui.MUTED);
        allApps.setOnCheckedChangeListener((button, checked) -> {
            if (!updating) Prefs.filter(this, checked, Prefs.apps(this));
        });
        apps = Ui.button(sources, "Choose download apps", v -> chooseApps());
        Ui.text(root, "For your HONOR X7c · Android 14\nA separate overlay, with no network access.", 14, Ui.MUTED);
        updateState();
    }
    private Switch toggle(LinearLayout parent, String label) {
        Switch toggle = new Switch(this);
        toggle.setText(label);
        toggle.setTextSize(18);
        toggle.setTextColor(Ui.TEXT);
        toggle.setMinHeight(Ui.dp(this, 58));
        toggle.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        parent.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
        return toggle;
    }
    @Override public void onResume() {
        super.onResume();
        PunchyRuntime.observers.add(stateChanged);
        if (notificationGranted() && PunchyRuntime.listener == null)
            NotificationListenerService.requestRebind(new ComponentName(this, DownloadListener.class));
        updateState();
    }
    @Override public void onPause() { PunchyRuntime.observers.remove(stateChanged); super.onPause(); }
    private boolean notificationGranted() {
        return getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(this, DownloadListener.class));
    }
    private boolean overlayEnabled() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        if (manager == null) return false;
        ComponentName component = new ComponentName(this, RingService.class);
        for (AccessibilityServiceInfo service : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (component.equals(ComponentName.unflattenFromString(service.getId()))) return true;
        }
        return false;
    }
    private void updateState() {
        if (enabled == null) return;
        updating = true;
        enabled.setChecked(Prefs.enabled(this));
        allApps.setChecked(Prefs.allApps(this));
        updating = false;
        boolean notif = notificationGranted(), overlay = overlayEnabled();
        notificationAccess.setText(notif ? "Notification access · Allowed" : "Allow notification access");
        overlayAccess.setText(overlay ? "Overlay service · Enabled" : "Enable overlay service");
        if (!notif || !overlay) status.setText(R.string.finish_setup);
        else if (!PunchyRuntime.ready()) status.setText(R.string.waiting_connection);
        else if (Prefs.enabled(this) && PunchyRuntime.overlay.lastError != null) status.setText(PunchyRuntime.overlay.lastError);
        else status.setText(Prefs.enabled(this)
            ? PunchyRuntime.music != null ? "On · Music capsule mode" : "On · Watching for downloads"
            : "Off · No progress overlay");
        ProgressTracker.Snapshot snapshot = PunchyRuntime.tracker.snapshot();
        if (snapshot.active() && Prefs.enabled(this)) {
            String count = getResources().getQuantityString(R.plurals.download_count, snapshot.count(), snapshot.count());
            live.setText(snapshot.indeterminate()
                ? getString(R.string.active_unknown, count, snapshot.title())
                : getString(R.string.active_known, count, snapshot.percent(), snapshot.title()));
            live.setVisibility(View.VISIBLE);
        } else live.setVisibility(View.GONE);
        preview.setEnabled(PunchyRuntime.ready() && Prefs.enabled(this));
        apps.setEnabled(!Prefs.allApps(this));
    }
    private void openNotificationAccess() {
        Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, new ComponentName(this, DownloadListener.class).flattenToString());
        try { startActivity(detail); }
        catch (ActivityNotFoundException e) { openSettings(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
    }
    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { toast("Open this setting from your phone's Settings app."); }
    }
    private void addTile() {
        StatusBarManager manager = getSystemService(StatusBarManager.class);
        if (manager == null) { toast("Edit Quick Settings and add Punchy manually."); return; }
        manager.requestAddTileService(new ComponentName(this, PunchyTile.class), "Punchy",
            Icon.createWithResource(this, R.drawable.ic_punchy), getMainExecutor(), result -> {
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
                    || result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) toast("Punchy is in Quick Settings.");
                else toast("You can also edit Quick Settings and add Punchy manually.");
            });
    }
    @SuppressWarnings("deprecation")
    private void chooseApps() {
        PackageManager pm = getPackageManager();
        Map<String, String> labels = new HashMap<>();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo item : pm.queryIntentActivities(launcher, 0)) {
            if (item.activityInfo == null || getPackageName().equals(item.activityInfo.packageName)) continue;
            labels.put(item.activityInfo.packageName, item.loadLabel(pm).toString());
        }
        for (String pkg : Prefs.apps(this)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                labels.put(pkg, pm.getApplicationLabel(info).toString());
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        List<String> packages = new ArrayList<>(labels.keySet());
        packages.sort(Comparator.comparing(labels::get, String.CASE_INSENSITIVE_ORDER));
        String[] names = new String[packages.size()];
        boolean[] checked = new boolean[packages.size()];
        Set<String> chosen = Prefs.apps(this);
        for (int i = 0; i < packages.size(); i++) { names[i] = labels.get(packages.get(i)); checked[i] = chosen.contains(packages.get(i)); }
        new AlertDialog.Builder(this).setTitle("Track progress from")
            .setMultiChoiceItems(names, checked, (dialog, which, selected) -> {
                if (selected) chosen.add(packages.get(which)); else chosen.remove(packages.get(which));
            })
            .setPositiveButton("Save", (dialog, which) -> Prefs.filter(this, false, chosen))
            .setNegativeButton("Cancel", null).show();
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
