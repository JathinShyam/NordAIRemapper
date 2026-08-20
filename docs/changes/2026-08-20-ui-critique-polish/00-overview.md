# UI critique polish — overview

Second-pass UI critique fixes: category accents on Home action cards, Settings hub color coding, onboarding motion + PhoneDiagram welcome, haptics on toggles, and small readability tweaks.

## Files

| Path | Role |
|------|------|
| `01-action-cards-home.md` | ActionCard accents + badge overlay; Home category tints, master switch elevation, diagram tap pulse |
| `02-settings-hub-exclusions.md` | HubRow accents, chevron icon, exclusions labels, theme copy, toggle haptics |
| `03-remap-onboarding-unlock.md` | Remap snackbar + category chip accents; onboarding AnimatedContent; Unlock ADB code block |
| `04-shared-components.md` | SectionLabel readability, NordPrimaryButton loading |
| `FILE_MANIFEST.md` | Path → doc map |

## Verify

1. Home: action cards show category-colored icon boxes with 1×/2×/⏳ badge overlay; master switch card has subtle elevation + primary border when on; tap phone diagram for brief pulse.
2. Settings: each hub row has distinct accent; exclusions show app label + package; theme subtitle updated; toggles haptic on change.
3. Remap: category chips tint by category when selected; Try now shows "Trying action…" snackbar.
4. Onboarding: page 0 shows PhoneDiagram; pages slide/fade; status chips use explicit tone from permission booleans.
5. Unlock: ADB command in monospace card; tap copies with "Tap to copy" hint.
6. `./gradlew :app:compileDebugKotlin` succeeds.
