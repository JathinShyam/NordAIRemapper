# 01 — Gradle dependencies

## Why

Android forbids normal apps from self-granting `READ_LOGS`. In-app Wireless Debugging pairing needs an ADB client library plus TLS/cert support.

## What

| File | Change |
|------|--------|
| `settings.gradle.kts` | Added JitPack: `maven(url = "https://jitpack.io")` under `dependencyResolutionManagement.repositories` |
| `gradle/libs.versions.toml` | Versions: `libAdbAndroid = "3.1.1"`, `sunSecurityAndroid = "1.1"`, `conscryptAndroid = "2.5.3"`; library aliases `libadb-android`, `sun-security-android`, `conscrypt-android` |
| `app/build.gradle.kts` | `implementation` of those three libs |
| `app/proguard-rules.pro` | Keep libadb / Conscrypt / `sun.security` / **`android.sun.security`** (see [09](./09-proguard-and-release.md)) |

## Notes

- Package for cert APIs from `sun-security-android` is **`android.sun.security.x509`**, not `sun.security.x509`.
- Prefer `CertAndKeyGen` from that library for self-signed ADB certs.
- Conscrypt is required for Wireless Debugging TLS without hidden-API bypass on modern API levels.

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:dependencies --configuration debugCompileClasspath | rg -i 'libadb|sun-security|conscrypt'
```

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| `Unresolved reference 'sun'` / `android.sun` | Wrong package import, or dependency not on classpath |
| JitPack 404 / resolve fail | Offline / sandbox network; need `https://jitpack.io` in settings |
| Release minify crashes on pair | Missing ProGuard keep rules for libadb / Conscrypt |
