# Nord AI Remapper

Remap the **OnePlus Nord 5 Plus Key** (the physical side button marketed for AI features; it replaced the alert slider) to your own actions.

Assign independent actions to **single press**, **double press**, and **long press**, plus an optional **floating overlay** with up to six quick actions — in the spirit of Essential Key remappers on Nothing Phone, adapted for OnePlus’s harder-to-intercept key path.

> **Not affiliated with, endorsed by, or connected to OnePlus / OPPO.**

| | |
|---|---|
| Package | `com.nordairemapper` |
| Version | `0.1.0` |
| Min / target SDK | 33 (Android 13) / 35 |
| Language / UI | Kotlin · Jetpack Compose (Material 3) |
| Repo | [github.com/JathinShyam/NordAIRemapper](https://github.com/JathinShyam/NordAIRemapper) |

---

## What it does

| Feature | Description |
|---------|-------------|
| Gesture remapping | Single / double / long press → any supported action |
| Action catalog | Launch apps, media controls, system shortcuts, URLs, overlay, or none |
| Floating overlay | Optional radial or pill menu (up to 6 slots) via “Show overlay” |
| Key learning | Runtime capture of keyCode + scanCode — no hard-coded device IDs |
| Dual detection | Accessibility key filter **or** logcat watcher (for OnePlus) |
| Onboarding | Guided Accessibility, overlay, notifications, and battery steps |
| Backup | Export/import JSON via SAF; named local snapshots |
| Settings | Theme, haptics, per-app exclusions, battery exemption, Developer tools |

---

## Why detection is special

On Nothing Phone, the Essential Key often reaches apps as a filterable key event (`keyCode=0`, stable `scanCode`). On OnePlus, the Plus Key is handled by system code (`OplusKeyEventUtil`) and **often never arrives** as a normal `KeyEvent`.

Nord AI Remapper therefore supports **two strategies** (Developer settings):

### Strategy A — Accessibility (try first)

1. Enable the app’s Accessibility service.
2. Open **Key setup**, press the Plus Key.
3. Save the captured `keyCode` / `scanCode`.

Uses `AccessibilityService` with `FLAG_REQUEST_FILTER_KEY_EVENTS`. Matching prefers `scanCode` when configured (the key may report `KEYCODE_UNKNOWN` / `0`).

### Strategy B — Logcat watcher (often required on OnePlus)

1. Grant `READ_LOGS` once via ADB (cannot be granted from a normal permission dialog):

   ```bash
   adb shell pm grant com.nordairemapper android.permission.READ_LOGS
   ```

2. In **Developer**, select **Logcat watcher** and (if needed) edit the match pattern (default: `KEYLOG_OplusKeyEventUtil`).

A foreground service tails `logcat` and emits presses into the same gesture pipeline as Strategy A.

### Important caveats

- **Without root, the stock Plus Key action may still fire** alongside your remap. Set the system Plus Key to a harmless default in OxygenOS settings when possible.
- **System actions** (screenshot, lock, recents, home, back, notification shade, quick settings) need the Accessibility service connected even if you detect presses via logcat.
- Gesture timings are configurable (double-press window 200–500 ms, default 300; long-press 300–1000 ms, default 500). Single press waits for the double window so it does not steal double taps.

---

## Remappable actions

| Category | Actions |
|----------|---------|
| Apps | Launch any installed app · Open URL / deep link |
| Media | Play/pause · Next · Previous · Volume up/down |
| System | Assistant · Camera (front/rear) · Flashlight · Screenshot · DND · Ringer cycle · Notification shade · Quick settings · Recents · Home · Back · Lock · Auto-rotate |
| Overlay | Show floating overlay menu |
| None | Disable that press type |

---

## Screens

| Screen | Role |
|--------|------|
| **Onboarding** | Welcome → Accessibility → Overlay → Notifications/battery → All set |
| **Home** | Phone silhouette, master toggle, service status, Single/Double/Long cards, troubleshooting banner |
| **Remap Config** | Categorized action list, app picker, “Try this action now” |
| **Overlay** | Enable, slots, layout/position/opacity/size/animation, live preview |
| **Backup & Restore** | SAF JSON export/import, named snapshots |
| **Settings** | Theme, dynamic color, haptics, exclusions, battery, links to advanced screens |
| **Developer / Key setup** | Detection strategy, timings, logcat pattern, raw key event log |

---

## How it works (architecture)

```
PlusKeyAccessibilityService  ──┐
                               ├──▶ KeyEventBus ──▶ RemapEngine ──▶ GestureClassifier
LogcatWatcherService         ──┘         │              │
                                         │              ├── filter strategy + key identity
                                         │              ├── honor per-app exclusions
                                         │              └── ActionDispatcher (RemapActionExecutor)
                                         │
                              Key learning / debug UI
```

- **Clean Architecture + MVVM**: `domain` · `data` (Room + DataStore) · `presentation` · `service`
- **DI**: Hilt
- **Persistence**: Room for remap/overlay/snapshots; DataStore for preferences
- **UI**: Jetpack Compose, Material 3, dark-first Nord palette (`#0A0A0A` / `#141414` / accent `#0AC6FF`)

Package layout (simplified):

```
com.nordairemapper/
├── data/           # Room, DataStore, repository impls
├── domain/         # models, repository interfaces
├── presentation/   # Compose screens + ViewModels
├── service/        # detection, overlay, boot, executors
├── ui/             # theme + shared components
└── di/             # Hilt modules
```

Deeper design notes: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Requirements

### To build

- **JDK 17**
- **Android SDK** with platform **35** and Build-Tools 35.x
- Linux / macOS / Windows with Gradle wrapper (`./gradlew`)

### To run on device

- Android **13+** (API 33+)
- Primary target: **OnePlus Nord 5** (other OxygenOS “Plus Key” devices: best-effort)
- USB debugging if you need Strategy B (`READ_LOGS`)

---

## Download (no PC build required)

Every push to `main` builds a **debug APK** and publishes **two** GitHub releases:

- **[Latest Debug](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug)** — always the newest (`NordAIRemapper-debug-latest.apk`)
- **`debug-<sha>`** — that commit’s APK, kept forever (e.g. `Debug · e92235f · 2026-08-14`)

[All versions](https://github.com/JathinShyam/NordAIRemapper/releases)

1. For the current build, download `NordAIRemapper-debug-latest.apk`.
2. For an older build, open Releases and pick `Debug · <sha> · <date>`, then `NordAIRemapper-debug-<sha>.apk`.
3. The same APK is also attached on the **Actions** run (Artifacts), kept for 14 days.
4. Install on the phone (allow Install unknown apps).

You can also trigger a rebuild manually: **Actions → Build Debug APK → Run workflow**.

---

## Build

```bash
git clone https://github.com/JathinShyam/NordAIRemapper.git
cd NordAIRemapper

# Point Gradle at your SDK (or let Android Studio create this)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the project in **Android Studio** (Ladybug / recent) and use Run if you prefer.

Release builds use minify/shrink (`./gradlew assembleRelease`); you must supply your own signing config for Play/distribution.

---

## First-run setup (device)

1. Install and open the app → complete **onboarding**.
2. Enable **Accessibility** for Nord AI Remapper when prompted.
3. (Optional) Grant **Display over other apps** for the floating overlay.
4. Allow **notifications** and consider **battery optimization exemption** so detection is not killed.
5. Open **Key setup** (Home banner or Developer):
   - Press the Plus Key.
   - If events appear → **Set as Plus Key** (Strategy A).
   - If nothing appears → Developer → **Logcat watcher** → run the ADB grant command above → restart watcher → confirm events.
6. On Home, tap **Single / Double / Long** and assign actions.
7. (Optional) Overlay screen → fill slots → assign **Show overlay** to a press type.
8. Toggle **Remapping enabled** on Home when you want it active.

---

## Permissions

| Permission | Why |
|------------|-----|
| Accessibility (`BIND_ACCESSIBILITY_SERVICE`) | Key filtering (Strategy A) + global actions |
| `SYSTEM_ALERT_WINDOW` | Floating overlay |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Logcat watcher + overlay FGS |
| `POST_NOTIFICATIONS` | Service / alert notifications |
| `RECEIVE_BOOT_COMPLETED` | Re-arm detection after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Reduce OEM kills |
| `ACCESS_NOTIFICATION_POLICY` | Toggle DND |
| `WRITE_SETTINGS` | Toggle auto-rotate |
| `READ_LOGS` | Strategy B only (ADB `pm grant`) |
| `VIBRATE` | Haptic feedback |

Backup uses the **Storage Access Framework** — no legacy storage permissions.

Privacy stance: Accessibility is for hardware keys (and window package for exclusions), not for reading passwords or screen content. Logcat lines stay on-device. No accounts or analytics in this project.

---

## Configuration & design notes

### Font

UI targets **OnePlus Sans**, which is not freely redistributable. The app ships with the **system font** (Roboto on stock). To use OnePlus Sans locally, drop `.ttf` files under `res/font/` and switch `NordFontFamily` in [`Type.kt`](app/src/main/java/com/nordairemapper/ui/theme/Type.kt).

### Theme

Dark-first OxygenOS-inspired palette; Light / System / optional Material You dynamic color in Settings.

---

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/PRD.md](docs/PRD.md) | Product requirements, journeys, acceptance |
| [docs/TRD.md](docs/TRD.md) | Technical requirements, APIs, permissions, build gates |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Pipelines, packages, ADRs |
| [docs/roadmap/README.md](docs/roadmap/README.md) | Phase-by-phase implementation status |

---

## Project status

All planned implementation phases (**1–12**) are in tree and compile. **On-device validation of Plus Key detection on a Nord 5** is still the critical gate before treating remapping as production-ready — confirm Strategy A and/or B on your firmware build and note any logcat pattern changes after OS updates.

---

## Contributing

1. Prefer small, phase-aligned changes; keep `./gradlew assembleDebug` green.
2. Do not hard-code Nord 5 key identities — extend the learn flow / editable log pattern instead.
3. Comment only non-obvious detection and gesture logic.
4. Open a PR against `main` with a short summary and test notes (device + strategy used).

---

## License

Specify a license in this repository when you are ready to distribute (e.g. MIT / Apache-2.0). Until then, treat the code as source-available for personal use and review.

---

## Acknowledgements

Inspired by community Essential Key remappers for Nothing Phone and OnePlus Plus Key workarounds (Accessibility filtering, logcat/`OplusKeyEventUtil`, Tasker/Shizuku patterns discussed publicly on XDA and elsewhere).
