# 01 — Interactive Nord Edge prototype

## Why

User asked for design animations / Figma-like interaction to feel the redesign before approving Compose work.

## What

| Path | Role |
|------|------|
| `design/nord-edge-prototype.html` | Clickable multi-screen end-to-end prototype with CSS spring motion |
| `docs/design/2026-08-20-nord-edge-proposal.md` | Linked open instructions + full screen list |

Screens (initial): Stage (Home), Unlock, Remap studio, Overlay chord. Play controls: Simulate Plus Key / Double / Long, Reset Unlock.

### Screen inventory (end-to-end expansion)

| Screen id | Nav group | Purpose |
|-----------|-----------|---------|
| `onboarding` | Flow | Welcome + continue → Unlock (2-step carousel) |
| `unlock` | Flow | USB-first detection timeline |
| `stage` | Stage | Home silhouette, pads, master toggle; gear → settings |
| `remap` | Stage | Remap studio — categories + actions from `RemapActionCatalog` (Apps / Media / System / Overlay / None); stacked rows; per-action cyan outline SVGs; back → stage |
| `overlay` | Stage | Live chord overlay (long sim / preview) |
| `settings` | Settings | Hub list; rows animate in; navigate to prefs |
| `feedback` | Settings | Haptic intensity + toggle + pulse preview |
| `preferences` | Settings | Theme chips, dynamic color, service notification |
| `visual` | Settings | Visual overlay toggle + bar morph |
| `lock` | Settings | Single / double / long on lock screen |
| `backup` | Settings | Export / import + success toast |
| `overlay-settings` | Settings | Edit 6 slots; preview → overlay |
| `keysetup` | Settings | Listening pulse + fake logcat + Nord 5 tip |
| `lab` | Lab | Strategy, timing sliders, USB copy, link Unlock |

## Verify

Open `design/nord-edge-prototype.html` in a browser; click nav + Simulate Plus Key; confirm edge ripple + pad morph.

Walk Remap: search “torch”, “dnd”, “vol”, “flsh” — fuzzy/alias hits across categories; clear restores chips.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Fonts look system-default | Offline / Google Fonts blocked — layout still works |
| Want real Figma | Export after Direction A lock-in; HTML is the interim interactive spec |
| Left nav clipped | Panel is sticky + scrollable — scroll the nav column |
