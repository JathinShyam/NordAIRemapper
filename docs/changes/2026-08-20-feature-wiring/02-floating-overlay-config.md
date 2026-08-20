# 02 — Floating overlay config honored

## Why

`OverlayConfig.enabled`, `animation`, `accentColorArgb`, `glowEffects`, and `visualStyle` were UI-only for the floating menu.

## What

| File | Change |
|------|--------|
| `OverlayConfig.kt` | Default `enabled = true` |
| `FloatingOverlayService.kt` | Gate on `enabled`; accent/glow/style; Fade/Scale/Slide entrance |
| `OverlaySettingsViewModel.kt` | Assigning a non-None slot auto-enables |
| `OverlaySettingsScreen.kt` | Copy clarifies the enable switch |
| `OverlayPreview.kt` | Preview uses accent color |

## Verify

1. Overlay settings → fill slots, Enable on → assign Show overlay → menu appears with accent/animation.
2. Enable off → Show overlay toasts to turn it on.
3. Fade vs Scale vs Slide changes tile entrance.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Toast “Enable overlay” | Switch off (old saves defaulted false — flip on) |
| Still cyan only | Accent not saved — recheck Visual Overlay / Overlay Room JSON |
