# Visual overlay state-aware feedback

## Why
Toggle actions (auto-rotate, flashlight, DND, ringer cycle) always showed the same icon — e.g. rotation-enabled glyph even when turning rotation off.

## What
| File | Change |
|------|--------|
| `ActionFeedback.kt` | `ActionFeedback` + `ActionFeedbackState` keys |
| `RemapActionExecutor.kt` | Run action first; overlay receives post-toggle state |
| `RemapActionUi.kt` | State-specific icons + captions (On/Off/Ring/…) |
| `VisualActionPopup.kt` | Optional caption under icon |
| `ActionFeedbackOverlayService.kt` | Accept `ActionFeedback` |

Stateful mappings:
- Auto-rotate → ScreenRotation + "On" / ScreenLockRotation + "Off"
- Flashlight → FlashlightOn/Off + "On"/"Off"
- DND → DoNotDisturbOn/Off + "On"/"Off"
- Ringer cycle → VolumeUp / Vibration / VolumeOff + Ring/Vibrate/Silent

## Verify
1. Map Single press → Auto-rotate; toggle twice on device.
2. Visual overlay should alternate lock vs rotate icons with **On** / **Off** captions.
3. Repeat for flashlight and DND if mapped.

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:testDebugUnitTest --tests com.nordairemapper.presentation.common.ActionFeedbackUiTest
```
