# Nord Edge — redesign proposal

**Status:** Approved for Compose implementation (2026-08-20).  
**Interactive spec:** [`design/nord-edge-prototype.html`](../../design/nord-edge-prototype.html)  
**Handoff prompt for implementer:** [`2026-08-20-nord-edge-impl-prompt.md`](./2026-08-20-nord-edge-impl-prompt.md)  
**Date:** 2026-08-20  
**Direction:** A — Edge Orchestra

## Creative brief

The Plus Key is a physical instrument on the left edge. The app is a precision
instrument panel for that key: OLED black, cyan signal, OnePlus red typographic
spark, springy motion that teaches gestures.

## Directions

| ID | Name | Verdict |
|----|------|---------|
| A | Edge Orchestra | **Recommended** |
| B | Instrument Cluster | Strong motion, risk of HUD/gamer feel |
| C | Quiet Luxury | Classy, weaker hardware teaching |

## Direction A highlights

- **Home / Stage:** compact silhouette + three gesture pads + live press mirror
- **Unlock:** USB-first; Wireless behind “No computer”; no pairing ports on happy path
- **Overlay:** staggered spring “chord” tiles
- **Lab:** advanced only (was Developer)
- **Motion:** edge ripple, pad morph, timeline bloom — purposeful, not decorative spam

## Phased implementation (if approved)

1. Theme tokens + Stage Home + Unlock USB-first  
2. Remap studio + motion  
3. Overlay chord + Lab cleanup  

## Artifacts

- **Interactive prototype (open in browser):** [`design/nord-edge-prototype.html`](../../design/nord-edge-prototype.html)
  End-to-end Direction A review set (all screens):
  - **Flow:** Onboarding (2-step), Unlock (USB-first timeline)
  - **Stage:** Home / Stage, Remap studio, Overlay chord
  - **Settings:** Hub, Feedback, Preferences, Visual overlay, Lock screen, Backup, Overlay editor, Key setup
  - **Lab:** Strategy chips, timing sliders, USB copy → Unlock
  - Play controls: Simulate Plus Key / Double / Long, Reset Unlock
- Canvas: `nord-edge-redesign.canvas.tsx` (Cursor canvases)
- Concept stills: Home / Unlock / Overlay (generated in session assets)

```bash
# from repo root
xdg-open design/nord-edge-prototype.html
# or: open design/nord-edge-prototype.html  (macOS)
```

**Not Figma** — this is a clickable HTML/CSS/JS prototype so you can feel motion without a Figma seat. If you want a real Figma file later, we can export frames from this once Direction A is locked.
