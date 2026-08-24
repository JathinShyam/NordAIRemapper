# 10 — End-to-end audit #2 fixes + Auto-Pause RCA instrumentation

Full second-pass audit after the Unlock/Built-In/Shizuku work landed.
Two sweeps (presentation, services/data/tests) + targeted RCA.

## Fixed

| Sev | Bug | Fix |
|-----|-----|-----|
| HIGH | Pattern edit leaked a zombie logcat tail: `collectLatest` cancelled the job but the old tail blocked in `readLine()` checking only the service scope — kept emitting with the OLD pattern forever | Tail loop now checks its own job context per line; outer finally destroys the process on exit (`LogcatWatcherService.tailLogcat`) |
| HIGH | Notification-reply pairing could exceed the ~10s broadcast grace (connect path up to ~40s) → process killed mid-grant | `pairAndGrant(quick=true)` from the receiver: 5s mDNS connect discovery + single connect attempt |
| HIGH | Shizuku permission result fanned out to BOTH embedded ViewModels (same request code registered twice) → duplicate grant runs | Single static listener owned by `ShizukuGrant` with one-shot callback; VMs no longer register listeners |
| MED | Rapid master-toggle off/on cleared the death-alarm suppress flag → false "detection stopped" alarm | Suppress flag only cleared by explicit starts (`intent != null`), not system restarts |
| MED | Heal/restart paths started the watcher FGS even with master toggle OFF | `onStartCommand` re-checks `settings.serviceEnabled` and stops |
| MED | Blind watchdog used wall clock — NTP jumps fired false BLIND alarms | Monotonic `elapsedRealtime()` tracking in `noteLineOrigin` |
| MED | Restore snapshot crashed on corrupt payload (import path handled it; restore didn't) | Wrapped + snackbar message |
| MED | Shizuku `bindUserService` had no timeout → "Granting…" forever when server dies silently | 10s timeout, presentable failure |
| MED | Shizuku service: unbounded blocking read pinned server threads; `exit()` could kill an in-flight command | Serialized commands, 10s waitFor+destroyForcibly, quiescent exit |
| MED | No single-flight guard around `pairAndGrant`; shared ADB manager raced | `grantMutex.withLock` |
| MED | PairingSession torn reads under concurrent re-arm | Immutable `Snapshot` swapped atomically; reply path reads via `current()` |
| MINOR | Step-3 stale port: retry watch kept old `discoveredPort`, UI claimed "Port X detected" while searching | Cleared at watch start |
| MINOR | KeyLearning stale "Accessibility inactive" after returning from settings | ON_RESUME observer |
| MINOR | Notification-permission callback marked granted even on DENY | Result honored; refresh() re-probes |
| MINOR | Unlock "Open Home" button just popped back | NavHost passes real `onContinue` |
| MINOR | `tryNowLoading` saveable → stranded spinner after process death | Back to plain remember |
| MINOR | Sheet interiors lost typed URL/search on rotation though sheets survived | Saveable inputs |
| MINOR | Opacity slider wrote DataStore per drag tick | Local drag + commit-on-finish |
| MINOR | Home pulse animator ticked even when not pulsing | Gated behind condition |
| MINOR | Malformed remap nav arg crashed `PressType.fromKey` | Safe fallback to SINGLE |
| LOW | `unlock_grants.log` unbounded; formatter churn | 256KB cap + static formatter |
| LOW | BootReceiver hardcoded `serviceEnabled=true` into coordinator call | Passes verbatim; boot nudge skipped for pure-ACCESSIBILITY strategy |
| LOW | `ActionFeedbackOverlayService` null-action path skipped startForeground (FGS contract violation) | startForeground first |

## Deferred (documented, not fixed)

- AUTO strategy cross-source skew can double-classify one press on devices
  where both channels deliver the key (not reachable on stock Nord 5). Needs
  per-source classifiers.
- Room v1 without schema export/migration policy — decide before any entity change.
- Home ribbon "Starting log stream…" cannot distinguish a *permanent* logd block;
  would need probe integration into Home state.
- Android 15 background-FGS-start rules may affect overlay services on future
  devices (verify when available).

## Auto-Pause RCA instrumentation (the new ask)

Added `service/DebugTrace.kt` — size-capped file traces pullable via run-as:

    adb shell run-as com.nordairemapper cat files/auto_pause.log

Traced points:
- `PlusKeyAccessibilityService.maybePauseForExcludedApp`: trigger pkg,
  canAutoResume verdict, hands-free vs fallback branch, secure-toggle result
- `AccessibilityAutoResumeService`: watch start, excluded→excluded handoff,
  restore attempt/result, timeout

Config verified correct meanwhile (`typeWindowStateChanged` present), so if
Auto-Pause still fails on-device, the trace will show which link breaks:
trigger never fires → hands-free chosen but toggle fails → watch never sees
the leave → restore write rejected.
