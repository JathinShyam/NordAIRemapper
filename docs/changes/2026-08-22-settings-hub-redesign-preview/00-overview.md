# Settings hub redesign — design preview

## Why

Settings felt like a stack of bordered cards; helpers were flat; theme used outlined buttons; battery / exclusions / About+GitHub were missing or weak in the prototype.

## What

Design-only HTML (no Compose):

| File | Change |
|------|--------|
| `design/settings-preview.html` | Standalone phone-frame Settings preview: Appearance / Reliability / Shortcuts / Tools / About |
| `design/nord-edge-prototype.html` | Settings hub synced to the same structure + interactions |

## Structure

1. **Appearance** — one group: pill theme segment, Dynamic color, Service notification  
2. **Shortcuts** — Feedback, Preferences, Visual overlay, Overlay settings  
3. **Tools** — Lock, Backup, Key setup, Lab, Restart onboarding  
4. **Reliability** — battery status chip + CTA; exclusions hub row (chip + chevron) → detail screen  
5. **About** — Version + GitHub (“Source code & issues”)

See also [03-exclusions-detail-page.md](./03-exclusions-detail-page.md).

## Verify

1. Open `design/settings-preview.html` in a browser.  
2. Open prototype → Settings: Reliability → **Per-App Exclusions** opens its own screen.  
3. Toggle theme segment, switches, battery CTA; add/remove exclusion apps; hub chip updates.

## Status

Compose Settings hub + exclusions page are implemented; keep HTML previews in sync for review.
