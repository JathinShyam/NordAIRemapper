# 01 — Unlock USB-first

## Why

Approved Nord Edge happy path: one USB grant. Wireless is advanced.

## What

`presentation/detection/EnableDetectionScreen.kt`

- Title **Unlock** / “One-time detection setup”
- Primary: USB command + Copy + Recheck
- Secondary expand: Wireless debugging pairing UI (unchanged logic)
- Success CTA: **Open Home**

## Verify

Open Unlock with READ_LOGS missing → USB card first. Expand “No computer” → Wireless fields appear.
