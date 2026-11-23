# Punchy 0.1.3 verification — 2026-11-23

APK: `dist/Punchy-0.1.3-debug.apk` (222,850 bytes), SHA-256 `e596585a67afad9de90e15d58e9805ebcefe32f91e1547e7007daa9b2221a21f`.

- Build and Android Lint pass, 0 errors. Advisories: intentional Android14 target, defaulted non-playing switch cases, and standard adaptive-icon v26 resource qualifier. A qualifier cleanup exposed an incremental AAPT link failure; restoring the known-working qualifier resolved it before validation.
- 51 core checks pass: 25 progress state, 14 percentage parser, 12 music/video source policy cases.
- 83 Android14 checks pass on the final APK: 49 UI/download/music flows, 15 music/video-source regressions, 19 glass/accessibility checks. Reports identify the exact final APK hash.
- Real 0.1.2→0.1.3 installation preserves X/Y, normal/music width and height, thickness and an explicit download-source filter; version code becomes4.
- Signature verifies; certificate matches the released0.1.2 APK. Staged binary equals tested build.
- Approved launcher icon path is unchanged; black adaptive background and acid-lime foreground replace the white presentation. Monochrome system asset remains unchanged.
- No new runtime permissions, screen-content retrieval, gestures or network permission. Fixture/probe APKs are separate from the shipped app.

## Actual visual and interaction checks

ROSS implemented app screens/icon, then inspected Main, alignment, normal/combined glass, disabled-blur fallback, white backdrop and150% text renders. His final refinement made the ON-thumb neon for state clarity. SARA integrated the compact floating island and public Android cross-window blur, and checked final source-selection/glass renders. Text and controls stay crisp while actual underlying content is softened inside the rounded window. A runtime blur toggle visibly changes the surface; unsupported/disabled blur has a darker translucent fallback. Sampled secondary-text contrast over white is 4.69:1. This is sampled pixel evidence, not a claim of exhaustive contrast over every possible image.

Close/Open app targets measure at least48dp. Close, outside tap, Back, media actions, font scaling and animation-disabled operation pass. Setup disclosure, individual app selection and detection recovery remain reachable. See `11-glass-download.png`, `12-glass-fallback.png`, `13-glass-music.png`, `14-glass-large-text.png`, `16-glass-white-backdrop.png`, `18-final-main.png`, `19-music-source-settings.png`, `20-music-source-chooser.png` and other flow renders.

## YouTube/video correction

The old monitor accepted all playing media sessions, so ordinary video/PiP playback could force a wide clear outline without a native music capsule. Only selected music apps now drive that profile; defaults are Muzio, Spotify and YouTube Music. Unknown/video/browser apps are unselected, and explicit movie-content sessions are excluded even from selected apps. The source list is editable independently of download filtering and geometry.

Two separate fixture app processes verify that an unknown-content-type video session leaves the normal shape and download-only card, cannot displace a selected music player, and cannot keep the wide shape after music pauses. Deselect/reselect changes existing sessions immediately. Explicit movie content is rejected and subsequent music recovers; download updates/cancellation remain correct. Source-policy cases separately cover ordinary YouTube, YouTube Music, video clients, browsers and user overrides. These are synthetic media-session tests, not an actual YouTube/HONOR run.

## Harness and physical boundaries

The final UI run continued its remaining11 checks from an asserted unchanged Main checkpoint after an injected tap was consumed during Quick Settings collapse. The harness now waits/retries that navigation, handles incomplete closing-window dumps and uses native uppercase SAVE. All music/video and glass checks completed in their own full final-build runs. No app code changed during these tests. Emulator was stopped afterwards.

Jaycee confirmed0.1.2 download behavior works on his HONOR, then supplied real expanded-card screenshots and the video-triggering bug. New0.1.3 appearance, actual native blur availability and the YouTube fix still need his physical retest. Media sessions do not expose HONOR’s private capsule bounds; selected-player detection is a controllable heuristic. Long-duration background survival remains unmeasured. Personal screenshots stay local/ignored and are excluded from publication.
