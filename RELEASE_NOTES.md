# PixelBoard Stable Release (v18.4.1 / Patch v1.0.5)

## Summary
PixelBoard v18.4.1 (Patch Bundle v1.0.5) brings the rock-solid in-line AI feature set directly to Google's latest **Gboard v18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`), introducing **Writing Tools V2 Gating**, **Device-Gated AI Inference Backend Selection**, and idle battery optimizations for Gemini Rambler Voice Typing.

---

## What's New & Changed

### 1. Updated to Gboard v18.4.1 Release Base
- Rebased primary build to official **Gboard 18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`, ~122 MB base).
- Fully supports arm64-v8a devices running Android 10 through Android 16 Preview.
- Backward compatibility maintained for Gboard 18.3.1.

### 2. Writing Tools V2 Gating (New)
- **"Suggested" Style Chips & Freeform Prompt Bar**: Unlocks Google's modern writing tools interface with quick style chips and "Describe your edit".
- **Hardware & Model Eligibility Gating**: Automatically hides V2 elements on unsupported devices to prevent errors or UI glitches.
- **Dedicated Advanced Settings Toggle**: Toggle Writing Tools V2 inside Advanced Settings with a Material You **BETA** badge.

### 3. Device-Gated AI Inference Backend Selector (New)
- On supported Pixel devices (e.g. Pixel 8 Pro / Pixel 9 series), users can select their preferred inference backend:
  - `GBOARD_SERVER` (Google Cloud Server Inference)
  - `PRIVATE_INFERENCE_AICORE` (On-Device Private Android AICore)
  - `PRIVATE_INFERENCE_ASTREA` (On-Device Astrea Framework)
- Dynamically hidden on unsupported hardware for a clean, native experience.

### 4. Gemini "Rambler" Natural Voice Typing & Battery Fix
- Unlocked Google's state-of-the-art Rambler dictation model.
- **Speech Cleanup**: Automatically filters hesitation, stutters, and filler words ("um", "ah", "like").
- **Thought Correction**: Seamlessly corrects self-revisions on the fly.
- **Context-Aware Punctuation**: Real-time auto-punctuation and capitalization.
- **Rambler ASR Battery & Service Lifecycle Fix**: Eliminated background `GoogleAsrService` (`com.google.android.tts`) service binder persistence after closing the keyboard by enforcing `immediately_end_dictation_on_keyboard_hidden=true` (credits: @PaoloDelCasale).

### 5. Offline Language Pack Download Loop Fix (#5)
- **Eliminated 150 req/sec Download Loop**: Fixed an aggressive loop where Gboard repeatedly retried downloading offline language packs for secondary dictation languages (e.g., Italian + English) after Google Speech (`com.google.android.tts`) rejected silent downloads for the modified package ID.
- **In-Process Download Guard**: Integrated `GboardLanguageDownloadGuard` to ensure each language pack download is requested at most once per process, slashing idle CPU usage from ~15% to 0.02% and reducing battery drain from 3.68 mAh/h to 0.43 mAh/h (investigation & candidate fix: @PaoloDelCasale).
- **Dual-Version Support**: Bytecode patch targets both Gboard 18.3.1 (`Lsnm;->c`) and 18.4.1 (`Lsqk;->c`).

### 6. Fix Advanced Voice Typing Reboot Persistence (#4)
- **Immediate Reboot Readiness**: Resolved an issue where Advanced Voice Typing did not initialize on cold boot (e.g., on Xiaomi HyperOS / Android 14–16) until "Restart Gboard" was tapped manually.
- **Direct Boot & Device-Protected Storage**: Migrated PixelBoard settings to Device-Protected (DE) storage with automatic seamless migration from Credential-Encrypted (CE) storage, ensuring preferences are accessible as soon as Gboard's `directBootAware` IME service launches.
- **Selector State De-Poisoning**: Fixed runtime selection gate so unconfigured/boot state defaults to enabled, and guarded against spurious background checks (`Laaeo.a`) overwriting the active selection to `false` during boot or keyboard typing sessions.

### 7. Standalone Coexistence & Signature Bypass
- Packaged under independent application ID `com.akshaykadam.pixelboard` with app label `PixelBoard` for safe side-by-side use alongside factory Gboard.
- Signature verification bypassed so patched APKs pass integrity and whitelist checks without root.

---

## Release Assets

| File | Size | Description |
| :--- | :--- | :--- |
| **PixelBoard.apk** | ~127 MB | Ready-to-install signed Stable APK (v18.4.1 release base) |
| **PixelBoard.mpp** | ~1.4 MB | Standalone patch bundle (v1.0.5) for Morphe Manager |

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
Select stock Gboard `v18.4.1.985164140-release-arm64-v8a` (or `v18.3.1`) and apply patches.

---

## Compatibility
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.4.1.985164140-release-arm64-v8a` (Primary Stable / Recommended)
  - `18.4.1.985164140-beta-arm64-v8a`
  - `18.3.1.977415014-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
