# Lean app launcher icon

## Why

Previous adaptive foreground was a fat filled phone body — user found it too heavy.

## What

| File | Change |
|------|--------|
| `drawable/ic_launcher_foreground.xml` | Thin cyan phone outline + slim Plus Key + remap arcs |
| `drawable/ic_launcher_monochrome.xml` | White outline variant for themed icons |
| `mipmap-anydpi-v26/ic_launcher.xml` | Point monochrome at new drawable |

## Verify

1. Uninstall/reinstall or clear launcher cache so the icon refreshes.
2. Home screen: lean cyan outline on `#0A0A0A`, Plus Key visible on the left edge.
3. Notification small icon still uses foreground (stroke reads OK on status bar).

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Old fat icon still shows | Launcher cache — reinstall APK |
| Glyph clipped | Adaptive safe zone; keep paths near center of 108vp |
