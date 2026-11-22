# PixelBoard Stable Release (Patch v1.3.6 / Gboard v18.6.3.100)

## 📌 Summary
PixelBoard v1.3.6 brings critical fixes for suggestion strip candidate rendering, emoji slot positioning, Morphe Manager bundle version metadata, and Advanced Settings stability:

* **Candidate Suggestion Strip Restored**: Fixed an issue where the suggestion strip would appear completely blank when typing. Unified bytecode hook signatures for `LatinFixedCountCandidatesHolderView.D` across all runtime targets.
* **Emoji Right Slot Placement**: Refined candidate slot index assignment (`candidate.l = i`). Primary word auto-completions now reliably occupy **Slot 1 (Center)**, while contextual emoji predictions are placed in **Slot 2 (Right)**, preventing emojis from displacing primary word completions.
* **Morphe Manager v1.3.6 Manifest Sync**: Corrected `META-INF/MANIFEST.MF` inside `.mpp` patch bundles so Morphe Manager properly identifies and displays version `1.3.6` (resolving the issue where it previously showed `1.3.5`).
* **Advanced Settings Crash Resolution**: Restored all extension Android resource `R` classes (`R`, `R$string`, `R$color`, `R$drawable`) in extension DEX runtime and added null safety guards in preference hooks, completely eliminating crashes when launching Advanced Settings (`GboardPatchesSettingsActivity`).
* **Safe Haptic Feedback Hook**: Hardened haptic feedback invocation (`performHapticFeedback`) across devices and custom themes to prevent null pointer or context crashes.
* **Android 17 / Pixel 8 Pro Verified**: Tested and verified zero-crash installation and smooth operation on Android 10 through Android 17 (tested on Google Pixel 8 Pro).
* **Screen Rotation Grace Period for Rambler Voice Dictation**: Maintains continuous voice typing across device orientation changes by introducing a 1.5s grace period during display rotation, preventing sessions from abruptly aborting ([#146](https://github.com/Akshayykadam/PixelBoard/issues/146), PR [#165](https://github.com/Akshayykadam/PixelBoard/pull/165) by Francisco Valentim Castilho).
* **Speech Diff Preservation for Voice Edits**: Retains speech diffs for voice replacements and mid-sentence voice commands (such as *"replace X with Y"*), word edits in respoken phrases, and mid-sentence cursor placements without overwriting surrounding compositions ([#167](https://github.com/Akshayykadam/PixelBoard/issues/167)).
* **Strict Language Enforcement & Anti-Drift**: Disabled proactive parallel ASR and seamless fallback in Rambler stock policy to prevent voice dictation from unintentionally switching languages or drifting to similar locales (e.g., Czech &rarr; Slovak) ([#154](https://github.com/Akshayykadam/PixelBoard/issues/154)).
* **Eliminate Text Duplication on Dictation Start**: Fixed an issue where starting dictation in an editor containing pre-existing text or recently pasted clipboard items would duplicate text at the cursor ([#166](https://github.com/Akshayykadam/PixelBoard/issues/166)).
* **Gboard 18.6.3 Native Split Readiness Gate Check**: Added native library readiness gate verification on split components for Gboard 18.6.3 (`Lsmi;`, `Locf;`), ensuring smooth and stable JNI initialization on fused and split APKs ([#168](https://github.com/Akshayykadam/PixelBoard/issues/168)).
* **AI Writing Tools Connectivity Fix**: Repaired request timeout and prompt template evaluation in `GboardAiWritingToolsRuntime`, eliminating *"cannot connect to server"* errors and restoring reliable Rephrase, Proofreading, and Tone adjustments via Google's Gboard server backend.
* **Direct Boot & Secure Storage Safety**: Added user unlock state checks (`UserManager.isUserUnlocked`) ensuring crash-free preference initialization during direct boot before initial unlock.
* **Android 10+ (API 29) Minimum SDK Compatibility**: Clamped `minSdkVersion` to 29 in the package manifest, enabling clean installation on Android 10 and 11 without parser errors.
* **Enhanced Clipboard Retention & 100-Item Cap**: Unpinned clips kept for 24 hours by default, history cap raised from 5 to 100 recent clips, customizable slider (1h, 24h, 7d, 30d, Never) and honest duration strings across 83 locales ([#129](https://github.com/Akshayykadam/PixelBoard/pull/129)).

---

## 📥 1-Click Download & Release Assets

| Asset | Type | Size | SHA-256 Checksum | Description |
| :--- | :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.6.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.6/PixelBoard-1.3.6.apk) | **Signed APK** *(Recommended)* | ~122 MB | `862ff3a7c277f1e5d6cc2655793a78ad3bfa312e99ebbda60cada4bf918b5760` | **1-Click Ready to install.** Pre-built, signed APK with all mods enabled. Installs side-by-side with stock Gboard without root (`com.akshaykadam.pixelboard`). Base Gboard v18.6.3.100. |
| [**PixelBoard.apk**](https://github.com/Akshayykadam/PixelBoard/releases/latest/download/PixelBoard.apk) | **Latest Signed APK** | ~122 MB | `862ff3a7c277f1e5d6cc2655793a78ad3bfa312e99ebbda60cada4bf918b5760` | Direct link to the latest stable APK release. |
| [**patches-1.3.6.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.6/patches-1.3.6.mpp) | **Morphe Patch Bundle** | ~1.7 MB | `7ade07bb79d1b9c8dcdb777ac72350c01b799c9407683924f98984304ffddf16` | Standalone patch bundle (v1.3.6) for Morphe Manager. ⚠️ **Strictly requires Gboard version 18.6.3.100** (`18.6.3.1002251336-release-arm64-v8a`). Older versions like 18.5 are unsupported. |
| [**PixelBoard.mpp**](https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp) | **Raw GitHub Patch Bundle** | ~1.7 MB | `7ade07bb79d1b9c8dcdb777ac72350c01b799c9407683924f98984304ffddf16` | Direct raw URL from main branch for on-device patchers. |

---

## 🌟 Detailed Changelog

### 🔤 Suggestion Strip & Candidate Layout
- **Candidate Suggestion Strip Restored**: Fixed an issue where the suggestion strip would display blank. Added matching bytecode hooks and method overloads (`List<?>` and `void`) for `LatinFixedCountCandidatesHolderView.D` in `GboardCandidateEmojiRuntime` to prevent `NoSuchMethodError`.
- **Right Slot Emoji Placement**: Refined candidate slot index assignment (`candidate.l = i`). Stock Gboard puts emojis in Slot 1 (center) by default; PixelBoard now places verbatim input in Slot 0 (left), word auto-completions in Slot 1 (center), and suggested emojis in Slot 2 (right), preserving quick 1-tap word completion on the spacebar.

### 🛠️ Stability & Crash Fixes
- **Advanced Settings Crash Resolution**: Restored all extension Android resource `R` classes (`R`, `R$string`, `R$color`, `R$drawable`) in extension DEX runtime, preventing `NoClassDefFoundError` crashes when opening Advanced Settings (`GboardPatchesSettingsActivity`).
- **Preference Hook Hardening**: Added null safety checks around `System.identityHashCode` in preference hook wrappers to prevent unexpected NullPointerExceptions.
- **Haptic Feedback Hardening**: Safe invocation wrapper (`performHapticFeedback`) avoiding NPE on unsupported device drivers or custom system themes.

### 📦 Morphe Manager Integration
- **Manifest Version Metadata Sync**: Updated `META-INF/MANIFEST.MF` inside both `PixelBoard.mpp` and `patches-1.3.6.mpp` to `Version: 1.3.6`, ensuring Morphe Manager correctly identifies and displays bundle version `1.3.6`.
- **DEX Packaging at Root**: Bundled compiled `classes.dex` directly at the patch bundle root via `:patches:buildAndroid` so Morphe Manager detects and loads all 4 patches out-of-the-box.

### 🎙️ Gemini Rambler Natural Voice Dictation
- **Screen Rotation Grace Period**: Maintains active Rambler voice dictation across device display orientation changes by introducing a 1.5s grace period during display rotation, preventing sessions from abruptly aborting ([#146](https://github.com/Akshayykadam/PixelBoard/issues/146), PR [#165](https://github.com/Akshayykadam/PixelBoard/pull/165)).
- **Preserve Speech Diff for Mid-Sentence Voice Edits**: Retains speech diffs for voice replacements and mid-sentence voice commands (such as *"replace X with Y"*), word edits in respoken phrases, and mid-sentence cursor placements without overwriting surrounding compositions ([#167](https://github.com/Akshayykadam/PixelBoard/issues/167)).
- **Strict Language Enforcement & Anti-Drift**: Disabled proactive parallel ASR and seamless fallback in Rambler stock policy to prevent voice dictation from unintentionally switching languages or drifting to similar locales (e.g., Czech &rarr; Slovak) ([#154](https://github.com/Akshayykadam/PixelBoard/issues/154)).
- **Eliminate Text Duplication on Dictation Start**: Fixed an issue where starting dictation in an editor containing pre-existing text or recently pasted clipboard items would duplicate text at the cursor ([#166](https://github.com/Akshayykadam/PixelBoard/issues/166)).
- **Native Split Readiness Gate Check for Gboard 18.6.3**: Added native library readiness gate verification on split components for Gboard 18.6.3 (`Lsmi;`, `Locf;`), ensuring smooth and stable JNI initialization on fused and split APKs ([#168](https://github.com/Akshayykadam/PixelBoard/issues/168)).

### 🤖 AI Writing Assistant
- **Server Connectivity & Timeout Repair**: Repaired request timeout and prompt template evaluation in `GboardAiWritingToolsRuntime`, eliminating *"cannot connect to server"* errors and restoring reliable Rephrase, Proofreading, and Tone adjustments via Google's Gboard server backend.
- **Direct Boot & Secure Storage Safety**: Added user unlock state checks (`UserManager.isUserUnlocked`) ensuring crash-free preference initialization during direct boot before initial unlock.

---

## 📱 Installation Instructions

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.6.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.6/PixelBoard-1.3.6.apk) directly to your Android device.
2. Open the downloaded APK from your browser or file manager.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.6.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard (v18.6.3.100 only) and apply patches.

---

## ⚙️ Compatibility

| Specification | Details |
| :--- | :--- |
| **Base App** | Google Keyboard (`com.google.android.inputmethod.latin`) |
| **Required Base Version** | `18.6.3.1002251336-release-arm64-v8a` (**v18.6.3.100** only) |
| **Unsupported Versions** | Gboard 18.5 and older are **not supported** by this patch bundle |
| **Architecture** | `arm64-v8a` |
| **Android Version Support** | Android 10 (API 29) through Android 17 (Tested on Pixel 8 Pro) |
| **Root Required** | **No** (Standalone coexistence as `com.akshaykadam.pixelboard`) |
