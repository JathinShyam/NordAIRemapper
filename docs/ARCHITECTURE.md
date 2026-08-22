# Architecture Document

**Product:** Keyforge  
**Companion docs:** [PRD.md](./PRD.md), [TRD.md](./TRD.md), [changes/](./changes/) (per-change debug notes)  
**Document version:** 1.1  
**Codebase package:** `com.nordairemapper`

---

## 1. Overview

Keyforge is a single-module Android app that remaps the OnePlus **Plus Key** using a **pluggable detection layer**, a shared **gesture classifier**, and a **sealed-action executor**. UI is Jetpack Compose; configuration is local (Room + DataStore).

The architecture optimizes for three realities:

1. Plus Key identity is **device/firmware-dependent** → learn at runtime.  
2. OnePlus may never deliver a normal `KeyEvent` → **Strategy B (logcat)** is a first-class peer, not an afterthought.  
3. The key dispatch path must stay **non-blocking** → thin detectors, fat engine off the hot path.

---

## 2. Architectural style

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation (Compose)                   │
│  Screens · ViewModels · NavHost · UI components · Theme      │
└────────────────────────────┬────────────────────────────────┘
                             │ StateFlow / suspend
┌────────────────────────────▼────────────────────────────────┐
│                     Domain                                   │
│  Models · Repository interfaces · (future use cases)         │
└────────────────────────────┬────────────────────────────────┘
                             │ implementations
┌────────────────────────────▼────────────────────────────────┐
│                     Data                                     │
│  Room DAOs/entities · DataStore · RepositoryImpl · Json      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     Service (runtime)                        │
│  Detectors → KeyEventBus → RemapEngine → GestureClassifier   │
│            → ActionDispatcher (RemapActionExecutor)          │
│  Overlay FGS · BootReceiver · Accessibility holder           │
└─────────────────────────────────────────────────────────────┘
```

- **MVVM** at the UI boundary.  
- **Clean Architecture** dependency rule: `presentation` → `domain` ← `data`; `service` depends on `domain` (+ Android APIs).  
- **Hilt** `SingletonComponent` for engine, bus, repositories, database, Json.

---

## 3. Package structure

```
com.nordairemapper/
├── NordRemapperApp.kt                 # @HiltAndroidApp
├── data/
│   ├── local/                         # Room DB, entities, DAOs
│   ├── datastore/                     # SettingsRepositoryImpl
│   └── repository/                    # RemapConfigRepositoryImpl
├── domain/
│   ├── model/                         # RemapAction, PressType, OverlayConfig, AppSettings, KeyIdentity…
│   └── repository/                    # Interfaces only
├── presentation/
│   ├── MainActivity.kt
│   ├── navigation/NordNavHost.kt
│   ├── home/
│   ├── detection/                     # Enable Plus Key detection (in-app READ_LOGS grant)
│   ├── remap/
│   ├── overlay/
│   ├── backup/
│   ├── settings/
│   ├── onboarding/
│   └── developer/                     # Key learning + Developer settings
├── service/
│   ├── DetectionCoordinator.kt        # Strategy accept + logcat watcher sync
│   ├── adb/                           # Wireless ADB pair → READ_LOGS grant only
│   ├── KeyEventBus.kt
│   ├── GestureClassifier.kt
│   ├── RemapEngine.kt
│   ├── PlusKeyAccessibilityService.kt
│   ├── LogcatWatcherService.kt
│   ├── RemapActionExecutor.kt
│   ├── ActionDispatcher.kt
│   ├── AccessibilityUtils.kt
│   ├── FloatingOverlayService.kt
│   ├── ActionFeedbackOverlayService.kt
│   └── BootReceiver.kt
├── ui/
│   ├── theme/                         # Color, Type, Shape, Theme
│   └── components/                    # PhoneDiagram, ActionCard…
└── di/                                # AppModule, ServiceModule, RepositoryModule
```

**Note:** Early code uses a shared event bus + engine rather than a formal `KeyDetector` interface type. Architecturally, `PlusKeyAccessibilityService` and `LogcatWatcherService` are the two detector adapters; extracting an explicit `KeyDetector` interface is optional cleanup if both remain thin emitters.

**READ_LOGS grant (2026-08-16):** consumer path is `presentation/detection` + `service/adb` (Wireless Debugging pair). Details: [changes/2026-08-16-in-app-read-logs-grant/](./changes/2026-08-16-in-app-read-logs-grant/).

---

## 4. Runtime detection pipeline

```
                 ┌──────────────────────────┐
                 │ PlusKeyAccessibilitySvc  │  Strategy A
                 │ onKeyEvent → emit DOWN/UP│
                 └────────────┬─────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │   KeyEventBus   │  SharedFlow&lt;RawKeyEvent&gt;
                     └────────┬────────┘
                              │
                 ┌────────────┴────────────┐
                 │                         │
                 ▼                         ▼
        ┌────────────────┐      ┌─────────────────────┐
        │  RemapEngine   │      │ KeyLearningViewModel│
        │ filter strategy│      │ (debug capture UI)  │
        │ + identity     │      └─────────────────────┘
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │GestureClassifier│  wait-then-decide
        └────────┬───────┘
                 │ Gesture → PressType
                 ▼
        ┌────────────────┐     ┌──────────────────┐
        │RemapConfigRepo │────▶│ ActionDispatcher │
        │ getAction()    │     │ RemapActionExec. │
        └────────────────┘     └──────────────────┘

                 ┌──────────────────────────┐
                 │ LogcatWatcherService     │  Strategy B
                 │ logcat -T 1 → match      │
                 │ pattern → DOWN/UP/PULSE  │
                 └────────────┬─────────────┘
                              │
                              └──▶ KeyEventBus (same path)
```

### 4.1 Identity matching (Strategy A)

```kotlin
// Conceptual — see RemapEngine.matchesPlusKey
if (identity.scanCode > 0) scanCode == identity.scanCode
else keyCode == identity.keyCode
```

Logcat events are considered pre-matched by the log pattern; they do not require keyCode/scanCode equality (fields may be -1).

### 4.2 Consume policy (Strategy A)

`onKeyEvent` returns `true` only when remapping is enabled, strategy is Accessibility, and the event matches the learned Plus Key. All other keys return `false` immediately after optional emit for the learning screen (emit happens for all keys so learning can capture candidates; consume stays gated).

**Design tension:** Learning needs to see unrecognized keys; remapping must not consume volume. Current approach: emit broadly, consume narrowly. Revisit if emitting every key is too chatty for production (gate emit behind “learning mode” flag if needed).

### 4.3 Gesture timing

Owned by `GestureClassifier` with timings read from live `AppSettings` via RemapEngine. Single-fire is deferred by `doublePressWindowMs`. Long-press cancels pending single/double for that hold.

---

## 5. Domain model (core)

### 5.1 Press & gesture

| Type | Values |
|---|---|
| `PressType` | SINGLE, DOUBLE, LONG |
| `Gesture` | SINGLE_PRESS, DOUBLE_PRESS, LONG_PRESS |
| `DetectionStrategy` | ACCESSIBILITY, LOGCAT |
| `KeyIdentity` | keyCode, scanCode; `UNCONFIGURED` = (-1, -1) |

### 5.2 `RemapAction` (sealed, serializable)

Categories for UI grouping: Apps, Media, System, Overlay, None.

Notable subtypes: `LaunchApp`, `OpenCamera(front)`, `AdjustMediaVolume(up)`, `OpenUrl`, `ShowOverlay`, `None`, plus system/media objects (`ToggleFlashlight`, `TakeScreenshot`, …).

### 5.3 Overlay & settings

- `OverlayConfig`: enabled, slots (≤6), position, opacity, icon size, animation, layout style.  
- `AppSettings`: toggles, timings, strategy, identity, exclusions, theme.

---

## 6. Data layer

```
SettingsRepository  ←→  DataStore Preferences
RemapConfigRepository ←→ Room (remap_configs, overlay_config)
Config snapshots     ←→ Room (config_snapshots)  // backup UI
Json (kotlinx)       ←→ polymorphic RemapAction / OverlayConfig
```

**Why JSON columns for actions?** Action shape evolves; polymorphic sealed serialization avoids a wide table of nullable columns. Trade-off: harder SQL queries (acceptable — configs are tiny).

**Repositories** expose `Flow` for observation; ViewModels use `stateIn(WhileSubscribed)`.

---

## 7. Presentation architecture

| Pattern | Application |
|---|---|
| Navigation | Compose Navigation; routes `home`, `key_learning`, `developer` (+ planned screens) |
| State | `StateFlow` / `collectAsStateWithLifecycle` |
| DI | `@HiltViewModel` |
| Theme | `NordAIRemapperTheme` wraps Material 3 color/typography/shapes |

**Temporary Home (current):** entry points to Key setup and Developer — intentional until phase 6 phone diagram ships.

**Future Home state sources:**

- Remap configs map  
- Settings (master toggle, identity configured)  
- Accessibility / Logcat service liveness  
- Banner visibility rules (TRD §11)

---

## 8. Action execution architecture

```
ActionDispatcher (interface)
        │
        ▼
RemapActionExecutor
  ├─ App context intents / AudioManager / CameraManager / Settings
  └─ AccessibilityServiceHolder.service?.performGlobalAction(...)
```

Global actions **require** Accessibility connected even if detection uses Logcat. Product implication: Accessibility may still be required for screenshot/lock/etc. while Logcat drives gesture detection — onboarding should not drop Accessibility solely because Strategy B is selected.

Haptics and soft-fail logging live in the executor.

---

## 9. Overlay architecture (planned)

```
RemapAction.ShowOverlay
        │
        ▼
FloatingOverlayService (FGS)
        │
        ▼
WindowManager.addView(TYPE_APPLICATION_OVERLAY)
  - ComposeView or View hierarchy
  - KeyguardManager: abort if locked
  - Touch outside → removeView + stop if appropriate
```

Overlay slot actions reuse `ActionDispatcher`. Preview on Overlay Settings screen is a **Compose mock**, not a second WindowManager instance.

---

## 10. Resilience architecture (planned)

| Concern | Approach |
|---|---|
| Process death | FGS for Logcat/Overlay; Accessibility is system-bound |
| Service death UX | Notification → deep link to re-enable |
| Boot | `BootReceiver` + `RECEIVE_BOOT_COMPLETED` → start watcher if strategy B + enabled; Accessibility re-enabled by user/OS |
| Battery kill | Prompt `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; Home banner if repeatedly dead |

Per-app exclusions: Accessibility `TYPE_WINDOW_STATE_CHANGED` (or equivalent) tracks foreground package; RemapEngine skips dispatch when package ∈ `excludedApps`.

---

## 11. Dependency graph (key types)

```
NordRemapperApp
MainActivity → NordNavHost → Screens → ViewModels
                                      ↓
                         SettingsRepository / RemapConfigRepository / KeyEventBus

PlusKeyAccessibilityService → KeyEventBus, RemapEngine
LogcatWatcherService        → KeyEventBus, RemapEngine, SettingsRepository
RemapEngine                 → KeyEventBus, SettingsRepository, RemapConfigRepository, ActionDispatcher
RemapActionExecutor         → Context, SettingsRepository, AccessibilityServiceHolder
```

Circular risk avoided: services do not depend on ViewModels; UI observes bus/repos only.

---

## 12. Security & privacy architecture

| Topic | Stance |
|---|---|
| Accessibility | Key filtering + minimal window events; disclosure string in resources |
| READ_LOGS | Optional Strategy B; no network exfiltration of log lines |
| Backup | User-initiated SAF; local snapshots only |
| Queries | Manifest queries limited to launcher apps |
| ProGuard | Keep kotlinx.serialization companions for release minify |

---

## 13. Build & CI expectations

- Local: JDK 17, Android SDK 35, `./gradlew assembleDebug`.  
- Phase discipline: no advancing UI phases until detection validated on hardware for at least one strategy.  
- Public GitHub: [JathinShyam/NordAIRemapper](https://github.com/JathinShyam/NordAIRemapper).

---

## 14. Evolution / ADR-style decisions

### ADR-001 — Dual detection, not Accessibility-only

**Context:** OnePlus Plus Key often handled by `OplusKeyEventUtil`.  
**Decision:** First-class Logcat strategy with ADB grant UX.  
**Consequence:** More complex Developer settings; better Nord 5 success rate.

### ADR-002 — Learn key identity at runtime

**Context:** keyCode/scanCode unstable across devices.  
**Decision:** Key learning screen + persisted `KeyIdentity`.  
**Consequence:** Extra onboarding step; no brittle hard-codes.

### ADR-003 — Polymorphic JSON for actions

**Context:** Many action shapes.  
**Decision:** kotlinx.serialization sealed hierarchy in Room text columns.  
**Consequence:** Flexible migrations via `ignoreUnknownKeys`; schema versioning needed for backup files.

### ADR-004 — Thin Accessibility hot path

**Context:** Filtering all key events can break volume if slow/wrong.  
**Decision:** Emit + consume boolean only; classify/dispatch on Main coroutine scope.  
**Consequence:** Slight asynchrony between key and action (acceptable).

### ADR-005 — Accessibility still needed for global actions

**Context:** Logcat cannot call `performGlobalAction`.  
**Decision:** Keep Accessibility service for system actions even under Strategy B.  
**Consequence:** Document in onboarding/PRD.

---

## 15. Current vs target maturity

| Area | Now | Target 1.0 |
|---|---|---|
| Detection A/B | Implemented | Hardware-validated defaults documented |
| Gesture + executors | Implemented | Unit tests for classifier |
| Key learning / Developer | Implemented | Polished copy + banners |
| Home / Remap / Onboarding | Stub / missing | Full UX per PRD |
| Overlay / Backup / Boot | Missing | Per TRD build order 9–12 |

---

## Document history

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-11 | Initial architecture aligned with phases 1–5 codebase |
