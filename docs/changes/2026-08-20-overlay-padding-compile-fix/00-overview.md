# Overlay padding compile fix

## Why

CI `Build Debug APK` (run 32382069677) failed on `:app:compileDebugKotlin` after settings/overlay UI polish. Previous commit `e8dfb52` was green.

## What

| File | Change |
|------|--------|
| `app/.../service/FloatingOverlayService.kt` | Replace invalid `Modifier.padding(horizontal=, top=, bottom=)` with `start/top/end/bottom` overload |

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| `None of the following candidates is applicable` on `padding` | Mixed named args across overloads (`horizontal` + `top`/`bottom`) |
