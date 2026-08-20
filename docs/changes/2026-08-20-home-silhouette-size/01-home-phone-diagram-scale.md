# 01 — Home PhoneDiagram scale

## Why

Home silhouette felt over-expanded after the earlier enlarge pass; user wants it fitted to the current frame and less dominant.

## What

| File | Change |
|------|--------|
| `presentation/home/HomeScreen.kt` | `PhoneDiagram` height `300.dp` → `200.dp` |
| `ui/components/PhoneDiagram.kt` | Horizontal padding `12` → `20`; body max width `0.92` → `0.72` of canvas; max height `0.98` → `0.90` |

## Verify

Open Home: silhouette sits inset in its band without crowding status chips / action cards.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Still too large | Parent height or another caller overrides; check `HomeScreen` modifier |
| Side keys clipped | Horizontal padding too small relative to `btnWidth` draw |
