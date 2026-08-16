# 02 — AGENTS.md and project rule hooks

## Why

Convention only works if agents load it every session.

## What

| File | Change |
|------|--------|
| `AGENTS.md` | After product-docs sentence: require `docs/changes/YYYY-MM-DD-<slug>/` notes + index update in the same turn as code |
| `.cursor/rules/project.mdc` | Same requirement (always-applied rule) |

## Verify

Grep for `docs/changes` in both files; new agents should see it at session start.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Future PRs lack change notes | Rule ignored or scoped as “docs unless asked” without noticing user standing request |
