#!/usr/bin/env python3
"""Read-only policy guard for PJI Palco paths, patch text, and Git commands."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass
from enum import IntEnum
from pathlib import PurePosixPath
from typing import Iterable


class Decision(IntEnum):
    ALLOW = 0
    REVIEW_REQUIRED = 2
    BLOCK = 3


@dataclass(frozen=True)
class GuardResult:
    decision: str
    kind: str
    value: str
    reasons: tuple[str, ...]


def _normalise(value: str) -> str:
    return value.strip().replace("\\", "/")


def _result(decision: Decision, kind: str, value: str, *reasons: str) -> GuardResult:
    return GuardResult(decision.name, kind, value, tuple(reasons))


def classify_path(raw_path: str) -> GuardResult:
    path = _normalise(raw_path)
    lower = path.lower()
    while lower.startswith("./"):
        lower = lower[2:]
    name = PurePosixPath(lower).name

    if (
        name == ".env"
        or name.startswith(".env.")
        or "/secrets/" in f"/{lower}/"
        or name.endswith((".pem", ".key", ".p12", ".pfx", ".jks"))
    ):
        return _result(Decision.BLOCK, "path", path, "secret or credential material must not be committed")

    build_segments = (
        "/target/",
        "/node_modules/",
        "/frontend/build/",
        "/dist/",
        "/coverage/",
        "/surefire-reports/",
    )
    padded = f"/{lower}/"
    if any(segment in padded for segment in build_segments) or name.endswith((".class", ".jar", ".war")):
        return _result(Decision.BLOCK, "path", path, "generated build artifact must stay outside version control")

    if lower.startswith("database/") or name.endswith(".sql") or "migration" in lower:
        return _result(Decision.REVIEW_REQUIRED, "path", path, "database/schema/migration change requires explicit approval")

    if lower.startswith("frontend/"):
        return _result(Decision.REVIEW_REQUIRED, "path", path, "functional frontend change requires explicit approval")

    sensitive_tokens = (
        "securityconfig",
        "jwt",
        "authentication",
        "authorization",
        "authconfig",
        "application.properties",
        "application.yml",
        "application.yaml",
    )
    if any(token in lower for token in sensitive_tokens):
        return _result(Decision.REVIEW_REQUIRED, "path", path, "sensitive authentication, authorization, or runtime configuration")

    build_files = {
        "pom.xml",
        "build.gradle",
        "build.gradle.kts",
        "settings.gradle",
        "settings.gradle.kts",
        "package.json",
        "package-lock.json",
        "pnpm-lock.yaml",
        "yarn.lock",
        "dockerfile",
        "docker-compose.yml",
        "docker-compose.yaml",
    }
    if name in build_files or lower.startswith(".github/workflows/"):
        return _result(Decision.REVIEW_REQUIRED, "path", path, "build, dependency, Docker, or CI configuration requires review")

    return _result(Decision.ALLOW, "path", path, "no protected path category detected")


def classify_text(raw_text: str) -> GuardResult:
    text = raw_text.strip()
    lower = text.lower()
    reasons: list[str] = []
    decision = Decision.ALLOW

    if re.search(r"(?:spring\.jpa\.hibernate\.)?ddl-auto\s*[:=]\s*(create|create-drop|update)\b", lower):
        return _result(Decision.BLOCK, "text", text, "ddl-auto create/update bypass is always prohibited")

    if "ddl-auto" in lower:
        decision = Decision.REVIEW_REQUIRED
        reasons.append("ddl-auto configuration is sensitive and must preserve validate")

    if re.search(r"(?i)-----begin (?:rsa |ec |openssh )?private key-----", text):
        return _result(Decision.BLOCK, "text", text, "private key material detected")

    if re.search(r"(?i)(?:password|secret|token|api[_-]?key)\s*[:=]\s*[^$\s{][^\s]*", text):
        return _result(Decision.BLOCK, "text", text, "literal secret-like assignment detected")

    if not reasons:
        reasons.append("no protected content category detected")
    return _result(decision, "text", text, *reasons)


def classify_git_command(raw_command: str) -> GuardResult:
    command = " ".join(raw_command.strip().split())
    lower = command.lower()

    if not re.search(r"(^|\s)git(?:\.exe)?\s", lower):
        return _result(Decision.ALLOW, "git_command", command, "not a Git command")

    if re.search(r"\bgit(?:\.exe)?\s+reset\s+--hard\b", lower):
        return _result(Decision.BLOCK, "git_command", command, "destructive reset is always prohibited")

    if re.search(r"\bgit(?:\.exe)?\s+push\b", lower) and (
        re.search(r"\bupstream\b", lower)
        or "manugomesds/projetopji" in lower
        or "--force" in lower
        or " -f" in f" {lower}"
    ):
        return _result(Decision.BLOCK, "git_command", command, "direct/forced push to upstream is prohibited")

    if re.search(r"\bgit(?:\.exe)?\s+(?:commit|merge|rebase|reset|push)\b", lower) and "checkpoint/pre-automacao-2026-08-22" in lower:
        return _result(Decision.BLOCK, "git_command", command, "checkpoint branch is immutable")

    if re.search(r"\bgit(?:\.exe)?\s+(merge|rebase|pull)\b", lower):
        return _result(Decision.REVIEW_REQUIRED, "git_command", command, "integration operation requires explicit scope and approval")

    if re.search(r"\bgit(?:\.exe)?\s+push\b", lower) and not re.search(r"\borigin\b", lower):
        return _result(Decision.REVIEW_REQUIRED, "git_command", command, "push destination is not explicitly the fork remote origin")

    return _result(Decision.ALLOW, "git_command", command, "read-only Git operation or fork-scoped non-forced push")


def aggregate(results: Iterable[GuardResult]) -> Decision:
    decisions = [Decision[result.decision] for result in results]
    return max(decisions, default=Decision.ALLOW)


def run_self_test() -> list[GuardResult]:
    cases = [
        (classify_path, "docs/AUTOMATION_RUNBOOK.md", Decision.ALLOW),
        (classify_path, "database/schema-test.sql", Decision.REVIEW_REQUIRED),
        (classify_path, "frontend/src/App.js", Decision.REVIEW_REQUIRED),
        (classify_path, ".env", Decision.BLOCK),
        (classify_path, "backend/target/app.jar", Decision.BLOCK),
        (classify_text, "spring.jpa.hibernate.ddl-auto=validate", Decision.REVIEW_REQUIRED),
        (classify_text, "spring.jpa.hibernate.ddl-auto=update", Decision.BLOCK),
        (classify_git_command, "git fetch upstream", Decision.ALLOW),
        (classify_git_command, "git merge upstream/projetoPJI-10-08-ajustes", Decision.REVIEW_REQUIRED),
        (classify_git_command, "git push upstream pji-automation", Decision.BLOCK),
    ]
    results: list[GuardResult] = []
    failures: list[str] = []
    for classifier, value, expected in cases:
        result = classifier(value)
        results.append(result)
        if result.decision != expected.name:
            failures.append(f"{value}: expected {expected.name}, got {result.decision}")
    if failures:
        raise AssertionError("; ".join(failures))
    return results


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--path", action="append", default=[], help="Changed path to classify")
    parser.add_argument("--text", action="append", default=[], help="Patch/config text to inspect")
    parser.add_argument("--git-command", action="append", default=[], help="Git command to classify")
    parser.add_argument("--json", action="store_true", help="Emit machine-readable JSON")
    parser.add_argument("--self-test", action="store_true", help="Run deterministic ALLOW/REVIEW_REQUIRED/BLOCK examples")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        results = run_self_test() if args.self_test else []
    except AssertionError as exc:
        print(f"SELF_TEST_FAILED: {exc}", file=sys.stderr)
        return 1

    results.extend(classify_path(value) for value in args.path)
    results.extend(classify_text(value) for value in args.text)
    results.extend(classify_git_command(value) for value in args.git_command)

    if not results:
        print("No input supplied. Use --path, --text, --git-command, or --self-test.", file=sys.stderr)
        return 1

    overall = aggregate(results)
    if args.json:
        print(json.dumps({"overall": overall.name, "results": [asdict(item) for item in results]}, ensure_ascii=False, indent=2))
    else:
        if args.self_test:
            print("SELF_TEST_PASSED")
        for item in results:
            print(f"{item.decision}\t{item.kind}\t{item.value}\t{' | '.join(item.reasons)}")
        print(f"OVERALL\t{overall.name}")

    return 0 if args.self_test else int(overall)


if __name__ == "__main__":
    raise SystemExit(main())
