# Nord AI Remapper

Remap the **OnePlus Nord 5's physical Plus Key** (the side-mounted "AI Key") to custom
actions — single press, double press, and long press — with an optional floating
overlay menu. Inspired by the Essential Remapper apps built for Nothing Phone.

> Not affiliated with, endorsed by, or connected to OnePlus / OPPO.

## How key detection works

Unlike Nothing Phone's Essential Key, the OnePlus Plus Key is handled by a system
component (`OplusKeyEventUtil`) and may never reach third-party apps as a normal
`KeyEvent`. The app therefore implements **two detection strategies** behind a common
`KeyDetector` interface, selectable in Settings → Developer:

- **Strategy A — Accessibility key filtering**: an `AccessibilityService` with
  `FLAG_REQUEST_FILTER_KEY_EVENTS` observes hardware keys. A "learn your key" flow
  captures the keyCode + scanCode at runtime instead of hard-coding anything.
- **Strategy B — Logcat watcher** (likely required on OnePlus): a foreground service
  tails logcat for `KEYLOG_OplusKeyEventUtil` entries. Requires the `READ_LOGS`
  permission, granted once via ADB:

  ```
  adb shell pm grant com.nordairemapper android.permission.READ_LOGS
  ```

Gestures (single / double / long press) are classified with wait-then-decide coroutine
logic; double-press window and long-press threshold are configurable.

## Tech stack

- Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture
- Hilt · Room · DataStore · kotlinx.serialization
- Min SDK 33 (Android 13) · Target SDK 35

## Building

```
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (platform 35).

## Font

The design targets OnePlus Sans, which is not freely redistributable. The app ships
with the system font; see `ui/theme/Type.kt` for how to drop in the OnePlus Sans
`.ttf` files locally.

## Docs

| Document | Purpose |
|---|---|
| [docs/PRD.md](docs/PRD.md) | Product requirements, journeys, success criteria |
| [docs/TRD.md](docs/TRD.md) | Technical requirements, APIs, permissions, build gates |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, packages, pipelines, ADRs |
| [docs/roadmap/README.md](docs/roadmap/README.md) | Phase-by-phase implementation roadmap |

## Status

Work in progress. Phases 1–5 (scaffold → detection → action executors) are in tree.
Next: Home UI, Remap Config, onboarding, overlay, backup, settings, resilience.
Validate Plus Key detection on a Nord 5 before investing heavily in chrome.
