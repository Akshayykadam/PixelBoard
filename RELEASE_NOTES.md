# PixelBoard Stable Release (v18.5.3-V1 / Patch v1.3.1)

## Summary
PixelBoard v18.5.3-V1 updates the base Google Keyboard to the official **Gboard v18.5.3 Release**, introduces **Extended Clipboard History Retention** (save copied clips up to 30 days or permanently), delivers a comprehensive **TalkBack accessibility overhaul**, adds complete **Arabic language support**, and introduces a built-in **in-app update checker** positioned conveniently right on the main Gboard settings screen.

---

## What's New in v18.5.3

- **Extended Clipboard History**: Save copied text and images for 1 hour, 24 hours, 7 days, 30 days, or permanently (Never).
- **Upgraded Base to Gboard v18.5.3**: Rebased on Google's latest stable release with smoother typing performance.
- **TalkBack Accessibility Overhaul**: Full screen reader optimization with clear labels, proper button roles, and audio cues.
- **Arabic Language Support**: Added complete native Arabic (العربية) translation across all settings and AI features.
- **Crash Fixes & Stability**: Resolved clipboard slider crashes, fixed settings layout issues, and improved battery efficiency.
- **In-App Update Checker**: Easily check for new versions and download updates directly from GitHub inside the app.
- **Emoji Recents & Frequent Toggle**: Restored the Recently Used emojis tab by default on Gboard 18.5+ with a setting to switch between Recent and Frequent emojis.

---

## Detailed Improvements

### 📋 Extended Clipboard History
  - **Customizable Retention**: Choose to keep copied items for 1 hour, 24 hours, 7 days, 30 days, or Never (permanent).
  - **Easy Access**: Configure your retention preference in native Gboard Settings > Clipboard or under PixelBoard Settings.
  - **Safe Migration**: Automatically migrates previous clipboard settings without crashes or resetting your history.

### 😊 Emoji Drawer Recents & Frequently Used Toggle
  - **Recent Emoji Tab by Default**: Restored the recently used emoji tab on Gboard 18.5+, replacing the forced frequent emoji drawer.
  - **Customizable Preference**: Switch between Recently used emojis and Frequently used emojis directly from PixelBoard Settings > Features.

### ♿ TalkBack & Accessibility Overhaul ([#65](https://github.com/Akshayykadam/PixelBoard/pull/65), [#66](https://github.com/Akshayykadam/PixelBoard/pull/66), [#67](https://github.com/Akshayykadam/PixelBoard/pull/67))
  - **Unified Row Controls**: Switches and radio options now announce as a single cohesive element instead of split stops.
  - **Heading Navigation**: Screen readers can now navigate settings by section headers on Android 9+.
  - **Accurate Preview Hints**: Test input fields now read typed characters instead of masking them.
  - **Spoken Announcements**: Audio confirmation when restarting Gboard or encountering configuration alerts.

### 🌐 Arabic Localization ([#53](https://github.com/Akshayykadam/PixelBoard/pull/53))
  - **Complete Translation**: Fully localized strings across Advanced Settings, AI Writing Tools, and Rambler dictation into Arabic (`ar`).

### 🛠️ Stability & Under-the-Hood Fixes
  - **Gboard 18.5.3 Release**: Upgraded to base build `18.5.3.993608717-release-arm64-v8a`.
  - **Slider Out-of-Bounds Fix**: Eliminated crashes caused by out-of-range slider preference values.
  - **Battery Drain Resolution**: Fixed background `GoogleAsrService` persistence to conserve battery.
  - **Autofill Compatibility**: Fixed inline password manager suggestion chips in keyboard text fields.

---

## Community & Acknowledgements

A huge thank you to the community contributors whose pull requests, testing, and feedback made this release possible:

| Contributor | Focus / Contribution |
| :--- | :--- |
| **Animesh Ahilya** ([@animeshahilya](https://github.com/animeshahilya)) | Complete TalkBack accessibility overhaul ([#65](https://github.com/Akshayykadam/PixelBoard/pull/65), [#66](https://github.com/Akshayykadam/PixelBoard/pull/66), [#67](https://github.com/Akshayykadam/PixelBoard/pull/67)) |
| **AbdulAziz Hatem** ([@AbdulAziz-Hatem](https://github.com/AbdulAziz-Hatem)) | Full Arabic (`ar`) localization for settings & AI tools ([#53](https://github.com/Akshayykadam/PixelBoard/pull/53)) |
| **Mohamed Technology** ([@mohamedtechnology](https://github.com/mohamedtechnology)) | TalkBack focus dead zone diagnosis & window focus testing ([#48](https://github.com/Akshayykadam/PixelBoard/issues/48)) |
| **Andrew Matten** ([@andrewmatten](https://github.com/andrewmatten)) | Native dictation split readiness bytecode patch ([#50](https://github.com/Akshayykadam/PixelBoard/issues/50)) |
| **Benestin** ([@Benestinlinson](https://github.com/Benestinlinson)) | Auto-correction testing & screenshot chip synchronization ([#52](https://github.com/Akshayykadam/PixelBoard/issues/52)) |
| **Paolo Del Casale** ([@PaoloDelCasale](https://github.com/PaoloDelCasale)) | BatteryStats background persistence investigation ([#3](https://github.com/Akshayykadam/PixelBoard/issues/3)) |
| **Alin Cornel** ([@alincornel](https://github.com/alincornel)) | Password manager inline autofill compatibility ([#24](https://github.com/Akshayykadam/PixelBoard/pull/24)) |
| **JasonWu** | Foundational reverse-engineering research and Smali patches |

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.1.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.1/PixelBoard-1.3.1.apk) | **Signed APK** *(Recommended)* | ~79 MB | **Ready to install.** Signed stable APK (v18.5.3 base, side-by-side coexistence). |
| [**patches-1.3.1.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.1/patches-1.3.1.mpp) | **Morphe Patch Bundle** | ~1.6 MB | Standalone patch file (v1.3.1) for Morphe Manager to patch your own base APK. |
| [**Stock Gboard v18.5.3**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only needed if patching manually)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.1.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.1/PixelBoard-1.3.1.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.1.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard [**v18.5.3.993608717-release-arm64-v8a**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) and apply patches.

---

## Compatibility

| Specification | Details |
| :--- | :--- |
| **Base App** | Google Keyboard (`com.google.android.inputmethod.latin`) |
| **Recommended Base Version** | `18.5.3.993608717-release-arm64-v8a` (APKMirror) |
| **Also Supported** | `18.4.1`, `18.3.1`, `18.0.3` |
| **Architecture** | `arm64-v8a` |
| **Minimum Android Version** | Android 10 (API 29) or higher |
| **Standalone Coexistence** | Supported (package `com.akshaykadam.pixelboard` installs alongside factory Gboard) |
