#!/usr/bin/env python3
"""Export accessible Codex conversation/tool records for this repository.

Host-managed model request envelopes, system/developer prompts, and encrypted
reasoning are not exposed by this export and are reported as NOT_CAPTURED.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


REPO = Path(__file__).resolve().parents[2]
CODEX_HOME = Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))
OUTPUT = REPO / "reports/audit"
PROJECT = str(REPO)

SECRET_KEYS = re.compile(
    r"(?i)(CLAIMS_[A-Z0-9_]*(?:PASSWORD|TOKEN|SECRET|KEY)|"
    r"(?:password|passwd|secret|csrfToken|access_token|refresh_token)"
    r"\s*[\"']?\s*[:=]\s*[\"']?)([^\s,\"'}]+)"
)
AUTH_VALUE = re.compile(r"(?i)(authorization\s*[:=]\s*['\"]?)([^\s,\"'}]+)")
BEARER = re.compile(r"(?i)\bBearer\s+[A-Za-z0-9._~+/=-]+")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def secret_values() -> list[str]:
    env = REPO / ".env"
    values: list[str] = []
    if env.is_file():
        for line in env.read_text(errors="replace").splitlines():
            if "=" not in line or line.lstrip().startswith("#"):
                continue
            key, value = line.split("=", 1)
            if value and any(word in key.upper() for word in ("PASSWORD", "TOKEN", "SECRET", "KEY")):
                values.append(value)
    return sorted(set(values), key=len, reverse=True)


def redact_text(text: str, values: list[str]) -> str:
    for value in values:
        text = text.replace(value, "[REDACTED_SECRET]")
    text = AUTH_VALUE.sub(r"\1[REDACTED_AUTH]", text)
    text = BEARER.sub("Bearer [REDACTED_TOKEN]", text)
    text = SECRET_KEYS.sub(r"\1[REDACTED_SECRET]", text)
    return text


def scrub(value: Any, secrets: list[str]) -> Any:
    if isinstance(value, str):
        return redact_text(value, secrets)
    if isinstance(value, list):
        return [scrub(item, secrets) for item in value]
    if isinstance(value, dict):
        sensitive_key = re.compile(
            r"(?i)(password|passwd|secret|token|api[_-]?key|authorization|"
            r"cookie|csrf|session[_-]?id|credential)"
        )
        return {
            key: "[REDACTED_SECRET]" if sensitive_key.search(str(key)) else scrub(item, secrets)
            for key, item in value.items()
        }
    return value


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    records = []
    with path.open(encoding="utf-8", errors="replace") as stream:
        for line in stream:
            try:
                item = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(item, dict):
                records.append(item)
    return records


def project_related(path: Path, records: list[dict[str, Any]]) -> bool:
    for record in records:
        payload = record.get("payload", {})
        if not isinstance(payload, dict):
            continue
        if payload.get("cwd") == PROJECT:
            return True
        roots = payload.get("runtime_workspace_roots", [])
        if isinstance(roots, list) and PROJECT in roots:
            return True
    return False


def exportable(record: dict[str, Any]) -> bool:
    kind = record.get("type")
    if kind in {"custom_tool_call", "custom_tool_call_output", "function_call", "function_call_output"}:
        return True
    if kind == "agent_message":
        return True
    if kind == "message":
        return record.get("role") in {"user", "assistant"}
    if kind == "reasoning":
        return bool(record.get("summary"))
    return False


def main() -> int:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    transcript_dir = OUTPUT / "transcripts"
    transcript_dir.mkdir(parents=True, exist_ok=True)
    repo_root = REPO.resolve()
    resolved_output = OUTPUT.resolve()
    resolved_transcripts = transcript_dir.resolve()
    if repo_root not in resolved_output.parents or resolved_output != repo_root / "reports/audit":
        raise ValueError(f"Audit output root must be repository-local: {resolved_output}")
    if resolved_transcripts != resolved_output / "transcripts":
        raise ValueError(f"Audit transcript directory must not redirect outside output root: {resolved_transcripts}")
    roots = [CODEX_HOME / "sessions", CODEX_HOME / "archived_sessions"]
    candidates = [p for root in roots if root.is_dir() for p in root.rglob("*.jsonl")]
    secrets = secret_values()
    inventory: list[dict[str, Any]] = []

    for path in sorted(candidates):
        records = load_jsonl(path)
        if not project_related(path, records):
            continue
        meta = next((r.get("payload", {}) for r in records if r.get("type") == "session_meta"), {})
        session_id = str(meta.get("id") or path.stem)
        counts = Counter(r.get("type", "UNKNOWN") for r in records)
        role_counts: Counter[str] = Counter()
        exported_count = 0
        reasoning_encrypted = 0
        source_digest = hashlib.sha256(str(path).encode("utf-8")).hexdigest()[:12]
        safe_id = re.sub(r"[^A-Za-z0-9_.-]", "_", session_id).strip(".") or "session"
        out_path = (transcript_dir / f"{safe_id}-{source_digest}.jsonl").resolve()
        if resolved_transcripts not in out_path.parents:
            raise ValueError(f"Unsafe transcript output path derived from {path}")
        with out_path.open("w", encoding="utf-8") as output:
            for record in records:
                if record.get("type") == "reasoning" and record.get("encrypted_content"):
                    reasoning_encrypted += 1
                    continue
                if not exportable(record.get("payload", {})):
                    continue
                payload = record["payload"]
                if payload.get("type") == "message":
                    role_counts[payload.get("role", "unknown")] += 1
                row = {
                    "timestamp": record.get("timestamp"),
                    "ordinal": record.get("ordinal"),
                    "record_type": record.get("type"),
                    "payload": scrub(payload, secrets),
                }
                output.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
                exported_count += 1
        source = meta.get("source", {})
        source = source if isinstance(source, dict) else {}
        subagent = source.get("subagent", {})
        subagent = subagent if isinstance(subagent, dict) else {}
        thread_spawn = subagent.get("thread_spawn", {})
        thread_spawn = thread_spawn if isinstance(thread_spawn, dict) else {}
        inventory.append({
            "session_id": session_id,
            "source_file": str(path),
            "source_sha256": sha256(path),
            "session_kind": "archived" if "archived_sessions" in str(path) else "active_or_retained",
            "cwd": meta.get("cwd", "NOT_CAPTURED"),
            "parent_thread_id": meta.get("parent_thread_id", "NOT_CAPTURED"),
            "agent_path": thread_spawn.get("agent_path", "NOT_CAPTURED"),
            "started_at": meta.get("timestamp", records[0].get("timestamp") if records else "NOT_CAPTURED"),
            "record_counts": dict(sorted(counts.items())),
            "exported_accessible_message_and_tool_records": exported_count,
            "exported_message_roles": dict(sorted(role_counts.items())),
            "encrypted_reasoning_records_excluded": reasoning_encrypted,
            "raw_host_model_request_response_envelopes": "NOT_CAPTURED",
            "developer_system_payloads": "NOT_CAPTURED",
            "transcript_path": str(out_path.relative_to(REPO)),
            "transcript_sha256": sha256(out_path),
        })

    report = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "repository": PROJECT,
        "codex_home": str(CODEX_HOME),
        "inventory_roots": [str(root) for root in roots],
        "candidate_jsonl_files": len(candidates),
        "project_related_sessions": len(inventory),
        "accessible_records_exported": sum(x["exported_accessible_message_and_tool_records"] for x in inventory),
        "redactions": {
            "applied": True,
            "fields": ["values found in local .env secret keys", "Authorization values", "Bearer tokens", "password/token/secret/key assignments"],
            "declaration": "All redactions use [REDACTED_SECRET], [REDACTED_AUTH], or [REDACTED_TOKEN].",
        },
        "not_captured": [
            "Host-managed model request/response wire payloads and request IDs",
            "System/developer prompt envelopes not exposed as user/assistant messages",
            "Encrypted reasoning contents",
            "Any archived session file inaccessible to this local process",
        ],
        "audit_status": "PARTIAL_NOT_CAPTURED",
        "sessions": inventory,
    }
    report_path = resolved_output / "REQ-017-session-inventory.json"
    if report_path.is_symlink() or report_path.resolve().parent != resolved_output:
        raise ValueError(f"Audit inventory target must be a regular repository-local file: {report_path}")
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Project sessions inventoried: {len(inventory)} / {len(candidates)} local JSONL files")
    print(f"Accessible message/tool records exported: {report['accessible_records_exported']}")
    print("Raw host-managed model envelopes: NOT_CAPTURED; audit status PARTIAL_NOT_CAPTURED")
    print(f"Inventory: {report_path.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
