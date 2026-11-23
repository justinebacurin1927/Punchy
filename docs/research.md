# Reference review — 2026-11-23

## Compared implementations

| Source | Inspected behavior | Fit for Punchy |
| --- | --- | --- |
| [Punch-hole Download Progress](https://github.com/hxreborn/punch-hole-download-progress/tree/3d7a185d40cbd9644cd5ccd9cfb0a3df245b2399) | `DownloadProgressHook` reads notification progress; `SystemUIHook` intercepts private SystemUI classes; Canvas ring renderers draw around the cutout. | Main behavioral reference. Replace Xposed plumbing; retain the concept of notification-driven progress and calibrated ring geometry. |
| [Islet](https://github.com/dylanneve1/islet/tree/0fb3bef2e36aa2d74ccb211c982f8dc2060f182a) | Notification listener, runtime cutout geometry, foreground application overlay, Quick Settings tile. Its notification converter excludes ongoing notifications. | Useful rootless lifecycle/geometry reference; its notification filter would reject downloads. Ordinary overlays sit below the system status bar. No source copied. |
| [DroidBubble-New](https://github.com/Shikateki/DroidBubble-New/tree/295f5b62293b5a2768681a7606736736c2e97f2c) | `IslandOverlayService` chooses accessibility overlay when available, otherwise application overlay; `NotificationReceiverService` reads standard progress extras including indeterminate progress. | Supports the accessibility-overlay approach. Broad media/notification features are outside Punchy's scope. No source copied. |

## Platform evidence

- [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService): public callbacks expose posted/removed notifications after the user grants access.
- [Notification progress fields](https://developer.android.com/reference/android/app/Notification#EXTRA_PROGRESS): use progress, maximum and indeterminate status. Notification removal does not prove completion.
- [Window types](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY): ordinary app overlays are below the status bar. Use a connected accessibility service's overlay to draw the camera-area border; verify actual HONOR layering.
- [Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles): native `TileService` provides an on/off tile.
- [HONOR Magic Capsule](https://www.honor.com/global/support/content/en-us15860432/): documented for preset functions. A public supported integration path for Punchy on MagicOS 8 was not established in this research. This is not proof that all private OEM interfaces are impossible.
- [AGP 8.7 compatibility](https://developer.android.com/build/releases/agp-8-7-0-release-notes): supports compile SDK 35 and requires Gradle 8.9+ / JDK 17+.

## Confirmed scope

Jaycee requested a project named Punchy and a punch-hole download indicator/toggle, supplied the upstream repository and phone screenshots, selected regular Android without root/Xposed, limited this to his own use, and confirmed using the repository as reference. In-app and Quick Settings toggles, circle/capsule choices, and calibration are implementation choices, not separately stated user preferences. Later on 2026-11-23, he explicitly requested putting the project/APK on his GitHub repository so he could download it. The selected destination is the new private `justinebacurin1927/Punchy` repository, using the locally authenticated account with ownership provenance from earlier explicit publication requests.

Phone screenshots containing hardware identifiers are not copied into the repository or memory. The initial home-screen image is retained only as a local visual reference in `docs/design/` and excluded from Git tracking.

## Music and popup revision — 2026-11-23

Jaycee confirmed preview calibration on the physical HONOR, requested a black center, automatic wide music shape, and download details on tap with music controls when music is active. Inspected the pinned [Islet MediaMonitor](https://github.com/dylanneve1/islet/blob/0fb3bef2e36aa2d74ccb211c982f8dc2060f182a/app/src/main/java/dev/dylanneve/islet/data/MediaMonitor.kt) and official [MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager) / controller callback APIs before adding the separate Java monitor. No source was copied.

The normal center is opaque black; the media outline keeps its center transparent. A media profile avoids overwriting the already accepted normal calibration. The own translucent popup adds standard transport actions and notification-content-intent navigation. Android 14 requires sender opt-in for the foreground Open app action; see [Android activity security](https://developer.android.com/guide/components/activities/secure-bal). No screen-read or gesture capability was added. Native capsule bounds and timer/call states remain outside the public media-session signal. Local phone feedback images stay in ignored `docs/design/local-feedback/`.

## Real-notification repair — 2026-11-23

Physical feedback establishes that v0.1.1 previews work while actual Play Store and video-downloader progress does not appear. Both apps show progress notifications in the shade. Their exact delivered fields remain unknown. Existing preset inclusion of `com.android.vending` means source filtering alone is insufficient as a diagnosis.

The original reference's fallback reads private `RemoteViews.mActions` fields. Punchy instead applies custom views using the public [RemoteViews.apply API](https://developer.android.com/reference/android/widget/RemoteViews#apply(android.content.Context,android.view.ViewGroup)) and inspects visible public ProgressBar values, without attaching that view or reading the screen. Standard Number fields remain first priority, followed by custom bars and guarded ongoing/task text percentages. Media/message/call notifications and common battery/volume text percentages are excluded. Summary-only progress is included; a readable child supersedes its group summary. Explicit 100% updates alone may trigger completion.

Checked [NotificationListenerService filter documentation](https://developer.android.com/reference/android/service/notification/NotificationListenerService#META_DATA_DEFAULT_FILTER_TYPES): absent filter metadata already allows all types; adding that metadata would not prove a repair. Delivery, field types/values, source filtering and overlay connection are surfaced in an on-device report instead. It excludes titles and bodies and only reaches the clipboard on a user action. No screen-content, gesture or network capability is added.

The wide shape now follows active playback rather than a retained paused controller. This corrects the implementation assumption contradicted by Jaycee's HONOR screenshot; Android media sessions still do not expose the actual native capsule bounds.
