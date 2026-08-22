# Phase 6 — Home Screen (end-to-end)

**Status:** ✅ Done  
**Depends on:** Phases 1–5  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 6: Home Screen — phone diagram, press cards, status, master toggle`

---

## 1. Goal

Replace the temporary two-button Home with the product Home: Nord 5 silhouette with Plus Key highlight, three press-type cards, master remapping toggle, service status, troubleshooting banner, and navigation to Remap (stub route OK if Phase 7 not done — navigate with press type), Developer, and Settings (placeholder until Phase 11).

---

## 2. User-visible outcome

1. Open app → Home feels like OxygenOS-adjacent remapper (dark, cards, Nord blue).  
2. See whether detection/service is healthy without opening Developer.  
3. Tap a press card → navigate to Remap Config (Phase 7) or temporary placeholder.  
4. Master toggle enables/disables remapping via `SettingsRepository.setServiceEnabled`.  
5. Banner appears when setup is incomplete (identity / Accessibility / READ_LOGS).

---

## 3. Detailed requirements

### 3.1 Layout (top → bottom)

| Zone | Spec |
|------|------|
| Top app bar | Title “Keyforge”; actions: Settings (icon), Developer (optional overflow or icon) |
| Status row | Green/red dot + “Service active/inactive”; tap inactive → open Accessibility settings (or Developer if Logcat strategy) |
| Master toggle | Pill / Switch “Remapping enabled” bound to `serviceEnabled` |
| Phone diagram | Centered composable silhouette; Plus Key on right edge with Nord blue glow (`#0AC6FF`) |
| Press cards | Three `ActionCard`s: Single / Double / Long — icon, title, assigned action label |
| Banner | Conditional troubleshooting card (see §3.3) |
| Conflict | Subtle warning on cards if two press types share same non-`None` action |

### 3.2 `PhoneDiagram` component

- File: `ui/components/PhoneDiagram.kt`  
- Draw with Compose Canvas / Vector: rounded rect phone body, side key capsule.  
- Animated glow on key (alpha pulse) when `serviceEnabled && detectionHealthy`.  
- Content description for a11y: “OnePlus Nord 5 outline with Plus Key highlighted”.

### 3.3 Troubleshooting banner rules

Show if **any**:

| Condition | Message / CTA |
|-----------|----------------|
| Accessibility not enabled | “Enable accessibility” → `AccessibilityUtils.openAccessibilitySettings` |
| Strategy ACCESSIBILITY + identity not configured | “Learn your Plus Key” → Key learning |
| Strategy LOGCAT + !READ_LOGS | “Grant READ_LOGS via ADB” → Developer |
| Strategy LOGCAT + READ_LOGS but service not running | “Start logcat watcher” → Developer / start service |
| Always mention | Stock Plus Key may still fire — set a harmless default in system Plus Key settings |

### 3.4 ViewModel state

`HomeViewModel` exposes `StateFlow<HomeUiState>`:

```
data class HomeUiState(
  val serviceEnabled: Boolean,
  val accessibilityEnabled: Boolean,
  val detectionStrategy: DetectionStrategy,
  val keyConfigured: Boolean,
  val readLogsGranted: Boolean,
  val actions: Map<PressType, RemapAction>,
  val conflictPressTypes: Set<PressType>, // press types that share an action
  val banner: HomeBanner?,
)
```

Refresh accessibility / READ_LOGS on `ON_RESUME` (Lifecycle).

### 3.5 Navigation hooks (prepare for later phases)

| Action | Route |
|--------|-------|
| Press card | `remap/{pressType}` (Phase 7) — until then temporary “Coming in Phase 7” screen OR implement empty Remap shell |
| Settings icon | `settings` placeholder route (Phase 11) |
| Key learning / Developer | Existing routes |
| Overlay shortcut (optional) | Defer to Phase 9 |

**Decision for Phase 6:** Add `remap/{pressType}` route with a **thin placeholder** screen (“Remap config — Phase 7”) so navigation works; Phase 7 replaces the body. Add `settings` placeholder similarly.

---

## 4. Files to create / change

| Path | Action |
|------|--------|
| `presentation/home/HomeViewModel.kt` | Create |
| `presentation/home/HomeScreen.kt` | Replace stub |
| `presentation/home/HomeUiState.kt` | Create (or colocated) |
| `ui/components/PhoneDiagram.kt` | Create |
| `ui/components/ActionCard.kt` | Create |
| `ui/components/StatusDot.kt` | Create (optional) |
| `presentation/navigation/NordNavHost.kt` | Wire Home VM destinations + placeholders |
| `presentation/remap/RemapPlaceholderScreen.kt` | Temporary |
| `presentation/settings/SettingsPlaceholderScreen.kt` | Temporary |

---

## 5. Action display helpers

Create `domain` or `presentation` helper:

- `RemapAction.displayName(): String`  
- `RemapAction.displayDescription(): String` (optional)  
- Icon mapping via Material Icons Extended  

Used by Home cards and Phase 7 list — put in `presentation/common/RemapActionUi.kt` to avoid duplicating later.

---

## 6. Implementation steps (ordered)

1. Add `RemapActionUi` label/icon helpers.  
2. Build `PhoneDiagram`, `ActionCard`.  
3. Implement `HomeViewModel` + banner logic.  
4. Rewrite `HomeScreen` layout.  
5. Add placeholder routes for remap + settings.  
6. Lifecycle resume refresh.  
7. `assembleDebug`.  
8. Manual smoke.  
9. Commit + mark roadmap ✅.

---

## 7. Acceptance checklist

- [ ] Home shows silhouette + three cards with live Room data  
- [ ] Master toggle persists and is read by RemapEngine (`serviceEnabled`)  
- [ ] Status reflects Accessibility enabled state  
- [ ] Banner appears for unconfigured key / missing a11y / missing READ_LOGS  
- [ ] Conflict badge when two press types share action  
- [ ] Navigate to key learning, developer, remap placeholder, settings placeholder  
- [ ] `./gradlew assembleDebug` green  

## 8. Out of scope

Full Remap UI, onboarding gate, overlay, backup, real Settings content.
