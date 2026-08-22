# Exclusions as dedicated Settings page

## Why

Inline Per-App Exclusions under Reliability crowded the hub. A separate page is cleaner and easier to manage.

## What

| File | Change |
|------|--------|
| `ExclusionsScreen.kt` | New screen: empty state + app rows (label/package) + picker |
| `SettingsScreen.kt` | Reliability hub row only (status chip + chevron) |
| `NordNavHost.kt` | `Routes.EXCLUSIONS` wired |
| `design/settings-preview.html` / `nord-edge-prototype.html` | Design sync |

## Verify

1. Settings → Reliability → Per-App Exclusions opens the new page  
2. Add / remove apps; chip on Settings hub updates on return  
3. Empty state shows Add Excluded App  

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Chip stuck at None | Settings screen not recomposing from `settings.excludedApps` |
| Picker empty | `queryLaunchableApps` failed / still loading |
