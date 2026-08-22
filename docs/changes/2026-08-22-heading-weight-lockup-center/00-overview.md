# Heading weight RCA + centered Never Settle lockup

## Why

1. Top-bar headings still looked thinner than design `.topbar .h` (`font-weight: 700`).
2. Never Settle lockup sat slightly below center (`offset(y = 6% of phone height)`).

## RCA — headings

| Finding | Detail |
|---------|--------|
| Font file | Single variable `space_grotesk.ttf` |
| Default weight | OS/2 / default instance ~**Light (300)** |
| Bug | `Font(R.font.space_grotesk, FontWeight.ExtraBold)` **without** `FontVariation.Settings` does not move the `wght` axis |
| Axis max | Space Grotesk tops at **700** — ExtraBold(800) is not a real cut; Compose soft-fakes or stays light |
| Design | `font-weight: 700` |

## What

| File | Change |
|------|--------|
| `ui/theme/Type.kt` | Every Space Grotesk `Font` uses `FontVariation.weight(...)`; Bold/ExtraBold/Black → 700; `titleLarge` = Bold 22sp |
| `ui/components/NordHeading.kt` | `FontSynthesis.None`; top bar forces Bold + LocalTextStyle override |
| `ui/components/PhoneDiagram.kt` | Lockup `Alignment.Center` only (no Y offset) |
| `design/silhouette-preview.html` / `nord-edge-prototype.html` | Lockup geometrically centered |

## Verify

1. Rebuild/install — “Plus Key”, “Settings”, etc. should match design boldness (w700 Space Grotesk).
2. Home silhouette: 1+ + NEVER/SETTLE block dead-center on the phone glass.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Still thin | Stale APK / font cache — reinstall |
| Compile ExperimentalTextApi | Missing `@OptIn` on `FontVariation` helper |
| Lockup overlaps punch-hole | Expected at true center; nudge only if user asks |
