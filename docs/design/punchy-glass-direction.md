# Punchy visual revision — 2026-11-23

User direction: Jaycee calls the v0.1.2 app screens old-looking and requests glassmorphism for the dynamic island. His correction keeps the existing progress-ring/download-arrow icon shape, changing its white treatment to a black base with neon color like ARKO. Inspected the actual local ARKO navbar logo: acid-lime above black. This authorizes a color reference, not copying ARKO’s mark.

ROSS first read the shared workflow, mobile, visual, typography, animation and icon-alignment notes plus the existing Punchy notebook. Applied user-expi → ui-design-index, native-mobile route. The `ui-mobile-patterns` companion remains absent locally; Android native controls and the primary platform guidance below supply implementation constraints. No gallery search or shared-account connector retrieval.

## Behavior and composition

Routine: read enable/status → inspect actual active progress or the explicitly labeled sample → preview or align. First-use: setup blockers stay next to the enable control and the two Android access rows. Secondary setup help/tile actions live under a labeled disclosure; source selection and detection remain one shallow section away. Working notification/media behavior and calibration keys are unchanged.

Main screen: compact icon/wordmark header; charcoal island/control card with a large enough sample to explain the feature; native labeled enable switch and persistent status; one neon primary preview button and an appearance action row. Secondary surfaces use icon-aligned settings rows, not repeated tall rectangular filled buttons. Alignment and detection share the same type, spacing and action treatment.

Popup handoff to SARA: compact 16sp heading and 48dp close target, 56dp rounded artwork, 16sp song/file identity, 13sp artist, compact transport actions, strong percent, thin rounded progress and pill Open app. Surface radius 28dp, inset20dp, light alpha border. Real Android window backdrop blur when available, darker translucent fallback when system blur is disabled. Content remains crisp; no screenshot/read-screen permission.

## Tokens and icon

- Base `#090D0E`, card `#151C1D`, raised `#1D2828`; alpha borders `#20FFFFFF`.
- Primary text `#F4F8F5`; secondary `#9CAFA8`; acid-lime accent `#D3FF5F` (OKLCH working target approximately L .94 / C .18 / h 125, rendered sRGB endpoint is authoritative); black/dark foreground on bright action fill. Related mint `#86E8C5` is subordinate.
- Native sans-serif; wordmark28sp, section18sp, body15–16sp, secondary13sp. Native `sp`, expanding row heights, no fixed-height text containers. Typeface padding trimmed only where glyphs remain safe.
- Screen inset22dp; card inset18dp, radius26dp; inner radii subtract the actual inset. Primary action radius18dp; settings rows12dp. Targets at least48dp and labels remain actual accessible text.
- Launcher: exact existing arc/down-arrow path scaled into the Android adaptive safe zone. Black background, neon-lime foreground and restrained same-path halo. Monochrome Quick Settings/status asset remains unchanged.
- Motion: .97 press scale with release/cancel/keyboard handling through native state animators; no animated loop in idle. Native system animator-disable preference suppresses extra motion. Setup disclosure uses a brief native bounds transition only on explicit expansion/collapse.

## Evidence ledger and limits

| Evidence | Strength and boundary | Design implication / verification |
| --- | --- | --- |
| [Android Views accessibility](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views) recommends 48dp interactive areas and labels; a visible glyph may be smaller than its target. | Strong platform guidance, not a measured success rate for this app. | Compact visual rows retain 48dp+ touch areas; no emoji or unlabeled icon actions. Verify actual hierarchy and taps. |
| [Android text scaling guidance](https://support.google.com/accessibility/android/answer/12159181) warns fixed-height text can crop when font size grows. | Strong implementation guidance. | Use sp and wrap-content text/rows; inspect 150% font size. |
| [Android accessibility codelab](https://developer.android.com/codelabs/starting-android-accessibility) explicitly separates scanner checks from whether people can complete common tasks. | Corrective finding: passing automated checks is insufficient evidence of usability. | Preserve setup, source recovery and preview actions; require actual renders and regression flow rather than treating a polished screenshot as functional acceptance. |
| [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) specifies layers and the protected central area. | Strong icon format guidance; actual OEM masks still require phone confirmation. | Keep the approved symbol in the safe zone, with a real black background layer and neon colored foreground. |

No new visual acceptance is implied until Jaycee tries the resulting APK. ROSS owns app screens/icon; SARA owns popup blur, packaging, tests, rendered verification and canonical note updates.

## Initial implementation checks

Owned UI diffs pass `git diff --check`. All launcher XML parses, and every foreground/monochrome mark path exactly equals the approved `ic_punchy.xml` path. Solid token contrast calculations: main text/card16.12:1, secondary text/card7.49:1, dark text/neon primary action14.69:1. These are token calculations, not measurements of the translucent popup. Actual Android build, interactive flow, rendered composition, 150% text and blur-enabled/disabled verification belong to SARA’s integration stage and are not yet claimed here.

## Video-source correction during integration

Jaycee’s later YouTube/PiP screenshots show the wide music profile without a native capsule. Added a compact Music capsule apps row in the existing source group. Only chosen music app sessions can drive the wide profile; Muzio/Spotify/YouTube Music defaults are distinct from ordinary YouTube and unknown video/browser packages. A native multi-choice list includes installed launchers and active session sources. Explicit movie-content sessions remain excluded. Choosing an app is a request to use its wide profile, not a public signal that HONOR’s native capsule is visible. Download-source selection and all normal/music geometry keys are independent.

ROSS’s completed render review found compact main/settings, normal/combined glass, disabled-blur fallback, white-backdrop and150% text layouts legible with no material fit issue observed. His final visual correction made the active toggle thumb neon so ON is clear. The additional music-source row reuses that reviewed style.
