# PixelBoard Stable Release (v18.4.1-V7 / Patch v1.2.0)

## Summary
PixelBoard v18.4.1-V7 (Patch Bundle v1.2.0) introduces Features Menu accessibility and TalkBack focus fixes, fused APK native split readiness verification for voice typing, automated clipboard screenshot observer synchronization, Sign-to-Text (ASL translation) camera integration, smart auto-correction with primary-key revert, and refined typing suggestions on top of Google's latest **Gboard v18.4.1 Release** (`18.4.1.985164140-release-arm64-v8a`).

---

## What's New in This Release (v18.4.1-V7 / Patch v1.2.0)

### 1. Features Menu Accessibility & TalkBack Navigation ([#48](https://github.com/Akshayykadam/PixelBoard/issues/48))
- **Screen Reader Announcements**: Resolved an issue where opening the features menu / "More options" (`AccessPointsPanel`, `AccessPointsPanelV2`, `AccessPointsPanelViewPage`) failed to notify TalkBack and accessibility services of window state changes.
- **Automated Focus Traversal**: Sets accessibility pane title (`resolvePaneTitle`), dispatches `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`, and automatically shifts focus to the first accessible tool or keycap (`SoftKeyView` / button).

### 2. Dictation JNI Readiness for Fused APKs & Offline Voice Typing ([#50](https://github.com/Akshayykadam/PixelBoard/issues/50))
- **Fused Base APK Compatibility**: Direct JNI library loading verification (`NativeLibHelper.c("dictation_jni", false)`) prevents dictation readiness checks from failing on fused APKs that lack Play feature-split markers.
- **Fused Target Hooks**: Added seamless bytecode patching for fused readiness target `slc` (`Lqeq;->e(Lqem;)Z`).
- **Offline Language Pack Download Interception**: Resolved offline voice typing download failures (such as traditional Chinese `zh-TW`) by handling offline recognition pack URL resolution cleanly.

### 3. Clipboard & Screenshot Observer Synchronization ([#52](https://github.com/Akshayykadam/PixelBoard/issues/52))
- **Foreground Screenshot Observer**: Added `ensureScreenshotObserver(handler)` calling `ClipboardDataHandler.q(true)` on `onStartInputView`, ensuring screenshot chips and clipboard items update immediately on display.
- **XML Default Overrides**: Enforced `defaultValue = "true"` directly in `res/xml/setting_clipboard.xml` for `enable_screenshot_in_clipboard` and `pref_key_enable_clipboard`.
- **Emoji Suggestions**: Unconditionally enables `pref_key_enable_emoji_suggestion` as default `true`.

### 4. Sign-to-Text (Signboard ASL Real-Time Translation)
- **American Sign Language Camera Translation**: Enabled native camera gesture translation to convert American Sign Language into text on supported devices.
- **Phenotype Flag Enforcement**: Activated `enable_signboard`, `signboard_prompt_manual_trigger`, `on_demand_feature_split`, `auto_show_signboard`, `signboard_auto_show_prompt`, `signboard_download`, `signboard_download_dialog`, `signboard_move_ap_prompt`, and `use_4c_in_signboard`.

### 5. Smart Auto-Correction & Typing Refinements ([#52](https://github.com/Akshayykadam/PixelBoard/issues/52))
- **Primary-Key Revert**: Enforced `auto_correct_with_replace_text = true` and `enable_revert_auto_correct_on_pk = true` (enables reverting auto-corrections via backspace/tap on physical and soft keyboards).
- **Out-of-the-Box Auto-Correction**: Pre-seeds `pref_key_auto_correction = true` by default.
- **Typo Protection**: Tuned `intended_oov_min_count` from `1L` to `2L` so mistyped words typed once (e.g. "olle") are not mistakenly learned into user vocabulary, allowing spacebar auto-correction to properly commit corrections.

### 6. Version 1.2.0 Synchronization
- **System-Wide Alignment**: Synchronized patch bundle version `1.2.0` across Gradle build properties, settings UI, preferences metadata, and bundle manifests.

---

## Community & Acknowledgements

A huge thank you to the community members whose testing, reproducible feedback, and bug reports directly helped identify and fix these issues:
- **[@mohamedtechnology](https://github.com/mohamedtechnology)**: For reporting the TalkBack/screen reader dead zone in the features menu and providing the root cause analysis on window focus and `TYPE_WINDOW_STATE_CHANGED` ([#48](https://github.com/Akshayykadam/PixelBoard/issues/48)).
- **[@andrewmatten](https://github.com/andrewmatten)**: For pinpointing the Gboard 18.4.1 native dictation split readiness gate in `slc.a()` and providing the verified bytecode patch for fused APKs on Pixel 8 Pro ([#50](https://github.com/Akshayykadam/PixelBoard/issues/50)).
- **[@Benestinlinson](https://github.com/Benestinlinson)**: For extensive multi-hour testing of V6, diagnosing the spacebar auto-correction behavior and reporting the screenshot chip sync ([#52](https://github.com/Akshayykadam/PixelBoard/issues/52)).

Your detailed reports, reproduction steps, and active testing make each release substantially more polished and reliable!

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-18.4.1-V7.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V7/PixelBoard-18.4.1-V7.apk) | **Signed APK** *(Recommended)* | ~127 MB | **Ready to install.** Signed stable APK (v18.4.1 release base, versionCode 176027079, side-by-side coexistence). |
| [**patches-1.2.0.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V7/patches-1.2.0.mpp) | **Morphe Patch Bundle** | ~1.6 MB | Standalone patch file (v1.2.0) for [Morphe Manager](#option-3-patch-via-morphe-manager) to patch your own base APK. |
| [**Stock Gboard v18.4.1**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only required if patching manually with Morphe)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-18.4.1-V7.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable-V7/PixelBoard-18.4.1-V7.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation (supports updating in-place over earlier builds).
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-18.4.1-V7.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard [**v18.4.1.985164140-release-arm64-v8a**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) (or `v18.3.1`) and apply patches.

---

## Compatibility
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.4.1.985164140-release-arm64-v8a` (Primary Stable / Recommended)
  - `18.3.1.977415014-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
- Standalone Coexistence: Supported (package `com.akshaykadam.pixelboard` installs alongside factory Gboard)
