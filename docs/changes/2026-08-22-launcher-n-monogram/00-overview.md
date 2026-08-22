# Launcher icon — N monogram (option 3)

## Why

User chose `design/launcher-options/opt3-vector.svg` as the app logo: cyan N + Torch Red Plus Key dash.

## What

| File | Change |
|------|--------|
| `drawable/ic_launcher_foreground.xml` | N monogram + soft ring + red key dash |
| `drawable/ic_launcher_monochrome.xml` | White themed-icon variant |
| `design/launcher-preview.svg` | Synced to option 3 |

`mipmap-anydpi-v26/ic_launcher.xml` already points at foreground/monochrome — no change needed.
Notification small icons keep using `ic_launcher_foreground`.
Candidate opt1/opt2/opt3 drawables and `design/launcher-options/` were removed after selection.

## Verify

1. Reinstall debug APK (launcher often caches icons).
2. Home: cyan N on `#0A0A0A`, red dash on the left.
3. Themed icons / monochrome: white N + dash.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Old phone outline still shows | Launcher cache — uninstall/reinstall |
| Red dash missing in themed icon | Expected — mono is single-color white |
