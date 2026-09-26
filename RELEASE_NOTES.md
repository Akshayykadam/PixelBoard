# PixelBoard Stable Release (v18.3.1 / Patch v1.0.4)

## Summary
PixelBoard v18.3.1 (Patch Bundle v1.0.4) brings the rock-solid in-line AI feature set directly to Google's latest **Gboard v18.3.1 Release** (`18.3.1.977415014-release-arm64-v8a`) along with critical idle battery optimizations for Gemini Rambler Voice Typing.

---

## What's New & Changed

### 1. Updated to Gboard v18.3.1 Release Base
- Rebased the primary stable build from Gboard 18.0.3 to official **Gboard 18.3.1 Release** (`18.3.1.977415014-release-arm64-v8a`, 81.2 MB).
- Fully supports arm64-v8a devices running Android 10 through Android 16 Preview.
- Backward compatibility maintained for Gboard 18.0.3.

### 2. Patch Bundle v1.0.4
- Bumped patch bundle to **v1.0.4** with official bytecode mappings and compatibility for Gboard 18.3.1 Release.
- **Rambler ASR Battery & Service Lifecycle Fix**: Eliminated background `GoogleAsrService` (`com.google.android.tts`) service binder persistence after closing the keyboard by enforcing `immediately_end_dictation_on_keyboard_hidden=true` and unforcing `enable_sticky_mic_background` (credits: @PaoloDelCasale).
- Reverted experimental V2 prompt keyboard overrides to restore the stable in-line writing tools architecture.
- Proofread and style chips (Rephrase, Emojify, Formal, Casual, Concise, Elaborate) render directly above the keyboard layout.
- Eliminated all errors associated with multi-role prompt splitting, empty draft payloads, and prompt bundle network failures.
- Zero "Could not suggest" or "Showing error" states during text transformation requests.

### 3. Gemini "Rambler" Natural Voice Typing
- Unlocked Google's state-of-the-art Rambler dictation model.
- Real-time speech cleanup: automatically filters hesitation, stutters, and filler words ("um", "ah", "like").
- Real-time thought correction: seamlessly corrects self-revisions during dictation.
- Context-aware auto-punctuation and proper noun capitalization.

### 4. Standalone Coexistence & Signature Bypass
- Packaged under independent application ID `com.akshaykadam.pixelboard` with app label `PixelBoard` for safe side-by-side use alongside factory Gboard.
- Signature verification bypassed so patched APKs pass integrity and whitelist checks without root.

---

## Release Assets

| File | Size | Description |
| :--- | :--- | :--- |
| **PixelBoard.apk** | ~82 MB | Ready-to-install signed Stable APK (v18.3.1 release base) |
| **PixelBoard.mpp** | ~1.4 MB | Standalone patch bundle (v1.0.4) for Morphe Manager |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download `PixelBoard.apk` directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard `v18.3.1.977415014-release-arm64-v8a` (or `v18.0.3`) and apply patches.

---

## Compatibility
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.3.1.977415014-release-arm64-v8a` (Recommended)
  - `18.3.1.977415014-beta-arm64-v8a`
  - `18.0.3.954559732-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
