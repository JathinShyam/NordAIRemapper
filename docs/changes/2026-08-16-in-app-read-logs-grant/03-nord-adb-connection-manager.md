# 03 — NordAdbConnectionManager

## Why

`libadb-android` requires an `AbsAdbConnectionManager` with a stable RSA private key + X.509 certificate so pairing and later TLS connects use the same identity.

## What

**New file:** `app/src/main/java/com/nordairemapper/service/adb/NordAdbConnectionManager.kt`

- Singleton via `getInstance(context)`
- Extends `AbsAdbConnectionManager`
- `setApi(Build.VERSION.SDK_INT)`
- Device name: `NordAIRemapper`
- Persists under `filesDir/adb/`:
  - `adbkey.pk8` — PKCS#8 private key
  - `adbkey.crt` — X.509 cert
- Generation via `android.sun.security.x509.CertAndKeyGen("RSA", "SHA512withRSA")` + `X500Name("CN=Nord AI Remapper")`, ~10 year validity
- Reloads from disk on subsequent launches; regenerates if load fails

## Verify

First grant creates the two files under the app’s private `files/adb/`. Second grant should reuse the same key (no re-pair needed if the daemon already trusts the key — may still need Wireless debugging on for connect).

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Pair fails after reinstall | New app signing / new key; must pair again |
| Cert generation crash | Wrong `sun` package; missing `sun-security-android` |
| `getInstance` races | Should be synchronized; check for duplicate instances only if that lock is removed |
