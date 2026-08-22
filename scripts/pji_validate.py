#!/usr/bin/env python3
"""Validate PJI Palco bootstrap state, policy, Git invariants, and diff scope."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

from pji_guard import run_self_test


ROOT = Path(__file__).resolve().parents[1]
PJI_DIR = ROOT / ".pji"
YAML_FILES = (
    "STATE.yaml",
    "ROADMAP.yaml",
    "POLICY.yaml",
    "APPROVALS.yaml",
    "BLOCKERS.yaml",
)
CHECKPOINT = "fe4a18a638a47cdc50acb5680c999280f8c26545"
INTEGRATION_SHA = "d351ff909266884446f78eac32364816c2b1e4b6"
BASELINE_SHA = "858088939ba4cab7d59bdad484ade9688fae114e"
MANDATORY_BLOCKERS = {
    "SECURITY-JWT-001": ("SECURITY_REGRESSION", "CLOSED"),
    "FRONTEND-TEST-001": ("TEST_GAP", "OPEN"),
    "RF24-SCHEMA-001": ("FUNCTIONAL_BLOCKER", "BLOCKED_BY_DATABASE"),
    "BASELINE-SYNC-001": ("INTEGRATION_PENDING", "OPEN"),
}
ALLOWED_DIFF_PREFIXES = (
    ".pji/",
    "docs/AUTOMATION_",
    "scripts/pji_",
    "RELATORIO_BOOTSTRAP_AUTOMACAO_FASE1.md",
    "RELATORIO_AUTOMACAO_FASE2_BASELINE.md",
    "RELATORIO_AUTOMACAO_FASE2B_FRONTEND.md",
)
SKILL_PATH = PJI_DIR / "codex-skill" / "pji-palco-control" / "SKILL.md"


def git(*args: str, check: bool = True) -> str:
    completed = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=check,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return completed.stdout.strip()


def load_json_yaml(filename: str) -> dict:
    with (PJI_DIR / filename).open("r", encoding="utf-8") as handle:
        return json.load(handle)


def changed_paths() -> set[str]:
    tracked = set(filter(None, git("diff", "--name-only", CHECKPOINT).splitlines()))
    untracked = set(filter(None, git("ls-files", "--others", "--exclude-standard").splitlines()))
    return {path.replace("\\", "/") for path in tracked | untracked}


def main() -> int:
    failures: list[str] = []
    passes: list[str] = []

    documents: dict[str, dict] = {}
    for filename in YAML_FILES:
        try:
            documents[filename] = load_json_yaml(filename)
            passes.append(f"parsed {filename} as JSON-compatible YAML")
        except (OSError, json.JSONDecodeError) as exc:
            failures.append(f"cannot parse {filename}: {exc}")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}")
        return 1

    state = documents["STATE.yaml"]
    blockers = {item["id"]: item for item in documents["BLOCKERS.yaml"]["blockers"]}
    approvals = {item["approval_id"]: item for item in documents["APPROVALS.yaml"]["approvals"]}

    expectations = {
        "phase": "BASELINE_SYNC",
        "checkpoint_sha": CHECKPOINT,
        "current_rf": "NONE_DURING_BASELINE_SYNC",
        "database_changed": "historical_rf25_only",
        "frontend_changed": True,
        "pr_ready": False,
    }
    for key, expected in expectations.items():
        if state.get(key) != expected:
            failures.append(f"STATE.{key}: expected {expected!r}, got {state.get(key)!r}")
        else:
            passes.append(f"STATE.{key} matches baseline")

    backend = state["tests"]["backend"]
    expected_backend = {"executed": 280, "passed": 280, "failures": 0, "errors": 0, "skipped": 0, "result": "PASSED"}
    for key, expected in expected_backend.items():
        if backend.get(key) != expected:
            failures.append(f"backend test {key}: expected {expected!r}, got {backend.get(key)!r}")
    frontend = state["tests"]["frontend"]
    if frontend.get("result") != "FAILED" or frontend.get("execution_status") != "FRONTEND_TEST_FAILURE":
        failures.append("frontend state must preserve the executed functional test failure")
    elif frontend.get("npm_ci") != "PASSED" or frontend.get("build") != "BUILD_SUCCESS":
        failures.append("frontend state must record successful npm ci and build")
    elif frontend.get("suites") != {"total": 4, "passed": 3, "failed": 1}:
        failures.append("frontend suite counts do not match executed evidence")
    elif frontend.get("tests") != {"total": 13, "passed": 11, "failed": 2, "skipped": 0}:
        failures.append("frontend test counts do not match executed evidence")
    else:
        passes.append("test baseline matches 280/280 backend and the executed frontend failure evidence")

    delivery = state.get("delivery", {})
    baseline_sync = state.get("baseline_sync", {})
    fork_backup = delivery.get("fork_backup", {})
    if any(delivery.get(key) is not False for key in ("ready_for_pr", "pr_created", "merge_approved", "merged")):
        failures.append("delivery state must remain not ready, without PR or merge authorization")
    elif fork_backup.get("allowed_when_blocked") is not True or fork_backup.get("baseline_pushed") is not True or fork_backup.get("baseline_remote_sha") != BASELINE_SHA:
        failures.append("fork backup state must record the preserved baseline SHA")
    elif baseline_sync.get("branch") != "sync/baseline-2026-08-25" or baseline_sync.get("base_sha") != INTEGRATION_SHA:
        failures.append("baseline sync branch/base does not match the audited upstream")
    elif baseline_sync.get("head_sha") != BASELINE_SHA or baseline_sync.get("prepared") is not False or baseline_sync.get("preserved_on_fork") is not True:
        failures.append("baseline sync head/prepared state does not match the failed frontend gate")
    else:
        passes.append("delivery and baseline sync state preserve the failed frontend gate")

    if set(state.get("blockers", [])) != set(MANDATORY_BLOCKERS):
        failures.append("STATE blocker references do not match mandatory blockers")
    for blocker_id, (expected_type, expected_status) in MANDATORY_BLOCKERS.items():
        item = blockers.get(blocker_id)
        if not item:
            failures.append(f"missing blocker {blocker_id}")
        elif item.get("type") != expected_type or item.get("status") != expected_status:
            failures.append(f"{blocker_id} type/status mismatch")
        else:
            passes.append(f"blocker {blocker_id} is registered correctly")

    approval = approvals.get("DB-RF25-001")
    expected_files = {
        "database/migration_rf25_motivo.sql",
        "database/schema-test.sql",
        "database/sos_artistas.sql",
        "backend/src/test/resources/db/schema-test.sql",
    }
    if not approval or approval.get("status") != "APPROVED_HISTORICAL" or set(approval.get("files", [])) != expected_files:
        failures.append("historical approval DB-RF25-001 is missing or out of scope")
    elif approval.get("scope_reuse_allowed") is not False:
        failures.append("DB-RF25-001 must not be reusable")
    else:
        passes.append("historical DB-RF25-001 scope is exact and non-reusable")

    policy = documents["POLICY.yaml"]
    if not all(policy.get(key) for key in ("allowed_automatic", "approval_required", "always_prohibited")):
        failures.append("policy categories must be populated")
    elif policy["phase_rules"]["BASELINE_SYNC"].get("new_rf_allowed") is not False:
        failures.append("BASELINE_SYNC must prohibit new RF work")
    elif policy["phase_rules"]["BASELINE_SYNC"].get("pr_creation_allowed") is not True:
        failures.append("BASELINE_SYNC must not logically block its own historical baseline PR")
    elif blockers["BASELINE-SYNC-001"].get("blocks_pr_ready") is not False or blockers["BASELINE-SYNC-001"].get("blocks_pr_creation") is not False:
        failures.append("BASELINE-SYNC-001 must not block pr_ready or historical PR creation")
    elif policy["delivery_semantics"]["push_to_fork"].get("allowed_when_blocked") is not True:
        failures.append("policy must allow fork backup/WIP while blocked")
    elif policy["delivery_semantics"]["pull_request"].get("requires_pr_ready") is not True:
        failures.append("pull requests must require pr_ready")
    elif policy["delivery_semantics"]["upstream_push"].get("allowed") is not False:
        failures.append("upstream push must remain prohibited")
    elif policy["delivery_semantics"]["merge"].get("requires_explicit_user_approval") is not True:
        failures.append("merge must require explicit user approval")
    else:
        passes.append("policy categories and corrected BASELINE_SYNC delivery semantics are active")

    roadmap = documents["ROADMAP.yaml"]
    rf24 = next((item for item in roadmap.get("items", []) if item.get("id") == "RF24"), None)
    if roadmap.get("next_automatic_rf") is not None or roadmap.get("automatic_execution_enabled") is not False:
        failures.append("roadmap must not schedule automatic RF execution")
    elif not rf24 or rf24.get("status") != "PARTIAL_BLOCKED_BY_DATABASE":
        failures.append("roadmap must keep RF24 partial and database-blocked")
    else:
        passes.append("roadmap has no automatic RF and preserves RF24 partial status")

    try:
        branch = git("branch", "--show-current")
        checkpoint_ref = git("rev-parse", "refs/heads/checkpoint/pre-automacao-2026-08-22")
        origin_checkpoint = git("rev-parse", "refs/remotes/origin/checkpoint/pre-automacao-2026-08-22")
        origin_baseline = git("rev-parse", "refs/remotes/origin/sync/baseline-2026-08-25")
        upstream_integration = git("rev-parse", "refs/remotes/upstream/projetoPJI-10-08-ajustes")
        if branch != "pji-automation":
            failures.append(f"current branch must be pji-automation, got {branch}")
        if checkpoint_ref != CHECKPOINT or origin_checkpoint != CHECKPOINT:
            failures.append("checkpoint local/fork ref drifted from immutable SHA")
        if origin_baseline != BASELINE_SHA:
            failures.append("fork baseline ref differs from the preserved local SHA")
        if upstream_integration != INTEGRATION_SHA:
            failures.append("upstream integration tracking SHA differs from recorded baseline")
        if not failures:
            passes.append("Git branch and baseline refs match recorded state")
    except (subprocess.CalledProcessError, OSError) as exc:
        failures.append(f"Git invariant check failed: {exc}")

    tree_names = set(filter(None, git("ls-tree", "-r", "--name-only", CHECKPOINT).splitlines()))
    forbidden_in_checkpoint = [
        path for path in tree_names
        if path.startswith(".pji/")
        or path.startswith("docs/AUTOMATION_")
        or path.startswith("scripts/pji_")
        or path == "RELATORIO_BOOTSTRAP_AUTOMACAO_FASE1.md"
        or path == "RELATORIO_AUTOMACAO_FASE2_BASELINE.md"
        or path == "RELATORIO_AUTOMACAO_FASE2B_FRONTEND.md"
    ]
    if forbidden_in_checkpoint:
        failures.append(f"automation files found in checkpoint: {forbidden_in_checkpoint}")
    else:
        passes.append("checkpoint contains no automation bootstrap files")

    unexpected = sorted(
        path for path in changed_paths()
        if not any(path.startswith(prefix) for prefix in ALLOWED_DIFF_PREFIXES)
    )
    if unexpected:
        failures.append(f"functional/out-of-scope paths changed: {unexpected}")
    else:
        passes.append("diff from checkpoint contains only automation infrastructure")

    try:
        guard_results = run_self_test()
        decisions = {item.decision for item in guard_results}
        if decisions != {"ALLOW", "REVIEW_REQUIRED", "BLOCK"}:
            failures.append(f"guard self-test did not exercise all decisions: {sorted(decisions)}")
        else:
            passes.append("guard examples cover ALLOW, REVIEW_REQUIRED, and BLOCK")
    except AssertionError as exc:
        failures.append(f"guard self-test failed: {exc}")

    try:
        skill_text = SKILL_PATH.read_text(encoding="utf-8")
        skill_parts = skill_text.split("---", 2)
        if len(skill_parts) != 3:
            failures.append("PJI skill frontmatter is missing")
        else:
            frontmatter = skill_parts[1]
            has_name = any(line.strip() == "name: pji-palco-control" for line in frontmatter.splitlines())
            has_description = any(line.strip().startswith("description:") and len(line.split(":", 1)[1].strip()) > 20 for line in frontmatter.splitlines())
            placeholders = ("TODO", "PLACEHOLDER", "REPLACE_ME")
            if not has_name or not has_description:
                failures.append("PJI skill name/description frontmatter is invalid")
            elif any(token in skill_text for token in placeholders):
                failures.append("PJI skill contains unfinished scaffold placeholders")
            else:
                passes.append("PJI skill has valid scoped frontmatter and no scaffold placeholders")
    except OSError as exc:
        failures.append(f"cannot read PJI skill: {exc}")

    for item in passes:
        print(f"PASS: {item}")
    for item in failures:
        print(f"FAIL: {item}")
    print(f"SUMMARY: {len(passes)} passed, {len(failures)} failed")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
