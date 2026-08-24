# 2026-08-24 — User-POV audit fixes

Full audit (technical + UI/UX, user point of view) of onboarding, detection,
key learning, gestures/actions, exclusions, overlay, backup, reliability, and
health visibility. This folder documents the fixes; findings that were NOT
fixed (deliberate scope) are listed at the end.

## Topic files

| # | File | Fix |
|---|------|-----|
| 01 | [01-false-stop-alarm.md](./01-false-stop-alarm.md) | Master-toggle-off no longer posts "Key detection stopped" |
| 02 | [02-home-detection-health.md](./02-home-detection-health.md) | Honest ribbon liveness, banner order, notifications-off signal, ticking timestamps |
| 03 | [03-learning-mode-gate.md](./03-learning-mode-gate.md) | Key setup suspends engine dispatch/consume |
| 04 | [04-action-failure-honesty.md](./04-action-failure-honesty.md) | Failed system actions render "Failed", haptic gating, overlay-grant hint |
| 05 | [05-light-theme-window.md](./05-light-theme-window.md) | Light theme no longer flashes black / hides status icons |
| 06 | [06-compose-a11y-polish.md](./06-compose-a11y-polish.md) | Saveable sheets/dialogs, 48dp targets, semantics, empty states, URL sheet, IO off main |
| 07 | [07-launchapp-icons.md](./07-launchapp-icons.md) | Real launcher logos on Home cards, Floating Menu slots + tiles |
| 08 | [08-unlock-methods-shizuku.md](./08-unlock-methods-shizuku.md) | Unlock screen: Built-In / Shizuku / Manual ADB method cards; Shizuku tap-to-grant via user service |
| 09 | [09-pairing-notification-reply.md](./09-pairing-notification-reply.md) | Built-In pairing: WhatsApp-style direct-reply notification — enter the 6-digit code without leaving the system dialog |

## Known gaps (audit found, not fixed here)

- Strings remain hardcoded English (i18n out of scope).
- No `@Preview` composables yet.
- Accent/destructive color pairs still duplicated across Settings vs ActionCategoryAccent.
- `AccessibilityAutoResumeService` polls UsageStats at 750ms for up to 6h while
  paused (FGS cost accepted for correctness).
- Boot-time `notifyOpenAfterBoot` fires on every boot with READ_LOGS granted;
  copy softened, behavior kept (matches OxygenOS per-boot consent reality).

## Verify (all changes)

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug :app:testDebugUnitTest   # green
scripts/device-smoke.sh                               # after install, all PASS
```

Manual: toggle master off (no alarm notification), reboot (nudge appears once,
opens app → heals), open Key setup and press key (no action fires), revoke
notifications → Home shows failure-alerts warning, Light theme cold start.
