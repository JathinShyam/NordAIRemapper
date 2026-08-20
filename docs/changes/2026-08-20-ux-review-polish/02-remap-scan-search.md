# 02 — Remap scan, search, Current, Done

## Why

Remap is the most-used picker: uniform blue icons, plain “Current” text, Material outlined search, generic subtitle, and no save confirmation on Done.

## What

| File | Change |
|------|--------|
| `presentation/remap/RemapScreen.kt` | Category accent colors; Current icon pill; pill search + clear; press-type subtitle; Toast “Saved” on Done |

## Verify

- Media icons green, Apps amber, System cyan, Overlay muted, None red.
- Search has magnifying glass + clear; Current mirrors list icon box.
- Done → brief “Saved” toast then back.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Toast missing after Done | OEM toast suppression; action still saved via `setAction` |
| Wrong category tint | `categoryFor()` fallback — check catalog match for parameterized actions |
