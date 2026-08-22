# Keyforge — agent instructions

Remap the **OnePlus Nord 5 Plus Key** (physical side button) to single / double / long press plus an optional overlay. Package `com.nordairemapper`. Not affiliated with OnePlus.

Read this file at the start of every session. Then load the matching skill under `.cursor/skills/` before editing that area.

| Skill | Use when |
|---|---|
| [plus-key-detection](.cursor/skills/plus-key-detection/SKILL.md) | Detection, gestures, logcat, accessibility, key learning, remap engine |
| [device-testing](.cursor/skills/device-testing/SKILL.md) | ADB, `READ_LOGS`, on-device debug, capturing Plus Key logs |
| [ship-debug-apk](.cursor/skills/ship-debug-apk/SKILL.md) | Build, install, GitHub `latest-debug` APK, signing mismatch |
| [nord-compose-ui](.cursor/skills/nord-compose-ui/SKILL.md) | Compose UI polish / rebuild (theme, screens, components); wraps RoninForge Compose skills |

Product/tech truth lives in `docs/PRD.md`, `docs/TRD.md`, `docs/ARCHITECTURE.md`. Do not invent a second architecture.

**Change logs:** For every non-trivial implementation, write separate notes under `docs/changes/YYYY-MM-DD-<slug>/` (one markdown file per logical change) and index them in `docs/changes/README.md`. Include why, what/files, verify steps, and debug tips. Do this in the same turn as the code.

## Stack

Kotlin 2.0 · Compose Material 3 · Hilt · Room + DataStore · coroutines/Flow · minSdk 33 / target 35.

```
presentation → domain ← data
service depends on domain + Android APIs
```

`domain` has no Android UI types. One ViewModel per screen. Detectors stay thin; `RemapEngine` owns decisions.

## Hard constraints

1. **Never hard-code Plus Key identity** as the only path (`TR-DET-01`). Strategy A learns `keyCode`/`scanCode` at runtime. Prefer `scanCode` when `> 0`.
2. **Strategy B (logcat) is first-class.** On Nord 5 the Plus Key usually never reaches Accessibility as a `KeyEvent`. Volume keys appearing in Key setup is expected; Plus Key missing is expected.
3. **Logcat events are pre-matched** by the user pattern (default `KEYCODE_ACTION_BUTTON_CLICK`). They use `keyCode/scanCode = -1`. Do **not** run `matchesPlusKey` on `DetectionStrategy.LOGCAT`. One press must emit one DOWN and one UP (`LogcatKeyEdgeCoalescer`).
4. **Do not treat logcat lines as completed taps.** Pair down/up (including KEYLOG lines with no ACTION_*). `"undefined"` contains the letters `down` — do not substring-match `"down"`.
5. **Accessibility `onKeyEvent` stays thin:** emit + consume decision only. Consume **only** the learned Plus Key. Volume/power must pass through (`return false`).
6. **Wait-then-decide gestures.** Single press must not fire before `doublePressWindowMs` (default 300). Long press fires at `longPressThresholdMs` (default 500) from DOWN, then ignore that UP.
7. **No root / Magisk / LSPosed.** Stock Plus Key may still fire without root — document, do not pretend we swallow it.
8. **Do not commit OnePlus Sans `.ttf` files.** Theme stays `FontFamily.Default` until fonts are supplied locally.
9. **No analytics, ads, accounts, or cloud sync.**
10. System actions (screenshot, lock, recents, shade, QS) still need Accessibility connected even when detection is logcat.

## Runtime pipeline

```
PlusKeyAccessibilityService  ─┐
                              ├──▶ KeyEventBus ──▶ RemapEngine ──▶ GestureClassifier
LogcatWatcherService         ─┘                      │
                                                     ├── strategy + identity filter
                                                     ├── excluded apps
                                                     └── ActionDispatcher
```

Hot files: `service/RemapEngine.kt`, `GestureClassifier.kt`, `LogcatWatcherService.kt`, `PlusKeyAccessibilityService.kt`, `KeyEventBus.kt`.

## Commands

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```

ADB (often not on PATH):

```bash
"$HOME/Android/Sdk/platform-tools/adb"
```

Local debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

CI on `main` updates [latest-debug](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug) and also creates an immutable `debug-<sha>` release so older APKs stay available. Local debug keystore **cannot** overwrite that install (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Prefer commit + push for device testing unless the user asks to uninstall.

## Product tone

Honest about OnePlus limitations. Prefer proving detection over polishing chrome. Keep diffs scoped; do not drive-by refactor or edit docs unless asked.
