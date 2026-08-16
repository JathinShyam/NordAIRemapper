# 09 — ProGuard / release

## Why

Release builds minify. libadb + Conscrypt + cert classes must survive R8 or pairing/grant crashes only in release.

## What

`app/proguard-rules.pro` additions:

```
-keep class io.github.muntashirakon.adb.** { *; }
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**
-dontwarn sun.security.**
-dontwarn android.sun.security.**
-keep class sun.security.** { *; }
-keep class android.sun.security.** { *; }
```

**Important:** `sun-security-android` ships classes under `android.sun.security.*` (not `sun.security.*`). Both keeps are required if anything still references either package.

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleRelease
```

Then smoke the Enable detection pair flow on a release (or minify-enabled) build.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Pair works in debug, ClassNotFound / AbstractMethodError in release | Missing keep for libadb / Conscrypt / `android.sun.security` |
| Conscrypt warnings during minify | Expected without `-dontwarn`; should not fail keep |
