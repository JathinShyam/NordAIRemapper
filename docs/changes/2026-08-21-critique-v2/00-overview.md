# Critique v2 — UI polish pass

## Why

Full UI/UX critique v2 after round 1: critical badge/snackbar issues, high-impact missing previews/haptics/perf, medium/minor polish.

## What

| ID | Change |
|----|--------|
| C1 | Long press badge `"L"` (no emoji) |
| C2 | `ActionCard` badge always overlays empty/`+` state |
| C3 | Remap Done → Snackbar `"Saved"` |
| H1 | `OverlayPreview` on Visual Overlay |
| H2 | Exclusion labels `remember(excludedApps)` |
| H3 | Remap category chips + leading icons |
| H5 | Home `plusKeyPulse` from `KeyEventBus` |
| H6/N2 | Haptics on Prefs / Feedback / LockScreen switches |
| H7 | Lab ADB tap-to-copy card |
| H8 | Overlay slots: category accents + slot badges |
| M1–M7 | Icon anim, GhostButton loading, Visual Overlay purple hub, LockScreen intro, badge `8.sp`, HubRow haptic, `RADIAL`→`GRID` |
| N1–N8 | Dot anim, KeyLearning split text, colour indicator 28dp, Try now loading, slider range labels, NordHeading sheet, bodyMedium intro |
| Design | Prototype: `L` badge, empty badges visible, Visual Overlay purple |

## Verify

1. Home: empty Double shows `2×` on `+`; Long shows `L`; live Plus Key flashes silhouette.
2. Remap Done → in-app snackbar; category chips show icons.
3. Settings → Visual Overlay hub is purple; exclusions don’t hitch on recompose.
4. `./gradlew :app:compileDebugKotlin`

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Old `RADIAL` in backup JSON | Serializer maps `RADIAL` → `GRID` |
| No live key flash | Detection not emitting to `KeyEventBus` |
| Badge missing on empty | ActionCard wrapper regression |
