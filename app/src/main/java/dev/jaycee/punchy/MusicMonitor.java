package dev.jaycee.punchy;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/** Observes playback through existing notification access; never reads the screen. */
final class MusicMonitor {
    private record Binding(MediaController controller, MediaController.Callback callback) {}
    private final MediaSessionManager manager;
    private final ComponentName listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Binding> bindings = new ArrayList<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener changes = this::bind;
    private boolean registered;

    MusicMonitor(Context context) {
        manager = context.getSystemService(MediaSessionManager.class);
        listener = new ComponentName(context, DownloadListener.class);
    }
    void start() {
        if (registered || manager == null) return;
        try {
            manager.addOnActiveSessionsChangedListener(changes, listener, handler);
            registered = true;
            bind(manager.getActiveSessions(listener));
        } catch (SecurityException e) { stop(); }
    }
    void stop() {
        if (registered && manager != null) manager.removeOnActiveSessionsChangedListener(changes);
        registered = false;
        clearBindings();
        PunchyRuntime.music = null;
    }
    private void clearBindings() {
        for (Binding binding : bindings) binding.controller().unregisterCallback(binding.callback());
        bindings.clear();
    }
    private void bind(List<MediaController> controllers) {
        clearBindings();
        if (controllers != null) for (MediaController controller : controllers) {
            MediaController.Callback callback = new MediaController.Callback() {
                @Override public void onPlaybackStateChanged(PlaybackState state) { update(); }
                @Override public void onMetadataChanged(MediaMetadata metadata) { update(); }
                @Override public void onSessionDestroyed() {
                    bindings.removeIf(binding -> binding.controller() == controller);
                    controller.unregisterCallback(this);
                    update();
                }
            };
            controller.registerCallback(callback, handler);
            bindings.add(new Binding(controller, callback));
        }
        update();
    }
    private void update() {
        MediaController chosen = null;
        int best = 0;
        for (Binding binding : bindings) {
            PlaybackState state = binding.controller().getPlaybackState();
            int rank = rank(state == null ? PlaybackState.STATE_NONE : state.getState());
            if (rank > best) { best = rank; chosen = binding.controller(); }
        }
        PunchyRuntime.music = chosen;
        PunchyRuntime.publish(false);
    }
    private static int rank(int state) {
        return switch (state) {
            case PlaybackState.STATE_PLAYING -> 3;
            case PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING -> 2;
            case PlaybackState.STATE_PAUSED -> 1;
            default -> 0;
        };
    }
}
