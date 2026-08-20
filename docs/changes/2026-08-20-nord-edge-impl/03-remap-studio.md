# 03 — Remap studio

## Why

Prototype Remap: category chips, icon rows, Short titles, fuzzy/alias search.

## What

`presentation/remap/RemapScreen.kt`

- Titles Single / Double / Long + “Assign an action”
- Horizontal category chips (Apps…None)
- Action rows with cyan icon badges + selection border
- Search aliases (torch, dnd, mute, …) + light fuzzy subsequence
- Try now / Done unchanged

## Verify

Open Remap for Double → title Double. Search “torch” → Flashlight. Chip System filters when search empty.
