# PixelBoard Stable Release (Patch v1.3.5 / Gboard v18.6.3.100)

## Summary
PixelBoard v1.3.5 delivers critical stability, lifecycle, and voice dictation enhancements across the board:
* **Screen Rotation Grace Period for Rambler Voice Dictation**: Maintains continuous voice typing and audio recording across device orientation changes without aborting active sessions ([#146](https://github.com/Akshayykadam/PixelBoard/issues/146), PR [#165](https://github.com/Akshayykadam/PixelBoard/pull/165) by Francisco Valentim Castilho).
* **Speech Diff Preservation for Voice Edits**: Correctly retains speech diffs for voice replacements and mid-sentence voice commands (such as *"replace X with Y"*), preventing accidental text overwrites ([#167](https://github.com/Akshayykadam/PixelBoard/issues/167)).
* **Strict Language Enforcement & Anti-Drift**: Disabled proactive parallel ASR and seamless fallback to prevent voice recognition from unintentionally switching languages or drifting to similar locales (e.g. Czech &rarr; Slovak) ([#154](https://github.com/Akshayykadam/PixelBoard/issues/154)).
* **Eliminate Text Duplication on Dictation Start**: Fixed an issue where starting dictation in an editor containing pre-existing text or recently pasted clipboard items would duplicate text at the cursor ([#166](https://github.com/Akshayykadam/PixelBoard/issues/166)).
* **Gboard 18.6.3 Native Split Readiness Gate Check**: Added native library readiness gate verification on split components for Gboard 18.6.3 (`Lsmi;`, `Locf;`), ensuring smooth and stable JNI loading on fused and split APKs ([#168](https://github.com/Akshayykadam/PixelBoard/issues/168)).
* **AI Writing Tools Server Connection Fix**: Repaired request timeout and prompt template evaluation to restore reliable server connection for Rephrase, Proofreading, and Tone adjustments without *"cannot connect to server"* errors.
* **Direct Boot & Secure Storage Safety**: Added user unlock state checks (`UserManager.isUserUnlocked`) ensuring crash-free preference initialization during direct boot before initial unlock.
* **Morphe Manager Patch Bundle DEX Packaging**: Included `classes.dex` directly at the root of `PixelBoard.mpp` via `:patches:buildAndroid` so Morphe Manager detects and loads all 4 patches out-of-the-box.
* **Android 10+ (API 29) Minimum SDK Compatibility**: Clamped `minSdkVersion` to 29 in the package manifest, enabling clean installation on Android 10 and 11 without parser errors.
* **Refined Softkey Label Sanitizer**: Cleaned up the symbol key label patch to focus strictly on `AutoCenterScaleTextView` keycap rendering, removing unnecessary input connection hooks to prevent text insertion side-effects.

---

## What's New in v1.3.5

### 🎙️ Gemini Rambler Natural Voice Dictation
- **Screen Rotation Grace Period**: Device rotation triggers Gboard to destroy and re-create its input method data, deactivating extensions for ~80ms. PixelBoard now provides a 1.5s grace period during display rotation, keeping voice typing alive without interrupting your dictation flow ([#146](https://github.com/Akshayykadam/PixelBoard/issues/146), PR [#165](https://github.com/Akshayykadam/PixelBoard/pull/165)).
- **Preserve Speech Diff for Mid-Sentence Voice Edits**: Retains speech diffs for voice replacements and mid-sentence voice commands (such as *"replace X with Y"*), word edits in respoken phrases, and mid-sentence cursor placements without overwriting surrounding compositions ([#167](https://github.com/Akshayykadam/PixelBoard/issues/167)).
- **Strict Language Enforcement & Anti-Drift**: Disabled proactive parallel ASR and seamless fallback in Rambler stock policy to prevent voice dictation from unintentionally switching languages or drifting to similar locales (e.g., Czech &rarr; Slovak) ([#154](https://github.com/Akshayykadam/PixelBoard/issues/154)).
- **Eliminate Text Duplication on Dictation Start**: Fixed an issue where starting dictation in an editor containing pre-existing text or recently pasted clipboard items would duplicate text at the cursor ([#166](https://github.com/Akshayykadam/PixelBoard/issues/166)).
- **Native Split Readiness Gate Check for Gboard 18.6.3**: Added native library readiness gate check for split APK components on Gboard 18.6.3 (`Lsmi;`, `Locf;`) ensuring seamless JNI loading on fused and split APKs ([#168](https://github.com/Akshayykadam/PixelBoard/issues/168)).

### 🤖 AI Writing Assistant
- **Server Connectivity & Timeout Repair**: Fixed an issue where writing tools timeout and prompt templates were improperly evaluated (*"cannot connect to server"*), restoring reliable Google Gboard server backend connectivity for Rephrase, Proofreading, and Tone adjustments (*Professional*, *Casual*, *Concise*, *Emotive*).
- **Direct Boot & Secure Storage Safety**: Added user unlock state checks (`UserManager.isUserUnlocked`) to avoid premature preference reads or crashes during direct boot before initial device unlock.

### 📦 Packaging, Platform & Quality of Life
- **Morphe Manager Out-of-the-Box Detection**: Bundled compiled `classes.dex` in the patch bundle root via `:patches:buildAndroid`, resolving the issue where Morphe Manager displayed 0 patches.
- **Android 10+ (API 29) Minimum SDK Compatibility**: Clamped `minSdkVersion` in the package manifest to API 29, enabling full installation and coexistence on Android 10 and Android 11 devices without package parser errors.
- **Refined Softkey Label Sanitizer**: Cleaned up the symbol key label patch to focus strictly on `AutoCenterScaleTextView` keycap rendering, removing unnecessary input connection hooks to prevent text insertion side-effects.
- **Base App Compatibility**: Full support and verification for Gboard v18.6.3.100 Release (`18.6.3.1002251336-release-arm64-v8a`).
- **Retained All v1.3.4 Features**: 24-hour default clipboard retention, 100 recent clips history cap, customizable retention duration slider (1h, 24h, 7d, 30d, Never) across 83 locales ([#129](https://github.com/Akshayykadam/PixelBoard/pull/129)), Google Sans Flex typography, and Undo action key.

---

## Community & Acknowledgements

Special thanks to our community contributors:

| Contributor | Focus / Contribution |
| :--- | :--- |
| **Francisco Valentim Castilho** ([@franciscovalentim](https://github.com/franciscovalentim)) | Diagnosed Rambler voice dictation deactivation on display rotation and contributed the rotation grace period state machine ([#146](https://github.com/Akshayykadam/PixelBoard/issues/146), [#165](https://github.com/Akshayykadam/PixelBoard/pull/165)) |
| **AnhDuck** ([@AnhDuck](https://github.com/AnhDuck)) | Clipboard retention slider, 24h default, honest text across 83 locales, 100-item history cap, Rambler voice toolbar minimize key & TalkBack labels ([#108](https://github.com/Akshayykadam/PixelBoard/pull/108), [#129](https://github.com/Akshayykadam/PixelBoard/pull/129)) |

---

## Release Assets

| Asset | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.5.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.5/PixelBoard-1.3.5.apk) | **Signed APK** *(Recommended)* | ~122 MB | **Ready to install.** Signed stable APK with all features enabled (v18.6.3 base, side-by-side coexistence). |
| [**patches-1.3.5.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.5/patches-1.3.5.mpp) | **Morphe Patch Bundle** | ~1.7 MB | Standalone patch bundle (v1.3.5) for Morphe Manager. ⚠️ **Strictly requires Gboard version 18.6.3.100** (`18.6.3.1002251336-release-arm64-v8a`). Older versions like 18.5 are unsupported. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.5.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.5/PixelBoard-1.3.5.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.5.apk
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
