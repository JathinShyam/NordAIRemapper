# 07 — Real launcher icons for LaunchApp everywhere

## Why

`ActionCard` and the Floating Menu tiles only accepted `ImageVector`, and
`RemapAction.icon()` maps `LaunchApp → Icons.Outlined.Apps`. So every surface
except the Visual Overlay popup showed a generic glyph after assigning an app:
Home press cards, Floating Menu settings slots, and the actual floating menu
tiles. Users read the generic icon as "the assignment didn't take".

## What

| File | Change |
|------|--------|
| `presentation/common/AppIcon.kt` (new) | `rememberAppIcon(packageName)` — launcher icon decoded on `Dispatchers.IO`, keyed by package, null while loading/unresolvable |
| `ui/components/ActionCard.kt` | + `appIcon: ImageBitmap?`; when set it replaces the vector in the 36dp slot (AnimatedContent target includes it so the swap animates) |
| `presentation/remap/RemapScreen.kt` | Current-selection pill resolves + shows the app logo (user follow-up report) |
| `presentation/home/HomeScreen.kt` | press cards resolve + pass the app icon |
| `presentation/overlay/OverlaySettingsScreen.kt` | slot cards same |
| `service/FloatingOverlayService.kt` | GridTile + PillTile render the bitmap inside the accent ring when present |

Precedent: `VisualActionPopup` already resolved real icons via
`resolveAppIcon()`; this reuses the same 96×96 decode pattern.

## Verify

1. Home: assign an app to any press → card shows that app's logo (was generic).
2. Floating Menu settings: same slot shows the logo.
3. Trigger Show floating menu → LaunchApp tiles show logos.
4. Unassign / switch to a non-app action → falls back to category glyph.
5. App uninstalled after assignment: label lookup already falls back to
   package name (`displayName`); icon silently stays generic.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Generic icon forever | package not launchable / PM throws — check logcat for PackageManager noise; `rememberAppIcon` swallows into null by design |
| Icon flickers on scroll | remember key changed — key is packageName only; ensure callers don't recreate the action object identity per frame |
