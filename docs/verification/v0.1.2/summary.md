# Punchy 0.1.2 verification — 2026-11-23

APK: `dist/Punchy-0.1.2-debug.apk` (184,886 bytes). SHA-256 `4919d7aa776a061858355dbd50eeed53120e53e897e4758e9132fb08395bc8bc`.

- Build and Android Lint pass, with no errors. Two warnings: the intentional Android-14 target (`OldTargetApi`) and `SwitchIntDef` for the music-state switch, whose default returns false for non-playing states.
- 25 core progress-state checks and 14 notification percentage-parser cases pass.
- All 80 Android 14 integration checks pass; see `emulator-results.json`. The host runner received SIGTERM after 50 checks. The remaining 30 continued in a second process after asserting the unchanged combined-card checkpoint on the same emulator; this was not a reported app crash.
- Actual 0.1.1 → 0.1.2 installation preserves X/Y, normal width/height, thickness, independent music dimensions and an explicitly saved source filter. Version code becomes 3. See `upgrade-results.json`.
- APK signature verifies and certificate matches the released 0.1.1 APK. The staged APK is byte-identical to the tested build.
- No new network permission, screen-content retrieval or gesture capability.

## Notification-format and playback evidence

Separate fixture APKs publish text-only progress, custom RemoteViews progress bars, Long byte counters, malformed numeric extras with valid text, summary-only progress and grouped children through Android's real notification service. Each supported format attaches the overlay, updates live and returns to idle on explicit completion/removal. Ordinary, media and battery percentages are rejected. Group summary and child progress are not counted twice. Explicit app filters still work; diagnostics explain exclusions, and Allow all progress sources rescans an existing notification.

Diagnostics identify notification delivery and progress format without including filenames or message bodies. `10-detection-report.png` was inspected. A paused session remains available to an open combined card, while closing that card restores the normal black-filled indicator; reopening during pause shows only downloads. Resuming music returns to the wide clear profile. Previous popup, notification action, media transport, concurrent progress, unknown total, Quick Settings, rotation, screen-off, preview cancellation and 150% text checks remain passing. New combined-card render was inspected.

## Physical phone status

Jaycee confirms normal black fill and preview calibration work on his HONOR. He reports v0.1.1 does not show actual Play Store or video-downloader progress, although both have progress notifications in the shade. Paused music also left an unwanted wide preview.

The exact data delivered by MagicOS for those notifications is still unknown. These synthetic-format tests do not prove that the physical-phone failure is repaired. Install 0.1.2 over the existing app and retest a real download. If missing, use Download detection → Allow all progress sources → Refresh notifications → Copy detection report while the download runs. Native HONOR capsule geometry and background survival remain physical-device checks. Personal screenshots stay local and ignored.
