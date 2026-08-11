# Phase 8 — Onboarding permission flow

**Status:** ✅ Done  
**Depends on:** Phase 6 (Home exists as destination)  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 8: Onboarding — five-step permission flow`

---

## 1. Goal

First-launch multi-step onboarding (one concern per screen) that explains why each permission is needed and deep-links to system settings. After completion, never show again unless reset from Settings (Phase 11).

---

## 2. Screens (exact order)

| # | Screen | Primary CTA | Skip |
|---|--------|-------------|------|
| 1 | Welcome — name, logo, one-liner | Next | Allowed |
| 2 | Accessibility — why key detection needs it | Open Accessibility settings | **Disabled** until enabled *or* user explicitly continues after returning (prefer: Next enabled only when `isServiceEnabled`) |
| 3 | Overlay — `SYSTEM_ALERT_WINDOW` | Open overlay settings | Disabled until granted (or Continue when granted) |
| 4 | Notifications + battery | Request `POST_NOTIFICATIONS`; open battery exemption | Notifications: runtime; battery: settings intent |
| 5 | All set — animated checkmark | “Let’s go” → Home | N/A |

**PRD note:** Skip disabled for steps 2–4. Implement as: cannot advance until permission/state satisfied, with “I’ve enabled it — Recheck” button.

---

## 3. Persistence

DataStore key `onboarding_completed: Boolean` (add to `SettingsRepository` / `AppSettings` or separate flag).

`MainActivity` / `NordNavHost` start destination:

```
if (!onboardingCompleted) ONBOARDING else HOME
```

---

## 4. Files

| Path | Action |
|------|--------|
| `presentation/onboarding/OnboardingScreen.kt` | Horizontal pager or nav graph nested |
| `presentation/onboarding/OnboardingViewModel.kt` | Create |
| `SettingsRepository` | `setOnboardingCompleted` / flow |
| `NordNavHost` | Conditional start |
| Manifest | Ensure overlay settings intent works |

---

## 5. Implementation steps

1. Add onboarding flag to DataStore.  
2. Build 5-page UI with icons/titles/body/CTA.  
3. Wire permission checks + resume refresh.  
4. Gate NavHost start destination.  
5. Build + wipe app data smoke test.  
6. Commit.

---

## 6. Acceptance

- [ ] Fresh install lands on Welcome  
- [ ] Cannot finish without Accessibility path addressed  
- [ ] Overlay + notification prompts work on API 33+  
- [ ] “Let’s go” sets flag and opens Home  
- [ ] Second launch skips onboarding  
- [ ] Build green  

## 7. Out of scope

Full Settings “Reset onboarding” (can add stub; complete in Phase 11).
