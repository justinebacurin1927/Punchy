# Install Punchy on your HONOR X7c

1. On your phone, open the [Punchy 0.1.5 release](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.5). Under **Assets**, download **Punchy-0.1.5-debug.apk** and install it over the existing app to retain your settings. You can also copy the local `dist/Punchy-0.1.5-debug.apk` file to your phone.
2. Open **Punchy**. Under **Setup**, tap **Allow notification access** and enable Punchy in Android's settings.
3. Return to Punchy. Tap **Enable overlay service**. In Accessibility settings, find **Punchy progress ring** and enable it.
4. If Android blocks either setting, open **Settings → Apps → Punchy**, use the menu at the top and choose **Allow restricted settings**. Then return to setup. MagicOS may use slightly different wording.
5. Turn on **Enable Punchy**. Tap **Preview on phone · 6 seconds**.
6. Use **Adjust shape & alignment** for normal position, size and thickness. Start music in Muzio or another selected music player: Punchy changes to a capsule. Choose players under Download apps → Music capsule apps. Under **Music capsule fit**, adjust its independent width and height; the usual X/Y and thickness apply to both shapes.
7. Under **Setup → More setup options**, tap **Add Quick Settings tile**, or edit your Quick Settings panel and add **Punchy**. That tile switches the indicator on and off.
8. Start an actual download in Play Store, your browser or video downloader. **All progress notifications** is enabled by default unless you previously saved a filter. To recover an excluded source, open **Download detection** and tap **Allow all progress sources**.

9. Tap the indicator for filename, progress and **Open app**. During playback, the card also shows music details and previous/play-pause/next. Pausing restores the normal collapsed shape; controls stay available if the combined card is already open. Close with X, Back, or a tap outside the card.

Punchy reads existing progress notifications; the source app's download notifications must be enabled. The ring hides when idle, off, locked, screen off, or in landscape. The normal center is black; the music outline stays clear inside. This is a separate overlay with an interactive touch target around the indicator.

This is a personal debug update. Public media sessions cannot expose native HONOR capsule geometry, so new fit, popup behavior and background survival require a further phone test. If MagicOS stops the service, inspect Punchy's app launch/battery settings. Disable the Punchy switch at any time to remove the ring; the overlay service and notification access can also be disabled in Android settings.

## If a download doesn’t appear

Keep the download running and open **Download detection**. Tap **Refresh notifications**.

Punchy will tell you whether it is ready, whether it found a download, or which setting needs attention. Follow the short message on the screen. Tap **Allow all progress sources** if the downloading app was left out.

If you still need help, tap **Copy detection report** and share it when asked. You do not need to understand that report; it contains extra details for troubleshooting and leaves out message text and filenames.

## Glass island and icon

The expanded download/music card now uses frosted dark glass, compact vector controls and a neon action. Android supplies actual background blur when the device and current power mode allow it. Punchy automatically uses a darker translucent surface if blur is unavailable, without requesting screen capture. The normal camera center remains black, and the music ring remains clear inside.

The launcher keeps Punchy’s original progress/download-arrow symbol, now neon lime on black. The app has compact setup and source rows; **More setup options** contains the restricted-settings help and Quick Settings tile action.

## Video playback and music shape

YouTube/video/browser media sessions no longer automatically select the wide music profile. **Download apps → Music capsule apps** controls which players can do so; Muzio, Spotify and YouTube Music are preselected. Leave video apps unselected, and select another music player only if you want Punchy to use its wide profile during playback. Movie-content sessions are excluded even from selected apps. This choice affects only the music shape and controls; download progress remains tracked through its separate source filter.

## Choosing apps

The download-app and music-app menus share the new rounded design. Search for an app, tap its row to select it, then tap **Save**. Your choices stay selected while you search for another app. **Cancel**, Back, or a tap outside closes the menu without changing your settings.


## Before using a banking app

1. In Punchy, tap **Turn off Accessibility** below the main status. Or tap an active ring and use **Turn off Accessibility** in the expanded download/music card.
2. The ring and card close. Punchy's main screen reports **Accessibility off · Ring paused** once Android confirms the service is off. If it instead says Accessibility is still on, follow the button to Android settings and switch off Punchy progress ring there.
3. Open your banking app. If the warning remains, follow that bank's instructions; this is not a promise of bank acceptance. Notification access is separate and stays enabled. You can revoke that too in Android settings if needed.
4. After banking, open Punchy → **Enable Accessibility**. In Android settings enable **Punchy progress ring**, return to Punchy, and turn on **Enable Punchy**. Your calibration and app choices are kept. Punchy cannot reenable Accessibility automatically.

The ordinary Enable Punchy switch and existing Quick Settings tile only stop the display. Use the specifically labeled Turn off Accessibility control when the bank asks for that access to be disabled.

See [Privacy and permissions](../PRIVACY.md), [Security policy](../SECURITY.md), and [APK verification](VERIFY.md) for inspectable project information.
