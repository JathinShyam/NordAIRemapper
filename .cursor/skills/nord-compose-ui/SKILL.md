---
name: nord-compose-ui
description: >-
  Rebuild or polish Nord AI Remapper Compose UI (theme, Home, Remap, Settings,
  Overlay, Backup, Onboarding, Developer). Use when editing presentation/ or
  ui/ screens and components. Loads RoninForge Compose guidance with Nord
  product constraints.
---

# Nord Compose UI

Read `AGENTS.md` first. For Compose quality patterns, also use the vendored
RoninForge skills under `.cursor/skills/roninforge-*` and rules
`.cursor/rules/roninforge-compose-*.mdc` (Material 3, lifecycle collection,
anti-patterns). **Nord product rules win** when they conflict.

## Nord constraints (do not violate)

- Dark-first OxygenOS-inspired palette: background `#0A0A0A`, surface `#141414`,
  accent `#0AC6FF`. Do not invent a purple/generic AI theme.
- `FontFamily.Default` only — do not commit OnePlus Sans `.ttf` files.
- Layers: `presentation → domain ← data`. No Android UI types in `domain`.
- One `@HiltViewModel` per screen; `StateFlow` + `collectAsStateWithLifecycle`.
- Do **not** migrate to Navigation 3, strict MVI, or Screen+Route splits unless
  the user explicitly asks.
- Do **not** bump AGP/Kotlin/Compose BOM “because the skill says so”.
- No analytics, ads, accounts, or cloud sync.
- Do not change detection / `RemapEngine` / logcat / accessibility in a UI pass.

## When polishing UI

1. Prefer shared components under `ui/components/` and theme under `ui/theme/`.
2. Material 3 imports only (`androidx.compose.material3.*`).
3. Clear hierarchy: one purpose per section; status chips for permissions;
   press-type cards on Home; denser action lists on Remap.
4. Keep ViewModels and nav graph behavior unchanged unless fixing a UI bug.
