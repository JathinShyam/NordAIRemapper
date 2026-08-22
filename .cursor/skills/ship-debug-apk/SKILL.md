---
name: ship-debug-apk
description: >-
  Build and ship the Keyforge debug APK. Use when assembling,
  installing on the phone, pushing to main for CI, or when install fails
  with INSTALL_FAILED_UPDATE_INCOMPATIBLE / signature mismatch.
---

# Ship debug APK

## Build locally

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Which binary the phone can install

| Install on phone | Signed by | Overwrite with |
|---|---|---|
| GitHub Release `latest-debug` | CI debug keystore | **Push to `main`** (or same CI key) |
| Android Studio / local `assembleDebug` | local `~/.android/debug.keystore` | local `adb install -r` only |

`adb install -r` of a local APK onto the GitHub-signed app fails:

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.nordairemapper signatures do not match
```

**Do not uninstall** unless the user agrees — that drops `READ_LOGS` and remap settings.

Default for device testing: **commit + push `main`**. CI publishes:

- Rolling [latest-debug](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug) (`NordAIRemapper-debug-latest.apk`)
- Immutable `debug-<sha>` release with `NordAIRemapper-debug-<sha>.apk` (older versions are kept)
- The same files on the Actions run (14-day artifacts)

Commit and push only when the user asks.

## Local install (same signature only)

```bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
```

USB must be file-transfer + debugging, not tethering-only. See [device-testing](../device-testing/SKILL.md).

## After install

Ask the user to confirm:

1. Detection still Logcat (or Accessibility if that is their setup)  
2. Single / double / long still mapped  
3. `READ_LOGS` still granted if using logcat  

If they had to uninstall, re-run the grant command.
