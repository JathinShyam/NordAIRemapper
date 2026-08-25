# Floating Menu look controls — denser

## Why

Layout / Position / Icon Size / Entry Animation / Opacity each used a full card plus explanatory subtext, which made Floating Menu feel tall and wordy.

## What

| File | Change |
|------|--------|
| `OverlaySettingsScreen.kt` | One **Look** group; drop layout/position/size/animation/opacity subtext; shared `SettingsSegmentedControl`; tighter padding |

Behavior unchanged (same ViewModel setters).

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:compileDebugKotlin
```

Manual: Floating Menu → switch Grid/Pill Bar (position options update); opacity slider still commits on release.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Position options stale after layout change | `positions` list not recomputed from `config.layoutStyle` |
