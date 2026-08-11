# Implementation Roadmap

**Product:** Nord AI Remapper  
**Source of truth:** [PRD](../PRD.md) · [TRD](../TRD.md) · [Architecture](../ARCHITECTURE.md)

Each phase has its own detailed plan under this folder. **Do not start the next phase until `./gradlew assembleDebug` is green and the phase acceptance checklist is checked.**

---

## Status board

| Phase | Title | Status | Plan |
|------:|-------|--------|------|
| 1 | Project setup & theme | ✅ Done | [phase-01.md](./phase-01.md) |
| 2 | Domain, Room, DataStore, repositories | ✅ Done | [phase-02.md](./phase-02.md) |
| 3 | Accessibility detection + gesture + key learning | ✅ Done | [phase-03.md](./phase-03.md) |
| 4 | Logcat watcher + Developer settings | ✅ Done | [phase-04.md](./phase-04.md) |
| 5 | RemapAction executors | ✅ Done | [phase-05.md](./phase-05.md) |
| 6 | Home Screen (diagram, cards, status, toggle) | ✅ Done | [phase-06.md](./phase-06.md) |
| 7 | Remap Config Screen + app picker | ✅ Done | [phase-07.md](./phase-07.md) |
| 8 | Onboarding permission flow | ✅ Done | [phase-08.md](./phase-08.md) |
| 9 | Floating overlay + Overlay Settings | ✅ Done | [phase-09.md](./phase-09.md) |
| 10 | Backup & Restore (SAF) | ✅ Done | [phase-10.md](./phase-10.md) |
| 11 | Settings + per-app exclusions | ✅ Done | [phase-11.md](./phase-11.md) |
| 12 | Service resilience (boot, death, battery) | ✅ Done | [phase-12.md](./phase-12.md) |

---

## Rules for every phase

1. Read the phase MD fully before coding.  
2. Implement only that phase’s deliverables (no drive-by refactors).  
3. Run `./gradlew assembleDebug` and fix all errors.  
4. Manual smoke checklist in the phase MD.  
5. Commit with message `Phase N: …`.  
6. Update this status board (✅ / 🔄 / ⏳).

---

## Dependency graph

```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
                      ↘︎ 9 (needs 7 for action catalog reuse)
                      ↘︎ 10 (needs 2; better after 7/9 for full payload)
                 6/7 → 11 (Settings links Home/Developer)
           3/4/9 → 12 (BootReceiver + death notifications)
```

**All planned implementation phases are complete in code.** Remaining work is on-device Nord 5 validation of Plus Key detection (Strategy A and/or B).
