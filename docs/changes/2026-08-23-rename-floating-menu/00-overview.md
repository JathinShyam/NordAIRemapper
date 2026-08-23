# Rename floating overlay → Floating Menu (user-facing)

## Why
"Overlay" and "Visual Overlay" sounded like the same feature. The floating quick-action panel is now **Floating Menu** everywhere users see it.

## What
| Area | Change |
|------|--------|
| Settings hub | Overlay Settings → **Floating Menu** |
| `OverlaySettingsScreen` | Screen title, enable toggle, slot copy |
| `RemapActionUi` / catalog | Category + **Show floating menu** action |
| `FloatingOverlayService` | Panel heading, toasts, notification title |
| Onboarding | Display-over-apps copy + CTA |
| `VisualOverlayScreen` | Clarifies distinction vs Floating Menu |
| Design prototypes | Matching labels |

Internal names unchanged: `ShowOverlay`, `OverlayConfig`, `overlay_settings` route, Room table, `TYPE_APPLICATION_OVERLAY`.

## Verify
1. Settings → Shortcuts: **Feedback**, **Visual Overlay**, **Floating Menu** (three distinct entries).
2. Remap → Floating Menu category → **Show floating menu**.
3. Assign long press → menu opens with **Floating Menu** heading.
4. `./gradlew :app:compileDebugKotlin`
