package dev.jaycee.punchy;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class Prefs {
    private Prefs() {}
    static SharedPreferences get(Context c) { return c.getApplicationContext().getSharedPreferences("punchy", Context.MODE_PRIVATE); }
    static boolean enabled(Context c) { return get(c).getBoolean("enabled", false); }
    static boolean allApps(Context c) { return get(c).getBoolean("all_apps", false); }
    static boolean capsule(Context c) { return get(c).getBoolean("capsule", false); }
    static int value(Context c, String name, int fallback) { return get(c).getInt(name, fallback); }
    static Set<String> apps(Context c) {
        Set<String> defaults = new HashSet<>(Arrays.asList(
            "com.android.providers.downloads", "com.android.providers.downloads.ui",
            "com.android.chrome", "com.brave.browser", "org.mozilla.firefox", "org.mozilla.fenix",
            "com.opera.browser", "com.opera.mini.native", "com.android.vending", "org.fdroid.fdroid",
            "com.hihonor.appmarket", "com.huawei.appmarket", "com.dv.adm", "com.dv.adm.pay",
            "com.kiwibrowser.browser", "com.vivaldi.browser", "mark.via.gp"));
        return new HashSet<>(get(c).getStringSet("apps", defaults));
    }
    static boolean tracks(Context c, String pkg) { return allApps(c) || apps(c).contains(pkg); }
    static void number(Context c, String key, int value) {
        get(c).edit().putInt(key, value).apply();
        PunchyRuntime.publish(false);
    }
    static void shape(Context c, boolean capsule) {
        get(c).edit().putBoolean("capsule", capsule).apply();
        PunchyRuntime.publish(false);
    }
    static void filter(Context c, boolean all, Set<String> packages) {
        get(c).edit().putBoolean("all_apps", all).putStringSet("apps", new HashSet<>(packages)).apply();
        if (PunchyRuntime.listener != null) PunchyRuntime.listener.rescan();
        PunchyRuntime.publish(false);
    }
    static void resetGeometry(Context c) {
        get(c).edit().remove("width").remove("height").remove("stroke").remove("x").remove("y").apply();
        PunchyRuntime.publish(false);
    }
}
