# 02 — prototype Settings hub sync

## Why

`nord-edge-prototype.html` Settings stopped at exclusions with no Power/About/GitHub and still used separate pref-cards.

## What

Same section order and grouped chrome as `settings-preview.html`; battery CTA + exclusion chips + GitHub open; accent icon colors retained; `tone-muted` fixed to neutral gray.

## Verify

Prototype → Settings → Appearance → Reliability (battery + exclusions) → Shortcuts → Tools → About.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Hub nav broken | Row missing `data-go` |
| Status chip looks like Remap filter chip | Use `.status-chip`, not `.chip` |
