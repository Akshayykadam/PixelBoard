# PixelBoard Stable Release (Patch v1.3.4 / Gboard v18.6.3.100)

## Summary
PixelBoard v1.3.4 brings comprehensive clipboard retention enhancements: **unpinned clips are now kept for 24 hours by default**, the clipboard history cap is **raised from 5 to 100 recent clips**, a customizable **retention slider** is available in Settings, and **accurate retention texts & pin tips** are localized across 83 languages. In addition, the base app is now updated to **Gboard v18.6.3** with clean symbol key labels, restored Advanced Settings, and RTL/LTR layout polish.

---

## What's New in v1.3.4

- **24-Hour Default Clipboard Retention**: Unpinned copied text and images are now kept for 24 hours (up from Google's 1-hour limit) by default.
- **100 Recent Clips History**: The recent unpinned clipboard items cap is raised from 5 to 100 clips, matching the clipboard's full capacity (#129).
- **Customizable Retention Slider**: Pick between 1 hour, 24 hours, 7 days, 30 days, or Never in PixelBoard Clipboard Settings.
- **Honest Multi-Language Pin Tips**: Pin tips and empty-state texts dynamically display the selected retention duration across 83 locales.
- **Base app is now updated to 18.6.3**
- **Clean Symbol Key Labels**: Fixed `?attr/` and `\?123` labels so question mark and symbol keys render cleanly.
- **Restored Advanced & Update Settings**: Ensured Settings and Updates sections are reliably accessible across all base APK configurations.

---

## Community & Acknowledgements

Special thanks to the community contributors:

| Contributor | Focus / Contribution |
| :--- | :--- |
| **AnhDuck** ([@AnhDuck](https://github.com/AnhDuck)) | Clipboard retention slider, 24h default, honest text across 83 locales, and 100-item history cap ([#129](https://github.com/Akshayykadam/PixelBoard/pull/129)) |

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.4.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.4/PixelBoard-1.3.4.apk) | **Signed APK** *(Recommended)* | ~122 MB | **Ready to install.** Signed stable APK (v18.6.3 base, side-by-side coexistence). |
| [**patches-1.3.4.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.4/patches-1.3.4.mpp) | **Morphe Patch Bundle** | ~1.7 MB | Standalone patch file (v1.3.4) for Morphe Manager. ⚠️ **Strictly requires Gboard version 18.6.3.100** (`18.6.3.1002251336-release-arm64-v8a`). Older versions like 18.5 are unsupported. |
| [**Stock Gboard v18.6.3.100**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-6-3-1002251336-release/) | **Base APK** | ~117 MB | Official unmodified base APK from APKMirror (`18.6.3.1002251336-release-arm64-v8a`). |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.4.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.4/PixelBoard-1.3.4.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.4.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard (v18.6.3.100 only) and apply patches.

---

## Compatibility

| Specification | Details |
| :--- | :--- |
| **Base App** | Google Keyboard (`com.google.android.inputmethod.latin`) |
| **Required Base Version** | `18.6.3.1002251336-release-arm64-v8a` (**v18.6.3.100** only) |
| **Unsupported Versions** | Gboard 18.5 and older are **not supported** by this patch bundle |
| **Architecture** | `arm64-v8a` |
| **Minimum Android Version** | Android 10 (API 29) or higher |
| **Standalone Coexistence** | Supported (package `com.akshaykadam.pixelboard` installs alongside factory Gboard) |
