# 00 — Home icon + overlay layout + type polish

## Why

Match Nord Edge Home toolbar (single Settings control), tighten floating overlay radial/pill layout, and bump heading weight/tracking to ExtraBold.

## What

| File | Change |
|------|--------|
| `HomeScreen.kt` | Single bordered Settings action; keep other nav params for NavHost |
| `FloatingOverlayService.kt` | Radial fixed 300dp bottom sheet; pill bar edge vs bottom layouts |
| `NordHeading.kt` / `Type.kt` | ExtraBold headings + tighter letter-spacing |

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:compileDebugKotlin
```
