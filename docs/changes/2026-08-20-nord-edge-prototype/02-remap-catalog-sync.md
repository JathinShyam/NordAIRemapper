# 02 — Remap studio catalog sync

## Why

Prototype Remap studio used invented groups/actions and generic diamond icons. Review required the real catalog and readable action icons.

## Source of truth

| File | Role |
|------|------|
| `presentation/common/RemapActionCatalog.kt` | Categories + full assignable list |
| `presentation/common/RemapActionUi.kt` | Display names + descriptions |

## Categories (chips)

Apps → Media → System → Overlay → None

## Actions (miss none)

| Category | Actions |
|----------|---------|
| Apps | Launch app…, Open URL / deep link… |
| Media | Play / pause, Next track, Previous track, Volume up, Volume down |
| System | Assistant, Rear camera, Front camera, Flashlight, Screenshot, Do Not Disturb, Ring / vibrate / silent, Notification shade, Quick settings, Recents, Home, Back, Lock screen, Auto-rotate |
| Overlay | Show overlay |
| None | No action |

## Icons

Per-action stroke SVGs on cyan badge (`--accent`); Overlay tone slight; None uses muted / soft-red when selected. No shared rhombus placeholder.
