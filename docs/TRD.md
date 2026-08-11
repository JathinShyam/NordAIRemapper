# Technical Requirements Document (TRD)

**Product:** Nord AI Remapper  
**Companion docs:** [PRD.md](./PRD.md), [ARCHITECTURE.md](./ARCHITECTURE.md)  
**Document version:** 1.0  
**Build baseline:** Gradle Kotlin DSL · AGP 8.5.x · Kotlin 2.0.x · compile/targetSdk 35 · minSdk 33

---

## 1. Purpose

This TRD specifies **how** Nord AI Remapper must be built and verified: stack, APIs, permissions, detection contracts, data schemas, non-functional requirements, and phased build gates. Product *why* lives in the PRD; module topology and runtime flows live in Architecture.

---

## 2. Tech stack (normative)

| Layer | Choice | Notes |
|---|---|---|
| Language | Kotlin | Prefer idiomatic sealed classes / Flow |
| UI | Jetpack Compose + Material 3 | One ViewModel per screen; StateFlow UI state |
| Architecture | MVVM + Clean (domain / data / presentation / service) | Domain has no Android UI types |
| DI | Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint` services/activities |
| Persistence | Room (configs/snapshots) + DataStore Preferences (settings) | |
| Serialization | kotlinx.serialization | Polymorphic `RemapAction` JSON |
| Async | Coroutines | Gesture wait-then-decide on Main / controlled scopes |
| Build | Gradle KTS + version catalog | `./gradlew assembleDebug` after each phase |

### 2.1 Font requirement

- Infrastructure under `res/font/` for OnePlus Sans (`regular` / `medium` / `bold`).  
- **Ship default:** `FontFamily.Default` (Roboto on stock).  
- Swap to OnePlus Sans must be a **one-line** change in `ui/theme/Type.kt` when `.ttf` files are supplied locally.  
- Do **not** commit proprietary OnePlus Sans binaries to the public repo.

---

## 3. Detection requirements (critical)

### 3.1 Dual strategy contract

Implement both strategies. Selection is persisted (`DetectionStrategy`) and changeable in **Settings → Developer**.

| Strategy | Mechanism | Identity matching |
|---|---|---|
| **A — Accessibility** | `AccessibilityService` + `canRequestFilterKeyEvents` + `FLAG_REQUEST_FILTER_KEY_EVENTS`; `onKeyEvent()` | Match learned `keyCode` and/or `scanCode` (prefer scanCode when &gt; 0; key may be `KEYCODE_UNKNOWN` / 0) |
| **B — Logcat** | Foreground service tails `logcat`; match editable pattern (default `KEYLOG_OplusKeyEventUtil`) | Events may be DOWN/UP/PULSE; PULSE path uses gesture `onPulse()` |

Shared bus: detectors emit `RawKeyEvent` → `RemapEngine` filters by strategy + learned identity → `GestureClassifier` → resolve `PressType` → `ActionDispatcher`.

### 3.2 Absolute constraints

| ID | Requirement |
|---|---|
| TR-DET-01 | **Never** hard-code Nord 5 key identity as the only path; learn-and-save is mandatory for Strategy A |
| TR-DET-02 | Accessibility `onKeyEvent` path must stay thin: emit + consume decision only; no Room/network/IO on the key dispatch thread |
| TR-DET-03 | Unrelated keys (volume, power, etc.) must pass through (`return false` unless learned Plus Key match) |
| TR-DET-04 | Strategy B requires `READ_LOGS`; show copyable ADB: `adb shell pm grant com.nordairemapper android.permission.READ_LOGS` |
| TR-DET-05 | Logcat match pattern is user-editable and persisted |
| TR-DET-06 | Logcat start uses `-T 1` (or equivalent) to avoid replaying historical lines |
| TR-DET-07 | Debounce duplicate PULSE lines (e.g. ~150 ms) |

### 3.3 Gesture classification

| Parameter | Range | Default |
|---|---|---|
| Double-press window | 200–500 ms | 300 ms |
| Long-press threshold | 300–1000 ms | 500 ms |

**Algorithm (wait-then-decide):**

1. Key down starts long-press timer.  
2. If hold ≥ long threshold → fire `LONG_PRESS`; ignore matching up for that gesture.  
3. Key up (before long) increments tap count.  
4. Second tap within window → `DOUBLE_PRESS`.  
5. Else after window → `SINGLE_PRESS`.  
6. Single must **not** fire before the double window expires.

Classifier is single-threaded relative to its CoroutineScope (Main.immediate in engine).

### 3.4 Dual-fire caveat (implementation)

Without root, stock Plus Key action may still run. UI must expose troubleshooting when detection is unconfirmed or dual-fire is reported. Do not claim “consume always works” for Strategy B.

---

## 4. Action execution requirements

### 4.1 `RemapAction` sealed hierarchy

All actions are kotlinx.serialization polymorphic types with stable `@SerialName` values (see Architecture / domain model). New actions require:

1. Sealed subtype + serial name  
2. Executor branch  
3. UI catalog entry (icon, title, description, category)

### 4.2 API mapping (normative)

| Action | Implementation notes |
|---|---|
| Launch app | `PackageManager.getLaunchIntentForPackage`; requires `<queries>` MAIN/LAUNCHER |
| Assistant | `ACTION_VOICE_COMMAND`; fallback `ACTION_ASSIST` |
| Camera | `MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA` + front extras when needed |
| Flashlight | `CameraManager.setTorchMode`; track torch via `TorchCallback`; no CAMERA permission |
| Screenshot / lock / recents / home / back / notifications / QS | `AccessibilityService.performGlobalAction(...)` via connected service holder |
| DND | `ACCESS_NOTIFICATION_POLICY` + `NotificationManager` interruption filter; else open policy settings |
| Ringer cycle | `AudioManager.ringerMode` NORMAL → VIBRATE → SILENT → NORMAL |
| Media keys | `dispatchMediaKeyEvent` DOWN+UP |
| Volume | `adjustStreamVolume(STREAM_MUSIC, …, FLAG_SHOW_UI)` |
| Auto-rotate | `WRITE_SETTINGS` / `Settings.System.ACCELEROMETER_ROTATION`; else `ACTION_MANAGE_WRITE_SETTINGS` |
| URL | `ACTION_VIEW` |
| Overlay | Start/show `FloatingOverlayService` (when implemented) |
| None | No-op |

Failures: log and fail soft; prefer deep-link to grant missing special permissions over crashing.

### 4.3 Haptics

If `hapticFeedback` preference is true and action ≠ None → short click vibration (`VibrationEffect`).

---

## 5. Persistence requirements

### 5.1 Room

| Entity | Purpose |
|---|---|
| `remap_configs` | PK `pressType` (`single`/`double`/`long`); `actionJson` |
| `overlay_config` | Single row id=0; full `OverlayConfig` JSON |
| `config_snapshots` | Auto id, name, timestamp, payload JSON |

Database name: `nord_remapper.db`. Export schema optional for MVP (`exportSchema = false` acceptable until migrations harden).

### 5.2 DataStore

Preferences for: service enabled, detection strategy, key identity (keyCode/scanCode), timings, logcat pattern, theme, dynamic color, notification, haptic, excluded package set.

### 5.3 Backup format

- Versioned JSON envelope (include `schemaVersion`, remap map, overlay, relevant settings).  
- SAF only — **no** `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` at minSdk 33.

---

## 6. UI / navigation requirements

| Screen | Technical notes |
|---|---|
| Home | Compose; observe configs + settings + service enabled state |
| Remap Config | Navigation arg: `PressType`; bottom sheet app picker |
| Overlay Settings | Live preview composable mirroring WindowManager layout params where practical |
| Backup | Activity result contracts for create/open document |
| Settings / Developer / Key Learning | Existing routes: `home`, `key_learning`, `developer` |
| Onboarding | Gated once via DataStore flag; skip disabled for required permission steps |

**Component rules:** 16 dp cards; pill toggles; bottom sheets preferred; no FABs; spring animations for state changes as specified in PRD.

**Theme tokens:** implement in `Color.kt` / `Type.kt` / `Shape.kt` / `Theme.kt` exactly as product palette.

---

## 7. Services & manifest

### 7.1 Components

| Component | Type | Role |
|---|---|---|
| `PlusKeyAccessibilityService` | AccessibilityService | Strategy A + global actions + foreground package for exclusions |
| `LogcatWatcherService` | FGS `specialUse` | Strategy B |
| `FloatingOverlayService` | FGS + overlay window | Overlay UI |
| `BootReceiver` | BroadcastReceiver | Re-arm detection after boot |

### 7.2 Permissions (declare / request as appropriate)

| Permission | Mode |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Service declaration only (system bind) |
| `SYSTEM_ALERT_WINDOW` | Special access settings |
| `FOREGROUND_SERVICE` | Normal |
| `FOREGROUND_SERVICE_SPECIAL_USE` | + `<property>` subtype string explaining Plus Key log watch / overlay |
| `RECEIVE_BOOT_COMPLETED` | Normal |
| `VIBRATE` | Normal |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Special access UX |
| `POST_NOTIFICATIONS` | Runtime (API 33+) |
| `ACCESS_NOTIFICATION_POLICY` | Special access for DND |
| `WRITE_SETTINGS` | Special access for auto-rotate |
| `READ_LOGS` | Signature/privileged; ADB `pm grant` only |

`<queries>`: MAIN + LAUNCHER for app enumeration.

### 7.3 Accessibility XML

Must include:

- `android:canRequestFilterKeyEvents="true"`  
- flags including `flagRequestFilterKeyEvents`  
- Minimal event types needed (window state for exclusions when implemented)  
- `canRetrieveWindowContent="false"` unless a future feature strictly requires otherwise  
- User-facing description string emphasizing key-only purpose  

---

## 8. Overlay technical requirements

| Item | Spec |
|---|---|
| Window type | `TYPE_APPLICATION_OVERLAY` |
| Layout styles | Radial/arc **or** horizontal pill bar |
| Slots | Max 6 |
| Position | Left edge / right edge / bottom center |
| Opacity | 0.3–1.0 |
| Icon size | S / M / L |
| Animation | Fade / scale / slide; spring scale+fade entrance |
| Lock screen | Do not show (check keyguard) |
| Dismiss | Outside tap |

---

## 9. Non-functional requirements

| Category | Requirement |
|---|---|
| Performance | Key path &lt; 1 ms of work before return from `onKeyEvent` |
| Battery | Logcat watcher only when Strategy B selected and service enabled; Importance MIN notification |
| Reliability | Sticky / START_STICKY where appropriate; death notification; boot re-arm |
| Security | No plaintext secrets; no telemetry by default |
| Maintainability | Comment only non-obvious detection/gesture logic |
| Build | JDK 17; `assembleDebug` clean after each build-order phase |

---

## 10. Testing requirements

| Level | Scope |
|---|---|
| Unit | `GestureClassifier` timings (single vs double vs long); JSON round-trip for `RemapAction` |
| Instrumented / manual | Key learning on Nord 5; Strategy A vs B; each action smoke test |
| Compile gate | `./gradlew assembleDebug` after every phase in §12 |

Device matrix for release: Nord 5 on current OxygenOS; note OS build in release notes.

---

## 11. Error handling & observability

- Soft-fail action execution with `Log.w`.  
- Developer raw event list capped (e.g. last 100).  
- Home troubleshooting banner when: service disabled, identity unconfigured (Strategy A), or `READ_LOGS` missing (Strategy B).  

---

## 12. Build order (implementation gates)

Each phase ends with **green** `./gradlew assembleDebug`.

| # | Deliverable | Status in repo (as of docs) |
|---|---|---|
| 1 | Project setup, theme | Done |
| 2 | Domain + Room + DataStore + repos | Done |
| 3 | Accessibility detector, classifier, key learning UI | Done |
| 4 | Logcat detector + Developer screen | Done |
| 5 | RemapAction executors | Done |
| 6 | Home (diagram, cards, status, toggle) | Pending |
| 7 | Remap Config + app picker | Pending |
| 8 | Onboarding | Pending |
| 9 | FloatingOverlayService + Overlay Settings | Pending |
| 10 | Backup & Restore (SAF) | Pending |
| 11 | Settings + per-app exclusions | Pending |
| 12 | BootReceiver, death notification, battery handling | Pending |

---

## 13. Explicit non-requirements (engineering)

- Root hooks, Xposed, Shizuku as hard dependencies for MVP.  
- Hard-coded scanCode tables per firmware in production path.  
- Storage permission-based backup.  
- Blocking work or GestureClassifier IO on Accessibility key thread.

---

## 14. Traceability

| PRD area | TRD sections |
|---|---|
| Detection | §3 |
| Actions | §4 |
| Screens / design | §6–§8, PRD §8 |
| Permissions | §7 |
| Resilience | §7, §9, build phase 12 |

---

## Document history

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-11 | Initial TRD |
