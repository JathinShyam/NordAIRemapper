# 06 — Compose UX/a11y polish batch

## Why

Audit found rotation-destroyed sheets/dialogs (including destructive-confirm
dialogs in Backup), sub-48dp touch targets, switch/selectable rows invisible to
TalkBack, missing empty states, a URL sheet that could save a prefilled
`"https://"`, and PackageManager binder calls on the main thread.

## What

| File | Change |
|------|--------|
| `presentation/remap/RemapScreen.kt` | sheet/query/filter/tryNowLoading → `rememberSaveable`; "Try now" disabled when action is `None`; "No actions match …" empty state; pill ellipsis; category chips + action rows `selectable`; search focus border uses `colorScheme.primary` |
| `presentation/backup/BackupScreen.kt` | snapshot name, expand flag, all three confirm-dialog ids → `rememberSaveable`; snapshots empty-state line; export prefix → `keyforge-*`; expand row gets onClickLabel |
| `presentation/overlay/OverlaySettingsScreen.kt` | slot/sheet indices → `rememberSaveable`; enable row `toggleable`; SegRow fills width, SegButtons equal-weight with single-line labels; opacity slider `stateDescription` |
| `presentation/settings/ExclusionsScreen.kt` | app query + label lookups on `Dispatchers.IO`; load failure distinguished from empty (`errorMessage` into picker); emptiness keyed off the setting, not async rows; Auto-Pause row `toggleable` |
| `presentation/remap/AppPickerSheet.kt` | + optional `errorMessage`; URL sheet: placeholder instead of prefilled value, Save disabled when blank, IME Done saves, shared `NordPrimaryButton` |
| `presentation/settings/SettingsUi.kt` | `SettingsToggleRow`, `SettingsChoiceRow`, hub toggle row: proper `toggleable`/`selectable` semantics, switch non-interactive to avoid double events; theme segments use scheme primary (not hardcoded NordBlue), taller targets |
| `presentation/settings/LockScreenSettingsScreen.kt` | three rows `toggleable`; titles "Single press / Double press / Long press" (consistent casing) |
| `presentation/settings/FeedbackScreen.kt` | haptic switch row `toggleable`; intensity segments equal-width |
| `presentation/settings/VisualOverlayScreen.kt` | accent swatches: 48dp hit area, `selectable` + check icon on selected |
| `ui/components/NordCards.kt` | Primary/Ghost buttons `defaultMinSize(minHeight = 48.dp)` — labels no longer clip at fontScale ≥1.3 |
| `ui/components/NordHeading.kt` | heading overflow ellipsis; top-bar subtitle maxLines+ellipsis |
| `ui/components/StatusChip.kt` | interactive chips expose selected state via `selectable(Role.RadioButton)` |
| `presentation/developer/KeyLearningScreen.kt` | "Listening…" empty state; shared time formatter; ticking last-seen |
| `presentation/onboarding/OnboardingScreen.kt` | ~~step content scrolls~~ **reverted same day**: scroll wrapper broke the centered layout (content jumped to top); original `weight(1f)` placement restored |

## Verify

1. Rotate on Remap (sheet open), Overlay (slot editor open), Backup (confirm
   dialog visible): state survives every time.
2. TalkBack: master switch, lock-screen rows, theme/theme-mode segments,
   strategy chips announce state ("on/off", "selected").
3. Set font scale 1.5: NordPrimaryButton labels don't clip; overlay segments
   stay one line.
4. URL flow: fresh sheet is empty; Save disabled until text entered;
   keyboard Done saves.
5. Exclusions with many apps scrolls without jank.
6. Onboarding pages keep the original centered layout (scroll experiment
   reverted — see file table).

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Sheet reopens unexpectedly after process death | rememberSaveable restored stale enum — acceptable; clear on dismiss already handled by `= None` writes |
| Switch double-toggles | a row kept both `toggleable` and interactive `Switch(onCheckedChange)` |
| Picker shows error though apps loaded | `loadFailed` not reset before retry — reset happens at each `openAppPicker()` |
