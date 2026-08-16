# 02 — Manifest permissions

## Why

Wireless ADB pairing uses loopback/network sockets and NSD (mDNS) to discover the TLS pairing port while “Pair device with pairing code” is open.

## What

File: `app/src/main/AndroidManifest.xml`

Added:

- `INTERNET` — ADB sockets
- `ACCESS_NETWORK_STATE`
- `CHANGE_WIFI_MULTICAST_STATE` — mDNS discovery
- `NEARBY_WIFI_DEVICES` (`neverForLocation`, `tools:targetApi="33"`) — NSD on API 33+

`READ_LOGS` was already declared with `tools:ignore="ProtectedPermissions"`.

## Verify

Install debug APK; on Enable detection, “Find pairing port” while the system pairing dialog is open should eventually show a discovered port (or time out with manual-port copy).

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Discovery always times out | Pairing dialog closed; Wi‑Fi off; multicast blocked; missing `CHANGE_WIFI_MULTICAST_STATE` / nearby Wi‑Fi |
| Pair works with manual port only | mDNS path broken; host/port from dialog still usable |
