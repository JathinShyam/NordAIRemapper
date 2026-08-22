# 09 — Detection health: last-seen line

## Why
Most support complaints are "it stopped working"; there was no signal for when
detection last actually saw the Plus Key.

## What
| File | Change |
|------|--------|
| `domain/model/AppSettings.kt`, `domain/repository/SettingsRepository.kt`, `data/datastore/SettingsRepositoryImpl.kt` | `lastPlusKeySeenAtMs` persisted |
| `service/RemapEngine.kt` | Writes timestamp on each classified gesture, throttled to 1 write/s (a DataStore write per press would be wasteful; strategy/enabled unchanged so no watcher sync churn) |
| `presentation/common/RelativeLastSeen.kt` (new) | Shared formatter (never / just now / Ns/Nm/Nh/Nd ago) |
| Home master card + Key setup status card | Show the line |

## Verify
1. Press Plus Key → Home line flips to "just now", then ages correctly.
2. Fresh install shows "never".

## Debug tips
| Symptom | Likely cause |
|---|---|
| Line stays "never" while pressing | Detection path broken upstream: check banner (READ_LOGS), watcher process alive, pattern matches current firmware lines |
