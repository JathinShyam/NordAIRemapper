# 04 — Documentation completeness audit

## Why

User asked to verify nothing was missing from change documentation and to make it proper.

## What added/fixed in the same turn

| Item | Location |
|------|----------|
| Full path inventory | `../2026-08-16-in-app-read-logs-grant/FILE_MANIFEST.md` |
| ProGuard/`android.sun.security` keeps | `09-proguard-and-release.md` + `app/proguard-rules.pro` fix |
| Onboarding index map | `10-onboarding-page-map.md` |
| Entry points matrix | `11-entry-points.md` |
| Privacy / non-claims / log tags | `12-privacy-and-limits.md` |
| ARCHITECTURE package tree + link | `docs/ARCHITECTURE.md` v1.1 |
| Stronger convention (template + checklist) | `docs/changes/README.md` |

## Verify

```bash
git status --short
# every modified/new app path appears in FILE_MANIFEST.md
find docs/changes/2026-08-16-in-app-read-logs-grant -name '*.md' | sort
```

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Manifest out of date after later edits | Re-run audit against `git status` before commit |
