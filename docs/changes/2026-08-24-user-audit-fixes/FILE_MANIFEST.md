# FILE_MANIFEST — 2026-08-24-user-audit-fixes

| Path | Change | Doc |
|------|--------|-----|
| `service/LearningMode.kt` (new) | learning-mode flag | 03 |
| `service/LogcatWatcherService.kt` | suppress death alarm on deliberate stop | 01 |
| `service/DetectionCoordinator.kt` | set suppression only for master-off stops | 01 |
| `service/RemapEngine.kt` | learning gate on consume + dispatch | 03 |
| `service/ServiceNotifications.kt` | boot-nudge copy softened | 02 (audit) |
| `service/RemapActionExecutor.kt` | failure-aware feedback, haptic gating, overlay hint | 04 |
| `domain/model/ActionFeedback.kt` | ACTION_FAILED state key | 04 |
| `presentation/common/RemapActionUi.kt` | render ACTION_FAILED icon/caption | 04 |
| `presentation/common/Ticker.kt` (new) | rememberNowTicker | 02, 06 |
| `presentation/home/HomeUiState.kt` | liveness/notification/tick fields | 02 |
| `presentation/home/HomeViewModel.kt` | runtime flags + ticker + banner order + notification deep link | 02 |
| `presentation/home/HomeScreen.kt` | live ribbon, alerts-off row, toggleable switch row, 48dp icon | 02, 06 |
| `presentation/developer/KeyLearningScreen.kt` | empty state, shared formatter, ticking timestamp | 06 |
| `presentation/developer/KeyLearningViewModel.kt` | learning mode lifecycle; dead code removed | 03 |
| `presentation/remap/RemapScreen.kt` | saveable state, Try-now guard, empty search, semantics | 06 |
| `presentation/remap/AppPickerSheet.kt` | errorMessage param; URL sheet validation | 06 |
| `presentation/backup/BackupScreen.kt` | saveable confirms, empty state, keyforge prefix | 06 |
| `presentation/overlay/OverlaySettingsScreen.kt` | saveable sheets, toggleable row, SegRow weights, slider semantics | 06 |
| `presentation/settings/ExclusionsScreen.kt` | IO lookups, error vs empty, toggleable row | 06 |
| `presentation/settings/SettingsUi.kt` | toggle/select semantics, theme segment tokens | 06 |
| `presentation/settings/LockScreenSettingsScreen.kt` | toggleable rows, consistent press-type titles | 06 |
| `presentation/settings/FeedbackScreen.kt` | toggleable row, equal-width segments | 06 |
| `presentation/settings/VisualOverlayScreen.kt` | 48dp selectable swatches | 06 |
| `presentation/onboarding/OnboardingScreen.kt` | scrollable step content | 06 |
| `ui/components/NordCards.kt` | min-height buttons | 06 |
| `ui/components/NordHeading.kt` | ellipsis overflow | 06 |
| `ui/components/StatusChip.kt` | selectable semantics | 06 |
| `res/values/colors.xml`, `res/values-night/*`, `res/values/themes.xml` | light/dark window + bars | 05 |
| `presentation/MainActivity.kt` | reactive edge-to-edge bar styles | 05 |
