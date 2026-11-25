# Accessibility pause and project transparency

Jaycee requested a real turn-off control before opening banking apps, and evidence on GitHub explaining Punchy's legitimacy. A supplied banking-app warning cites unofficial-store installation with Accessibility enabled. No banking-app detection code has been inspected; the app cannot certify its own acceptance.

## Flow and layout

Native mobile route: retain the current black/neon screen and the shared 52dp minimum, wrapping 15sp button. Put **Turn off Accessibility** immediately below the main status, ahead of the preview. A nearby 13sp explanation states the consequence, including notification access remaining enabled. This applies the existing diary's grouping, alignment, type scaling and press/reduced-motion rules. No unrelated gallery aesthetic or dependency is introduced. The router's `ui-mobile-patterns` companion remains unavailable; existing native controls and Android guidance supply the scoped fallback.

Flow: active → tap Turn off Accessibility → remove ring and disable the actual Android service → observe **Accessibility off · Ring paused**. The reverse button is **Enable Accessibility** and opens Android's standard Accessibility settings; enable Punchy progress ring there. Only the user can enable it there. Return to Punchy and turn on Enable Punchy; never resume silently. Geometry and source choices survive. The ordinary main switch and existing Quick Settings tile remain display toggles and are documented separately. Service-specific Settings constants were unavailable in the public compile-SDK API, so use the documented general settings action instead of relying on hidden OEM-dependent extras.

Jaycee subsequently requested the same disable function in the Dynamic Island. The expanded download/music card gets a secondary full-width **Turn off Accessibility** action below Open app with a brief consequence label. Tap ring → expand → disable the same service → close the card. Preserve download/music actions, the glass styling and scrollable larger-text recovery. The tiny collapsed ring remains the entry target rather than gaining an ambiguous tap/long-press shortcut.

Disconnected/error path: if Android still lists the service as enabled but no connected service can call the API, open settings and tell the user to switch it off there. Never report access off solely because the window disappeared or a preference changed. Observe Android's services-state callback while the activity is resumed; avoid polling or bank-app monitoring. Self-disable only affects Punchy's own service.

## Evidence ledger

| Source and scope | Strength and limit | Decision and falsifier |
| --- | --- | --- |
| [Android accessible controls](https://developer.android.com/guide/topics/ui/accessibility/apps): platform guidance on labels, scalable text and 48dp targets. | Strong platform guidance; no task-specific user study. | Full-width labeled control, wrap-content height and text scaling. Reject if the actual control clips or becomes unreachable at 150% text. |
| [W3C status messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html): persistent, programmatic feedback without forced focus changes. | Standards evidence, applied by analogy to native Android. A toast or changed preference alone does not establish the system state. | Existing polite status live region plus a system-enabled-list check. Reject if UI says off while the Android grant remains enabled. |
| [Android disableSelf](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#disableSelf()): disables the service and changes its Android setting. | Strong API contract; actual bank and HONOR acceptance are separate. | Use the public self-disable API with local cleanup, no secure-settings permission or automatic reenable. Verify Android grant, binding and overlay separately. |

## Inspected open-source approaches

- [Termux X11 KeyInterceptor](https://github.com/termux/termux-x11/blob/24ce2076fe68eff1599da286c5c162f7693c0501/lorie/src/main/java/com/termux/x11/utils/KeyInterceptor.java): `shutdown` uses `disableSelf` and clears its own state. Its separate privileged automatic-launch path writes secure settings; that path does not fit Punchy's permissions and is excluded.
- [Key Mapper controller](https://github.com/keymapperorg/KeyMapper/blob/8073a40dd548c29f70105022224a6ed1db4a18ae/base/src/main/java/io/github/sds100/keymapper/base/system/accessibility/BaseAccessibilityServiceController.kt): a DisableService event invokes the bound service's `disableSelf`. Its event/controller framework would add unnecessary machinery to this single-process app.

Use Android's small public API directly in the existing service; no source or runtime library is copied from either reference.

## GitHub evidence

Publish readable permission/privacy documentation, known limits, a security reporting path, exact release SHA-256 and APK signer certificate fingerprint, and source/test evidence. Explicitly disclose development/debug signing and lack of independent audit or bank certification. Checksums demonstrate matching bytes; signatures demonstrate signing-key continuity. Neither proves harmlessness or bank approval. No fabricated certification, trust badge, malware-scan result or bypass is added.

Validate actual disable while active and while the ordinary switch is already off, process/restart persistence, explicit settings recovery, unchanged saved calibration/source choices, another service remaining enabled, text scaling, signature continuity and fresh GitHub asset equality. A synthetic emulator is not a successful MariBank test.


## Test harness corrections

The AVD's simulated camera hole rejects a tap at the exact physical camera center. Use the ring's lower touch-target area and assert the actual foreground DownloadPanelActivity/heading, since the main screen now contains a button with the same label. Read `topResumedActivity` rather than assuming an older `mResumedActivity` dump format.

Initial emulator runs also produced System UI and process-restart ANRs. The app trace waits in HardwareRenderer/RenderProxy while RenderThread is blocked in the emulator OpenGL pipe during glCreateProgram; this is renderer evidence, not an identified Java permission-logic failure. Restart the owned AVD with a current supported software-rendering mode and rerun all final checks. No ANR is silently dismissed by the test suite.


The graphics stalls recurred with the Intel host renderer during fresh installation/data-clear setup; changing renderers alone did not establish a fix. Final focused testing therefore verifies installed APK bytes against the final build, resets Punchy's preferences/fixture processes while retaining Android rendering caches, records the preexisting ANR header, and checks for new ANRs during the installed-app flows. This does not establish fresh-install performance or HONOR/bank acceptance. The final report distinguishes those limits from successful self-disable/grant checks.

The local verification job runs as a transient user service so chat-server restarts do not discard its stdout/stderr or stop the check partway through. Its files remain local; only the completed synthetic test report/screenshots are eligible for publication.
