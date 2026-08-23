# OLED Black appearance toggle

## Why
OLED panels benefit from true black backgrounds; some users prefer `#000000` over the default `#0A0A0A` scaffold in dark mode.

## What
| File | Change |
|------|--------|
| `AppSettings.kt` | `oledBlack: Boolean` |
| `SettingsRepository` + `SettingsRepositoryImpl` | Persist `oled_black` |
| `SettingsViewModel` | `setOledBlack()` |
| `AppearanceSettingsScreen.kt` | Toggle with subtitle "Use Pure Black Background In Dark Mode" |
| `Theme.kt` | `oledBlack` param; `withOledBlackBackground()` overrides `background` only |
| `MainActivity.kt` | Pass `settings.oledBlack` into theme |
| `OledBlackThemeTest.kt` | Unit test for background override |

## Verify
1. Settings → Appearance → enable **OLED Black** while theme is Dark (or System + device dark).
2. Scaffold/screen background should be pure black; cards/surfaces remain `#141414`.
3. Disable toggle → background returns to `#0A0A0A`.
4. In Light theme, toggle persists but has no visible effect until dark mode is active.

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:testDebugUnitTest --tests com.nordairemapper.ui.theme.OledBlackThemeTest
```
