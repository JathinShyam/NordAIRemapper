# Codebase Audit & Improvement Plan

**Scope:** Full read of every source file (service, data, domain, presentation, UI), manifest,
gradle config, CI workflow, tests, and resources. Audit only — no code changed.
**Date:** 2026-08-22

---

## Executive summary

The codebase is in good shape for its stage: clean `presentation → domain ← data` layering, honest
Nord-5-specific detection docs, a well-tested logcat parser/coalescer, polished Compose UI, and
thoughtful permission UX (in-app Wireless ADB grant is genuinely well done). The biggest problems
are concentrated in one file: **`LogcatWatcherService` starts a new `logcat` process on every
`start()` call, and something calls it on every settings change** — an unbounded process leak that
also means pattern edits and watcher restarts behave incorrectly. Around that: a potential
boot-time crash (`LOCKED_BOOT_COMPLETED` + DataStore), silent watcher death with no recovery,
missing tests for exactly what TRD §10 requires (classifier timings, JSON round-trip), CI never
running the existing tests, light theme being visually broken via hard-coded dark accent colors,
and one significant product gap — overlay slots cannot launch apps or URLs.

---

## Findings

### A. Technical / Engineering

**T1 — LogcatWatcherService leaks a new process on every start; every settings change triggers a start**
- *What:* `LogcatWatcherService.onStartCommand` (`service/LogcatWatcherService.kt:53`) has no
  "already watching" guard; each call launches another `watchLogcat()` → new
  `ProcessBuilder("logcat", …)` (`:84`). Meanwhile `RemapEngine.start()`'s settings collector
  (`service/RemapEngine.kt:56–65`) calls `DetectionCoordinator.syncLogcatWatcher(...)` on **every**
  DataStore emission, which calls `startForegroundService`. So any unrelated toggle (theme,
  haptics, hold-duration slider) spawns another concurrent logcat tail. `onDestroy`
  (`:170–175`) destroys only the last handle.
- *Amplifiers:* `DeveloperScreen.TimingSlider` writes per drag tick (`onValueChange`, not
  `onValueChangeFinished`) → dozens of processes per gesture. Same for the Visual Overlay
  hold-duration slider. `HomeViewModel.setServiceEnabled` also syncs directly.
- *Why it matters:* battery/CPU drain grows over the session, N duplicate event streams race into
  `KeyEventBus`, memory growth — this directly undermines "remaps stop working" complaints.
- *Fix:* add a watching flag in the service (skip if already tailing); make the engine collector
  react only to strategy/enabled changes (`distinctUntilChanged`); move sliders to
  `onValueChangeFinished`. Effort **S**, Impact **High**, Risk **Low**.

**T2 — Watcher dies silently; pattern edits don't take effect cleanly**
- *What:* `watchLogcat()` reads the pattern once at startup (`:78–84`). If the logcat stream ends
  (logd restart), the loop just exits while the FGS notification still says "Plus Key detection
  active" — no reconnect, no user signal. Pattern changes require the leaky restart from T1.
- *Fix:* wrap the reader loop in retry-with-backoff; stop+start (not start-on-top) when the
  pattern changes; emit the existing death notification when the stream ends unexpectedly.
  Effort **M**, Impact **High**, Risk **Low**.

**T3 — BootReceiver can crash at boot (`LOCKED_BOOT_COMPLETED` + directBootAware)**
- *What:* Manifest declares `directBootAware="true"` and filters `LOCKED_BOOT_COMPLETED`.
  At that point credential-encrypted storage (DataStore under `filesDir`) isn't readable;
  `BootReceiver.kt:26–41` awaits `settings.first()` inside a bare `CoroutineScope(Dispatchers.IO)`
  with no try/catch — an exception crashes the app during boot.
- *Fix:* drop `LOCKED_BOOT_COMPLETED`/`directBootAware` (the FGS needs unlocked storage anyway) or
  wrap the body in `runCatching`. Effort **S**, Impact **Med**, Risk **Low**.

**T4 — Unguarded background FGS-start exceptions**
- *What:* `syncLogcatWatcher` runs from the engine's Main-immediate collector and service contexts;
  if invoked while backgrounded (Android 12+ restriction), `startForegroundService` throws
  `ForegroundServiceStartNotAllowedException` uncaught → app crash (`DetectionCoordinator.kt:41–54`).
- *Fix:* wrap start/stop in `runCatching` and log. Effort **S**, Impact **Med**, Risk **Low**.

**T5 — Test coverage misses the project's own TRD §10 requirements; CI never runs tests**
- *What:* Only `DetectionCoordinatorTest` + `LogcatKeyParserTest` exist. No `GestureClassifier`
  timing tests (single vs double vs long — explicitly required by TRD §10), no `RemapAction` JSON
  round-trip test (also required). `.github/workflows/build-debug-apk.yml` runs `assembleDebug`
  only — the two existing test files are never executed anywhere.
- *Fix:* add classifier timing tests + serialization round-trip; add `./gradlew testDebugUnitTest`
  step to CI before release upload. Effort **M**, Impact **Med-High**, Risk **Low**.

**T6 — Home "Plus Key pulse" flashes for volume/power keys too**
- *What:* `HomeViewModel.init` collects **all** `keyEventBus.rawEvents` and pulses on any
  DOWN/PULSE (`HomeViewModel.kt:51–58`) — pressing volume on Home makes the silhouette flash as if
  the Plus Key fired. Misleading feedback on exactly the screen users use to verify detection.
- *Fix:* filter like `RemapEngine.isPlusKeyEvent` does (logcat source or learned identity match).
  Effort **S**, Impact **Med**, Risk **Low**.

**T7 — Foreground-app exclusion tracking is approximate**
- *What:* `PlusKeyAccessibilityService.onAccessibilityEvent` updates the tracker from any
  `TYPE_WINDOW_STATE_CHANGED` package, including SystemUI windows (shade/QS pulled over an
  excluded app flips the tracker). Exclusion checks can then misfire both ways.
- *Fix (incremental):* ignore known SystemUI packages; document limitation. Effort **S/M**,
  Impact **Low-Med**, Risk **Low-Med**.

**T8 — Key setup lets users learn volume/power as the Plus Key with no warning**
- *What:* Every non-logcat captured row shows an enabled "Set as Plus Key"
  (`KeyLearningScreen.PressRow`). Saving Volume-Up then consumes volume keys forever
  (`RemapEngine.shouldConsume` matches learned identity), breaking hardware volume until re-learn.
- *Fix:* warn/block for `KEYCODE_VOLUME_*`/`KEYCODE_POWER` rows. Effort **S**, Impact **Med**,
  Risk **Low**.

**T9 — Misc technical**
- `FloatingOverlayService` radial glow center hard-codes `0.3×1080 × 0.2×2400` px
  (`FloatingOverlayService.kt:407–411`) — misplaced glow on other resolutions. **S/Low/Low.**
- `dispatchMediaKey` sends to **all** active sessions (`RemapActionExecutor.kt:294–297`) — can
  skip multiple tracks when several sessions exist. **S/Low/Low.**
- `versionCode = 1` never bumped; `SettingsViewModel.versionName()` hard-codes `"0.1.0"` fallback;
  no release signing config; `allowBackup="true"` without `dataExtractionRules` (Room/DataStore go
  to cloud backups silently). **S/Low/Low.**

### B. UX / UI

**U1 — Light theme is visually broken by hard-coded dark accent containers**
- *What:* `categoryAccent()` (`presentation/common/ActionCategoryAccent.kt`) returns near-black
  containers (`0xFF3D2E14`, `0xFF14321F`, `0xFF2A2A2A`, `0xFF3A1818`) used across Home action
  cards, Remap chips/rows, overlay slot rows, and Settings hub rows. With `ThemeMode.LIGHT`
  these render as dark blobs on light surfaces, and amber/violet tints lose contrast.
  Same pattern in `SettingsScreen.HubRow` call sites.
- *Why it matters:* Light is a shipped, user-selectable mode; it looks unfinished.
- *Fix:* theme-aware accent pairs (light variants per category) resolved from
  `isSystemInDarkTheme()`/scheme luminance. Effort **M**, Impact **Med**, Risk **Low**.

**U2 — App picker jank + missing empty state**
- *What:* `AppPickerSheet.AppRow` loads icons synchronously during composition
  (`getApplicationIcon` binder call + `toBitmap(96,96)` per row) — janky scrolling; no
  "No apps found" state when search matches nothing.
- *Fix:* load icon off-main into state (or `AsyncImage`-style caching); empty-state row.
  Effort **S/M**, Impact **Med**, Risk **Low**.

**U3 — Main-thread app enumeration**
- *What:* `RemapViewModel.loadInstalledApps` and Settings' "Add excluded app" run
  `queryIntentActivities(MATCH_ALL)` + label loading on Main — visible hitch with many apps.
- *Fix:* move to `Dispatchers.Default` with a loading flag. Effort **S**, Impact **Med**, Risk **Low**.

**U4 — Backup safety inconsistency**
- *What:* Snapshot restore confirms; **Import applies immediately** with no confirmation and
  overwrites all remaps/overlay/settings; snapshot delete has no confirmation either
  (`BackupScreen.kt`).
- *Fix:* confirm dialog for import (mirror restore copy); optional delete confirm.
  Effort **S**, Impact **Med**, Risk **Low**.

**U5 — Minor polish**
- Remap "Done" always snackbar-saves even when nothing changed; `tryNowLoading` is a fake 500 ms;
  VM emits raw class names ("Saved: LaunchApp") as event text. Onboarding has no back-to-previous-
  step and loses page on process death (`remember`, not `rememberSaveable`). Effort **S**,
  Impact **Low**, Risk **Low**.

### C. Product / Opportunity

**P1 — Floating overlay cannot contain app shortcuts or URLs**
- *What:* `OverlaySettingsScreen.ActionCatalogSheet` explicitly filters out `LaunchApp` and
  `OpenUrl`; only media/system actions are assignable to slots. README markets the overlay as
  "Apps, media, system shortcuts". App shortcuts are the most-wanted overlay content for a
  quick-actions menu.
- *Fix:* include LaunchApp (with the existing AppPickerSheet flow per slot) and OpenUrl.
  Executor already supports both. Effort **M**, Impact **High**, Risk **Low**.

**P2 — Triple-press produces a ghost SINGLE ~300 ms later**
- *What:* `GestureClassifier.registerCompletedPress` fires DOUBLE immediately on the second UP and
  resets; a third tap then starts a new cycle whose SINGLE fires after the window. Users doing
  triple taps get an unintended extra action. At minimum document; better: suppress the trailing
  single after a double within the same burst.
- Effort **S/M**, Impact **Med**, Risk **Med** (timing change needs device verification).

**P3 — No detection health signal**
- *What:* Beyond static banners, there's no "last Plus Key seen Xm ago" indicator. Most support
  complaints will be "it stopped working"; a last-seen timestamp (persisted on each classified
  gesture, shown on Home/Key setup) would cut debugging time massively.
- Effort **M**, Impact **Med-High**, Risk **Low**.

**P4 — Housekeeping**
- No LICENSE file (README defers); backup `schemaVersion` ignored on import (no forward-compat
  check); snapshots table unbounded (trivial). Effort **S**, Impact **Low**, Risk **Low**.

---

## Quick wins (high impact · low effort · low risk)

1. **T1 guard** — stop spawning duplicate logcat processes (single biggest stability/battery win).
2. **T3** — remove `LOCKED_BOOT_COMPLETED`/`directBootAware` or wrap BootReceiver body.
3. **CI** — add `./gradlew testDebugUnitTest` to the debug-APK workflow.
4. **T6** — filter Home pulse events to the actual Plus Key identity/source.
5. **U4** — confirmation dialog before import.
6. **U2/U3** — off-main app-icon loading + empty state in AppPickerSheet.
7. **T9 housekeeping** — versionName fallback, dataExtractionRules.

## Suggested order (if approved incrementally)

1. T1 (+slider fix) → 2. T3+T4 → 3. CI+tests (T5) → 4. P1 overlay apps → 5. U1 light theme →
6. remaining UX quick wins → 7. P2/P3 (need device testing).

## Open questions / assumptions

- `shouldConsume` returning true under **AUTO** strategy is intentional? (On Nord 5 the key never
  reaches Accessibility so it's moot today, but it defines behavior on other devices.)
- Logcat companion running under **ACCESSIBILITY** strategy (`needsLogcatWatcher` = true always)
  is intentional per RCA comments — confirmed assumption, kept as-is.
- Single-module layout and minSdk 33 remain targets; fonts shipped are OFL (Space Grotesk/Inter),
  satisfying the "no OnePlus Sans binaries" constraint — assumed redistributable per OFL.
- Device testing availability (physical Nord 5) assumed for anything touching gesture timing (P2).
