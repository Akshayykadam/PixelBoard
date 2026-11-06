# PixelBoard + Simple Calculator — Combined Build (v1.4.0)

PixelBoard's working Gemini **Rambler** voice dictation **and** the **Simple
Calculator** (math auto-calculation) from jasonwu1994/Gboard-patches, together
in one patch set for the first time.

- Base: Akshayykadam/PixelBoard v1.3.3 source (Rambler untouched)
- Added: Simple Calculator from jasonwu1994/Gboard-patches, ported to
  PixelBoard's architecture and package (`com.akshaykadam.pixelboard`)
- Version: patches version bumped to **1.4.0**; `options.json` enables the new
  **Simple Calculator** patch by default

## Why the two features never worked together

1. **The calculator is not a flag.** In Gboard-patches it is a self-contained
   runtime (a separate extension dex, `gboard-calculator.rve`) driven by a
   lifecycle patch that hooks Gboard's IME service
   (`onStartInputView` / `onUpdateSelection` / `onWindowHidden` on class
   `Lphr;`). PixelBoard never had any of this code — no runtime, no patch, no
   `RuntimeCallId`s — so there was nothing to switch on.
2. **Gboard-patches is pinned to one Gboard build.** Its Rambler selector
   hard-checks `targetVersion == "18.5.4"` and verifies 18.5.4 structural
   fingerprints before patching, and its generated bindings target 18.5.4
   only. On PixelBoard's base (Gboard **18.5.3**) that check fails, which is
   why Rambler does not work in the Gboard-patches build. PixelBoard's
   Rambler instead detects 18.0.3 / 18.3.1 / 18.4.1 / 18.5.3 targets at patch
   time and skips gracefully when a build is unknown.
3. **The calculator is off until enabled.** Its runtime reads
   `pref_simple_calculator_enabled` (default `false`) from the
   `gboard_patches_settings` preferences, so the settings toggle is part of
   the feature, not an optional extra. This port includes the toggle.

## What was ported

Extension (`extensions/extension/.../extension/calculator/`, package-renamed,
otherwise byte-identical logic from upstream):

- `GboardCalculatorEngine` — safe arithmetic evaluator (BigDecimal,
  16-digit precision, bounded input/operations/parenthesis depth)
- `GboardCalculatorRuntime`, `GboardCalculatorLifecycleRuntime`,
  `GboardCalculatorSessionState`, `GboardCalculatorSafety`
- `GboardCalculatorSuggestion`, `GboardCalculatorSuggestionView`,
  `GboardCalculatorDisplaySnapshot`, `GboardCalculatorText`
- `GboardCalculatorSettings`, `GboardCalculatorSettingsFeature` — the
  Advanced Settings screen with the enable toggle
- Unit tests for Engine / Safety / SessionState / Suggestion (from upstream)

Extension wiring:

- `GboardPatchesFeatureAvailability.FEATURE_SIMPLE_CALCULATOR`
  (`com.akshaykadam.pixelboard.feature.simple_calculator`)
- `GboardPatchesSettingsFeatureRegistry` registers the calculator feature
- 12 localized strings added to `settings-text/gboard_settings_text.xml`

Patches (`patches/.../patches/gboard/`):

- `features/calculator/GboardCalculatorLifecyclePatch.kt` — hooks the IME
  lifecycle and delegates to the runtime via three new `RuntimeAbi` calls
  (`CALCULATOR_RUNTIME_ON_INPUT_VIEW_STARTED`,
  `CALCULATOR_RUNTIME_ON_SELECTION_UPDATED`,
  `CALCULATOR_RUNTIME_ON_INPUT_WINDOW_HIDDEN`)
- `features/calculator/GboardCalculatorTargets.kt`
- `features/calculator/GboardCalculatorFeatureMarkerPatch.kt`
- `registry/GboardPatchRegistry.kt` — new public **Simple Calculator** patch
- `registry/GboardContributionWiring.kt` — `simple_calculator.lifecycle` unit
- `gboard-port-product-catalog.json` — new `simple_calculator` feature
  (catalog 1.13.0), plus `patches-list.json` and `options.json` entries

### Deliberate improvement over upstream

Upstream's lifecycle patch hard-codes `Lphr;` and *throws* on any other
Gboard build. This port:

1. tries the verified `Lphr;` / field `i` fast path (18.5.3 + 18.5.4),
2. falls back to locating the IME service class by its framework lifecycle
   signatures (the three override signatures + an `InputView` field) — the
   same find-by-signature style PixelBoard's spell-checker patch uses,
3. **skips silently** if a future Gboard moves the class, instead of failing
   the entire patch run and taking Rambler down with it.

It also drops upstream's 18.5.4-only `registerCount == 8` assertion on
`onUpdateSelection`; the injected call only relies on the framework
parameter positions (`p3`/`p4` = new selection start/end), which are stable.

## Verification done

- All 9 calculator runtime classes compile clean against Android API 35.
- Settings classes compile clean against PixelBoard's real settings
  contract/settings sources in this repo.
- Upstream unit tests pass: **13/13** (Engine, Safety, SessionState,
  Suggestion).
- New Kotlin patch files parse clean and show the same no-classpath error
  profile as PixelBoard's own shipping patch files (full type-check happens
  in the Gradle build below).

## Build the patch bundle (.mpp)

Requirements: **JDK 21** and a **GitHub token with `read:packages`** —
the Morphe plugin (`app.morphe.patches`) is served from GitHub Packages,
which requires authentication even for public packages. Without the token
the build stops at plugin resolution (verified).

```bash
# one-time: provide GitHub Packages credentials
#   ~/.gradle/gradle.properties:  gpr.user=<github-username>
#                                 gpr.key=<token with read:packages>
# or env: GITHUB_ACTOR=<username>  GITHUB_TOKEN=<token>

./build.sh -r            # rebuilds patches-1.4.0.mpp from source
./build.sh input/gboard-18.5.3.apk   # optional: produce a ready APK
```

Or apply `patches-1.4.0.mpp` in **Morphe Manager** on-device to a stock
Gboard 18.5.3 APK, with all five patches enabled (Advanced Voice Typing,
AI Writing Tools, Add Gboard Signature Bypass, Package Rename,
Simple Calculator).

## Use it

1. Install the patched APK (installs side-by-side as PixelBoard).
2. Gboard settings → **Advanced settings** (PixelBoard) →
   **Simple Calculator** → enable **Enable Simple Calculator**.
3. Reopen the keyboard, type `2+3×4` in any app — `= 14` appears above the
   keyboard. Tap it to replace the expression; long-press to copy.
4. Rambler: Gboard settings → Voice typing → select the Rambler/advanced
   voice typing option exactly as in stock PixelBoard — unchanged.

## Credits

- Rambler, AI Writing Tools and the PixelBoard patch framework:
  Akshay Kadam (@Akshayykadam), based on the Gboard patch framework by
  Jason Wu (@jasonwu1994)
- Simple Calculator: Jason Wu (@jasonwu1994), Gboard-patches (GPL-3.0)
