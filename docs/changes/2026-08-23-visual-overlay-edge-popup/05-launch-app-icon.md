# Visual overlay: Launch App shows real icon

## Why
Launch App remaps showed the generic Apps glyph in the Visual Overlay popup; users expect the target app’s launcher icon.

## What
| File | Change |
|------|--------|
| `VisualActionPopup.kt` | Optional `appIcon: ImageBitmap?` on layer/pill; prefer bitmap over vector |
| `ActionFeedbackOverlayService.kt` | Load `PackageManager.getApplicationIcon` on IO for `RemapAction.LaunchApp` |

Falls back to the generic Apps icon if the package is missing or icon load fails.

## Verify
1. Map a press to Launch App (e.g. Camera or Chrome).
2. Fire the press with Visual Overlay enabled — popup shows that app’s icon.
3. Non-app actions still show vector icons as before.

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```
