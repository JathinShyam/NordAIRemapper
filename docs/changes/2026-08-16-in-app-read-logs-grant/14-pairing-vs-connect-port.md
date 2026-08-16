# 14 — Pairing port vs connection port (failed to connect)

## Why

Users enter the pairing code + pairing dialog port correctly, then see “failed to connect”. Pairing can succeed while the post-pair TLS connect fails.

## Root cause

Wireless debugging uses **two ports**:

| Port | Where shown | Used for |
|------|-------------|----------|
| Pairing port | Under the 6-digit code (“Pair device with pairing code”) | `pair()` only — temporary |
| Connection port | Wireless debugging **main** page → “IP address & port” | `connect()` after pairing |

After `pair()`, the app must connect via `adb-tls-connect` (mDNS) or the main-page port — **not** the pairing port. On OxygenOS, mDNS often fails, so a manual Connection port is required.

## What changed

| File | Change |
|------|--------|
| `ReadLogsGrantViaWirelessAdb.kt` | After pair: delay + connect via manual connect port, mDNS TLS connect, then `connectTls`/`autoConnect` retries; prefer Wi‑Fi IPv4 over `127.0.0.1`; clearer errors |
| `EnableDetectionViewModel.kt` | Separate `pairingPort` + `connectPort`; accept `IP:port` paste |
| `EnableDetectionScreen.kt` | UI explains two ports; Connection port field |

## What the user should do now

1. Open **Pair device with pairing code** → enter **6-digit code** + **pairing** IP:port in the app.
2. Close pairing dialog (or leave Wireless debugging on).
3. On the Wireless debugging **detail** page, copy **IP address & port** into **Connection port**.
4. Tap Pair and grant again (fresh pairing code if the old one expired).

## Verify

Grant succeeds when Connection port is filled from the main Wireless debugging page.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Failed to connect after correct code | Missing connection port; used pairing port for connect |
| Pairing failed | Code expired; reopen Pair device with pairing code |
| Still fails with both ports | Wireless debugging off; wrong Wi‑Fi; try USB Advanced fallback |
| logcat `ReadLogsWirelessAdb` | Shows which host:port connect attempts |
