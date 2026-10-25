# PixelBoard Stable Release (Patch v1.3.2 / Gboard v18.5.3)

## Summary
PixelBoard Patch v1.3.2 delivers essential bug fixes and refinements for Gboard v18.5.3, including **native Personal Dictionary and Physical Keyboard preservation** across Gboard 18.5+ resource changes, **full voice editing contextual command execution** in Rambler dictation, **instantaneous dictation latency optimization**, **Writing Tools stability safeguards**, and **Material You settings UI visual polish**.

---

## 🚀 What's New & Improvements in v1.3.2

- **Personal Dictionary & Physical Keyboard Preservation**: Fixed a settings cleanup regression where shifting resource IDs in Gboard 18.5+ caused native Dictionary (`LatinDictionarySettingsFragment`), Personal dictionary, and Physical keyboard preferences to be mistakenly removed. Semantic string resource keys (`settings_header_rate_us`, `settings_header_help_and_feedback`) are now resolved dynamically, strictly preserving all native Dictionary and Physical Keyboard entries across all Gboard versions ([#91](https://github.com/Akshayykadam/PixelBoard/issues/91), [#94](https://github.com/Akshayykadam/PixelBoard/pull/94)).
- **Voice Dictation Contextual Commands Restored**: Resolved an issue where voice editing commands (e.g. "Translate to Bengali", "Fix grammar", "Clear that", "Undo") were inadvertently overwritten with raw spoken text. Verbatim transcript restoration is now selectively restricted to conversational AI question/prompt answers (`INTENT_COMPOSE` = 4), while allowing contextual voice rewrite, edit, and history actions (`INTENT_REWRITE`, `INTENT_EDIT`, `INTENT_EDIT_HISTORY`) to execute natively on dictated text ([#90](https://github.com/Akshayykadam/PixelBoard/issues/90)).
- **Instantaneous Voice Typing Latency**: Eliminated the agentic dictation deactivation delay (`AGENTIC_DICTATION_DEACTIVATION_DELAY_MS_FLAG` set to `0L`) for zero-lag voice typing stop/start response, and pruned obsolete floating toolbar positioning and animation flags.
- **Writing Tools Stability & Safe Backend Migration**: Enforced reliable Google Cloud processing (`GBOARD_SERVER`) by default with automatic one-time migration for existing devices, removing experimental unsupported backend options from the UI. Disabled experimental cooperative mode and selection helper flags to guarantee rock-solid text proofreading and rewriting.
- **Material You Settings Polish**: Streamlined the "What's New" settings card and layout by removing redundant borders and legacy icon artifacts, refining typography, line spacing, button styles, and divider lines for a cleaner Material You appearance.

---

## 🤝 Community & Contributions

Special thanks to the contributor whose pull request and research made this release possible:

| Contributor | Focus / Contribution |
| :--- | :--- |
| **Akash Mahedy** ([@akashmahedy](https://github.com/akashmahedy)) | Fixed settings cleanup removing native Personal Dictionary & Physical Keyboard preferences on Gboard 18.5+ ([#91](https://github.com/Akshayykadam/PixelBoard/issues/91), [#94](https://github.com/Akshayykadam/PixelBoard/pull/94)) |

---

## 📥 Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.2.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.2/PixelBoard-1.3.2.apk) | **Signed APK** *(Recommended)* | ~79 MB | **Ready to install.** Signed stable APK (v18.5.3 base, side-by-side coexistence). |
| [**patches-1.3.2.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.2/patches-1.3.2.mpp) | **Morphe Patch Bundle** | ~1.6 MB | Standalone patch file (v1.3.2) for Morphe Manager to patch your own base APK. |
| [**Stock Gboard v18.5.3**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only needed if patching manually)*. |

---

## 📲 Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.2.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.2/PixelBoard-1.3.2.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.2.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard [**v18.5.3.993608717-release-arm64-v8a**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) and apply patches.

---

## 📱 Compatibility

- **Target App**: Google Keyboard (Gboard) `v18.5.3.993608717-release-arm64-v8a`
- **Supported Architecture**: `arm64-v8a`
- **Android Version**: Android 10 through Android 16 (API 29–36)
- **Root Status**: Not required (installs side-by-side as `com.akshaykadam.pixelboard`)
