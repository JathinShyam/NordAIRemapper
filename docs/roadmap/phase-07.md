# Phase 7 — Remap Config Screen + app picker

**Status:** ⏳ Pending  
**Depends on:** Phase 6 (navigation + ActionCard helpers)  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 7: Remap Config — categorized actions, app picker, test button`

---

## 1. Goal

Let the user assign any `RemapAction` to Single / Double / Long press with searchable app picker, confirmation animation, conflict awareness, and “Try this action now”.

---

## 2. Detailed requirements

### 2.1 Screen structure

| Element | Spec |
|---------|------|
| Top bar | Press type title (“Single Press”); back |
| Conflict banner | If this press’s action matches another press type |
| Categorized list | Apps / Media / System / Overlay / None — sticky headers |
| Row | Icon, name, short description; selected state checkmark |
| Launch App | Opens modal bottom sheet: search field + installed launchers |
| Open URL | Bottom sheet with text field + save (or inline dialog → prefer sheet) |
| Camera | Two rows or sub-options: Rear / Front |
| Volume | Two rows: Up / Down |
| Footer | “Try this action now” button — calls `ActionDispatcher.execute` for current selection |
| Save UX | Tap action → `setAction` → brief animated confirmation (snackbar or check spring) |

### 2.2 App picker

- Query `PackageManager` with MAIN/LAUNCHER (manifest `<queries>` already present).  
- Sort by label; filter by query string.  
- Show app icon via `Drawable` → `rememberDrawablePainter` or Coil if added; prefer **no new deps** — use `AndroidView` ImageView or `painter` from bitmap.  
- On select → `RemapAction.LaunchApp(packageName, label)` → save → dismiss sheet.

### 2.3 Catalog source of truth

`RemapActionCatalog` object listing all selectable templates (excluding parameterized until filled). Share with Overlay Settings (Phase 9).

### 2.4 ViewModel

```
RemapViewModel(pressType: PressType)
- observe current action
- observe all configs for conflict
- setAction(action)
- tryNow()
- loadInstalledApps(): List<AppInfo>
```

Use `SavedStateHandle` for nav arg `pressType`.

### 2.5 Navigation

Replace Phase 6 placeholder:

`composable("remap/{pressType}")` with typed arg.

---

## 3. Files

| Path | Action |
|------|--------|
| `presentation/remap/RemapScreen.kt` | Create |
| `presentation/remap/RemapViewModel.kt` | Create |
| `presentation/remap/AppPickerSheet.kt` | Create |
| `presentation/common/RemapActionCatalog.kt` | Create |
| `NordNavHost.kt` | Wire real screen |
| Delete placeholder remap screen | |

---

## 4. Implementation steps

1. Catalog + reuse `RemapActionUi`.  
2. RemapViewModel with nav arg.  
3. List UI + selection.  
4. App picker sheet.  
5. URL / camera / volume parameter UX.  
6. Try now + confirmation.  
7. `assembleDebug` + smoke + commit.

---

## 5. Acceptance

- [ ] Assign each category action and see it on Home cards  
- [ ] App search works; Launch App persists  
- [ ] Try now executes (Accessibility connected for global actions)  
- [ ] Conflict warning visible when duplicate  
- [ ] Build green  

## 6. Out of scope

Overlay slot assignment UI (Phase 9 reuses catalog).
