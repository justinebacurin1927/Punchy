# Install Punchy on your HONOR X7c

1. On your phone, sign in to GitHub and open the [Punchy 0.1.0 release](https://github.com/justinebacurin1927/Punchy/releases/tag/v0.1.0). Under **Assets**, download **Punchy-0.1.0-debug.apk** and open it to install. The repository is private; use its owning account. You can also copy the local `dist/Punchy-0.1.0-debug.apk` file to your phone.
2. Open **Punchy**. Under **Setup**, tap **Allow notification access** and enable Punchy in Android's settings.
3. Return to Punchy. Tap **Enable overlay service**. In Accessibility settings, find **Punchy progress ring** and enable it.
4. If Android blocks either setting, open **Settings → Apps → Punchy**, use the menu at the top and choose **Allow restricted settings**. Then return to setup. MagicOS may use slightly different wording.
5. Turn on **Enable Punchy**. Tap **Preview on phone · 6 seconds**.
6. Use **Adjust shape & alignment** if the ring does not line up. Choose **Capsule** to fit a border around the music pill, then adjust width, height and position while previewing.
7. Tap **Add Quick Settings tile**, or edit your Quick Settings panel and add **Punchy**. That tile switches the indicator on and off.
8. Start a download in Brave, Chrome, or another selected download app. If needed, select that app under **Download apps**. **All progress notifications** also tracks transfers and app updates.

Punchy reads existing progress notifications; the source app's download notifications must be enabled. The ring hides when idle, off, locked, screen off, or in landscape. It is a separate transparent overlay and passes touches through.

This is the first personal debug build. Native HONOR capsule integration and MagicOS background survival have not been verified on the physical phone. If MagicOS stops the service, inspect Punchy's app launch/battery settings. Disable the Punchy switch at any time to remove the ring; the overlay service and notification access can also be disabled in Android settings.
