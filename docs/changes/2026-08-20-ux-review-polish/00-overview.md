# UX review polish — overview

Addressed the Critical UX & UI Review (high → low) against Nord Edge design: scannable Home/Remap, clearer status hierarchy, Material Icons on Settings, and small empty-state / motion polish.

## Files

| Path | Role |
|------|------|
| `01-home-action-status.md` | Home action icons, empty slots, status ribbon + master switch |
| `02-remap-scan-search.md` | Remap category colors, Current pill, search bar, subtitle, Done toast |
| `03-settings-feedback-overlay-backup.md` | Hub icons, exclusions empty, pulse preview, overlay anim/hint, backup collapse |
| `04-phone-diagram-ripple.md` | Edge ripple + lit screen when remapping is on |
| `05-design-prototype-sync.md` | `design/nord-edge-prototype.html` mirrored to Compose UX |
| `FILE_MANIFEST.md` | Path → doc map |

## Verify

1. Home: action cards show icons; empty slots dashed + “+ Assign”; status ribbon above master switch with pulse when active.
2. Remap: pill search with clear; Current row has icon pill; list icons tinted by category; Done shows “Saved” toast; subtitle includes press type.
3. Settings hub uses Material Icons; exclusions empty copy; Feedback pulse animates with vibrate; Overlay preview scales on slot change; Backup local snapshot collapsed by default.
4. `./gradlew :app:compileDebugKotlin` succeeds.
