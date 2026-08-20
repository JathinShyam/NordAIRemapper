# 06 — Reverification fixes

## Why

Audit found Overlay settings slots without Remap icons, and Home banners still Wireless-first.

## What

| File | Fix |
|------|-----|
| `OverlaySettingsScreen.kt` | Slot `ActionCard` uses `action.icon()` |
| `HomeViewModel.kt` | Banner copy/CTA → Unlock detection, USB preferred |
| `KeyLearningScreen.kt` | Hint CTA → Unlock detection |

## Verdict after fix

Core Nord Edge checklist **READY**. Remaining deltas are polish-only (Home edge ripple, Unlock visual timeline bloom) — intentional/optional per prompt.
