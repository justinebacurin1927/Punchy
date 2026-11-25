# Privacy and permissions

Applies to Punchy 0.1.5. Package: `dev.jaycee.punchy`.

Punchy is a personal Android download-progress display. It reads progress reported by other apps and draws a ring around the camera. It does not download files itself. This document explains the current implementation; it is not a security certification.

## Notification access

Android notification access can expose notifications from other apps, including sensitive messages. Punchy receives notification objects and holds current entries in app memory. It checks progress fields, custom notification progress views and guarded progress text, and shows a download title in the expanded card. App-source filters determine which downloads appear; they are not an Android permission boundary that stops other notifications being delivered to the listener.

The same access allows Punchy to observe active media sessions. For chosen music players, it uses available title, artist, artwork, playback state and standard playback controls. Pressing Open app sends the source notification's action or opens its app; pressing a music button sends the chosen media action. These are explicit user actions.

Punchy does not intentionally save notification content, download names or music artwork to persistent app storage or upload them. Current notification entries are held in memory, including while only the ring's display switch is off. To revoke this capability, disable Punchy's **notification access** in Android settings. Android itself may retain notifications outside Punchy.

## Accessibility access

Punchy's Accessibility service supplies an overlay that reaches the camera/status-bar area. The ring has a touch target to open Punchy's own details card.

The service configuration disables screen-content retrieval and gesture dispatch. Its runtime configuration requests zero Accessibility events and zero Accessibility flags. It does not use screen capture, read another app's screen, fill forms or perform taps/swipes in another app. Ordinary UI accessibility labels are separate from this service capability.

**Turn off Accessibility** in Punchy's main screen or expanded island calls Android's public self-disable API, stops the ring and turns off this service in Android's Accessibility settings. It leaves notification access enabled. The normal Enable Punchy switch and existing Quick Settings tile only control display; they do not revoke access.

Enabling the service again requires your action in Android settings. Punchy does not monitor which banking app you open, hide an enabled service from a bank, or automatically reenable it. Whether a bank accepts the resulting device state remains the bank's decision.

## Local settings and diagnostic sharing

Punchy saves display preferences, shape/alignment values, chosen app package names, the on/off switch and Accessibility-pause state in private SharedPreferences. Android backup/data extraction is disabled in the app configuration. Clearing Punchy's app data or uninstalling it removes those settings.

**Copy detection report** deliberately copies technical status to the Android clipboard after you tap it. The report includes package names, progress-field summaries and connection information, excluding notification titles, message bodies and filenames. Clipboard content can be exposed when you paste/share it; Android's clipboard policies apply. Punchy does not send that report to a server.

## Network and third parties

The app declares no Android INTERNET permission and includes no ads, analytics SDK, account system or automatic update client. Using GitHub, Android settings or a source app involves those services' own policies. Information you choose to submit to GitHub issues or security reports is handled by GitHub and the repository maintainers.

## Inspect the implementation

- [Manifest](app/src/main/AndroidManifest.xml)
- [Accessibility configuration](app/src/main/res/xml/ring_accessibility.xml) and [ring service](app/src/main/java/dev/jaycee/punchy/RingService.java)
- [Notification listener](app/src/main/java/dev/jaycee/punchy/DownloadListener.java) and [media monitor](app/src/main/java/dev/jaycee/punchy/MusicMonitor.java)
- [Saved settings](app/src/main/java/dev/jaycee/punchy/Prefs.java)
- [Security policy](SECURITY.md) and [APK verification](docs/VERIFY.md)
