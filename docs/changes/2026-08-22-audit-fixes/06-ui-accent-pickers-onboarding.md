# 06 — UI: theme-aware accents; picker perf; onboarding back-nav

## Why
Light mode shipped broken: `categoryAccent()` returned near-black containers
regardless of scheme (dark blobs on light surfaces, poor tint contrast) across
Home/Remap/Overlay/Settings. App picker did binder calls + bitmap decode per
row on Main and enumeration on Main (scroll jank, hitch opening sheet), with no
empty state. Onboarding lost page state on process death and could not go back.

## What
| File | Change |
|------|--------|
| `presentation/common/ActionCategoryAccent.kt` | `adaptiveAccent()` picks dark/light pairs; per-category light twins chosen for contrast; SYSTEM uses scheme primaryContainer + LightNavy in light |
| `presentation/settings/SettingsScreen.kt` | Hard-coded hub-row pairs routed through `adaptiveAccent()` |
| `presentation/remap/InstalledApps.kt` (new) | Shared off-main launcher enumeration (`Dispatchers.Default`) |
| `RemapViewModel.kt`, `OverlaySettingsViewModel.kt`, `SettingsScreen.kt` | Use it; loading flags |
| `presentation/remap/AppPickerSheet.kt` | Icons decoded on IO per row into state; spinner while loading; "No apps match your search" empty row |
| `presentation/remap/RemapScreen.kt`, `RemapViewModel.kt` | "Done" just navigates (real saves already announce); Try-now spinner tracks actual execution; VM emits stable event constants |
| `presentation/onboarding/OnboardingScreen.kt` | `rememberSaveable` page state; back arrow to previous step |

## Verify
1. ThemeMode LIGHT: Home action cards/chips, Settings hub rows show pastel
   containers with readable tints.
2. Open app picker on a 300+ app device: sheet opens without jank; scroll smooth.
3. Search gibberish → empty-state text.
4. Kill onboarding process mid-flow → reopens on same step; back arrow works.

## Debug tips
| Symptom | Likely cause |
|---|---|
| New accent looks wrong somewhere | A call site still passing literal colors instead of `categoryAccent()/adaptiveAccent()` |
