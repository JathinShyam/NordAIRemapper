# Product Requirements Document (PRD)

**Product:** Nord AI Remapper  
**Platform:** Android (OnePlus Nord 5 primary; OxygenOS / ColorOS family)  
**Package:** `com.nordairemapper`  
**Document version:** 1.0  
**Status:** Active — MVP in progress (phases 1–5 shipped; UI & overlay remaining)

---

## 1. Vision

Nord AI Remapper turns the OnePlus Nord 5’s physical **Plus Key** (marketed around AI features; it replaced the alert slider) into a fully customizable hardware shortcut.

Users assign independent actions to **single press**, **double press**, and **long press**, and optionally open a **floating overlay** of up to six quick actions — similar in spirit to Essential Key remappers on Nothing Phone, adapted to OnePlus’s harder-to-intercept key path.

> Not affiliated with, endorsed by, or connected to OnePlus / OPPO.

---

## 2. Problem statement

| Constraint | User impact |
|---|---|
| Stock Plus Key options are limited (ringer, flashlight, camera, screenshot, Mind Space, etc.) | Power users cannot launch arbitrary apps or custom workflows from the side key |
| OnePlus handles the key in system code (`OplusKeyEventUtil`) | Unlike Nothing’s Essential Key, the press often never reaches apps as a normal `KeyEvent` |
| No official third-party remapping API | Users resort to Tasker + Shizuku, root modules, or abandon the hardware key |

**Job to be done:** “When I press the Plus Key, I want *my* action — reliably, without root if possible — with clear setup when the OS fights me.”

---

## 3. Goals & non-goals

### 3.1 Goals (MVP)

1. Detect Plus Key presses on Nord 5 without inventing hard-coded keycodes.
2. Classify single / double / long press with configurable timings.
3. Map each gesture to a curated set of system and app actions.
4. Optional floating overlay as a multi-action launcher.
5. Persist configuration locally; export/import via JSON.
6. Onboard users through required permissions with honest explanations.
7. Surface detection failures with actionable troubleshooting (including dual-fire with stock Plus Key behavior).

### 3.2 Non-goals (MVP)

- Root / Magisk / LSPosed modules.
- Remapping volume, power, or Bixby-style keys on other OEMs as a primary target.
- Cloud sync, accounts, analytics SDKs, ads.
- Fully suppressing the stock Plus Key action without root (document and mitigate only).
- Shipping OnePlus Sans font binaries in the public repo.

### 3.3 Success metrics (qualitative for v1)

| Metric | Target |
|---|---|
| On-device key learn succeeds (Strategy A or B) on Nord 5 | Confirmed in first-session setup |
| Gesture misclassification rate (user-reported) | Rare after timing tweak |
| Time from install → first successful remap | &lt; 5 minutes when Accessibility works; &lt; 10 with ADB logcat path |
| Crash-free sessions for detection services | Stable overnight with battery exemption |

---

## 4. Target users & personas

**Primary — Nord 5 power user**  
Owns Nord 5, wants flashlight / app / media / overlay from the side key; comfortable enabling Accessibility; may accept one ADB command.

**Secondary — Nothing-migrator**  
Used Essential Remapper-style apps; expects phone silhouette UI, press cards, overlay menu.

**Out of scope for messaging**  
Users unwilling to grant Accessibility or run ADB when Strategy A fails.

---

## 5. Product principles

1. **Detection before chrome** — prove the key is seen before polishing Home UI.
2. **Runtime-learned identity** — never ship a hard-coded keyCode/scanCode as the only path.
3. **Honest limitations** — dual-fire with system Plus Key is a known caveat, not a silent bug.
4. **Minimal privilege** — request only what each feature needs; SAF instead of storage permissions.
5. **OxygenOS-adjacent UI** — dark-first, clean cards, Nord blue accent, no clutter.

---

## 6. User journeys

### 6.1 First launch (happy path — Accessibility)

1. Welcome → enable Accessibility for Nord AI Remapper.  
2. Grant overlay (if planning to use overlay).  
3. Notifications + battery exemption.  
4. Home: master toggle on; green status.  
5. Key setup: press Plus Key → save identity.  
6. Assign Single → Launch App; test from Remap screen.  
7. Use hardware key in daily use.

### 6.2 First launch (fallback — Logcat)

1–4 as above.  
5. Key setup shows no Accessibility events for Plus Key.  
6. Developer → switch to Logcat watcher → copy ADB `READ_LOGS` command → grant → restart watcher.  
7. Confirm pulses / down-up in event log → map actions.

### 6.3 Overlay power user

1. Overlay Settings: enable; trigger = Double.  
2. Fill 4–6 slots; pick pill or radial layout.  
3. Double-press opens overlay; outside tap dismisses; lock screen never shows overlay.

### 6.4 Backup

1. Create named snapshot or export JSON via SAF.  
2. After wipe/reinstall, import JSON or restore snapshot.

---

## 7. Feature requirements

### 7.1 Key detection (P0)

| ID | Requirement | Priority |
|---|---|---|
| DET-01 | Dual strategies: Accessibility key filtering and Logcat watcher, selectable in Developer settings | P0 |
| DET-02 | Learn-your-key flow captures keyCode + scanCode + down/up; user saves identity | P0 |
| DET-03 | No hard-coded device key identity as the sole detection path | P0 |
| DET-04 | Gesture classifier: single / double / long; wait-then-decide so single does not fire before double window ends | P0 |
| DET-05 | Double-press window 200–500 ms (default 300); long-press 300–1000 ms (default 500) | P0 |
| DET-06 | Home banner when detection not confirmed; troubleshooting includes setting stock Plus Key to a harmless default | P0 |
| DET-07 | Volume/power and unrelated keys must not be delayed or broken on the Accessibility path | P0 |

### 7.2 Remappable actions (P0)

Each press type maps to exactly one `RemapAction` (or None).

| Category | Actions |
|---|---|
| Apps | Launch any installed app (searchable picker) |
| Media | Play/pause, next, previous, media volume ± |
| System | Assistant, camera (front/rear), flashlight, screenshot, DND, ringer cycle, notification shade, quick settings, recents, home, back, lock, auto-rotate, open URL/deep link |
| Overlay | Show floating overlay |
| None | Disable this press type |

Permission-gated actions must deep-link to the correct system settings when not granted (DND policy, write settings, etc.).

### 7.3 Home (P0)

- Phone silhouette with Plus Key highlighted (Nord blue glow).  
- Three cards: Single / Double / Long — icon + label of current action.  
- Pill master toggle for remapping engine.  
- Service status (green/red; tap-to-fix when inactive).  
- Troubleshooting banner slot.  
- Conflict badge when two press types share the same non-None action.

### 7.4 Remap Config (P0)

- Categorized action list; tap to save with confirmation animation.  
- App picker bottom sheet for Launch App.  
- “Try this action now” test control.

### 7.5 Overlay (P1)

- FloatingOverlayService, `TYPE_APPLICATION_OVERLAY`.  
- Radial/arc **or** horizontal pill bar.  
- Up to 6 slots; position / opacity (30–100%) / icon size / animation.  
- Live preview in settings.  
- Dismiss on outside tap; never on lock screen.  
- Spring entrance/exit.

### 7.6 Onboarding (P0)

Five screens; skip disabled for steps that grant critical permissions (Accessibility, Overlay, Notifications/battery as defined in build brief).

1. Welcome  
2. Accessibility  
3. Overlay (`SYSTEM_ALERT_WINDOW`)  
4. Notifications (`POST_NOTIFICATIONS`) + battery optimization exemption  
5. All set → Home  

### 7.7 Backup & Restore (P1)

- Export/import JSON via SAF (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).  
- Local named, timestamped snapshots; swipe delete; confirm restore.  
- No legacy storage permissions (minSdk 33).

### 7.8 Settings (P1)

- Theme: Dark / Light / System; optional Material 3 dynamic color.  
- Custom font toggle (infrastructure for OnePlus Sans; Roboto default).  
- Persistent notification while detection active.  
- Haptic on trigger.  
- Per-app exclusions (no remap in selected packages).  
- Battery optimization prompt.  
- Developer: strategy A/B, key learning, raw event log, logcat pattern editor.  
- About: version, GitHub, licenses.

### 7.9 Resilience (P1)

- Detection survives app process death where the OS allows.  
- Service death → notification to re-enable.  
- BootReceiver re-arms after reboot.  
- Graceful handling when killed by battery optimization.

---

## 8. Design requirements (product-facing)

| Token | Value |
|---|---|
| Background | `#0A0A0A` |
| Surface / card | `#141414` |
| Accent | `#0AC6FF` (Nord blue) |
| Text | `#FFFFFF` / `#8A8A8A` |
| Destructive | `#FF4D4D` |
| Cards | 16 dp radius |
| Controls | Pill toggles; bottom sheets over dialogs; no FABs |
| Motion | Spring with `DampingRatioMediumBouncy` for meaningful state changes |
| Typography | OnePlus Sans when user-supplied; Roboto fallback; 400 / 500 / 700 |

Dark mode first; light + dynamic color optional.

---

## 9. Privacy & trust

- Accessibility disclosure: hardware key observation (and window state only as needed for exclusions); **no** reading passwords or screen content for remapping.  
- Logcat strategy: local log parsing only; no upload of logs.  
- No account; data stays on device unless user exports JSON.  
- Clear in-app copy for `READ_LOGS` ADB grant.

---

## 10. Platforms & compatibility

| Item | Requirement |
|---|---|
| Primary device | OnePlus Nord 5 |
| Min SDK | 33 (Android 13) |
| Target SDK | 35 |
| Secondary | Other OxygenOS devices with Plus Key — best-effort if log pattern / learn flow works |

---

## 11. Release phases (product)

| Phase | Product outcome |
|---|---|
| Alpha (current) | Detection engine + key learning + action executors; temporary Home |
| Beta | Full Home, Remap UI, onboarding, Settings |
| RC | Overlay, backup, resilience polish |
| 1.0 | Nord 5–validated detection paths documented in README |

---

## 12. Open product risks

1. **Strategy A may never see the Plus Key** on stock Nord 5 → Logcat + ADB becomes the real primary path for many users.  
2. **System action still fires** alongside remap → user education + “harmless default” stock mapping.  
3. **Logcat pattern drift** across OS updates → editable pattern + Developer tools.  
4. **Play Store policy** for Accessibility + `READ_LOGS` messaging if published later.

---

## 13. Acceptance criteria (MVP)

- [ ] User can learn and save Plus Key identity on Nord 5 via A or B.  
- [ ] Single / double / long each trigger distinct configured actions.  
- [ ] Master toggle disables remapping without uninstalling.  
- [ ] At least Launch App, flashlight, screenshot, media play/pause work end-to-end.  
- [ ] Onboarding completes without skip on critical permission steps.  
- [ ] Export → clear data → import restores mappings.  
- [ ] Overlay (when enabled) appears only unlocked and dismisses on outside tap.

---

## Document history

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-11 | Initial PRD from product brief + repo state |
