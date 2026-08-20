# 03 — Settings, Feedback, Overlay, Backup

## Why

Unicode hub glyphs were inconsistent; exclusions and empty overlay slots lacked guidance; pulse preview didn’t animate; overlay tiles snapped; Backup front-loaded local snapshots.

## What

| File | Change |
|------|--------|
| `presentation/settings/SettingsScreen.kt` | HubRow `ImageVector` Material Icons; exclusions empty copy |
| `presentation/settings/FeedbackScreen.kt` | Pulse ring + core scale/alpha on preview tap |
| `ui/components/OverlayPreview.kt` | Scale/fade `AnimatedContent` on slot/layout change |
| `presentation/overlay/OverlaySettingsScreen.kt` | Empty-slot hint; empty ActionCards |
| `presentation/backup/BackupScreen.kt` | Local snapshot collapsed behind expand row |

## Verify

- Settings rows show Vibration / Visibility / Lock / Backup icons in 36dp boxes.
- Pulse preview vibrates and expands ring.
- Changing an overlay slot animates preview; all-None shows assign hint.
- Backup defaults to Export/Import only.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Hub icon missing at compile | Material Icons extended not on classpath for that glyph |
| Preview no animation | Slot `conflictKey()` unchanged (same action type) |
