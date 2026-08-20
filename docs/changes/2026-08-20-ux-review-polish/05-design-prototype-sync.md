# 05 — Design prototype sync

## Why

Compose UX review changes must stay mirrored in `design/nord-edge-prototype.html` so the interactive design file remains the source of visual truth.

## What

| File | Change |
|------|--------|
| `design/nord-edge-prototype.html` | Status ribbon inside master card; Home action icons + empty Double; continuous edge ripple; Remap Current pill / press subtitle / category tones / Saved toast; SVG hub icons; exclusions empty; Backup local snapshot collapsed; overlay empty hint + slot pop |

## Verify

Open `design/nord-edge-prototype.html` in a browser:

1. Home: ribbon above remapping switch; Double dashed empty; Single/Long show action icons; edge glow while active.
2. Remap: Current pill; category-colored list icons; Done → “Saved”.
3. Settings: SVG icons; exclusions empty copy.
4. Backup: expand “Local snapshot”.
5. Overlay settings: double-click a slot → empty hint.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Pad still shows “+” after Done | `findActionMeta` name mismatch (ellipsis / short label) |
| No category color | Missing `groupTone` for that catalog group |
