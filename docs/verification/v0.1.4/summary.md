# Punchy 0.1.4 verification — 2026-11-24

APK: `dist/Punchy-0.1.4-debug.apk` (254,002 bytes). SHA-256 `e1ef078b2049e1a61bd9a20f1250b90538469bc5cb6663a9b208fa8689ac692a`.

- Build and lint pass: 0 errors,3 existing advisories (Android14 target, defaulted music-state switch, adaptive-icon resource qualifier).
- All27 focused Android14 chooser/status checks pass on the final APK. See `chooser-results.json`.
- Actual0.1.3→0.1.4 installation preserves X/Y, normal/music dimensions, stroke, explicit download filtering and both app-choice sets, including apps absent from the installed chooser. Version code becomes5. See `upgrade-results.json`.
- APK signature verifies and certificate matches the released0.1.3 build. Staged APK equals tested output.
- README logo SVG parses and retains the approved icon geometry. README download/setup instructions are simplified, with developer details under a disclosure.

## Changed behavior verified

Both app lists use the rounded dark/neon picker with app icons, visible checked state, search and explicit Save/Cancel. Searches retain hidden drafts and accurate selection count. Save persists the right app set; Cancel, Back and outside taps discard edits. Music/download choices remain independent; selected packages absent from the visible list survive saving. Empty searches provide recovery text. Cancel/Save targets measure at least48dp.

Normal detection shows plain ready/download/off states and a short next step. It contains no package names, field types or notification flags. An explicit clipboard copy retains the technical progress data while still excluding filenames. The network footer is absent, and setup/alignment explanations use everyday language.

Actual renders inspected: `01-music-chooser.png`, `03-simple-idle-status.png`, `04-simple-download-status.png`, `05-chooser-large-text-keyboard.png` and remaining flow captures. At150% text, Save/Cancel bounds are fully above the actual keyboard region, rather than merely present in the view tree. The menu uses the available window height, allowing its list to shrink while actions stay visible. Focused search may pan the heading; actions and search remain available. No app fatal exception or leaked dialog is recorded in the final run.

## Corrected verification findings

Early visual review found clipped button corners from horizontal-row gravity and a narrow-margin outside tap ignored by native touch slop. Those were corrected. Initial node-presence checks falsely counted covered buttons as visible when the keyboard opened. Final checks compare actual screen bounds with the keyboard region, and the flexible-height picker passes. The first upgrade run completed its installation/assertions but could not serialize sets into its JSON report; that reporting bug was fixed and the complete upgrade test rerun successfully.

## Scope and physical limits

The progress tracker/parser, media monitor/source policy, source preferences, ring service and glass popup code are unchanged from0.1.3. Their earlier51 core/83 Android checks remain historical evidence; the full unchanged suite was not repeated for this UI/copy refinement. DownloadListener adds only presentation status/timestamp accessors. This release uses focused UI, clipboard and actual upgrade checks.

The HONOR screenshot confirms0.1.3 is installed and motivated the chooser/plain-language correction. New0.1.4 menu/copy appearance still needs user review; prior actual YouTube/background acceptance has not been separately reported. No new permissions or hosted ChatGPT artifacts. Personal phone screenshots remain local/ignored.
