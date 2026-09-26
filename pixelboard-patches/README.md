# 🎹 PixelBoard Patch Source

<div align="center">

[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Version](https://img.shields.io/badge/Patch%20Version-1.0.4-blue?style=flat-square)](https://github.com/Akshayykadam/PixelBoard)
[![Author](https://img.shields.io/badge/Author-Akshay%20Kadam-9cf?style=flat-square&logo=github)](https://github.com/Akshayykadam)

**PixelBoard Patch Source for Gboard**

---

### 📥 Looking for the Ready-to-Install APK?

**You don't need to build from source unless you want to customize patches:**

<p>
  <a href="https://github.com/Akshayykadam/PixelBoard/raw/main/output/PixelBoard.apk">
    <img src="https://img.shields.io/badge/📥_Download_Stable_APK-v18.3.1_(82_MB)-00C853?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Stable APK"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp">
    <img src="https://img.shields.io/badge/📦_Download_Patch_Bundle-PixelBoard.mpp_(1.4_MB)-2979FF?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download PixelBoard Patch Bundle"/>
  </a>
</p>

[**⬇️ Direct Download Stable APK (v18.3.1)**](https://github.com/Akshayykadam/PixelBoard/raw/main/output/PixelBoard.apk) • [**📦 Download Patch Bundle (.mpp)**](https://github.com/Akshayykadam/PixelBoard/raw/main/patches/PixelBoard.mpp) • [**📦 View GitHub Releases**](https://github.com/Akshayykadam/PixelBoard/releases)

---

</div>

## Overview

This repository contains the patch definitions and extension bytecode for **PixelBoard**, specifically configured for **AI Writing Assistant** and **Gemini Rambler Voice Dictation**.

- **App Label**: `PixelBoard`
- **Package ID**: `com.akshaykadam.pixelboard`
- **Target App**: Google Keyboard (Gboard) `v18.3.1` (Release) & `v18.0.3`

---

## Compilation

Build the Android patch bundle (`.mpp`):
```bash
./gradlew :patches:buildAndroid
```

The compiled patch bundle will be located at:
```text
patches/build/libs/patches-1.0.4.mpp
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
