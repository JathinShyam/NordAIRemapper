# 03 — Backfill: in-app READ_LOGS grant notes

## Why

The Wireless ADB grant implementation landed before the change-log convention existed; notes were written immediately after enabling the convention.

## What

Created `docs/changes/2026-08-16-in-app-read-logs-grant/` with:

- `00-overview.md`
- `01-gradle-dependencies.md` … `08-detection-sync-and-docs.md`

Covers deps, manifest, ADB manager, grant API, Enable detection UI, nav/Home/onboarding, Shizuku de-emphasize, watcher sync + device-testing skill.

This folder (`2026-08-16-change-log-convention`) documents *creating* that backfill and the convention itself — not the grant code again.

## Verify

Both folders appear in `docs/changes/README.md` index.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Looking for grant debug tips here | Use `../2026-08-16-in-app-read-logs-grant/` instead |
