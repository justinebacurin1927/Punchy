# Punchy 0.1.0 verification — 2026-11-23

Personal debug APK: `dist/Punchy-0.1.0-debug.apk` (110,534 bytes).

SHA-256: `46b3b335253b78a1d1b5b8db8808be98b224063b32a18f67da9c0480154a9a61`.

- `:app:assembleDebug :app:lintDebug`: **pass**. Lint: 0 errors, 1 `OldTargetApi` warning. The app deliberately targets the user's Android 14 and is not being published to an app store. Full lint output is in `lint-results.txt`.
- `scripts/test-core.sh`: **25 checks pass**. Covers simultaneous downloads, completion versus cancellation, invalid/unknown totals, notification reuse, reconnect/disable clearing, repeated completion and large counters.
- `scripts/smoke-emulator.py`: **27 checks pass** in the isolated AOSP Android 14 emulator at 720 × 1610. Full results: `emulator-results.json`.
- APK signature: verified using APK Signature Scheme v2, one signer. The staged APK matches the compiled and emulator-tested binary exactly.
- Packaged manifest: no requested `uses-permission` entries. Notification/accessibility/tile services are protected by Android binding permissions. Overlay XML explicitly sets screen-content retrieval and gesture capability to false; the bound emulator accessibility service reports capabilities 0.
- Application source has no third-party runtime dependencies. The notification producer and UI probe are separate test APKs, excluded from the delivered Punchy APK.

## Android flow covered

Setup gating and service connection; real OS progress-notification delivery from a separate synthetic source; 45% progress; simultaneous downloads; last-item completion; cancellation; indeterminate state; Quick Settings off/on and rescan; capsule preview; landscape hiding and portrait restoration; screen off/unlock; idle removal; preview cancellation by the master switch; 150% font scale; and no Punchy fatal exception during the final flow.

Access grants in the isolated emulator were made through ADB. Manual HONOR setup, its camera cutout geometry, native music capsule overlap, actual browser-specific notifications, and MagicOS background policy still need physical-phone testing. The result is a separate overlay; native Magic Capsule integration has not been established.

## Actual renders

- `01-first-use.png`: setup before access is granted.
- `02-ready.png`: enabled, waiting for a download.
- `03-active-circle.png`: 45% progress from the synthetic notification source.
- `04-indeterminate.png`: unknown total.
- `05-alignment-capsule.png`: capsule shape and calibration screen.
- `06-large-text.png`: 150% system font scale, Punchy off.

The initial home-screen reference is in `../design/honor-home-reference.png`. About phone images with device identifiers were not copied into the project or vault.
