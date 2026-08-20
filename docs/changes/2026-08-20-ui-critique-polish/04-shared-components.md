# 04 — Shared components

## Why
Section labels were hard to scan; primary buttons needed loading state for async flows.

## What
| File | Change |
|------|--------|
| `ui/components/StatusChip.kt` | SectionLabel: 12.sp, alpha 0.85, letterSpacing 0.4.sp |
| `ui/components/NordCards.kt` | NordPrimaryButton optional `loading` with CircularProgressIndicator |

## Verify
- Section headers (Actions, Appearance, etc.) slightly larger and brighter
- NordPrimaryButton with `loading=true` shows spinner and is disabled

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Button still clickable while loading | Pass `loading=true` without also setting `enabled=false` — loading disables internally |
