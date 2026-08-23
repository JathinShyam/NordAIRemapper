# Change logs

Per-change notes for debugging and future agents. Product/architecture truth still lives in `docs/PRD.md`, `docs/TRD.md`, `docs/ARCHITECTURE.md`.

## Convention

1. Every non-trivial change set gets a folder: `docs/changes/YYYY-MM-DD-<short-slug>/`.
2. Inside that folder, **one markdown file per logical change** (deps, new class, screen wire-up, etc.) — not one giant dump.
3. Prefer also a **`FILE_MANIFEST.md`** listing every touched path → doc mapping.
4. Each topic file should include:
   - **Why** (problem / constraint)
   - **What** (files + behavior)
   - **How to verify**
   - **Debug tips** (symptoms → likely cause)
5. Add a row to the index table below when creating a new folder.
6. If architecture packages change, update `docs/ARCHITECTURE.md` package tree and link the change folder.
7. Do **not** edit Cursor plan files under `~/.cursor/plans/`; keep durable notes here.

Going forward: write/update these notes in the **same turn** as the code.

## Topic file template

```markdown
# NN — short title

## Why
…

## What
| File | Change |
|------|--------|
| `path` | … |

## Verify
…

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| … | … |
```

## Completeness checklist (before calling a change set “documented”)

- [ ] Folder + `00-overview.md` with file index
- [ ] `FILE_MANIFEST.md` covers every new/modified path from `git status`
- [ ] Index row in this README
- [ ] ARCHITECTURE package tree updated if packages/routes added
- [ ] Skills updated if operator workflow changed (e.g. device-testing)
- [ ] ProGuard/privacy/limits noted when new native/crypto/network code lands

## Index

| Date | Folder | Summary |
|------|--------|---------|
| 2026-08-16 | [in-app-read-logs-grant](./2026-08-16-in-app-read-logs-grant/) | Phone-only Wireless ADB path to grant `READ_LOGS` (no Shizuku / no laptop) |
| 2026-08-16 | [change-log-convention](./2026-08-16-change-log-convention/) | Introduced `docs/changes/` per-change notes + AGENTS/project rule hooks |
| 2026-08-20 | [home-silhouette-size](./2026-08-20-home-silhouette-size/) | Shrink Home Nord 5 silhouette to fit the frame |
| 2026-08-20 | [nord-edge-proposal](../design/2026-08-20-nord-edge-proposal.md) | Nord Edge Direction A — approved; Compose implementing |
| 2026-08-20 | [nord-edge-prototype](./2026-08-20-nord-edge-prototype/) | Interactive HTML prototype for Nord Edge Direction A (+ Nord 5 silhouette / Plus Key glow) |
| 2026-08-20 | [nord-edge-impl](./2026-08-20-nord-edge-impl/) | Nord Edge Compose: Unlock USB-first, Home/Settings/Lab, Remap studio, overlay chord |
| 2026-08-20 | [feature-wiring](./2026-08-20-feature-wiring/) | Wire Visual Overlay popup, floating overlay config, haptics preview, service notif |
| 2026-08-20 | [readme](./2026-08-20-readme/) | Full README: onboarding, Unlock why, permissions, limits |
| 2026-08-20 | [overlay-padding-compile-fix](./2026-08-20-overlay-padding-compile-fix/) | Fix invalid `Modifier.padding(horizontal, top, bottom)` breaking CI assembleDebug |
| 2026-08-20 | [home-overlay-type-polish](./2026-08-20-home-overlay-type-polish/) | Home Settings icon, overlay radial/pill layout, ExtraBold headings |
| 2026-08-20 | [ux-review-polish](./2026-08-20-ux-review-polish/) | Critical UX review: Home/Remap scan, status ribbon, Material hub icons, motion/empty states |
| 2026-08-20 | [overlay-never-settle-positions](./2026-08-20-overlay-never-settle-positions/) | Never Settle on silhouette; Top/Middle/Bottom overlay; pill bar align; slot picker accents |
| 2026-08-20 | [ui-critique-polish](./2026-08-20-ui-critique-polish/) | Second UI critique: category accents, hub colors, onboarding motion, haptics, ADB copy block |
| 2026-08-20 | [design-sync-never-settle](./2026-08-20-design-sync-never-settle/) | Prototype sync + exact Never Settle lockup (USPTO motto + authentic 1+ mark) |
| 2026-08-21 | [critique-v2](./2026-08-21-critique-v2/) | UI critique v2: badges, snackbar, live key pulse, haptics, GRID rename, prototype sync |
| 2026-08-21 | [lean-launcher-icon](./2026-08-21-lean-launcher-icon/) | Replace fat filled phone launcher with lean cyan outline + Plus Key |
| 2026-08-21 | [vertical-pill-bar](./2026-08-21-vertical-pill-bar/) | Pill = vertical Left/Right strip; Grid keeps Top/Middle/Bottom |
| 2026-08-21 | [pill-bottom-vibrant-lockup](./2026-08-21-pill-bottom-vibrant-lockup/) | Pill Bottom scroll row; vector lockup; real Bold top titles |
| 2026-08-22 | [audit-fixes](./2026-08-22-audit-fixes/) | Full audit: logcat watcher leak/reconnect, boot+FGS guards, tests+CI gate, light-theme accents, picker perf, backup confirmations, overlay app/URL slots, last-seen health line |
| 2026-08-22 | [home-silhouette-compact](./2026-08-22-home-silhouette-compact/) | Shrink Home silhouette to design ~220dp so action cards fit without scroll |
| 2026-08-22 | [launcher-n-monogram](./2026-08-22-launcher-n-monogram/) | App logo = cyan N monogram + Torch Red Plus Key dash (option 3) |
| 2026-08-22 | [heading-weight-lockup-center](./2026-08-22-heading-weight-lockup-center/) | RCA: variable Space Grotesk needs FontVariation w700; center Never Settle lockup |
| 2026-08-22 | [rename-keyforge](./2026-08-22-rename-keyforge/) | Display name Nord AI Remapper → Keyforge (package unchanged) |
| 2026-08-22 | [settings-hub-redesign-preview](./2026-08-22-settings-hub-redesign-preview/) | Design-first Settings hub: grouped surfaces, battery/exclusions chips, About+GitHub (HTML only) |
| 2026-08-22 | [settings-hub-compose](./2026-08-22-settings-hub-compose/) | Compose port of Settings hub from `settings-preview.html` (+ exclusions detail page) |
| 2026-08-23 | [exclusions-app-icons](./2026-08-23-exclusions-app-icons/) | Per-app exclusions list shows app logos; Version bold; design parity |
| 2026-08-23 | [exclusions-accessibility-limit](./2026-08-23-exclusions-accessibility-limit/) | Banking Accessibility: Auto-Pause + one-time Unlock for hands-free resume |
| 2026-08-23 | [readlogs-logd-blind](./2026-08-23-readlogs-logd-blind/) | RCA: READ_LOGS granted but logd serves only self logs → blind watcher; blindness watchdog + alert |
| 2026-08-23 | [change-safety](./2026-08-23-change-safety/) | Mandatory regression review protocol + `scripts/device-smoke.sh` post-install gate |
