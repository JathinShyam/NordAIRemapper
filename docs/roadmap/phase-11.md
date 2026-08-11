# Phase 11 — Settings Screen + per-app exclusions

**Status:** ✅ Done  
**Depends on:** Phase 6 placeholders; Phase 4 Developer; Phase 8 onboarding flag  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 11: Settings — theme, haptics, exclusions, links to Developer/Backup`

---

## 1. Goal

Full Settings hub replacing placeholder; implement per-app exclusion list enforced in `RemapEngine` / Accessibility foreground tracking.

---

## 2. Settings sections

| Section | Controls |
|---------|----------|
| Appearance | Theme Dark/Light/System; dynamic color toggle; custom font toggle (wires Type.kt flag via DataStore) |
| Behavior | Persistent notification toggle; haptic toggle |
| Exclusions | List of packages; add via app picker sheet; remove |
| Power | Battery optimization exemption button + status |
| Advanced | Navigate Developer, Key learning, Backup, Overlay settings |
| About | VersionName, GitHub link, open-source licenses (Libraries or simple screen) |
| Onboarding | “Show onboarding again” |

Apply theme from `MainActivity` collecting `themeMode` / `dynamicColor`.

---

## 3. Per-app exclusions (technical)

1. `PlusKeyAccessibilityService.onAccessibilityEvent`: on window state changed, store `foregroundPackage` in singleton / `ForegroundAppTracker`.  
2. `RemapEngine.onGesture`: if package in `excludedApps`, skip dispatch.  
3. Settings UI edits `excludedApps` set via repository.

---

## 4. Files

| Path | Action |
|------|--------|
| `presentation/settings/SettingsScreen.kt` | Create |
| `presentation/settings/SettingsViewModel.kt` | Create |
| `service/ForegroundAppTracker.kt` | Create |
| `PlusKeyAccessibilityService.kt` | Track package |
| `RemapEngine.kt` | Honor exclusions |
| `MainActivity` / theme | Collect theme mode |
| DataStore | Font toggle if missing |
| Remove Settings placeholder | |

---

## 5. Acceptance

- [ ] Theme changes apply  
- [ ] Exclusion blocks remap when that app is foreground  
- [ ] Links to Developer / Backup / Overlay / Key learning work  
- [ ] Battery prompt opens system UI  
- [ ] Build green  

## 6. Out of scope

Play licensing; analytics.
