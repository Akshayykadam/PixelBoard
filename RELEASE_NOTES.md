# PixelBoard Stable Release (v18.5.3-V1 / Patch v1.3.0)

## Summary
PixelBoard v18.5.3-V1 (Patch Bundle v1.3.0) updates the base Google Keyboard to the latest official **Gboard v18.5.3 Release** (`18.5.3.993608717-release-arm64-v8a`), introduces the **Extended Clipboard History Retention** feature (save copied clips for 1 hour, 24 hours, 7 days, 30 days, or permanently/Never), resolves slider out-of-bounds crashes, introduces a complete TalkBack and screen reader accessibility overhaul across settings (PRs #65, #66, #67), adds complete Arabic (`ar`) localization (PR #53), and aligns all patch catalog metadata to version 1.3.0.

---

## What's New in This Release (v18.5.3-V1 / Patch v1.3.0)

### 1. Base Gboard v18.5.3 Release Upgrade
- **Latest Target Compatibility**: Full support for Google Keyboard **v18.5.3 Release** (`18.5.3.993608717-release-arm64-v8a`).
- **Updated Bindings & Projection**: Refreshed Smali bytecode anchors, version bindings, and patch catalog contracts for seamless zero-warning patching.
- **Slimmer APK Footprint**: Optimized library packaging (~79 MB) for `arm64-v8a` devices.

### 2. Extended Clipboard History Retention Duration (New Feature)
- **Save Clips for Up to 30 Days or Permanently**: Break free from stock Gboard's rigid 24-hour limit! You can now choose exactly how long copied text and clips remain in your clipboard history with 5 customizable duration tiers:
  - **1 hour**
  - **24 hours** (Stock Gboard default)
  - **7 days** (1 week)
  - **30 days** (1 month)
  - **Never** (Permanent / Manual deletion only)
- **Accessible Everywhere**: Configurable directly within native Gboard **Settings > Clipboard** as well as under **PixelBoard Settings**.
- **Seamless Value Migration & Crash-Free Clamping**: Includes built-in preference migration and value clamping logic. Previously saved settings (such as stock 24h at index 6) are automatically mapped to the new duration tiers (`targetIndex = 1`), preventing the Material Slider out-of-bounds `IllegalStateException` (`Slider value(6.0) ...`) and ensuring smooth, reliable layout.

### 3. Comprehensive TalkBack & Screen Reader Accessibility Overhaul ([#65](https://github.com/Akshayykadam/PixelBoard/pull/65), [#66](https://github.com/Akshayykadam/PixelBoard/pull/66), [#67](https://github.com/Akshayykadam/PixelBoard/pull/67))
- **Unified Row Semantics ([#65](https://github.com/Akshayykadam/PixelBoard/pull/65))**:
  - Introduced `AccessibleRow` (`LinearLayout` subclass) merging a switch or radio button's role and checked state into the row itself, giving TalkBack one cohesive stop instead of two.
  - Back and restart controls properly report `Button` role.
  - Hides decorative chevron characters from screen readers.
  - Choice rows cleanly announce their selection (e.g., `"Current: <value>"`).
- **Focus Preservation Across Dynamic Rebuilds ([#66](https://github.com/Akshayykadam/PixelBoard/pull/66))**:
  - Automatically tracks TalkBack focus by row title across dynamic settings rebuilds, restoring focus to the active row or shifting focus to page titles on navigation.
  - Marked toolbar and section titles as accessibility headings on Android 9+ (API 28+).
- **Test Field Hints, Spoken Warnings & Restart Announcements ([#67](https://github.com/Akshayykadam/PixelBoard/pull/67))**:
  - Uses `setHint` instead of `setContentDescription` on keyboard preview `EditText` so TalkBack reads typed characters instead of masking them.
  - Status blocks prefix titles with `"Warning:"`.
  - Restart button announces `"Restarting Gboard"` via `stateDescription` (API 30+) or `announceForAccessibility` (older Android versions).

### 4. Complete Arabic (ar) Localization ([#53](https://github.com/Akshayykadam/PixelBoard/pull/53))
- **Native Arabic Translations**: Complete Arabic language localization covering all Advanced Settings, AI Writing Tools, and Rambler dictation preferences.
- Escaped apostrophes, preserved string formatting specifiers, and standardized single-line strings.

### 5. Version 1.3.0 Synchronization
- **System-Wide Alignment**: Synchronized patch bundle version `1.3.0` across Gradle build properties, settings UI, preferences metadata, and Morphe repository manifests (`patches-bundle.json` and `patches-list.json`).

---

## Community & Acknowledgements

A huge thank you to the community contributors whose pull requests, testing, and detailed bug reports made this release possible:
- **Animesh Ahilya ([@animeshahilya](https://github.com/animeshahilya))**: For authoring and contributing the complete TalkBack accessibility overhaul across PRs [#65](https://github.com/Akshayykadam/PixelBoard/pull/65), [#66](https://github.com/Akshayykadam/PixelBoard/pull/66), and [#67](https://github.com/Akshayykadam/PixelBoard/pull/67).
- **AbdulAziz Hatem ([@AbdulAziz-Hatem](https://github.com/AbdulAziz-Hatem))**: For contributing complete Arabic (`ar`) localization across all Advanced Settings and AI features ([#53](https://github.com/Akshayykadam/PixelBoard/pull/53)).
- **Mohamed Technology ([@mohamedtechnology](https://github.com/mohamedtechnology))**: For reporting the TalkBack dead zone in the features menu and providing root cause analysis on window focus ([#48](https://github.com/Akshayykadam/PixelBoard/issues/48)).
- **Andrew Matten ([@andrewmatten](https://github.com/andrewmatten))**: For pinpointing the native dictation split readiness gate in `slc.a()` and contributing the verified bytecode patch for fused APKs ([#50](https://github.com/Akshayykadam/PixelBoard/issues/50)).
- **Benestin ([@Benestinlinson](https://github.com/Benestinlinson))**: For extensive testing across releases, diagnosing spacebar auto-correction behavior, and reporting screenshot chip sync ([#52](https://github.com/Akshayykadam/PixelBoard/issues/52)).
- **Paolo Del Casale ([@PaoloDelCasale](https://github.com/PaoloDelCasale))**: For deep BatteryStats investigations and candidate fixes resolving background `GoogleAsrService` persistence ([#3](https://github.com/Akshayykadam/PixelBoard/issues/3)).
- **Alin Cornel ([@alincornel](https://github.com/alincornel))**: For identifying and resolving inline autofill suggestions from password managers ([#24](https://github.com/Akshayykadam/PixelBoard/pull/24)).
- **JasonWu**: For the foundational reverse-engineering research and Smali patch framework.

---

## Release Assets

| Package | Type | Size | Description |
| :--- | :--- | :--- | :--- |
| [**PixelBoard-18.5.3-V1.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.5.3-Stable-V1/PixelBoard-18.5.3-V1.apk) | **Signed APK** *(Recommended)* | ~79 MB | **Ready to install.** Signed stable APK (v18.5.3 release base, versionCode 176049646, side-by-side coexistence). |
| [**patches-1.3.0.mpp**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.0/patches-1.3.0.mpp) | **Morphe Patch Bundle** | ~1.6 MB | Standalone patch file (v1.3.0) for [Morphe Manager](#option-3-patch-via-morphe-manager) to patch your own base APK. |
| [**Stock Gboard v18.5.3**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) | **Base APK** | ~67 MB | Official unmodified base APK from APKMirror *(only required if patching manually with Morphe)*. |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download [**PixelBoard-18.5.3-V1.apk**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.5.3-Stable-V1/PixelBoard-18.5.3-V1.apk) directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation (supports updating in-place over earlier builds).
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard-18.5.3-V1.apk
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
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.5.3.993608717-release-arm64-v8a` (Primary Stable / Recommended)
  - `18.4.1.985164140-release-arm64-v8a`
  - `18.3.1.977415014-release-arm64-v8a`
  - `18.0.3.954559732-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
- Standalone Coexistence: Supported (package `com.akshaykadam.pixelboard` installs alongside factory Gboard)
