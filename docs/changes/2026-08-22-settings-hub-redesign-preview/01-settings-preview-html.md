# 01 — settings-preview.html

## Why

Need a single file reviewers can open before any Compose port.

## What

Phone frame (`#0A0A0A` / `#141414` / `#0AC6FF`, Space Grotesk + Inter) with:

- Grouped surfaces (hairline dividers, not N cards)
- Interactive theme segment, switches, battery exempt toggle, exclusion chip demo
- About version + GitHub row

## Verify

Open `design/settings-preview.html` — full scroll; battery / exclusions / GitHub intentional at bottom.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Theme segment not highlighting | Click missed `button[data-v]` |
| Exclusion chips never show | `#addExcl` / empty panel hidden class mismatch |
