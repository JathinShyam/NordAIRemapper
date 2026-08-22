# Display name → Keyforge

## Why

User chose **Keyforge** as the product name (replacing “Nord AI Remapper” for launcher / UI / docs).

## What

| File | Change |
|------|--------|
| `res/values/strings.xml` | `app_name`, accessibility label → Keyforge |
| Home / Onboarding / banners / notifications / ADB cert CN | User-facing copy |
| `README.md`, `AGENTS.md`, `docs/PRD.md` / TRD / ARCHITECTURE | Product title |
| Design HTML + tokens | Prototypes say Keyforge |

**Unchanged:** package `com.nordairemapper` (keeps installs / `READ_LOGS` grants). Repo folder / CI APK filenames still `NordAIRemapper-*` for now.

## Verify

1. Launcher label shows **Keyforge**.
2. Accessibility list shows **Keyforge key detection**.
3. Home subtitle `Keyforge · Home`; onboarding title Keyforge.
4. `adb shell pm grant com.nordairemapper …` still works (package unchanged).

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Old name on home screen | Launcher cache — reinstall |
| Grant fails after rename | Must still use `com.nordairemapper`, not a new id |
