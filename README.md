# Punchy

A personal Android app for Jaycee that shows existing downloads as progress around the camera cutout, with an app switch and Quick Settings toggle.

Target: **HONOR X7c (ALT-LX2), MagicOS 8.0, Android 14, 720 × 1610**. Android 14 minimum. No root or Xposed. Personal sideloaded APK.

## Download

[Download Punchy 0.1.3 APK](https://github.com/justinebacurin1927/Punchy/releases/download/v0.1.3/Punchy-0.1.3-debug.apk) · [Release page](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.3) · [Install guide](docs/INSTALL.md)

This is a private repository. Sign in to its owning GitHub account to download. Install over the existing app to retain calibration. Version 0.1.2’s download behavior was confirmed working on Jaycee’s HONOR; 0.1.3 refreshes the interface with ROSS, a black/neon launcher icon using the same approved symbol, and a compact frosted-glass expanded island.

[Version 0.1.3 verification](docs/verification/v0.1.3/summary.md) records the actual renders, Android 14 interaction and glass/fallback checks, and settings-preserving upgrade. The new appearance and hardware blur still need a HONOR visual check.

## Reference

[hxreborn/punch-hole-download-progress](https://github.com/hxreborn/punch-hole-download-progress), inspected at `3d7a185d40cbd9644cd5ccd9cfb0a3df245b2399` on 2026-11-23. The unmodified shallow clone is in `references/punch-hole-download-progress/` (excluded from this project's Git tracking). Its license is GPL-3.0. Punchy's app implementation is separate; the upstream project is a behavioral and architectural reference.

The reference hooks SystemUI through Xposed. Punchy instead reads Android notification progress through `NotificationListenerService` and draws using an accessibility overlay. It is a separate overlay; integration with HONOR's native Magic Capsule has not been verified. It does not download files itself.

## Setup on the phone

1. Install the APK and open Punchy.
2. Allow **Notification access** for Punchy.
3. Enable **Punchy progress ring** in Accessibility settings.
4. Enable Punchy, tap **Preview on phone**, and adjust alignment if needed.
5. Add the **Punchy** tile to Quick Settings for one-tap control.

If Android blocks the two access settings for a sideloaded APK, open Settings → Apps → Punchy → the overflow menu → Allow restricted settings, then return to setup. MagicOS wording may differ. If the service is stopped in the background, check Punchy's app launch/battery settings.

The overlay service requests no screen-content or gesture access. The app has no internet, storage, camera, microphone, root, analytics, account, or advertising dependency.

## Behavior and limits

- Includes all readable progress sources by default; an explicitly saved app selection is respected. Use **Download apps** to change the selection. Reads standard numeric fields, custom notification progress bars and percentages in ongoing/task notifications. Summary-only progress is supported without counting a readable child twice.
- A determinate download draws a progress border. Unknown totals use a rotating segment; disabled system animations keep it static.
- Multiple determinate downloads use the mean of their percentages. Different apps' progress units cannot safely be treated as comparable byte counts.
- Only explicit 100% progress triggers the completion hold. Notification removal alone is never treated as success.
- Off removes the overlay immediately. Idle has no overlay and no animation loop. Screen off, lock screen, and landscape hide it.
- Normal indicators have an opaque black center. Music automatically uses an independent wide capsule profile, with a clear center to retain native artwork/waveforms. Playback selects the wide profile; pausing restores the normal shape. An already-open combined card retains paused playback controls. Existing X/Y and border thickness apply to both profiles.
- Tap the indicator for a compact glass card with filename, progress and **Open app**. Native cross-window blur softens the actual background when supported; a darker translucent surface keeps text readable when blur is disabled. No screen image is captured. During music, the card also shows artwork when available, title/artist and previous/play-pause/next controls. Close with X, Back or an outside tap. The touch target is at least 48dp; touches outside it work normally.
- **Music capsule apps** limits the wide profile to chosen players. Muzio, Spotify and YouTube Music are preselected; ordinary video/browser apps stay unselected. Explicit movie-content sessions are excluded. This source selection is independent of download tracking.
- Detection uses public media sessions through existing notification access. It cannot read HONOR's private capsule bounds; adjust **Music capsule fit** once if needed. Timer/call capsules are outside this detection path.
- Apps that publish no readable progress notification cannot be tracked. This does not inspect hidden download databases or use private Android APIs.
- Jaycee confirmed the download indicator works seamlessly after v0.1.2, along with earlier preview/alignment/black-center acceptance. Version 0.1.3 changes presentation; its appearance and native hardware blur need a new HONOR visual check. Long-duration background behavior remains unmeasured. Use **Download detection → Refresh notifications** during a download; its copyable report excludes filenames and message bodies and is never uploaded automatically.

## Build

JDK 17+ (JDK 21 works), Android SDK platform 35, Build Tools 34.0.0. Set `sdk.dir` in untracked `local.properties`, then:

```sh
./gradlew :app:assembleDebug :app:lintDebug
./scripts/test-core.sh
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. See `docs/research.md` for inspected source and rationale. Canonical design decisions are maintained in Jaycee's local design notebook; actual emulator renders and validation are in `docs/verification/`.

The local SDK path is configured in untracked `local.properties`. The official Gradle 8.10.2 wrapper checks its distribution checksum. The app has no third-party runtime dependencies.

## Android integration checks

`tools/test-source` and `tools/ui-probe` are separate emulator-only APKs; neither is bundled in Punchy. The source posts synthetic progress notifications. The UI probe uses `UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, so inspection does not pause the overlay being tested.

On the isolated AVD **Punchy_API_34**, Android 14, at **emulator-5556**:

```sh
./gradlew :test-source:assembleDebug :ui-probe:assembleDebug
python3 scripts/smoke-emulator.py emulator-5556 flows
python3 scripts/test-media-emulator.py
python3 scripts/test-glass-emulator.py
python3 scripts/test-upgrade.py
```

This script checks the emulator identity before changing any settings, clears only its Punchy fixture/app data, and writes the UI-flow results/screenshots under `docs/verification/v0.1.3/`. It refuses to run against a physical phone. Physical HONOR testing remains a separate step.

The optional `formats` suite covers the v0.1.2 notification-format cases separately from UI flows, keeping each run short. The accepted notification parser, download-source filters and calibration keys are unchanged in 0.1.3. Music detection now uses selected music apps, so ordinary video/browser sessions cannot force the wide shape. Muzio, Spotify and YouTube Music are preselected; choose other players under Music capsule apps. All fixture APKs are separate from the shipped app.
