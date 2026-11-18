# VidBrowser

A lightweight Android browser (Chromium WebView) with a built-in video player and one-tap video downloads, inspired by UC Browser.

## Features
- Chromium-based browsing (Android System WebView), search/URL bar, back/forward/home
- Automatic video detection (`.mp4`, `.m3u8`, `.webm`, `.mkv`, `<video>` tags) with a 🎬 counter
- Built-in Media3 ExoPlayer: HLS support, 0.75x–2x speed, in-player **Download** button
- Downloads saved to `Downloads/VidBrowser` via Android DownloadManager
- Basic ad-host blocking

## Get the APK
**From CI:** Actions → *Build APK* → latest run → artifact `VidBrowser-apk`.
**From Releases:** push a tag (`git tag v1.0 && git push --tags`) and the APK is attached to a GitHub Release automatically.
**Locally:** open in Android Studio → Build → Build APK(s), or `gradle assembleRelease` (Gradle 8.7, JDK 17, Android SDK 34).

## Roadmap
HLS segment downloader, tabs, bookmarks, history, incognito, blob video capture.

## Notes
Only download content you have the rights to. Release builds are signed with the debug key; use your own keystore for Play Store distribution.
