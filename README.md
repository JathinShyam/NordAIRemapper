# Keyforge

**Remap the OnePlus Nord 5 Plus Key** — the physical side button marketed for AI — into *your* shortcuts: single press, double press, long press, plus an optional floating menu.

No root. No Magisk. No cloud. Local-only configuration.

> **Not affiliated with, endorsed by, or connected to OnePlus / OPPO.**

| | |
|---|---|
| Package | `com.nordairemapper` |
| Primary device | OnePlus Nord 5 (OxygenOS) |
| Min / target SDK | 33 (Android 13) / 35 |
| Stack | Kotlin · Jetpack Compose · Hilt · Room + DataStore |
| Latest APK | [latest-debug release](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug) |

---

## Why this exists

Stock Plus Key options are limited (flashlight, camera, Mind Space, and a few others). Power users want arbitrary apps, media, overlays, and system actions from that button.

On Nothing Phone, the Essential Key often reaches apps as a normal key event. **On OnePlus, OxygenOS usually handles the Plus Key in system code** (`OplusKeyEventUtil` / `KEYCODE_ACTION_BUTTON_CLICK`) so it **never arrives** as a `KeyEvent` to Accessibility.

That is the whole product problem:

| What you expect | What OxygenOS does |
|---|---|
| Apps can “see” the Plus Key like volume | Plus Key is eaten by system code |
| One permission dialog for remapping | `READ_LOGS` cannot be granted from a normal dialog |
| Remap always swallows the stock action | Without root, stock Plus Key may still fire (**dual-fire**) |

Keyforge is honest about that. It unlocks **logcat detection** once, keeps Accessibility for system actions, and lets you assign real remaps.

---

## What you get after setup

| Feature | What it means |
|---|---|
| **Single / Double / Long** | Three independent remaps on Home |
| **Action catalog** | Apps, media, system shortcuts, URL, floating overlay, or none |
| **Floating overlay** | Up to 6 slots (pill or radial) when you assign **Show overlay** |
| **Visual Overlay** | Brief on-screen popup when a remap fires (style, accent, glow, hold) |
| **Haptics** | On/off + Light / Medium / Heavy intensity |
| **Lock Screen** | Per-gesture allow/deny while locked |
| **Exclusions** | Disable remapping in chosen apps |
| **Backup & Restore** | Export/import JSON + local snapshots |
| **Lab** | Strategy, gesture timings, logcat pattern, USB unlock tools |

---

## Quick start (download)

1. Open **[Latest Debug](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug)**.
2. Install `NordAIRemapper-debug-latest.apk` (allow Install unknown apps).
3. Walk onboarding below — especially **Unlock detection**.

Every push to `main` also publishes an immutable `debug-<sha>` release so older builds stay available: [all releases](https://github.com/JathinShyam/NordAIRemapper/releases).

> **Signature tip:** GitHub APKs and local Android Studio builds use different debug keystores. You cannot overwrite one with the other (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) without uninstalling — which clears `READ_LOGS` and settings. Prefer the GitHub APK for day-to-day use, or stick to one signing path.

---

## Onboarding — step by step (and *why*)

First launch walks six screens. You can skip some, but skipping detection usually means the Plus Key never remaps on Nord 5.

### 1. Welcome

Introduces the product: remap single, double, and long press on the Plus Key.

### 2. Accessibility — required

**What you do:** Settings → Accessibility → enable **Keyforge**.

**Why:**

- On some devices / firmwares, hardware keys can be filtered here (Strategy A).
- Even when Plus Key detection uses logcat, **system actions still need Accessibility**: screenshot, lock, recents, home, back, notification shade, quick settings.
- The service observes hardware keys and foreground package for exclusions — **not** your screen content or passwords.

If Key setup later shows volume keys but never the Plus Key, that is expected on Nord 5 — not a broken Accessibility toggle.

### 3. Unlock Plus Key detection (`READ_LOGS`) — required on Nord 5

**What you do:** Tap **Unlock detection** (same flow as Home banners / Lab).

**Why:** Android will not show a normal “Allow logs?” dialog for `READ_LOGS`. OnePlus rarely delivers the Plus Key as a `KeyEvent`, so the app must watch system log lines (default pattern `KEYCODE_ACTION_BUTTON_CLICK`). That needs a one-time grant.

**Preferred — USB (computer once):**

1. Enable USB debugging; use **File transfer**, not tethering-only.
2. On a computer:

   ```bash
   adb shell pm grant com.nordairemapper android.permission.READ_LOGS
   ```

3. In Unlock → **Recheck**. When granted, you can unplug.

If OxygenOS blocks the grant (`GRANT_RUNTIME_PERMISSIONS`), disable **permission monitoring** under USB debugging / developer security settings, then retry.

**Advanced — no computer (Wireless debugging):**

1. Developer options → **Wireless debugging** → turn on.
2. If you see Network name / Wi‑Fi address with **Allow** — that is network approval, **not** pairing. Tap Allow, then open the Wireless debugging **detail page**.
3. **Pair device with pairing code** → enter the 6-digit code (and pairing port if asked) in the app.
4. If connect fails after pairing: enter the **Connection port** from the Wireless debugging main page (**IP address & port**) — that port is **different** from the pairing port.
5. Grant succeeds → you may turn Wireless debugging **off**. `READ_LOGS` persists across reboots.

**What “unlocked” means:** the logcat companion can see Plus Key presses. Accessibility stays on for system actions. Remapping still needs the master switch on Home.

### 4. Display over other apps — optional

**What you do:** Allow **Display over other apps** for Keyforge.

**Why:** Required for:

- Floating **Show overlay** menu
- **Visual Overlay** action popup

Skip if you only want single remaps that do not draw over other apps.

### 5. Keep it alive — strongly recommended

**What you do:**

- Allow **notifications** (foreground detection / overlay services)
- **Exempt from battery optimization** so OxygenOS does not kill the watcher overnight

**Why:** Logcat detection runs as a foreground service. Aggressive battery policies are the usual reason remaps “stop working after a while.”

### 6. You’re all set → Home

Confirm detection, then assign actions.

---

## After onboarding — first remaps

1. On **Home**, leave remapping **on** (master switch).
2. Tap **Single**, **Double**, or **Long** → Remap studio → pick an action → **Try now** → **Done**.
3. Optional floating menu:
   - **Overlay settings** → Enable overlay → fill slots → layout/position
   - Assign **Show overlay** to a press type
4. Optional polish:
   - **Visual Overlay** — popup chrome when actions fire
   - **Feedback** — haptic intensity
   - **Lock Screen** — which gestures work while locked
5. Optional: set OxygenOS’s stock Plus Key action to something harmless to reduce dual-fire annoyance.

### Confirming detection

| Where | What good looks like |
|---|---|
| **Key setup** (Settings → Advanced) | Plus Key appears as a **logcat** row (volume keys via Accessibility are normal) |
| **Home** | Status chip active; no Unlock banner |
| Remap **Try now** | Action runs (bypasses some gates — hardware press is the real test) |

Lab (advanced): Auto / Accessibility / Logcat strategy, double-press window, long-press threshold, log match pattern.

---

## How detection works (short)

```
Accessibility service  ─┐
                        ├──▶ KeyEventBus ──▶ RemapEngine ──▶ GestureClassifier
Logcat watcher         ─┘                      │
                                               ├── strategy + identity
                                               ├── exclusions / lock screen
                                               └── ActionDispatcher
```

| Strategy | Mechanism | Nord 5 reality |
|---|---|---|
| **Auto** (default) | Accessibility when the OS delivers the key; logcat when it does not | Recommended |
| **Accessibility** | Learn `keyCode` / `scanCode` in Key setup | Plus Key usually **missing**; volume may appear |
| **Logcat** | Match log lines after `READ_LOGS` | Usual path for Plus Key on Nord 5 |

Gestures are **wait-then-decide**: single press waits for the double-press window (default 300 ms) so it does not steal double taps. Long press fires at threshold (default 500 ms) from down.

---

## Remappable actions

| Category | Actions |
|---|---|
| Apps | Launch app · Open URL / deep link |
| Media | Play/pause · Next · Previous · Volume up/down |
| System | Assistant · Camera (front/rear) · Flashlight · Screenshot · DND · Ringer cycle · Shade · Quick settings · Recents · Home · Back · Lock · Auto-rotate |
| Overlay | Show floating overlay |
| None | Disable that press type |

Some actions need extra system access (e.g. **Do Not Disturb access** for ringer/DND, **Modify system settings** for auto-rotate). The app soft-fails with a toast and deep-link when possible.

---

## Permissions (what each one is for)

| Permission / access | Why we ask |
|---|---|
| Accessibility | Key filtering when available + system global actions |
| `READ_LOGS` (ADB / Unlock) | See Plus Key in logcat on Nord 5 |
| Display over other apps | Floating menu + visual action popup |
| Notifications | Foreground service visibility |
| Battery exemption | Keep detection alive |
| Boot completed | Re-arm watcher after reboot |
| Do Not Disturb access | DND / ring-vibrate-silent cycle |
| Modify system settings | Auto-rotate toggle |
| Camera (runtime) | Flashlight on stricter OEMs |
| Vibrate | Haptic feedback |

Backup uses the **Storage Access Framework** — no broad storage permission.

**Privacy:** No accounts, analytics, ads, or cloud sync. Accessibility is not used to scrape screen content. Logcat matching stays on-device.

---

## Known limitations

- **No root** → stock Plus Key action may still run (**dual-fire**). Documented, not a silent bug.
- **OS updates** can change logcat tags/patterns — editable in Lab if detection breaks after an update.
- **Other OxygenOS “Plus Key” phones** — best-effort; Nord 5 is the primary target.
- Debug APKs are for testing, not Play Store distribution.

---

## Build from source

```bash
git clone https://github.com/JathinShyam/NordAIRemapper.git
cd NordAIRemapper

# JDK 17
export JAVA_HOME="$HOME/.jdks/jdk17"   # or your JDK 17 path

# Point Gradle at the Android SDK (Android Studio also creates this)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
"$HOME/Android/Sdk/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Documentation

| Doc | Contents |
|---|---|
| [docs/PRD.md](docs/PRD.md) | Product requirements & journeys |
| [docs/TRD.md](docs/TRD.md) | Technical contracts, permissions, detection rules |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Pipelines, packages, ADRs |
| [docs/changes/](docs/changes/) | Per-change debug notes (why / what / verify) |
| [design/nord-edge-prototype.html](design/nord-edge-prototype.html) | Interactive Nord Edge UI prototype |
| [AGENTS.md](AGENTS.md) | Contributor / agent constraints |

---

## Contributing

1. Keep `./gradlew :app:assembleDebug` green.
2. Never hard-code Plus Key identity as the only detection path — learn at runtime / editable log pattern.
3. Do not pretend logcat events need `matchesPlusKey`; do not treat Accessibility as sufficient alone on Nord 5.
4. No analytics, ads, or OnePlus Sans font binaries in the repo.
5. Document non-trivial changes under `docs/changes/`.
6. Open a PR against `main` with device + strategy notes when you touch detection.

---

## License

Specify a license when you are ready to distribute (e.g. MIT / Apache-2.0). Until then, treat the code as source-available for personal use and review.

---

## Acknowledgements

Inspired by community Essential Key remappers for Nothing Phone and public OnePlus Plus Key workarounds (Accessibility filtering, logcat / `OplusKeyEventUtil`, Tasker / Shizuku discussions on XDA and elsewhere). This app’s preferred Nord 5 path is **in-app Unlock** (USB or Wireless) — not Shizuku.
