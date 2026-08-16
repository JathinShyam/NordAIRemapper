# 01 — docs/changes layout

## Why

User asked to document each change in separate files for future debug context.

## What

| Path | Role |
|------|------|
| `docs/changes/README.md` | Convention + index + template + completeness checklist |
| `docs/changes/YYYY-MM-DD-<slug>/` | One folder per change set |
| `00-overview.md` + numbered topic files | One markdown file per logical change |
| `FILE_MANIFEST.md` (recommended) | Every touched path → topic doc |

Each topic file should include: **Why**, **What** (files/behavior), **Verify**, **Debug tips**.

## Verify

Open `docs/changes/README.md` and confirm the index lists this folder and the READ_LOGS grant folder.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Code changed but no notes | Agent skipped the same-turn docs rule |
| One giant dump file | Convention violated — split by logical change |
