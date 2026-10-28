# PixelBoard Stable Release (Patch v1.3.3 / Gboard v18.5.3)

## Summary
PixelBoard v1.3.3 brings important daily usability fixes and AI stability updates: **word suggestions are back**, the **glowing voice bar no longer gets stuck**, the **one-tap blue "Fix it" proofreading button is restored**, you can now **minimize the voice typing bar** easily, and **AI Writing Tools (Rephrase, Tone changes) work reliably across all your apps** without errors.

---

## What's New in v1.3.3

- 💡 **Word Suggestions Are Back**: Next-word suggestions and predictive text now display normally above the keyboard as you type.
- 🎨 **Fixed Stuck Voice Bar**: The animated glowing gradient bar no longer stays stuck across your keyboard when you pause speaking.
- ✨ **"Fix it" Button Restored**: The blue "Fix it" button and inline grammar suggestions now appear automatically whenever spelling or grammar mistakes are detected.
- 🎙️ **Minimize Voice Typing Bar**: Tap the minimize arrow during voice typing to smoothly collapse the voice bar and return to the keyboard anytime.
- 🤖 **More Reliable AI Writing Tools**: Changing tone (Professional, Casual, Concise) and rephrasing text now works smoothly without timeouts or "can't use writing tools" errors.
- 🌐 **Works in Every App**: AI Proofread and Writing Tools now work across all third-party messaging and notes apps with zero restrictions.
- ♿ **Clear Voice Button Labels**: TalkBack screen readers now clearly announce voice buttons as *"Minimize keyboard"* and *"Back to keyboard"*.
- ⚡ **Zero Setup Needed**: Modern Google Sans font and the convenient Undo button are enabled automatically from the start.

---

## Detailed Improvements

### 💡 Word Suggestions & Stuck Voice Bar Fix ([#109](https://github.com/Akshayykadam/PixelBoard/issues/109))
  - **Word Suggestions Restored**: When typing, predictive next-word candidates were previously blocked because the voice toolbar remained active. Word suggestions now display cleanly and responsively above your keys.
  - **Fixed Stuck Glowing Bar**: After using voice dictation, the rainbow gradient bar previously stayed stuck across the top of the keyboard even when idle. It now hides immediately when speech is done, keeping your keyboard clean and clutter-free.

### ✨ "Fix it" Proofreading Button & Grammar Checks ([#109](https://github.com/Akshayykadam/PixelBoard/issues/109))
  - **One-Tap "Fix it" Button**: Restored the prominent blue "Fix it" chip above your keyboard. Whenever you type a misspelled word or grammar error, tapping "Fix it" instantly corrects it.
  - **Inline Grammar Chips**: Helpful suggestions for punctuation and grammar now appear directly in the suggestion strip as interactive, clickable chips.

### 🎙️ Minimize Voice Typing Toolbar ([#108](https://github.com/Akshayykadam/PixelBoard/pull/108))
  - **Easy Collapse**: Added back the down-arrow minimize key on the voice typing bar. You can smoothly collapse the voice bar back down to a floating button or return to the regular keyboard with one tap.
  - **TalkBack Screen Reader Support**: Added clear spoken descriptions (*"Minimize keyboard"* and *"Back to keyboard"*) so users with screen readers can easily navigate voice typing controls.

### 🤖 Reliable AI Writing Tools Across All Apps
  - **Smooth Rewriting & Tone Switching**: Fixed connection issues that caused Rephrase, Professional, Casual, Concise, and Emotive tones to fail or time out. Text generation is now fast and dependable.
  - **No App Restrictions**: Removed artificial app limits—use AI proofreading and rewriting inside any messaging app, social media, or text editor.

### 🎨 Clean Defaults Out of the Box
  - **Google Sans Font**: Keycaps use modern Google Sans variable typography automatically on Android 12+.
  - **Quick Undo**: The Undo button is ready in your suggestion strip whenever autocorrect changes a word you didn't want changed.
  - **Emoji Quick Bar & Magic Emoji**: Fast emoji bar and magic emoji suggestions (🪄) are ready out of the box with all-app support.

### 🛠️ Stability & Under-the-Hood Fixes
  - Fully verified and tested against the latest **Gboard v18.5.3 Release** (`18.5.3.993608717-release-arm64-v8a`).
  - Preserves native **Personal dictionary** and **Physical keyboard** settings without collisions.
  - Passes all 249 unit tests across patch wiring and runtime feature policies.

---

## Community & Acknowledgements

Special thanks to the community contributor whose pull request made this release possible:

| Contributor | Focus / Contribution |
| :--- | :--- |
| **AnhDuck** ([@AnhDuck](https://github.com/AnhDuck)) | Restored Rambler voice toolbar minimize key and added TalkBack accessibility labels ([#108](https://github.com/Akshayykadam/PixelBoard/pull/108)) |

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-1.3.3.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.3/PixelBoard-1.3.3.apk) | **Signed APK** *(Recommended)* | ~79 MB | **Ready to install.** Signed stable APK (v18.5.3 base, side-by-side coexistence). |
| [**patches-1.3.3.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.3/patches-1.3.3.mpp) | **Morphe Patch Bundle** | ~1.6 MB | Standalone patch file (v1.3.3) for Morphe Manager to patch your own base APK. |
| [**Stock Gboard v18.5.3**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only needed if patching manually)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-1.3.3.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.3/PixelBoard-1.3.3.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-1.3.3.apk
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
