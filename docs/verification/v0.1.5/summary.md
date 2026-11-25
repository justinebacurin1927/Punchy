# Punchy 0.1.5 verification — 2026-11-25

Tested APK: `Punchy-0.1.5-debug.apk`, 261,446 bytes, SHA-256:

```text
e8f5f593c49c781506ef05ca7bc947b1fa0966b51226918b736aa0a02abca939
```

## Result

- `assembleDebug` and `lintDebug` pass: 0 errors, 3 existing advisories (`OldTargetApi`, `SwitchIntDef`, `ObsoleteSdkInt`).
- All 51 core checks pass: 25 progress-state, 14 percentage-parser and 12 music-source-policy cases.
- All 30 focused Android 14 installed-app checks pass on the isolated `Punchy_API_34` emulator at 720 × 1610 / 320dpi.
- Actual 0.1.4 → 0.1.5 installation preserves normal/music dimensions, X/Y, thickness, explicit download filter, both app-choice sets and saved display switch. Updating does not create a pause state.
- APK signing certificate matches every earlier Punchy release. No new app permission; the final APK declares no INTERNET permission. Separate test fixtures are not bundled in Punchy.

## What was observed

Main-screen and expanded-island controls remove Punchy from Android's enabled Accessibility service list and unbind it. The ring disappears and cannot reappear from subsequent progress notifications. A separate test Accessibility service stays enabled/bound; notification access remains enabled. Both download-only and combined music/download cards close after disable. The source music keeps playing.

The persistent paused state survives app process restart. Enable Accessibility opens Android settings; returning without granting does not resume. Explicit test grants model the user changing Android's service setting; Punchy then remains display-off until Enable Punchy is turned on. A plain display-off switch is separately verified to leave the Accessibility grant enabled. No bank-app monitoring or automatic grant is used.

Buttons remain reachable at 150% text; the expanded-card disable target is at least 48dp. Actual renders were inspected for hierarchy, wrapping, contrast and clipping. Final focused flow reports no fatal exception and no new app/System UI ANR.

## Evidence and limits

[Build/core report](build-results.json) · [30 Android checks](accessibility-pause-results.json) · [Upgrade report](upgrade-results.json).

Initial fresh-install/data-clear emulator attempts produced rendering ANRs. Traces included HardwareRenderer/RenderProxy waits and RenderThread work in the emulator OpenGL pipe; the exact root cause is not established. Changing software/host renderers alone did not establish a repair. The completed focused flow verifies installed APK hashes, resets Punchy's preferences and fixture processes while retaining renderer/code caches, and checks that the existing ANR record did not change. It does not establish fresh-install performance. The report retains the prior ANR header instead of claiming none ever occurred.

No physical HONOR or banking app was tested. Disabling Accessibility follows the supplied warning's offered recovery path; bank acceptance, HONOR rendering/battery behavior and bank review remain unverified. No independent security audit or malware-free certification is claimed.

## Actual synthetic renders

- [Main with active download](01-main-active.png)
- [Paused main screen](02-main-paused.png)
- [Android confirms Punchy off, companion on](03-android-settings.png)
- [Download-only island](04-download-island.png)
- [Music and download island](05-music-island.png)
- [Paused at 150% text](06-large-text-paused.png)
- [Combined island at 150% text](07-large-text-island.png)
