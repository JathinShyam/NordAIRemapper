# Overlay padding compile fix

## Why

CI `Build Debug APK` failed on `:app:compileDebugKotlin` when overlay polish used an invalid Compose padding overload. Reintroduced again in `ac877be` (Polish overlay settings).

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
