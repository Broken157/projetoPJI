#!/usr/bin/env python3
"""Display the machine-readable PJI Palco automation state."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def load_json_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    state = load_json_yaml(args.root / ".pji" / "STATE.yaml")
    blocker_data = load_json_yaml(args.root / ".pji" / "BLOCKERS.yaml")
    blockers = blocker_data["blockers"]
    summary = {
        "project": state["project"]["name"],
        "phase": state["phase"],
        "current_week": state["current_week"],
        "current_rf": state["current_rf"],
        "checkpoint_sha": state["checkpoint_sha"],
        "pr_ready": state["pr_ready"],
        "backend_tests": state["tests"]["backend"],
        "frontend_tests": state["tests"]["frontend"],
        "blockers": [
            {
                "id": blocker["id"],
                "type": blocker["type"],
                "status": blocker["status"],
                "blocks": blocker["blocks"],
            }
            for blocker in blockers
        ],
    }

    if args.json:
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        return 0

    print(f"Project: {summary['project']}")
    print(f"Phase: {summary['phase']}")
    print(f"Week: {summary['current_week']}")
    print(f"Current RF: {summary['current_rf']}")
    print(f"Checkpoint: {summary['checkpoint_sha']}")
    print(f"PR ready: {str(summary['pr_ready']).lower()}")
    print(f"Backend tests: {summary['backend_tests']['passed']}/{summary['backend_tests']['executed']} {summary['backend_tests']['build']}")
    print(f"Frontend tests: {summary['frontend_tests']['result']}")
    print("Blockers:")
    for blocker in summary["blockers"]:
        print(f"- {blocker['id']}: {blocker['status']} ({blocker['type']})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
