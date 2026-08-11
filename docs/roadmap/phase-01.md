# Phase 1 — Project setup & theme

**Status:** ✅ Done  
**Gate:** `./gradlew assembleDebug`

## Goal

Scaffold a compile-ready Android app with Gradle KTS, Hilt, Room, DataStore, Compose Material 3, and Nord dark-first theme tokens.

## Deliverables

| Item | Detail |
|------|--------|
| Gradle | Root + `:app`, version catalog, AGP/Kotlin/KSP/Hilt plugins |
| App entry | `NordRemapperApp` (`@HiltAndroidApp`), `MainActivity` |
| Theme | `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` — palette `#0A0A0A` / `#141414` / `#0AC6FF` |
| Font | Infrastructure note for OnePlus Sans; ship `FontFamily.Default` |
| Resources | Launcher adaptive icon, strings, themes XML |
| Docs | Root README |

## Acceptance

- [x] Project opens / builds
- [x] App launches to placeholder UI
- [x] Theme colors match PRD

## Out of scope

Detection, Room schema usage, screens beyond shell.
