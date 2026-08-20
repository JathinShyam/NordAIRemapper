# 01 — README onboarding rewrite

## Why

Repo README was outdated (Wireless-first ADB-only unlock story, incomplete onboarding). Users need an honest end-to-end setup guide.

## What

| File | Change |
|------|--------|
| `README.md` | Full rewrite: problem, 6-step onboarding with why, USB-first Unlock + Wireless advanced, features, permissions, limits, build/download |

## Verify

Skim README against current `OnboardingScreen` (6 steps) and `EnableDetectionScreen` (USB preferred). Confirm latest-debug link and dual-fire caveats remain.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| README says Wireless-only | Stale copy — Unlock is USB-first as of Nord Edge |
| User stuck at READ_LOGS | Point them to Unlock section + pairing vs connection port |

