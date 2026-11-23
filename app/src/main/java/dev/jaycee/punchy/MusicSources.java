package dev.jaycee.punchy;

import java.util.Set;

/** A media session is not evidence of a visible HONOR music capsule. Use chosen players. */
final class MusicSources {
    static final Set<String> DEFAULTS = Set.of("com.shaiban.audioplayer.mplayer",
        "com.spotify.music", "com.google.android.apps.youtube.music");
    private MusicSources() {}
    static boolean accepts(Set<String> selected, String packageName, boolean movie) {
        return !movie && packageName != null && selected.contains(packageName);
    }
}
