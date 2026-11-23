# Install Punchy on your HONOR X7c

1. On your phone, sign in to GitHub and open the [Punchy 0.1.1 release](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.1). Under **Assets**, download **Punchy-0.1.1-debug.apk** and install it over the existing app to retain your settings. The repository is private; use its owning account. You can also copy the local `dist/Punchy-0.1.1-debug.apk` file to your phone.
2. Open **Punchy**. Under **Setup**, tap **Allow notification access** and enable Punchy in Android's settings.
3. Return to Punchy. Tap **Enable overlay service**. In Accessibility settings, find **Punchy progress ring** and enable it.
4. If Android blocks either setting, open **Settings → Apps → Punchy**, use the menu at the top and choose **Allow restricted settings**. Then return to setup. MagicOS may use slightly different wording.
5. Turn on **Enable Punchy**. Tap **Preview on phone · 6 seconds**.
6. Use **Adjust shape & alignment** for normal position, size and thickness. Start music in Muzio or another player: Punchy automatically changes to a capsule. Under **Music capsule fit**, adjust its independent width and height; the usual X/Y and thickness apply to both shapes.
7. Tap **Add Quick Settings tile**, or edit your Quick Settings panel and add **Punchy**. That tile switches the indicator on and off.
8. Start a download in Brave, Chrome, or another selected download app. If needed, select that app under **Download apps**. **All progress notifications** also tracks transfers and app updates.

9. Tap the indicator for filename, progress and **Open app**. While music is active or paused, the card also shows music details and previous/play-pause/next. Close with X, Back, or a tap outside the card.

Punchy reads existing progress notifications; the source app's download notifications must be enabled. The ring hides when idle, off, locked, screen off, or in landscape. The normal center is black; the music outline stays clear inside. This is a separate overlay with an interactive touch target around the indicator.

This is a personal debug update. Public media sessions cannot expose native HONOR capsule geometry, so new fit, popup behavior and background survival require a further phone test. If MagicOS stops the service, inspect Punchy's app launch/battery settings. Disable the Punchy switch at any time to remove the ring; the overlay service and notification access can also be disabled in Android settings.
