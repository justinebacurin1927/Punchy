<p align="center">
  <img src="docs/assets/punchy-logo.svg" alt="Punchy logo" width="112" height="112">
</p>

<h1 align="center">Punchy</h1>

<p align="center">Your downloads, around your camera.</p>

<p align="center">
  <a href="https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.4"><strong>Download the latest APK</strong></a>
</p>

Punchy puts a download progress ring around your phone’s camera. Tap it to see your download, or view your music and download together in a glass card.

Made for Jaycee’s **HONOR X7c · Android 14 / MagicOS 8**. No root or Xposed needed.

## Get started

1. Sign in to GitHub and open the [latest release](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.4).
2. Under **Assets**, download **Punchy-0.1.4-debug.apk** and install it. If you already have Punchy, install over it to keep your settings.
3. Open Punchy and allow the two settings under **Setup**.
4. Turn on **Enable Punchy**, then try **Preview on phone**.
5. Use **Adjust shape & alignment** to fit the ring around your camera.

This repository is private. Use the GitHub account that owns it to download the APK.

## Make it yours

- **Download apps:** include every app, or choose the ones you want.
- **Music capsule apps:** choose music players for the wide ring. Muzio, Spotify and YouTube Music are included by default. Video apps are left out.
- **Quick Settings:** open **More setup options → Add Quick Settings tile** for an easy on/off switch.

<img src="docs/verification/v0.1.3/13-glass-music.png" alt="Punchy’s expanded glass card showing music controls and download progress" width="280">

The glass uses your phone’s background blur when available. If your phone turns blur off, Punchy uses a darker background to keep the text readable.

## If a download doesn’t appear

Make sure the downloading app shows progress in your phone’s notifications. Then open **Download detection** in Punchy and tap **Refresh notifications**.

Punchy shows a short status and the next step to try. If you need help, tap **Copy detection report** and share it when asked. The copied report leaves out message text and filenames.

[More setup help](docs/INSTALL.md)

## What’s new in 0.1.4

- Rounded app choosers with app icons, search and clear Save/Cancel buttons.
- Easier explanations and a short download-status card.
- Technical details stay in the copied help report.
- Removed the unnecessary network note from the main screen.

The glass island, black/neon logo, music-only shape and your saved settings are kept.

## About the project

Punchy reads download notifications from other apps; it does not download files itself. It is a separate camera-area display and cannot read HONOR’s private capsule size. The ring hides when idle, locked or in landscape.

The original idea came from [hxreborn/punch-hole-download-progress](https://github.com/hxreborn/punch-hole-download-progress). That project uses Xposed; Punchy uses Android’s notification access and a drawing-only accessibility service. [Reference notes](docs/research.md).

The app has no ads or analytics. Its accessibility service does not read your screen or control taps.

<details>
<summary>Build and verification</summary>

Native Java Android app, minimum Android 14. JDK 17+, Android SDK 35 and Build Tools 34.0.0. Configure your local SDK in untracked `local.properties`, then run:

```sh
./gradlew :app:assembleDebug :app:lintDebug
./scripts/test-core.sh
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

[Latest verification](docs/verification/v0.1.4/summary.md) · [Design notes](docs/design/punchy-glass-direction.md)

Emulator-only fixtures are separate from the installed Punchy app. [Test scripts](scripts/) refuse to run against a physical phone.

</details>
