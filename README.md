# Punchy

A personal Android app for Jaycee that shows existing downloads as progress around the camera cutout, with an app switch and Quick Settings toggle.

Target: **HONOR X7c (ALT-LX2), MagicOS 8.0, Android 14, 720 × 1610**. Android 14 minimum. No root or Xposed. Personal sideloaded APK.

## Download

[Download Punchy 0.1.1 APK](https://github.com/justinebacurin1927/Punchy/releases/download/v0.1.1/Punchy-0.1.1-debug.apk) · [Release page](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.1) · [Install guide](docs/INSTALL.md)

This is a private repository. Sign in to the owning GitHub account on your phone to access the download. Version 0.1.1 responds to the first HONOR preview test with a black center, automatic music capsule and expandable details. Install over the existing app to retain calibration. Updated fit and popup need another physical HONOR check.

[Version 0.1.1 verification](docs/verification/v0.1.1/summary.md): build/lint, 25 core state checks, 43 Android 14 flow checks and the 0.1.0 → 0.1.1 settings-preservation test pass.

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

- Reads progress notifications from selected browser/download apps; use **Download apps** to change the selection.
- A determinate download draws a progress border. Unknown totals use a rotating segment; disabled system animations keep it static.
- Multiple determinate downloads use the mean of their percentages. Different apps' progress units cannot safely be treated as comparable byte counts.
- Only explicit 100% progress triggers the completion hold. Notification removal alone is never treated as success.
- Off removes the overlay immediately. Idle has no overlay and no animation loop. Screen off, lock screen, and landscape hide it.
- Normal indicators have an opaque black center. Music automatically uses an independent wide capsule profile, with a clear center to retain native artwork/waveforms. Active and paused music sessions are included. Existing X/Y and border thickness apply to both profiles.
- Tap the indicator for filename, progress and **Open app**. During music, the card also shows artwork when available, title/artist and previous/play-pause/next controls. Close with X, Back or an outside tap. The touch target is at least 48dp; touches outside it work normally.
- Detection uses public media sessions through existing notification access. It cannot read HONOR's private capsule bounds; adjust **Music capsule fit** once if needed. Timer/call capsules are outside this detection path.
- Apps that publish no readable progress notification cannot be tracked. This does not inspect hidden download databases or use private Android APIs.
- Jaycee confirmed preview rendering, X/Y calibration and border thickness on his HONOR. Updated capsule fit, popup, real source-app downloads and background survival need another phone test.

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
python3 scripts/smoke-emulator.py emulator-5556
```

This script checks the emulator identity before changing any settings, clears only its Punchy fixture/app data, and writes v0.1.1 results/screenshots under `docs/verification/v0.1.1/`. It refuses to run against a physical phone. Physical HONOR testing remains a separate step.
