# NoSleep!

NoSleep! is a small Android TV utility that keeps the TV screen awake only while selected apps are in the foreground.

It was built for cases where a video app keeps playing but Android TV still starts the screensaver or goes to sleep.

## Features

- Android TV first interface with remote-friendly focus states.
- English and Russian localization, selected automatically from the TV language.
- Per-app settings for keeping the screen awake and softly unloading apps from memory after they stay in the background.
- Background unload intervals: 15 minutes, 30 minutes, 1 hour, 2 hours, 4 hours, or 8 hours.
- Accessibility service foreground-app detection.
- Invisible keep-awake overlay plus screen wake lock while protected apps are active.
- In-app setup checks for required permissions.
- GitHub Releases update check with APK download/install prompt.

## Install

Install the release APK manually on Android TV. On first launch, NoSleep! will show only the setup items that are still missing:

- enable the NoSleep accessibility service;
- allow display over other apps.

After that, open an app from the list and choose whether it should keep the screen awake, unload from the background, or both.

Background unload uses Android's normal `killBackgroundProcesses` API. It is a soft memory unload, not a root-level force stop, so Android may restart an app if that app schedules its own background work.

## Updates

The app checks `chassehiboux/nosleep-tv` GitHub Releases when the NoSleep! window becomes active and also from the manual update button.

Release APKs must be signed with the same signing key as the already installed app, otherwise Android will reject the update.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew assembleRelease
```

The signed APK is produced at:

```text
app/build/outputs/apk/release/app-release.apk
```

Signing files are intentionally ignored by git:

- `signing/nosleep-release.jks`
- `keystore.properties`
