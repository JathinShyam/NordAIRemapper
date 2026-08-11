# Phase 9 — Floating overlay + Overlay Settings

**Status:** ⏳ Pending  
**Depends on:** Phases 5–7 (executor + action catalog)  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 9: Floating overlay service + Overlay Settings with live preview`

---

## 1. Goal

User configures up to 6 overlay slots and triggers a floating menu from a press type (`RemapAction.ShowOverlay` or dedicated “which press triggers overlay” setting).

---

## 2. Product rules

| Rule | Spec |
|------|------|
| Service | `FloatingOverlayService` FGS + `TYPE_APPLICATION_OVERLAY` |
| Layout | User choice: radial/arc **or** horizontal pill bar |
| Slots | 0–6; each a `RemapAction` |
| Trigger | Overlay Settings: which press type shows overlay (default Double) **and/or** assigning `ShowOverlay` action — pick one clear model: **recommend** press-type trigger in OverlayConfig + allow `ShowOverlay` as alias that shows overlay |
| Position | Left / Right / Bottom center |
| Opacity | 30–100% |
| Icon size | S/M/L |
| Animation | Fade / Scale / Slide; spring entrance |
| Dismiss | Outside tap |
| Lock screen | Do not show (`KeyguardManager`) |
| Preview | Compose live preview card on settings screen |

### Recommended data model tweak

Extend `OverlayConfig`:

```
val triggerPressType: PressType = DOUBLE
```

`RemapEngine` after resolving action: if action is `ShowOverlay` **or** (optional) auto-trigger — keep simple: **only** `ShowOverlay` action and/or engine checks overlay enabled + gesture == triggerPressType.  

**Decision:** If overlay enabled and gesture == `triggerPressType`, show overlay **in addition to or instead of** the press’s remap action?  

**Product-safe decision:** Showing overlay is itself the action — user sets Double → `ShowOverlay`. Overlay Settings stores slots/layout; `triggerPressType` is informational default when assigning from Overlay Settings “Assign to double press” helper. Engine remains: execute `ShowOverlay` → start overlay service.

---

## 3. Service design

1. `RemapActionExecutor` branch `ShowOverlay` → `FloatingOverlayService.show(context)`.  
2. Service reads `OverlayConfig` from repo (inject).  
3. Inflate ComposeView in WindowManager.  
4. Button click → `actionDispatcher.execute` → dismiss.  
5. Outside touch → dismiss + `stopSelf` if appropriate.

Manifest: overlay permission already needed; declare service + FGS type (specialUse or specialUse already used — may use `specialUse` with different subtype or `dataSync` — prefer **specialUse** with overlay subtype property, or separate service without conflicting type). Follow TRD: FGS for overlay.

---

## 4. Overlay Settings UI

- Enable toggle  
- Slot editors (tap → action catalog sheet from Phase 7)  
- Position / opacity / size / animation / layout style  
- Live preview  
- Nav from Home overflow or Settings  

---

## 5. Files

| Path | Action |
|------|--------|
| `service/FloatingOverlayService.kt` | Create |
| `presentation/overlay/OverlaySettingsScreen.kt` | Create |
| `presentation/overlay/OverlaySettingsViewModel.kt` | Create |
| `ui/components/OverlayPreview.kt` | Create |
| `RemapActionExecutor.kt` | Wire ShowOverlay |
| Manifest | Service + ensure SYSTEM_ALERT_WINDOW |
| `OverlayConfig.kt` | Fields if missing |
| Nav routes | `overlay_settings` |

---

## 6. Acceptance

- [ ] With overlay permission, ShowOverlay displays menu  
- [ ] Up to 6 actions fire correctly  
- [ ] Dismiss outside; no show on lock screen  
- [ ] Preview updates with sliders  
- [ ] Config persists Room  
- [ ] Build green  

## 7. Out of scope

Backup of overlay (Phase 10 includes in payload).
