package dev.jaycee.punchy;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class PunchyTile extends TileService {
    @Override public void onStartListening() { super.onStartListening(); update(); }
    @Override public void onClick() {
        super.onClick();
        if (isLocked()) unlockAndRun(this::toggle); else toggle();
    }
    private void toggle() {
        if (Prefs.enabled(this)) PunchyRuntime.setEnabled(this, false);
        else if (PunchyRuntime.ready()) PunchyRuntime.setEnabled(this, true);
        else openSetup();
        update();
    }
    private void openSetup() {
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
    private void update() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean enabled = Prefs.enabled(this);
        tile.setState(enabled && PunchyRuntime.ready() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Punchy");
        tile.setSubtitle(!PunchyRuntime.ready() ? "Open setup" : enabled ? "On" : "Off");
        tile.updateTile();
    }
}
