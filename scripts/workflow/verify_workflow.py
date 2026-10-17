#!/usr/bin/env python3
"""Validate the workflow manifest and task-scoped patch paths (stdlib only)."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path, PurePosixPath
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "rules/workflow/tasks.json"
STATUSES = {"planned", "running", "submitted", "in_review", "accepted", "blocked"}
TASK_FIELDS = {
    "id", "title", "role", "model", "reasoning_effort", "status",
    "depends_on", "allowed_paths", "verification", "case_ids", "acceptance",
    "evidence", "notes",
}
HEX_256 = re.compile(r"^[0-9a-f]{64}$")


def _safe_repo_path(value: Any) -> bool:
    if not isinstance(value, str) or not value or "\\" in value:
        return False
    path = PurePosixPath(value)
    return not path.is_absolute() and all(part not in ("", ".", "..") for part in path.parts)


def validate_manifest(data: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(data, dict):
        return ["manifest root must be an object"]
    if data.get("schema_version") != 1:
        errors.append("schema_version must be 1")
    if not isinstance(data.get("tasks"), list) or not data["tasks"]:
        return errors + ["tasks must be a nonempty array"]
    if not isinstance(data.get("shared_paths"), list):
        errors.append("shared_paths must be an array")
    else:
        for i, path in enumerate(data["shared_paths"]):
            if not _safe_repo_path(path):
                errors.append(f"shared_paths[{i}] is not a safe repository path")

    by_id: dict[str, dict[str, Any]] = {}
    for i, task in enumerate(data["tasks"]):
        at = f"tasks[{i}]"
        if not isinstance(task, dict):
            errors.append(f"{at} must be an object")
            continue
        missing = sorted(TASK_FIELDS - task.keys())
        if missing:
            errors.append(f"{at} is missing fields: {', '.join(missing)}")
        task_id = task.get("id")
        if not isinstance(task_id, str) or not task_id.strip():
            errors.append(f"{at}.id must be a nonempty string")
            continue
        if task_id in by_id:
            errors.append(f"duplicate task id: {task_id}")
        by_id[task_id] = task
        if task.get("status") not in STATUSES:
            errors.append(f"{task_id}.status is invalid")
        for field in ("depends_on", "allowed_paths", "verification", "case_ids", "acceptance", "evidence"):
            if not isinstance(task.get(field), list):
                errors.append(f"{task_id}.{field} must be an array")
        for j, path in enumerate(task.get("allowed_paths", [])):
            if not _safe_repo_path(path):
                errors.append(f"{task_id}.allowed_paths[{j}] is not a safe repository path")
        if task.get("status") == "accepted":
            evidence = task.get("evidence", [])
            has_check = any(
                isinstance(item, dict)
                and isinstance(item.get("command"), str) and item["command"].strip()
                and item.get("exit_code") == 0
                and isinstance(item.get("source_hash"), str) and HEX_256.fullmatch(item["source_hash"])
                and isinstance(item.get("artifact"), str) and item["artifact"].strip()
                and item.get("result") in {"PASS", "PASS_PENDING_REVIEW"}
                for item in evidence
            )
            has_review = any(
                isinstance(item, dict)
                and isinstance(item.get("reviewer_agent"), str) and item["reviewer_agent"].strip()
                and isinstance(item.get("source_hash"), str) and HEX_256.fullmatch(item["source_hash"])
                and isinstance(item.get("artifact"), str) and item["artifact"].strip()
                and item.get("result") == "PASS"
                for item in evidence
            )
            if not has_check:
                errors.append(f"{task_id} is accepted without a passing command/source/artifact evidence record")
            if not has_review:
                errors.append(f"{task_id} is accepted without a passing independent review evidence record")

    for task_id, task in by_id.items():
        for dependency in task.get("depends_on", []) if isinstance(task.get("depends_on"), list) else []:
            if dependency not in by_id:
                errors.append(f"{task_id} depends on unknown task {dependency}")
            elif dependency == task_id:
                errors.append(f"{task_id} cannot depend on itself")

    # Detect dependency cycles separately from missing references.
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(task_id: str) -> None:
        if task_id in visiting:
            errors.append(f"dependency cycle includes {task_id}")
            return
        if task_id in visited:
            return
        visiting.add(task_id)
        task = by_id[task_id]
        for dependency in task.get("depends_on", []) if isinstance(task.get("depends_on"), list) else []:
            if dependency in by_id:
                visit(dependency)
        visiting.remove(task_id)
        visited.add(task_id)

    for task_id in by_id:
        visit(task_id)
    return errors


def ready_tasks(data: dict[str, Any]) -> list[str]:
    by_id = {task["id"]: task for task in data["tasks"]}
    return sorted(
        task["id"] for task in data["tasks"]
        if task["status"] == "planned"
        and all(by_id[dependency]["status"] == "accepted" for dependency in task["depends_on"])
    )


def _path_matches(changed: str, allowed: str) -> bool:
    base = allowed.rstrip("/")
    return changed == base or changed.startswith(base + "/")


def validate_patch(task_id: str, patch_path: Path, data: dict[str, Any], root: Path = ROOT) -> list[str]:
    task = next((item for item in data["tasks"] if item["id"] == task_id), None)
    if task is None:
        return [f"unknown task id: {task_id}"]
    if task.get("role") == "reviewer":
        return [f"review task {task_id} cannot provide source patches"]
    try:
        content = patch_path.read_text(encoding="utf-8")
    except OSError as exc:
        return [f"cannot read patch {patch_path}: {exc}"]
    if not content.strip():
        return ["patch is empty"]

    changed: set[str] = set()
    for line in content.splitlines():
        if line.startswith("diff --git "):
            match = re.fullmatch(r"diff --git a/(.+) b/(.+)", line)
            if not match:
                return ["patch contains an unsupported or malformed diff header"]
            old, new = match.groups()
            for path in (old, new):
                if path != "/dev/null" and not _safe_repo_path(path):
                    return [f"patch contains unsafe path: {path}"]
            if old != "/dev/null":
                changed.add(old)
            if new != "/dev/null":
                changed.add(new)
    if not changed:
        return ["patch contains no Git file diffs"]

    allowed = task.get("allowed_paths", [])
    shared = data.get("shared_paths", [])
    errors: list[str] = []
    for path in sorted(changed):
        if not any(_path_matches(path, item) for item in allowed):
            errors.append(f"{path} is outside {task_id}.allowed_paths")
        if task.get("role") == "worker" and any(_path_matches(path, item) for item in shared):
            errors.append(f"worker task {task_id} cannot modify shared path {path}")
    return errors


def load_manifest(path: Path = MANIFEST) -> dict[str, Any]:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=MANIFEST, help=argparse.SUPPRESS)
    parser.add_argument("--ready", action="store_true", help="list planned tasks whose dependencies are accepted")
    parser.add_argument("--patch", nargs=2, metavar=("TASK", "PATH"), help="validate a candidate patch against task path ownership")
    args = parser.parse_args(argv)
    try:
        data = load_manifest(args.manifest)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: cannot load manifest: {exc}", file=sys.stderr)
        return 2
    errors = validate_manifest(data)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    if args.patch:
        task_id, patch = args.patch
        patch_path = Path(patch)
        if not patch_path.is_absolute():
            patch_path = ROOT / patch_path
        errors = validate_patch(task_id, patch_path, data)
        if errors:
            for error in errors:
                print(f"ERROR: {error}", file=sys.stderr)
            return 1
        print(f"PASS: patch paths are within {task_id} ownership")
        return 0
    if args.ready:
        tasks = ready_tasks(data)
        print("Ready tasks: " + (", ".join(tasks) if tasks else "none"))
        return 0
    print(f"PASS: workflow manifest has {len(data['tasks'])} unique, dependency-valid tasks")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
