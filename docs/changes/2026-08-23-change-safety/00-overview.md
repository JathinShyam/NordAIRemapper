# Change safety: regression review protocol + device smoke script

## Why
Two silent regressions in one week (component-format toggle failure; logd serving
only self logs despite granted READ_LOGS) showed that "build passes" proves
nothing about existing functionality. Review discipline and device verification
need to be enforced, not remembered.

## What
| Path | Purpose |
|------|---------|
| `scripts/device-smoke.sh` | 20-second post-install check: install freshness, accessibility listed+bound, grants on user 0, **logd visibility from app uid** (the RCA probe, automated), watcher FGS, gesture-health advisory. Exit code gates. |
| `.cursor/skills/change-safety/SKILL.md` | Pre-push regression checklist (behavior inventory per touched file, silent-failure test), smoke-test interpretation table, breakage triage flow. |
| `AGENTS.md` | New mandatory "Change safety" section + skills-table row — read every session, so the rule is always loaded. |

## Verify
1. `scripts/device-smoke.sh` runs green on a healthy device.
2. On a logd-blind device it FAILs with the remediation pointer (verified live 2026-08-23).

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Script exits 2 | No device / USB debugging off / tethering mode |
| run-as probe fails to run | APK not debuggable (release build) |
