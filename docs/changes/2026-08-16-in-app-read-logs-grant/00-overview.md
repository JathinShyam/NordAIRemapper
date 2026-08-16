# In-app READ_LOGS grant (2026-08-16)

Phone-only setup so normal users can detect the Nord 5 Plus Key without Shizuku or a laptop.

## Hard limit (do not regress)

On stock Nord 5, OxygenOS/`OplusKeyEventUtil` never delivers the Plus Key to `AccessibilityService.onKeyEvent`. Logcat + `READ_LOGS` is the working path. Accessibility remains required for system actions.

## File index

| File | Covers |
|------|--------|
| [FILE_MANIFEST.md](./FILE_MANIFEST.md) | Every new/modified path → doc map |
| [01-gradle-dependencies.md](./01-gradle-dependencies.md) | JitPack, libadb-android, Conscrypt, sun-security-android |
| [02-manifest-permissions.md](./02-manifest-permissions.md) | INTERNET, multicast, NEARBY_WIFI_DEVICES |
| [03-nord-adb-connection-manager.md](./03-nord-adb-connection-manager.md) | RSA key/cert persistence for ADB pairing |
| [04-read-logs-grant-via-wireless-adb.md](./04-read-logs-grant-via-wireless-adb.md) | Pair → connect → `pm grant` → verify → sync watcher |
| [05-enable-detection-ui.md](./05-enable-detection-ui.md) | Enable detection screen + ViewModel |
| [06-nav-home-onboarding-wiring.md](./06-nav-home-onboarding-wiring.md) | Routes, Home banner, onboarding step |
| [07-developer-shizuku-deemphasize.md](./07-developer-shizuku-deemphasize.md) | Remove Shizuku as primary path; USB fallback |
| [08-detection-sync-and-docs.md](./08-detection-sync-and-docs.md) | Auto-start logcat watcher + device-testing skill |
| [09-proguard-and-release.md](./09-proguard-and-release.md) | R8 keeps for libadb / Conscrypt / `android.sun.security` |
| [10-onboarding-page-map.md](./10-onboarding-page-map.md) | Exact onboarding indices after insert |
| [11-entry-points.md](./11-entry-points.md) | All navigation paths to Enable detection |
| [12-privacy-and-limits.md](./12-privacy-and-limits.md) | Claims, non-claims, log tags, shell surface |
| [13-wireless-debugging-ssid-allow.md](./13-wireless-debugging-ssid-allow.md) | SSID Allow dialog vs pairing code page |
| [14-pairing-vs-connect-port.md](./14-pairing-vs-connect-port.md) | Failed to connect: pairing port ≠ connection port |

## End-to-end flow

```
Accessibility on
  → Enable detection screen
  → Open Wireless debugging → “Pair device with pairing code”
  → App mDNS-discovers pairing port (or user enters port)
  → User enters 6-digit code
  → NordAdbConnectionManager.pair → connectTls
  → shell: pm grant com.nordairemapper android.permission.READ_LOGS
  → DetectionCoordinator.syncLogcatWatcher
  → User may turn Wireless debugging off (grant persists)
```
