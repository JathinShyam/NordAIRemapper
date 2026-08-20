# 04 — Overlay chord motion

## Why

Prototype staggered spring tiles.

## What

`service/FloatingOverlayService.kt` — `OverlayActionButton` fades/scales/slides in with per-index delay; rounded square icon wells (not circles).

Detection/executor unchanged.

## Verify

Trigger Show overlay — tiles cascade in. Tap outside still dismisses.
