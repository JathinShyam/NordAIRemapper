# Change logs

Per-change notes for debugging and future agents. Product/architecture truth still lives in `docs/PRD.md`, `docs/TRD.md`, `docs/ARCHITECTURE.md`.

## Convention

1. Every non-trivial change set gets a folder: `docs/changes/YYYY-MM-DD-<short-slug>/`.
2. Inside that folder, **one markdown file per logical change** (deps, new class, screen wire-up, etc.) — not one giant dump.
3. Prefer also a **`FILE_MANIFEST.md`** listing every touched path → doc mapping.
4. Each topic file should include:
   - **Why** (problem / constraint)
   - **What** (files + behavior)
   - **How to verify**
   - **Debug tips** (symptoms → likely cause)
5. Add a row to the index table below when creating a new folder.
6. If architecture packages change, update `docs/ARCHITECTURE.md` package tree and link the change folder.
7. Do **not** edit Cursor plan files under `~/.cursor/plans/`; keep durable notes here.

Going forward: write/update these notes in the **same turn** as the code.

## Topic file template

```markdown
# NN — short title

## Why
…

## What
| File | Change |
|------|--------|
| `path` | … |

## Verify
…

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| … | … |
```

## Completeness checklist (before calling a change set “documented”)

- [ ] Folder + `00-overview.md` with file index
- [ ] `FILE_MANIFEST.md` covers every new/modified path from `git status`
- [ ] Index row in this README
- [ ] ARCHITECTURE package tree updated if packages/routes added
- [ ] Skills updated if operator workflow changed (e.g. device-testing)
- [ ] ProGuard/privacy/limits noted when new native/crypto/network code lands

## Index

| Date | Folder | Summary |
|------|--------|---------|
| 2026-08-16 | [in-app-read-logs-grant](./2026-08-16-in-app-read-logs-grant/) | Phone-only Wireless ADB path to grant `READ_LOGS` (no Shizuku / no laptop) |
| 2026-08-16 | [change-log-convention](./2026-08-16-change-log-convention/) | Introduced `docs/changes/` per-change notes + AGENTS/project rule hooks |
