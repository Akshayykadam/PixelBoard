# 🎹 PixelBoard Patch Source

<div align="center">

[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Version](https://img.shields.io/badge/Patch%20Version-1.0.5-blue?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Target](https://img.shields.io/badge/Gboard-v18.4.1_Release-orange?style=flat-square&logo=google)](https://github.com/Akshayykadam/PixelBoard)
[![Author](https://img.shields.io/badge/Author-Akshay%20Kadam-9cf?style=flat-square&logo=github)](https://github.com/Akshayykadam)

**PixelBoard Patch Source for Gboard**

---

### 📥 Looking for the Ready-to-Install APK?

**You don't need to build from source unless you want to customize patches:**

<p>
  <a href="https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable/PixelBoard-18.4.1.apk">
    <img src="https://img.shields.io/badge/📥_Download_Stable_APK-v18.4.1_(127_MB)-00C853?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Stable APK"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/">
    <img src="https://img.shields.io/badge/🌐_Stock_Base_APK-APKMirror_(v18.4.1)-FF6F00?style=for-the-badge&logo=google&logoColor=white" height="40" alt="Download Stock Gboard Base APK from APKMirror"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp">
    <img src="https://img.shields.io/badge/📦_Download_Patch_Bundle-PixelBoard.mpp_(1.5_MB)-2979FF?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Patch Bundle"/>
  </a>
</p>

[**⬇️ Direct Download Stable APK (v18.4.1)**](https://github.com/Akshayykadam/PixelBoard/releases/download/v18.4.1-Stable/PixelBoard-18.4.1.apk) • [**🌐 Stock Base APK (APKMirror)**](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-release/) • [**📦 Patch Bundle (.mpp)**](https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp) • [**📦 GitHub Releases**](https://github.com/Akshayykadam/PixelBoard/releases)

---

</div>

## Overview

This repository contains the patch definitions and extension bytecode for **PixelBoard**, specifically configured for:
- **Gemini Rambler Voice Dictation**: Speech cleanup, sentence restructuring, auto-punctuation, and idle battery drain fix.
- **In-Line AI Writing Assistant**: Proofread, rewrite with tone selection (Professional, Casual, Concise, Emotive), and smart editing.
- **Writing Tools V2 Gating**: "Suggested" chips, "Describe your edit" prompt bar, hardware/model eligibility device-gating, and dedicated Material You **BETA** toggle in Advanced Settings.
- **Device-Gated Backend Selector**: Selectable inference engine (`GBOARD_SERVER`, `PRIVATE_INFERENCE_AICORE`, `PRIVATE_INFERENCE_ASTREA`) on supported Pixel devices.
- **Standalone Coexistence & Signature Bypass**: Safe side-by-side installation with stock Gboard and bypassed signature checks.

- **App Label**: `PixelBoard`
- **Package ID**: `com.akshaykadam.pixelboard`
- **Target App**: Google Keyboard (Gboard) `v18.4.1` (Release) & `v18.3.1` (Release / Beta)

> [!NOTE]
> **Google Pixel Device Requirement**: New v18.4 features (**Writing Tools V2** and **On-Device AICore / Astrea inference**) are available **only on Google Pixel devices**. Core Rambler Voice Typing and AI writing tools work across all Android devices.

---

## Compilation

Build the Android patch bundle (`.mpp`):
```bash
./gradlew :patches:buildAndroid
```

The compiled patch bundle will be located at:
```text
patches/build/libs/patches-1.0.5.mpp
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
- **Patch Tooling & Runtime Framework**: PixelBoard Patch Engine / Open-Source Patcher Toolchain.
- **License**: GNU General Public License v3.0 ([LICENSE](../LICENSE)).
