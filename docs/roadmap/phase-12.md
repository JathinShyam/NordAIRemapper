# Phase 12 — Service resilience

**Status:** ✅ Done  
**Depends on:** Phases 3, 4, 9 (services exist)  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 12: Resilience — BootReceiver, death notification, battery handling`

---

## 1. Goal

Detection and overlay survive reboot and process death as well as Android allows; user is prompted when something dies.

---

## 2. Deliverables

### 2.1 BootReceiver

- Permission `RECEIVE_BOOT_COMPLETED`  
- On boot: if `serviceEnabled`:  
  - Strategy LOGCAT + READ_LOGS → start `LogcatWatcherService`  
  - Accessibility cannot be force-enabled without secure settings — show notification “Re-enable Accessibility if needed”  
- Manifest receiver exported=`false` with BOOT_COMPLETED intent filter (and LOCKED_BOOT if needed — skip unless required)

### 2.2 Death / disconnect notification

- Accessibility `onDestroy` / `onUnbind`: if remapping was enabled, post notification “Key detection stopped — tap to fix”  
- Logcat service `onDestroy`: same if strategy still LOGCAT and enabled  
- Channel: high enough to be noticed; tap → MainActivity or Accessibility settings  

### 2.3 Persistent notification toggle

- Honor `showServiceNotification` for Logcat FGS (cannot remove FGS notification entirely while running — keep MIN importance; if toggle off, still legally required FGS notification but copy can be minimal)  
- Document limitation in Settings helper text  

### 2.4 Battery optimization

- On Home / Settings: if not ignoring optimizations, banner/CTA  
- Use `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` carefully (Play policies) — intent `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with package URI  

### 2.5 RemapEngine start

- Ensure engine starts from Accessibility connect **and** Logcat start **and** Application `onCreate` observer if needed so UI-assigned actions work after boot path  

---

## 3. Files

| Path | Action |
|------|--------|
| `service/BootReceiver.kt` | Create |
| `service/ServiceNotifications.kt` | Helper channels |
| Manifest | Receiver + BOOT permission |
| `PlusKeyAccessibilityService` | Death notify |
| `LogcatWatcherService` | Death notify |
| Home/Settings banners | Battery state |

---

## 4. Acceptance

- [ ] Reboot with Logcat strategy restarts watcher (permission granted)  
- [ ] Killing accessibility yields re-enable notification  
- [ ] Battery CTA visible when not exempt  
- [ ] Build green  
- [ ] Update roadmap README all phases ✅  
- [ ] Update TRD status table  

## 5. Out of scope

Root keep-alives; manufacturer-specific autostart whitelists beyond documentation in README.
