package dev.jaycee.punchy;

import java.util.Set;

public final class MusicSourcesTest {
    public static void main(String[] args) {
        String muzio = "com.shaiban.audioplayer.mplayer", youtube = "com.google.android.youtube";
        Object[][] cases = {
            {MusicSources.DEFAULTS, muzio, false, true},
            {MusicSources.DEFAULTS, youtube, false, false},
            {MusicSources.DEFAULTS, "com.google.android.apps.youtube.music", false, true},
            {MusicSources.DEFAULTS, "org.schabi.newpipe", false, false},
            {MusicSources.DEFAULTS, "app.revanced.android.youtube", false, false},
            {MusicSources.DEFAULTS, "com.android.chrome", false, false},
            {MusicSources.DEFAULTS, muzio, true, false},
            {Set.of(), muzio, false, false},
            {Set.of("my.custom.player"), "my.custom.player", false, true},
            {Set.of(youtube), youtube, true, false},
            {Set.of(youtube), youtube, false, true},
            {MusicSources.DEFAULTS, null, false, false}
        };
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked") Set<String> selected = (Set<String>) c[0];
            if (MusicSources.accepts(selected, (String) c[1], (boolean) c[2]) != (boolean) c[3])
                throw new AssertionError("Unexpected source eligibility: " + c[1]);
        }
        System.out.println("PASS: " + cases.length + " music/video source cases");
    }
}
