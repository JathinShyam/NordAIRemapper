# 05 — Light theme window fix

## Why

`values/themes.xml` hardcoded `windowBackground=@android:color/black` and
`windowLightStatusBar=false`. In Light theme every cold start flashed black and
status-bar icons stayed white on the light background (invisible).

## What

| File | Change |
|------|--------|
| `res/values/colors.xml` | + `nord_window_background` (#F5F5F5) |
| `res/values-night/colors.xml` (new) | night override → #0A0A0A |
| `res/values/themes.xml` | base = `Theme.Material.Light.NoActionBar`, light bg, `windowLightStatusBar=true` |
| `res/values-night/themes.xml` (new) | dark bg + dark status icons for OS night mode |
| `presentation/MainActivity.kt` | `SideEffect { enableEdgeToEdge(...) }` re-applies bar styles reactively from the in-app DARK/LIGHT/SYSTEM setting, which is independent of OS night mode |

The manifest theme follows OS night mode (cold-start correctness); Compose
theme remains the source of truth in-app. Parent changed Dark→Light: safe
because all UI is Compose-drawn; system-styled widgets are only toasts.

## Verify

1. System in Light + app LIGHT: cold start has no black flash; status icons dark.
2. App DARK while system Light: bars switch to white icons immediately.
3. Flip DARK/LIGHT/SYSTEM live in Appearance: bar icon contrast follows without
   recreating the activity.
4. OLED black option still renders pure black scaffold (Compose-level).

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Wrong bar icons after switching theme | SideEffect not firing — check `darkTheme` computation |
| Black flash returns | windowBackground override lost in values-night merge |
