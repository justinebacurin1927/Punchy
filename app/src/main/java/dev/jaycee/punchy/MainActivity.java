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
import java.util.HashMap;
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
    private Button accessibilityPause;
    private TextView bankingHelp;
    private Switch allApps;
    private boolean updating;
    private AppChooser chooser;
    private final Runnable stateChanged = this::updateState;
    private final AccessibilityManager.AccessibilityServicesStateChangeListener accessChanged = manager -> updateState();

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
        accessibilityPause = Ui.button(main, "Turn off Accessibility", v -> turnOffAccessibility());
        bankingHelp = Ui.text(main, "", 13, Ui.MUTED);
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
        Ui.text(access, "Allow these two settings to show your downloads.", 13, Ui.MUTED);
        notificationAccess = Ui.actionRow(access, "Allow notification access", GlyphDrawable.NOTIFICATION, v -> openNotificationAccess());
        Ui.divider(access);
        overlayAccess = Ui.actionRow(access, "Enable overlay service", GlyphDrawable.LAYERS, v -> openOverlaySettings());
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
        Ui.text(moreSetup, "Open Accessibility and turn on Punchy progress ring.", 13, Ui.MUTED);
        Ui.actionRow(moreSetup, "Android blocking access?", GlyphDrawable.HELP, v -> new AlertDialog.Builder(this)
            .setTitle("Allow restricted settings")
            .setMessage("If Android won’t let you turn on a setting:\n\nOpen Settings → Apps → Punchy. Tap the menu at the top, then Allow restricted settings. Come back and try again.")
            .setPositiveButton("App settings", (dialog, which) -> openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:" + getPackageName()))))
            .setNegativeButton("Close", null).show());
        Ui.actionRow(moreSetup, "Add Quick Settings tile", GlyphDrawable.TILE, v -> addTile());

        LinearLayout sources = Ui.card(root);
        Ui.heading(sources, "Download apps", 18);
        allApps = Ui.toggle(sources, "All progress notifications");
        allApps.setChecked(Prefs.allApps(this));
        Ui.text(sources, "Show downloads from any app. Turn off to choose your apps.", 13, Ui.MUTED);
        allApps.setOnCheckedChangeListener((button, checked) -> {
            if (!updating) Prefs.filter(this, checked, Prefs.apps(this));
        });
        apps = Ui.actionRow(sources, "Choose download apps", GlyphDrawable.APPS, v -> chooseApps());
        Ui.divider(sources);
        Ui.actionRow(sources, "Download detection", GlyphDrawable.SCAN, v -> startActivity(new Intent(this, DetectionActivity.class)));
        Ui.divider(sources);
        Ui.actionRow(sources, "Music capsule apps", GlyphDrawable.APPS, v -> chooseMusicApps());
        Ui.text(sources, "Choose music players for the wide ring. Video apps are left out.", 13, Ui.MUTED);
        updateState();
    }
    @Override public void onResume() {
        super.onResume();
        PunchyRuntime.observers.add(stateChanged);
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        if (manager != null) manager.addAccessibilityServicesStateChangeListener(getMainExecutor(), accessChanged);
        if (notificationGranted() && PunchyRuntime.listener == null)
            NotificationListenerService.requestRebind(new ComponentName(this, DownloadListener.class));
        updateState();
    }
    @Override public void onPause() {
        PunchyRuntime.observers.remove(stateChanged);
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        if (manager != null) manager.removeAccessibilityServicesStateChangeListener(accessChanged);
        super.onPause();
    }
    @Override public void onDestroy() {
        if (chooser != null) chooser.dismiss();
        super.onDestroy();
    }
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
        boolean paused = Prefs.accessibilityPaused(this);
        notificationAccess.setText(notif ? "Notification access · Allowed" : "Allow notification access");
        overlayAccess.setText(overlay ? "Overlay service · Enabled" : "Enable overlay service");
        accessibilityPause.setText(!overlay ? "Enable Accessibility" : paused ? "Open Accessibility settings" : "Turn off Accessibility");
        bankingHelp.setText(!overlay
            ? "Accessibility is off. To use the ring, enable Punchy progress ring in Android settings, then turn on Enable Punchy."
            : paused ? "Accessibility is still on. Turn off Punchy progress ring in Android settings."
            : "Banking app blocking Punchy? Turn off Accessibility here before opening it. The ring stops; notification access stays on.");
        if (paused && !overlay) status.setText(R.string.accessibility_paused);
        else if (!notif || !overlay) status.setText(R.string.finish_setup);
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
    private void turnOffAccessibility() {
        if (!overlayEnabled() || Prefs.accessibilityPaused(this)) { openOverlaySettings(); return; }
        RingService service = PunchyRuntime.overlay;
        if (service == null) {
            toast("Turn off Punchy progress ring in Android settings.");
            openOverlaySettings();
            return;
        }
        service.turnOffAccessibility();
        updateState();
    }
    private void openOverlaySettings() {
        openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
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
        chooser = AppChooser.show(this, "Music capsule apps", labels, chosen, selected -> Prefs.musicApps(this, selected));
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
        Set<String> chosen = Prefs.apps(this);
        chooser = AppChooser.show(this, "Choose download apps", labels, chosen, selected -> Prefs.filter(this, false, selected));
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
