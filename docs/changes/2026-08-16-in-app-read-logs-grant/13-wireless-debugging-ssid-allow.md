# 13 — OxygenOS Wireless debugging SSID Allow vs pairing

## Why

Users enable Wireless debugging and see Network name (SSID) / Wi‑Fi address (BSSID) with Cancel / Allow. That screen has no pairing code or port, so Enable detection looks broken.

## What Android is showing

That dialog is **network approval for Wireless debugging** (“allow wireless debugging on this Wi‑Fi”), not pairing.

Correct sequence on Nord 5 / OxygenOS:

1. Tap **Allow** on the SSID/BSSID dialog.
2. Ensure Wireless debugging toggle is **on**.
3. Tap the **Wireless debugging** row (open the detail page — not only the switch).
4. Tap **Pair device with pairing code**.
5. Leave that dialog open; it shows **6-digit code** and **IP:port**.
6. Enter code (and port if needed) back in Nord AI Remapper.

## Code / copy changes

| File | Change |
|------|--------|
| `EnableDetectionScreen.kt` | Explicit SSID Allow guidance; Developer options secondary button; request `NEARBY_WIFI_DEVICES` before mDNS |
| `EnableDetectionViewModel.kt` | `openDeveloperOptions`, `onNearbyWifiDenied`; clearer discovery status copy |
| `.cursor/skills/device-testing/SKILL.md` | Same operator steps |

## Verify

On device: Allow SSID → open Wireless debugging detail → Pair with pairing code → code+port visible → grant succeeds.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Only SSID / Allow / Cancel | Still on network approval; tap Allow, then open detail page |
| No “Pair device with pairing code” | Wireless debugging off, or still on Developer list — open the Wireless debugging *page* |
| Port never auto-discovered | Pairing dialog closed; deny Nearby Wi‑Fi; enter port manually after colon |
