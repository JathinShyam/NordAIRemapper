# 01 — Home action icons & status hierarchy

## Why

Action cards lacked visual anchors; empty “No action” looked assigned; status chip was easy to miss next to the master switch.

## What

| File | Change |
|------|--------|
| `ui/components/ActionCard.kt` | Prefer `icon`; `empty` dashed border + “+ Assign an action” |
| `presentation/home/HomeScreen.kt` | Pass `action.icon()` / `empty`; `StatusRibbon` inside master-switch card |

## Verify

- Assigned press → icon + name; unassigned → dashed card + assign cue.
- Ribbon shows “Service active” with pulsing dot when remapping is on.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| No icon on empty slot | Expected — empty uses Add glyph, not action icon |
| Ribbon not pulsing | Need accessibility on + service enabled |
