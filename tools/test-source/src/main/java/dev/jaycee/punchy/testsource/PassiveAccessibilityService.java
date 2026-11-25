package dev.jaycee.punchy.testsource;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/** Isolated emulator fixture: verify Punchy's self-disable leaves other services alone. */
public final class PassiveAccessibilityService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
}
