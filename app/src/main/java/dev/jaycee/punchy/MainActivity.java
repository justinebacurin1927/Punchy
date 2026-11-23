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
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL); brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this); logo.setImageResource(R.mipmap.ic_launcher);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams logoSize = new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46));
        logoSize.setMarginEnd(Ui.dp(this, 12)); brand.addView(logo, logoSize);
        LinearLayout identity = new LinearLayout(this); identity.setOrientation(LinearLayout.VERTICAL);
        brand.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        Ui.heading(identity, "Punchy", 28);
        Ui.text(identity, "A little space for your downloads.", 13, Ui.MUTED);
        root.addView(brand, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout main = Ui.card(root);
        enabled = Ui.toggle(main, "Enable Punchy");
        status = Ui.text(main, "Off", 13, Ui.MUTED);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            if (!PunchyRuntime.setEnabled(this, checked)) toast("Complete notification access and overlay setup below first.");
            updateState();
        });
        live = Ui.text(main, "", 15, Ui.TEXT);
        live.setMaxLines(3);
        live.setEllipsize(android.text.TextUtils.TruncateAt.END);

        FrameLayout sample = new FrameLayout(this);
        sample.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        main.addView(sample, new LinearLayout.LayoutParams(-1, Ui.dp(this, 100)));
        sample.addView(new IslandPreviewView(this), new FrameLayout.LayoutParams(-1, -1));
        Ui.text(main, "Sample progress: 65%", 12, Ui.MUTED).setGravity(Gravity.CENTER);
        preview = Ui.primaryButton(main, "Preview on phone · 6 seconds", v -> {
            if (PunchyRuntime.overlay != null) { PunchyRuntime.overlay.preview(); toast("Sample ring shown for 6 seconds."); }
        });
        Ui.actionRow(main, "Adjust shape & alignment", GlyphDrawable.SLIDERS, v -> startActivity(new Intent(this, AlignmentActivity.class)));

        LinearLayout access = Ui.card(root);
        Ui.heading(access, "Setup", 18);
        Ui.text(access, "Two connections. One quiet little island.", 13, Ui.MUTED);
        notificationAccess = Ui.actionRow(access, "Allow notification access", GlyphDrawable.NOTIFICATION, v -> openNotificationAccess());
        Ui.divider(access);
        overlayAccess = Ui.actionRow(access, "Enable overlay service", GlyphDrawable.LAYERS, v -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout moreSetup = new LinearLayout(this); moreSetup.setOrientation(LinearLayout.VERTICAL);
        moreSetup.setVisibility(notificationGranted() && overlayEnabled() ? View.GONE : View.VISIBLE);
        Button more = Ui.actionRow(access, "More setup options", GlyphDrawable.HELP, v -> {
            boolean show = moreSetup.getVisibility() != View.VISIBLE;
            Ui.disclosure(access, moreSetup, show);
            ((Button) v).setText(show ? "Fewer setup options" : "More setup options");
            v.setStateDescription(show ? "Expanded" : "Collapsed");
        });
        more.setText(moreSetup.getVisibility() == View.VISIBLE ? "Fewer setup options" : "More setup options");
        more.setStateDescription(moreSetup.getVisibility() == View.VISIBLE ? "Expanded" : "Collapsed");
        access.addView(moreSetup, new LinearLayout.LayoutParams(-1, -2));
        Ui.text(moreSetup, "In Accessibility, choose Punchy progress ring. It never reads your screen or controls gestures.", 13, Ui.MUTED);
        Ui.actionRow(moreSetup, "Android blocking access?", GlyphDrawable.HELP, v -> new AlertDialog.Builder(this)
            .setTitle("Allow restricted settings")
            .setMessage("For a sideloaded app, Android may block notification or accessibility access.\n\nOpen Settings → Apps → Punchy, tap the menu at the top, then Allow restricted settings. Return here and try setup again. MagicOS may use slightly different names.")
            .setPositiveButton("App settings", (dialog, which) -> openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:" + getPackageName()))))
            .setNegativeButton("Close", null).show());
        Ui.actionRow(moreSetup, "Add Quick Settings tile", GlyphDrawable.TILE, v -> addTile());

        LinearLayout sources = Ui.card(root);
        Ui.heading(sources, "Download apps", 18);
        allApps = Ui.toggle(sources, "All progress notifications");
        allApps.setChecked(Prefs.allApps(this));
        Ui.text(sources, "Includes transfers and app updates. Turn off to choose individual apps.", 13, Ui.MUTED);
        allApps.setOnCheckedChangeListener((button, checked) -> {
            if (!updating) Prefs.filter(this, checked, Prefs.apps(this));
        });
        apps = Ui.actionRow(sources, "Choose download apps", GlyphDrawable.APPS, v -> chooseApps());
        Ui.divider(sources);
        Ui.actionRow(sources, "Download detection", GlyphDrawable.SCAN, v -> startActivity(new Intent(this, DetectionActivity.class)));
        Ui.divider(sources);
        Ui.actionRow(sources, "Music capsule apps", GlyphDrawable.APPS, v -> chooseMusicApps());
        Ui.text(sources, "Only selected music players use the wide shape. Video apps stay off by default.", 13, Ui.MUTED);
        TextView privateNote = Ui.text(root, "Punchy has no network access.", 12, Ui.MUTED);
        privateNote.setGravity(Gravity.CENTER); privateNote.setPadding(0, Ui.dp(this, 20), 0, 0);
        updateState();
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
            ? PunchyRuntime.musicPlaying() ? "On · Music capsule mode" : "On · Watching for downloads"
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
        apps.setVisibility(Prefs.allApps(this) ? View.GONE : View.VISIBLE);
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
    private void chooseMusicApps() {
        PackageManager pm = getPackageManager();
        Map<String, String> labels = new HashMap<>();
        Set<String> chosen = Prefs.musicApps(this);
        for (ResolveInfo item : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
            if (item.activityInfo != null && !getPackageName().equals(item.activityInfo.packageName))
                labels.put(item.activityInfo.packageName, item.loadLabel(pm).toString());
        }
        Set<String> packages = new java.util.HashSet<>(chosen);
        if (PunchyRuntime.listener != null) packages.addAll(PunchyRuntime.listener.mediaPackages());
        for (String pkg : packages) {
            try { labels.put(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()); }
            catch (PackageManager.NameNotFoundException ignored) {
                if (PunchyRuntime.listener != null && PunchyRuntime.listener.mediaPackages().contains(pkg)) labels.put(pkg, pkg);
            }
        }
        List<String> ordered = new ArrayList<>(labels.keySet());
        ordered.sort(Comparator.comparing(labels::get, String.CASE_INSENSITIVE_ORDER));
        String[] names = new String[ordered.size()];
        boolean[] checked = new boolean[ordered.size()];
        for (int i = 0; i < ordered.size(); i++) { names[i] = labels.get(ordered.get(i)); checked[i] = chosen.contains(ordered.get(i)); }
        new AlertDialog.Builder(this).setTitle("Music capsule apps")
            .setMultiChoiceItems(names, checked, (dialog, which, selected) -> {
                if (selected) chosen.add(ordered.get(which)); else chosen.remove(ordered.get(which));
            })
            .setPositiveButton("Save", (dialog, which) -> Prefs.musicApps(this, chosen))
            .setNegativeButton("Cancel", null).show();
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
