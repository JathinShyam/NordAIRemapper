# 04 — PhoneDiagram edge ripple

## Why

Silhouette only glowed the Plus Key; design expects edge motion when remapping is live.

## What

| File | Change |
|------|--------|
| `ui/components/PhoneDiagram.kt` | `edgeRipple` traveling left-edge glow; slightly lit screen when `highlightKey` |
| `presentation/home/HomeScreen.kt` | Passes `edgeRipple = state.serviceEnabled` |

## Verify

- Master remapping on → cyan key + traveling edge glow + cooler screen fill.
- Off → neutral key, no ripple.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| No ripple | `edgeRipple` false or `highlightKey` false |
