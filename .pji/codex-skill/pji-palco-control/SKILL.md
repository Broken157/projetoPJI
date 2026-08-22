---
name: pji-palco-control
description: Govern controlled work on the PJI Palco/projetoPJI repository when a request mentions PJI Palco, projetoPJI, RF/RNF, the pre-automation checkpoint, or pji-automation. Enforce fork-only automation state, phase gates, approvals, checkpoint immutability, testing, and reporting. Do not use for unrelated repositories.
---

# PJI Palco controlled work

Apply this workflow only to PJI Palco/projetoPJI work.

## Load authoritative operational state

Before proposing or implementing functionality, read these files from the local `pji-automation` branch without merging that branch:

- `.pji/STATE.yaml`
- `.pji/POLICY.yaml`
- `.pji/APPROVALS.yaml`
- `.pji/BLOCKERS.yaml`
- `.pji/ROADMAP.yaml`

When the current worktree does not contain them, use read-only commands such as `git show pji-automation:.pji/STATE.yaml`. Never merge `pji-automation` into a functional branch.

## Preflight

Confirm the repository root, current branch, `HEAD`, remotes, working-tree state, current phase, checkpoint SHA, open blockers, approval scope, and test baseline. Audit before changing anything and distinguish code presence from end-to-end validation.

## Enforce invariants

- Treat `checkpoint/pre-automacao-2026-08-22` as immutable: no commit, merge, rebase, reset, or force operation.
- Keep `pji-automation` exclusive to the fork and to governance/validation infrastructure. Never include its files in a PR to upstream.
- Never push directly to upstream or use force integration.
- While phase is `BASELINE_SYNC`, do not implement a new RF, create a functional PR, merge, or enable scheduled/automatic RF execution.
- Require explicit approval for every category in `.pji/POLICY.yaml`; approvals are exact-scope and non-reusable unless the record explicitly says otherwise.
- Do not change database/schema/migrations for RF24 or mark RF24 complete while `RF24-SCHEMA-001` is blocked.
- Do not fix `SECURITY-JWT-001` during bootstrap; a significant JWT change requires explicit authorization.
- Do not report frontend tests as passed while `FRONTEND-TEST-001` is open.

## Before any commit or PR-ready claim

Run the repository guard and validator from `pji-automation`. Preserve failures in reports. Confirm that only the intended fork branch is pushed. A `PR_READY` claim requires every blocking gate in `.pji/BLOCKERS.yaml` to be closed with evidence.

Stop and request direction when an approval is missing, a blocker prevents the requested transition, evidence is unavailable, or the action would mutate upstream/checkpoint/history.
