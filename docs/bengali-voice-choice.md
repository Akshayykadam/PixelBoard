# Bengali voice typing: per-language engine choice

Implements the selector requested in #93 on the **Gboard 18.5.3 base used by PixelBoard 1.3.1**.

## User behavior

Open **PixelBoard Settings → Languages → বাংলা (বাংলাদেশ) → Language settings → Voice typing**.

The native single-choice dialog offers:

- **Standard — Bengali script** (default).
- **Rambler — AI cleanup; may use Roman text**.

The same option is available for Bengali (India). It is hidden for every non-Bengali language, irrespective of the phone's display language. A choice applies to every layout of the same Bengali region, including transliteration and handwriting. Bangladesh and India retain separate choices. Changes apply on the next microphone activation and survive a keyboard/process restart.

| Active keyboard | Saved local choice | Dictation route |
| --- | --- | --- |
| Bengali, missing preference | Standard default | Standard |
| Bengali | Standard | Standard, even when global Rambler is selected |
| Bengali | Rambler | Rambler, even when global Standard is selected |
| English, Chinese, or another language | Any Bengali preference | Existing global behavior |
| Unknown input language / unsupported base | Any | Existing native behavior |

The feature selects the existing engine. It does not add Bengali support to Google's Rambler model or transliterate recognition results. Standard recognition still depends on the device's Bengali recognition service/language availability.

## UI implementation

`GboardBengaliVoiceSettingsUi.afterLanguageSettingsRebuilt(Object)` runs after the native per-language category has been rebuilt. The native screen is shared by all languages and clears its children on each rebuild, so a shared XML-only preference would have the wrong visibility and would disappear during updates.

The adapter reads the **edited language** from fragment field `am`, and the native **Language settings** category from `ar`. It attaches an ordinary native Preference only when the locale language is `bn`. It preserves existing rows, replaces only its own row on repeat calls, and restores the previous row/visibility if attaching fails. A cancelled dialog writes nothing. A failed preference write keeps the prior summary and reports the failure.

The UI uses the host's native Preference and Android radio dialog, with existing theme, text sizing, focus and touch behavior. The focused UX search supplied no verified match for this exact host screen; native control defaults are used. Small-screen, landscape, large-text, dark/light and TalkBack rendering still require Android verification.

Labels are defined in `gboard_settings_text.xml` in English and Bengali and read through `GboardSettingsText`. This is required because the extension carrier merges **DEX classes**, not the extension's Android resource table. Passing extension resource IDs directly to the host's `Context.getString` can collide with base resources.

## Persistence and routing

`GboardBengaliVoicePolicy` strictly validates language tags and normalizes Bengali layouts to `bn` plus region. `GboardBengaliVoiceSettings` stores only `pref_bengali_voice_mode.<locale>` in PixelBoard's existing device-protected settings preferences. It never writes the global `enable_jetson` preference.

Android's [SharedPreferences implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/SharedPreferencesImpl.java#607) updates memory before reporting disk-write success. A failed or throwing save therefore attempts to restore only the prior effective voice choice, or remove a previously absent key. This is best effort; unavailable storage cannot guarantee disk rollback.

The runtime uses the native **current input language** at decision time. It does not infer it from `Locale.getDefault()`, the display locale, or the language settings screen currently being edited. The native selected-language accessor is queried live, so a Bengali preference does not persist into a later English/Chinese session. Immutable version/reflection metadata may be cached; the current language is not.

The three injected boundaries are:

| Native anchor in 18.5.3 | Hook | Purpose |
| --- | --- | --- |
| `LanguageSpecificSettingFragment.aU()V`, before each return | `afterLanguageSettingsRebuilt(Object)` | Add the row after native rebuild |
| `nca.a(Context)Z`, after the existing global-selection observer | `afterNativeSelection(Z)Z` | Override the local returned route without corrupting the saved/global selection cache |
| `mql.B(Context)Z`, before existing mic gating | `activeRouteOverride()I` | Explicitly choose Standard (`1 → false`) or Rambler (`2 → true`); `0` follows original code |

The selected-language anchor is `pem.e()Locale`, which returns the current input language or null. The similarly named display/context locale accessor and subtype construction path are unsuitable: subtype construction can itself read feature flags. Target-flag filtering and a re-entry guard prevent recursive native lookups from the shared flag getter.

Register safety is deliberate: `aU` uses 19 registers, so the fragment is passed using `invoke-static/range {p0 .. p0}` (physical `v18`). In `nca.a`, the original Context register is reused for the boolean result; the post-selection hook accepts only that boolean. No Context is read from an overwritten register and no register-count rewrite is needed.

The installer runs only for the existing 18.5.3 selector target and validates all required native methods. Older base versions receive no new native UI/selection hooks. Runtime version/type checks preserve existing behavior if the selected-language binding is unavailable. Voice-settings and default-initialization scopes continue using the existing global selector.

## Source map

- [Locale policy](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/bengalivoice/GboardBengaliVoicePolicy.java) and [persisted choices](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/bengalivoice/GboardBengaliVoiceSettings.java).
- [Native UI adapter](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/bengalivoice/GboardBengaliVoiceSettingsUi.java) and [live routing](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/bengalivoice/GboardBengaliVoiceRuntime.java).
- [Flag/readiness integration](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/advancedvoice/GboardAdvancedVoice1803Runtime.java), [global scope protection](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/rambler/GboardRambler1803OfficialSelectionRuntime.java), and [explicit flag choices](../pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/rambler/GboardRambler1803StockPolicy.java).
- [Native injection boundaries](../pixelboard-patches/patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/bengalivoice/GboardBengaliVoiceChoiceHooks.kt) and [typed runtime ABI](../pixelboard-patches/patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/shared/runtimeabi/RuntimeAbi.kt).
- [Behavior tests](../pixelboard-patches/extensions/extension/src/test/java/com/akshaykadam/pixelboard/extension/bengalivoice/) and [bytecode tests](../pixelboard-patches/patches/src/test/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/bengalivoice/GboardBengaliVoiceChoiceHooksTest.kt).

The complete paths and implementation are visible in this PR's file diff. No change to the separate Personal Dictionary fix is included.

## Validation and integration

Focused harnesses compile the production Java/Kotlin sources and execute the tracked regression bodies. UI/runtime harnesses use explicit minimal Android collaborators; they are not Robolectric or native Android rendering runs. The hook harness uses the bundled Morphe MutableMethod, smali assembler and actual published APK methods; it applies the installer twice, checks register/return behavior and serializes the modified methods into a scratch DEX. It does not produce or execute a rebuilt APK.

| Focused validation | Result |
| --- | --- |
| Native UI adapter, persistence, cancel/failure handling, resource-ID collision | 8/8 passed |
| Locale policy/settings/runtime: 18 tracked cases and 11 integration scenarios | 29/29 passed |
| Kotlin hook fixtures using the real Morphe assembler/mutable methods | 4/4 passed |
| Actual published APK anchors: double application, register checks, DEX serialization | Passed |

The runtime scenarios include global Standard/Rambler cross-checks, keyboard-language switching, unrelated flags performing zero native locale lookups, one lookup for a targeted flag, cached immutable package metadata and a native locale getter re-entering the flag getter without recursion. Independent review found no remaining blocker after fixing string-resource integration and the recursive subtype lookup.

The full project Gradle build is currently blocked before compilation by unresolved `app.morphe.patches:1.3.3` in the configured repositories. Once that dependency is available, run the extension's Bengali tests and `GboardBengaliVoiceChoiceHooksTest`, rebuild the patch bundle, and apply it to the original supported base APK through the project's normal build workflow.

On an Android device, verify:

1. Bengali Bangladesh/India show the row; English and Chinese do not. The phone's display language must not determine visibility or routing.
2. A fresh Bengali choice is Standard. Switch both directions, leave/reopen the screen and restart the keyboard; the choice remains.
3. Bengali Standard with global Rambler uses the standard recognizer. Bengali Rambler with global Standard uses Rambler. Neither changes the global selection for other languages.
4. Dictate “কেমন আছো?” and a mixed Bengali/English sentence in Standard; check the actual recognition output and service availability. Rambler remains an explicit user choice with its current backend limitations.
5. Switch Bengali → English → Chinese → Bengali and confirm there is no stale engine choice, recursion, crash or duplicate settings row.
6. Check cancellation, storage failure, TalkBack selected-state announcement, large text, small-screen/landscape layout and both themes.

These device checks and full APK integration remain pending; the PR should remain a draft until they are completed.
