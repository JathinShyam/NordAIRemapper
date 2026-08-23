---
name: change-safety
description: >-
  Mandatory regression review before pushing any app-code change, and the
  post-install device smoke test. Use when committing/pushing changes to
  service/, detection, permissions, manifest, or UI flows — or after a user
  reports "X stopped working" on device.
---

# Change safety

Read `AGENTS.md` first. Purpose: no change ships unless existing functionality
is proven intact — code review alone missed real regressions before
(see `docs/changes/2026-08-23-readlogs-logd-blind/`).

## Before every push (non-trivial app-code change)

1. **Build + tests**: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
2. **Diff review against behavior inventory**, not just correctness of new code:
   - List each file you touched → name the existing behaviors that flow through it.
   - For hot files (`RemapEngine`, `GestureClassifier`, `LogcatWatcherService`,
     `PlusKeyAccessibilityService`, `KeyEventBus`), re-read the pipeline rules in
     [plus-key-detection](../plus-key-detection/SKILL.md) and confirm none were violated.
   - Explicitly answer: what breaks if this change is wrong at runtime *silently*?
     If any failure mode would be invisible (no crash, no log), add a visible signal
     (notification / health field) for it.
3. **Permissions/manifest touched?** Re-run Unlock flow mentally end-to-end:
   grant list in `ElevatedPermissions.UNLOCK_SHELL_COMMANDS`, USB paste block,
   Wireless path, and both status chips must stay consistent.
4. **Change docs** under `docs/changes/` in the same turn.

## After installing a build on the phone

Run the smoke test — it automates the probes that found the logd RCA:

```bash
scripts/device-smoke.sh
```

All lines must be PASS except the advisory gesture-health WARN. Interpretation:

| Failure | Meaning | Fix |
|---|---|---|
| accessibility listed/bound | service off or soft-disabled by Auto-Pause fallback | toggle in Settings; check pause notification |
| READ_LOGS missing | Unlock lost (uninstall/reinstall wipes grants) | run Unlock |
| **logd BLIND** | granted but not honored — watcher sees only its own logs | reboot; if persists: USB debugging (Security settings) ON → reboot → Unlock |
| watcher FGS | master toggle off or service crashed | open app Home; check battery optimizer |

## Regression triage (user reports breakage)

1. Run `scripts/device-smoke.sh` first — it bisects most reports in 20 seconds.
2. Then `adb logcat -d | grep -E 'LogcatWatcher|RemapEngine|PlusKeyA11y|A11ySecureToggle'`.
3. Check stored settings: `run-as com.nordairemapper cat files/datastore/settings.preferences_pb | strings`
   (only non-defaults appear; absence of `last_plus_key_seen_ms` = zero gestures classified).
4. Compare firmware side: `logcat -d -b main | grep KEYCODE_ACTION_BUTTON_CLICK | tail`.
5. Only then suspect code; use git log + change docs for the exact files recently touched.
