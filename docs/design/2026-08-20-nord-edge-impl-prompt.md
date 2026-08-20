# Agent prompt — implement Nord Edge (approved)

Copy everything below the line into a **new Cursor agent** chat.

---

## Mission

Implement the approved **Nord Edge (Direction A — Edge Orchestra)** redesign in the Nord AI Remapper Android app **end-to-end**, matching the interactive prototype **exactly** in structure, copy, interaction model, and motion intent.

**This is not a vibe redesign.** Treat the HTML prototype as the visual/IA source of truth. Do not invent alternate layouts, categories, or copy.

## Source of truth (read these first, in order)

1. `AGENTS.md` + `.cursor/skills/nord-compose-ui/SKILL.md` (and RoninForge Compose rules it loads)
2. **Approved prototype:** `design/nord-edge-prototype.html` — open in a browser and walk every screen before coding
3. Design notes:
   - `docs/design/2026-08-20-nord-edge-proposal.md` (Direction A)
   - `docs/changes/2026-08-20-nord-edge-prototype/` (especially `02-remap-catalog-sync.md`, `03-nord5-silhouette-plus-key.md`, `04-onboarding-full-steps.md`, `05-prototype-audit-ux.md`)
4. Product architecture (do not violate): `docs/PRD.md`, `docs/TRD.md`, `docs/ARCHITECTURE.md`
5. Existing presentation code under `app/src/main/java/com/nordairemapper/presentation/` and `ui/`

**Status:** Design is **user-approved**. Implement Compose. Update the proposal doc status from “awaiting approval” to “approved — implementing” when you start.

## Hard constraints (never break)

- Package `com.nordairemapper`; layers `presentation → domain ← data`
- Do **not** change Plus Key detection / `RemapEngine` / logcat / accessibility behavior except UI wiring that already exists
- Never hard-code Plus Key identity; logcat first-class; no `matchesPlusKey` on LOGCAT events
- No OnePlus Sans font commits; keep `FontFamily.Default`
- No analytics / ads / accounts / cloud
- Nord palette wins: OLED `#0A0A0A`, surface `#141414`, accent `#0AC6FF`, heading red `#EB0028`
- Document non-trivial work under `docs/changes/YYYY-MM-DD-nord-edge-impl/` (one file per logical change) + index `docs/changes/README.md`
- Keep diffs scoped; no drive-by refactors outside Nord Edge UI
- Do **not** commit or push unless the user asks

## Design language (must match prototype)

| Token | Value |
|-------|--------|
| Background | `#0A0A0A` |
| Surface | `#141414` / `#1A1A1A` |
| Outline | `#2A2A2A` |
| Muted text | `#8A8A8A` |
| Text | `#F5F5F5` |
| Accent (cyan) | `#0AC6FF` |
| Accent dim | `rgba(10,198,255,0.14)` |
| Brand red (first letter of titles) | `#EB0028` |
| OK / warn | `#2ECC71` / `#FFB020` |

- **Typography:** Material 3 + existing Nord heading helpers; first letter of screen titles uses brand red (like prototype `.h::first-letter`)
- **Motion:** purposeful springs only — edge ripple on press, pad morph, staggered overlay tiles, timeline bloom on Unlock, screen enter. No decorative spam, purple glow, or generic AI chrome
- **Naming (user-facing):** **Home** (not “Stage”), **Plus Key** (not “AI key”), **Lab** for advanced (was Developer — rename UI strings; keep nav routes working), **Overlay settings**, **Backup & Restore**, **Visual Overlay**, **Lock Screen**
- Prototype playbar / left review nav is **not** part of the app — do not ship it

## Screen-by-screen requirements

Implement / rebuild each screen to match the prototype + real app data. Preserve ViewModels and repositories unless a UI state field is required.

### 1. Onboarding (6 steps — match `OnboardingScreen` + prototype)

1. Welcome — Nord AI Remapper  
2. Accessibility  
3. Enable Plus Key detection (USB-first happy path per prototype; keep Wireless as advanced/secondary — align with Unlock)  
4. Display over apps  
5. Keep it alive (notifications + battery)  
6. You’re all set → **Home**

Status chips, primary / secondary / skip actions, themed outline icons. Copy must match app + prototype audit (no inventing steps).

### 2. Unlock / Enable detection (USB-first)

- Timeline: Accessibility → Detection unlock (USB `pm grant READ_LOGS`) → Ready for Home  
- Happy path: USB command + Recheck  
- “No computer” = advanced Wireless path (existing Enable Detection), not the primary story  
- Do not put pairing ports on the happy path  
- Final CTA: **Open Home**

### 3. Home (was Stage)

- Compact **Nord 5** silhouette at exact GSMArena ratio **77 / 163.4** mm (`PhoneDiagram`)
- Layout: left short Plus Key; right volume + power; centered punch-hole; thin bezels; slightly-rounded frame
- **Remapping ON** → Plus Key cyan + soft pulse; **OFF** → key matches other buttons (normal device). Driven by remapping master switch (`serviceEnabled`)
- Status chip vocabulary aligned with app (e.g. Service active / Remapping paused / Accessibility off)
- Master switch + subtitle “Master switch for Plus Key actions”
- Section **Actions** with three pads: Single / Double / Long — show assigned action labels from config
- Default labels in prototype: Flashlight / Rear camera / Show overlay — use real saved configs when present
- Tap pad → Remap for that press type
- Edge ripple when a gesture fires (or preview) if feasible without hacking detection
- Settings entry (and keep useful shortcuts if already on Home toolbar)

### 4. Remap

- Title = press type only (**Single** / **Double** / **Long**); subtitle “Assign an action”
- Search: “Search actions or apps” with partial + alias/fuzzy filter across all categories when querying
- Category chips from `RemapActionCatalog`: **Apps · Media · System · Overlay · None** — miss none
- Actions = exact catalog list + `RemapActionUi` names/descriptions/icons — no invented actions
- Stacked rows (not 2-column tiles); cyan outline icons per action (Material icons already in `RemapActionUi` — keep them consistent, polish if needed)
- Selected row: cyan emphasis
- Bottom: **Try now** + **Done** (match app RemapScreen behavior)
- Saving must update the **opened** press type only

### 5. Overlay (live floating menu)

- Chord / staggered appearance for tiles when shown
- Same action icons as Remap for slot actions
- Dismissible (system back / tap outside as appropriate for overlay service UI)
- Wire to existing `FloatingOverlayService` / overlay config — do not rebuild detection

### 6. Settings hub

Sections matching prototype audit:

- **Appearance:** Theme, Dynamic color, Service notification (these live on Settings in the app — keep that, style like prototype cards)
- **Feedback & overlay:** Feedback, Preferences, Visual Overlay, Overlay settings  
- **Behavior:** Lock Screen, Backup & Restore  
- **Advanced:** Key setup, Lab, Restart onboarding (if present)

Row titles/subtitles must match content (Preferences ≠ theme).

### 7. Feedback

- Toggle: Haptic feedback  
- Vibration intensity: **Light / Medium / Heavy** (not Strong; no Off in intensity segment)  
- Optional preview pulse if cheap

### 8. Preferences

Exact app meaning:

- Intro: how you’re notified when an action triggers  
- Toggles: Haptic feedback, Visual overlay  
- Overlay style: OnePlus / Stock  
- CTA: Customize visual overlay → Visual Overlay screen  

**Do not** put Theme here.

### 9. Visual Overlay

- Title **Visual Overlay**  
- Enable toggle + existing style/accent/glow controls from `VisualOverlayScreen` — polish to Nord Edge; keep real controls, don’t strip to a fake bar only

### 10. Lock Screen

- Title **Lock Screen**  
- Single / Double / Long Press with Enabled/Disabled when locked subtitles

### 11. Backup & Restore

- Title **Backup & Restore**  
- Export / Import (+ existing snapshot UI if already present)

### 12. Overlay settings

- Title **Overlay** / settings for floating menu  
- Slots with Remap-consistent icons/labels  
- Keep layout/position/opacity/size/animation if already in app — style to Nord Edge  
- Preview → live overlay

### 13. Key setup

- Listening UI; honest Nord 5 tip (Plus Key rarely reaches Accessibility as KeyEvent)  
- Prefer “Plus Key” wording over “AI key”

### 14. Lab (Developer)

- Rename user-visible **Developer → Lab** where appropriate  
- Strategy: Auto / **Accessibility** / Logcat (spell out Accessibility)  
- Timing sliders: double-press window / long-press threshold  
- USB unlock command + link to Unlock  
- Do not remove real advanced tools (pattern, identity, READ_LOGS status) — organize under Lab chrome

## Implementation phases (execute in order; ship docs each phase)

### Phase 1 — Foundation
Theme tokens, `NordHeading` red first-letter, Home silhouette + remapping glow, Settings hub IA/copy, Unlock USB-first framing.

### Phase 2 — Remap + Onboarding
Remap studio search/categories/rows/footer; Onboarding 6-step polish; Home pads wiring.

### Phase 3 — Overlay + Lab + polish
Overlay chord motion + slot icons; Lab rename/cleanup; Visual/Preferences/Feedback/Lock/Backup polish; motion pass; build `:app:assembleDebug`.

## Acceptance criteria

- [ ] Browser prototype and app screens match in IA, titles, and primary interactions  
- [ ] Remap catalog complete (23 actions / 5 categories) — zero invented actions  
- [ ] Home silhouette uses 77∶163.4; Plus Key cyan only when remapping on  
- [ ] USB-first Unlock; Wireless secondary  
- [ ] Preferences ≠ Appearance; Feedback uses Heavy  
- [ ] Detection pipeline unchanged (no regressions in RemapEngine/logcat/a11y)  
- [ ] `./gradlew :app:assembleDebug` succeeds (`JAVA_HOME` per AGENTS.md)  
- [ ] `docs/changes/` written for the implementation  

## Out of scope

- Figma export  
- Shipping web fonts into the APK  
- Rewriting detection strategies  
- Adding analytics  
- Prototype-only play controls / review chrome  

## Working style

- Read `nord-compose-ui` skill before editing Compose  
- Prefer shared components under `ui/components/` and theme under `ui/theme/`  
- One ViewModel per screen; `StateFlow` + `collectAsStateWithLifecycle`  
- When unsure, **open the HTML prototype** and mirror it — do not guess  

## First actions for the agent

1. Open and click through `design/nord-edge-prototype.html`  
2. Skim `docs/changes/2026-08-20-nord-edge-prototype/05-prototype-audit-ux.md`  
3. Mark proposal approved in `docs/design/2026-08-20-nord-edge-proposal.md`  
4. Start Phase 1 and report a short plan before large edits  

---

End of agent prompt.
