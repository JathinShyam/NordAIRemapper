# Exclusions app icons + Settings design parity

## Why
Compose Settings still drifted from `design/nord-edge-prototype.html` on the exclusions detail page (no app logos, extra Done CTA, empty-state hint placement) and Version was not bolded.

## What
| File | Change |
|------|--------|
| `ExclusionsScreen.kt` | App logos on each row; remove Done; hint only when list is filled |
| `SettingsScreen.kt` | Version uses `VersionMeta` (bold version name) |
| `SettingsUi.kt` | Annotated-string imports for `VersionMeta` |

## Verify
1. Settings → Reliability → Per-App Exclusions shows empty state without bottom hint.
2. Add apps — each row shows the real app icon, label, package, remove.
3. About → Version shows bold version · build label.
4. `./gradlew :app:compileDebugKotlin`

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Blank icon placeholder | Package uninstalled / icon load failed on IO |
| Version not bold | `subtitleContent` not wired / `VersionMeta` unused |
