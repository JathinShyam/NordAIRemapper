# Settings instrument redesign

## Why
Settings felt like a Material catalog clone: rainbow icon wells, Title Case Everywhere, UPPERCASE status chips, and two visual dialects (hub vs card-per-toggle). Align with Nord Edge instrument panel + AOSP guidance (status over description).

## What
| Area | Change |
|------|--------|
| `SettingsUi.kt` | `SettingsGroup`, quiet `SettingsNavRow`, unified `SettingsToggleRow`, `SettingsSegmentedControl`; status via `StatusChip` |
| `SettingsScreen.kt` | Monochrome icons; Reliability folds Permissions; Advanced → Lab; status secondary text |
| Settings subpages | Appearance, Feedback, Visual Overlay, Lock, Permissions, Exclusions share one dialect |
| Tools destinations | Floating Menu / Backup / Lab / Key Setup padding + Lab segment chrome |
| Design HTML | `settings-preview.html` + prototype copy/IA sync |

## Verify
```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug :app:testDebugUnitTest
```
Walk Settings → every subpage; hub has no rainbow wells; Feedback/Lock are grouped toggles; Lab uses segmented strategy control.
