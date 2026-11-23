# Punchy 0.1.1 verification — 2026-11-23

APK: `dist/Punchy-0.1.1-debug.apk` (150,098 bytes), SHA-256 `54851b21466fb3d43246886dae714652bdeb7f1401fa76155977fec0f0e09e52`.

- Build and lint pass: 0 lint errors, one intentional `OldTargetApi` warning for this Android 14 target.
- 25 core progress-state checks pass.
- All 43 Android 14 integration checks pass; details in `emulator-results.json`.
- Actual 0.1.0 → 0.1.1 installation test passes: X/Y, normal width/height and thickness values persist, and versionCode becomes 2. See `upgrade-results.json`.
- Signature verifies and the certificate matches the previously released 0.1.0 APK, supporting an in-place update.
- No network permission, screen-content retrieval or gesture capability was added.

## New behaviors verified

Screenshot pixel check confirms normal-center RGB black and a clear music center. A synthetic media session triggers a wider capsule and returns to the original circle when released. Tapping opens the download card; combined mode shows music metadata and download details. Pause/play/next/previous reach the session. Open app follows the actual notification content intent, with Android 14 sender opt-in. Previous simultaneous, unknown-total, cancellation/completion, tile, screen-off, rotation and 150%-font checks remain passing.

Actual rendered screenshots: `03-active-circle.png`, `07-download-card.png`, `08-music-capsule.png`, `09-combined-card.png` plus the remaining flow renders. These were inspected against the intended black native-style card and retained references.

## Physical phone evidence and limits

Jaycee confirmed 0.1.0 preview drawing, X/Y adjustment and border thickness on his HONOR X7c. His supplied images demonstrated the missing black center and circle/music-capsule clash. They are retained locally in ignored `docs/design/local-feedback/`, excluded from GitHub.

Version 0.1.1 uses public active/paused media sessions; it cannot read private HONOR capsule bounds or timer/call capsule states. The capsule's independent dimensions are initial adjustable estimates. Updated fit, popup behavior with Muzio, actual source downloads and MagicOS background survival still need a physical-phone check. Synthetic fixture APKs/probe are separate from the delivered app.
