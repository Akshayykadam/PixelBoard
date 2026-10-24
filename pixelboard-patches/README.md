# 🎹 PixelBoard Patch Source

<div align="center">

[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Version](https://img.shields.io/badge/Patch%20Version-1.3.0-blue?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Target](https://img.shields.io/badge/Gboard-v18.5.3_Release-orange?style=flat-square&logo=google)](https://github.com/Akshayykadam/PixelBoard)
[![Author](https://img.shields.io/badge/Author-Akshay%20Kadam-9cf?style=flat-square&logo=github)](https://github.com/Akshayykadam)

**PixelBoard Patch Source for Gboard**

---

### 📥 Looking for the Ready-to-Install APK?

**You don't need to build from source unless you want to customize patches:**

<p>
  <a href="https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.0/PixelBoard-1.3.0.apk">
    <img src="https://img.shields.io/badge/📥_Download_Stable_APK-v1.3.0_(79_MB)-00C853?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Stable APK"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/">
    <img src="https://img.shields.io/badge/🌐_Stock_Base_APK-APKMirror_(v18.5.3)-FF6F00?style=for-the-badge&logo=google&logoColor=white" height="40" alt="Download Stock Gboard Base APK from APKMirror"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp">
    <img src="https://img.shields.io/badge/📦_Download_Patch_Bundle-PixelBoard.mpp_(1.6_MB)-2979FF?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Patch Bundle"/>
  </a>
</p>

[**⬇️ Direct Download Stable APK (v1.3.0)**](https://github.com/Akshayykadam/PixelBoard/releases/download/1.3.0/PixelBoard-1.3.0.apk) • [**🌐 Stock Base APK (APKMirror)**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) • [**📦 Patch Bundle (.mpp)**](https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp) • [**📦 GitHub Releases**](https://github.com/Akshayykadam/PixelBoard/releases)

---

</div>

## Overview

This repository contains the patch definitions and extension bytecode for **PixelBoard**, specifically configured for:
- **Gemini Rambler Voice Dictation**: Speech cleanup, sentence restructuring, auto-punctuation, Gboard 18.5.3/18.4.1 obfuscated selector hooks, standalone out-of-the-box initialization (`enable_jetson = true`), and idle battery drain fix.
- **In-Line AI Writing Assistant**: Proofread, rewrite with tone selection (Professional, Casual, Concise, Emotive), smart editing, and Android 16 Quick Insert crash safeguard.
- **Modern Spell Checker Suggestion Chips & Grammar Checking**: Enforces Android 12+ inline spellcheck suggestion chips and add-to-dictionary UI on all devices (`GboardSpellCheckerChipsPatch`).
- **Scan Text / Camera OCR & Floating Screenshot Banner**: Direct optical character recognition camera tool and floating screenshot suggestion chips with permissions.
- **Emoji Palette V2 & Super Insert Hub**: Dedicated recent/frequent tabs, rich multi-view super insert, and in-line Smart Compose ghost autocompletion.
- **Writing Tools V2 Gating**: "Suggested" chips, "Describe your edit" prompt bar, hardware/model eligibility device-gating, and dedicated Material You toggles.
- **Device-Gated Backend Selector**: Selectable inference engine (`GBOARD_SERVER`, `PRIVATE_INFERENCE_AICORE`, `PRIVATE_INFERENCE_ASTREA`) on supported Pixel devices.
- **Material 3 Expressive Rounded Keys**: Belka rounded keyboard styling toggle with runtime Phenotype flag enforcement (#14).
- **Restored Emoji Size Slider & Vibrant Candidate Emojis**: Restores missing emoji scale slider setting and ensures 100% opacity for candidate emojis (#26).
- **Extended Clipboard History Retention (New Feature)**: Break free from the stock 24-hour limit! Save copied items for 1 hour, 24 hours, 7 days, 30 days, or permanently (Never), integrated into Gboard preferences and PixelBoard Settings with crash-free value migration.
- **Verbatim Voice Dictation**: Fixed speech dictation in Rambler mode committing spoken prompts verbatim without triggering AI answer responses.
- **Clipboard History Sync**: Foreground clipboard sync fix for Samsung One UI & Vivo devices (#17).
- **Multilingual UI & Bloatware Removal**: Fully translated Advanced Settings across 20+ locales with promotional clutter removed.
- **Complete Arabic (ar) Localization**: Full Arabic translation for Advanced Settings and AI features (#53).
- **Standalone Coexistence & Signature Bypass**: Safe side-by-side installation with stock Gboard and bypassed signature checks.
- **TalkBack & Screen Reader Accessibility**: Settings rows read as single controls with correct role and state, focus preserved across screen rebuilds, heading navigation, spoken warnings, and restart announcements (#65, #66, #67).

- **App Label**: `PixelBoard`
- **Package ID**: `com.akshaykadam.pixelboard`
- **Target App**: Google Keyboard (Gboard) `v18.5.3` (Release), `v18.4.1` (Release), & `v18.3.1` (Release / Beta)

> [!NOTE]
> **Google Pixel Device Requirement**: New v18.4/v18.5 features (**Writing Tools V2** and **On-Device AICore / Astrea inference**) are available **only on Google Pixel devices**. Core Rambler Voice Typing, spell checker chips, OCR, and AI writing tools work across all Android devices.

---

## Compilation

Build the Android patch bundle (`.mpp`):
```bash
./gradlew :patches:buildAndroid
```

The compiled patch bundle will be located at:
```text
patches/build/libs/patches-1.3.0.mpp
```

Run tests to verify all patch contracts and bytecode transformations:
```bash
./gradlew test
```

---

## Author & Maintainer

**Akshay Kadam**
- GitHub: [@Akshayykadam](https://github.com/Akshayykadam)
- Repository: [PixelBoard](https://github.com/Akshayykadam/PixelBoard)

---

## Credits & Upstream Acknowledgements

- **Original Gboard Patches**: Developed by Jason Wu ([@jasonwu1994](https://github.com/jasonwu1994)).
- **Rambler ASR Lifecycle Optimization**: Discovered and benchmarked by Paolo Del Casale ([@PaoloDelCasale](https://github.com/PaoloDelCasale)).
- **TalkBack & Screen Reader Accessibility**: Settings row semantics, focus restoration, headings, and spoken feedback contributed by Animesh Ahilya ([@animeshahilya](https://github.com/animeshahilya)) ([#65](https://github.com/Akshayykadam/PixelBoard/pull/65), [#66](https://github.com/Akshayykadam/PixelBoard/pull/66), [#67](https://github.com/Akshayykadam/PixelBoard/pull/67)).
- **Arabic Localization**: Complete Arabic (`ar`) translations for all settings and AI features contributed by AbdulAziz Hatem ([@AbdulAziz-Hatem](https://github.com/AbdulAziz-Hatem)) ([#53](https://github.com/Akshayykadam/PixelBoard/pull/53)).
- **Inline Autofill & Password Manager Support**: Investigated and resolved by Alin Cornel ([@alincornel](https://github.com/alincornel)) for renamed package coexistence ([#24](https://github.com/Akshayykadam/PixelBoard/pull/24)).
- **Patch Tooling & Runtime Framework**: PixelBoard Patch Engine / Open-Source Patcher Toolchain.
- **License**: GNU General Public License v3.0 ([LICENSE](../LICENSE)).
