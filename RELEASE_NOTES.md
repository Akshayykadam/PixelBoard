# PixelBoard v1.0.3 Release Notes

## Summary
PixelBoard v1.0.3 introduces official support for Gboard v18.3.1 (Stable Release), activates the new Writing Tools v2 user interface with "Describe your edit", and resolves model configuration errors on Google's GenAI backend.

## What's New

### Official Support for Gboard v18.3.1 Release
- Added full patch compatibility for Gboard version 18.3.1.977415014-release-arm64-v8a.
- Maintained backward compatibility for 18.0.3.954559732-release-arm64-v8a and 18.3.1.977415014-beta-arm64-v8a.

### Writing Tools v2 UI
- Activated the modern Writing Tools panel directly on the keyboard toolbar.
- Integrated full support for open-ended prompt editing via "Describe your edit".
- Preserved style and tone presets (Proofread, Professional, Casual, Concise, Elaborate).

### Bug Fixes and Stability Improvements
- Fixed "Couldn't load suggestions" in Writing Tools: Disabled external prompt bundle downloading (`writing_tools_v2_enable_prompt_download = false`). Standalone renamed packages cannot authenticate to Google Superpacks MDD prompt groups, causing prompt file read failures. Disabling this flag safely forces Gboard to use the built-in modular prompt templates packaged inside the APK.
- Increased Stylization Candidate Count: Increased `writing_tools_pi_stylization_candidate_count` and `writing_tools_v2_top_k` from 1 to 3, allowing the GenAI backend to generate multiple variations per request and reducing instances where candidates are filtered out.
- Added Automatic Retry: Set `writing_tools_v2_retry_max_attempts = 1` to gracefully handle transient network timeouts or connection drops during stylization requests.
- Corrected Text Stylization Routing: Verified routing to the server-supported "composer_stylization_base" model.
- Fixed Panel Crash on Open: Disabled incompatible zero-state MobileBERT suggestion flags, ensuring the prompt input panel opens smoothly.
- Fixed Prompt Network Initialization: Avoided premature offline states by handling network readiness checks properly.

### Note on Writing Tools "Looks Good" / Preset Zero-State Cards
- When using tone presets (such as Emojify, Formalize, or Casualize) on text that is already considered optimal, very short, or where the model determines no changes are required, Gboard's native behavior is to present a positive zero-state notice (for example: "Your text works better without adding emojis", "Your tone already sounds professional", or "Looks good"). Providing richer or more expressive sentences allows the model to suggest creative transformations.

### Standalone Coexistence and Security
- Package renamed to com.akshaykadam.pixelboard for side-by-side installation with the official stock Gboard.
- Signature verification bypassed so patched builds pass internal whitelist checks without root.

## Compatibility
- Target Package: com.google.android.inputmethod.latin
- Target Versions:
  - 18.3.1.977415014-release-arm64-v8a (Recommended)
  - 18.3.1.977415014-beta-arm64-v8a
  - 18.0.3.954559732-release-arm64-v8a
- Architecture: arm64-v8a
- Minimum Android Version: Android 10 (API 29)+
- Verified On: Android 14, Android 15, Android 16 Preview

## Installation
1. Download PixelBoard.apk from the Assets section.
2. Install the APK on your device (enable installation from unknown sources if prompted).
3. Open PixelBoard, follow the setup wizard to enable the keyboard, and select it as your default input method.
