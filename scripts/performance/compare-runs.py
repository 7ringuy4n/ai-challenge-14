#!/usr/bin/env python3
"""Aggregate measured JMeter and system/database snapshots against approved SLOs."""
import json
import re
import sys
from pathlib import Path

root = Path(sys.argv[1] if len(sys.argv) > 1 else "reports/performance")
results = []
for run in sorted(root.glob("*/run.json")):
    directory = run.parent
    try:
        meta = json.loads(run.read_text())
        if meta.get("status") == "DIAGNOSTIC_RUN_METADATA_INVALID":
            continue
        sample = json.loads((directory / "summary.json").read_text())
    except (OSError, json.JSONDecodeError):
        continue
    containers = {}
    dbconn, txcommit, txrollback, pending, failed, delivered, oldest, sample_times = [], [], [], [], [], [], [], []
    metrics_path = directory / "system-metrics.jsonl"
    if metrics_path.exists():
        for line in metrics_path.read_text().splitlines():
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                continue
            for container in record.get("containers", []):
                name = container.get("Name", "unknown")
                summary = containers.setdefault(name, {"max_cpu_percent": None, "max_memory_mib": None})
                try:
                    cpu = float(container.get("CPUPerc", "").rstrip("%"))
                    summary["max_cpu_percent"] = max(cpu, summary["max_cpu_percent"] or 0)
                except (ValueError, TypeError):
                    pass
                memory = re.match(r"([0-9.]+)([KMG]iB)", container.get("MemUsage", ""))
                if memory:
                    value = float(memory.group(1)) * {"KiB": 1 / 1024, "MiB": 1, "GiB": 1024}[memory.group(2)]
                    summary["max_memory_mib"] = max(value, summary["max_memory_mib"] or 0)
            db = record.get("database", {})
            if isinstance(db, dict):
                sample_times.append(record.get("timestamp_utc"))
                for key, dest in (("active_connections", dbconn), ("transactions_committed", txcommit),
                                  ("transactions_rolled_back", txrollback), ("outbox_pending", pending),
                                  ("outbox_failed", failed), ("outbox_delivered", delivered),
                                  ("outbox_oldest_pending_seconds", oldest)):
                    if isinstance(db.get(key), (int, float)):
                        dest.append(db[key])
    targets = meta["approved_targets"]
    latency = sample.get("latency_ms", {})
    passed = (latency.get("p95") is not None and latency.get("p95") <= targets["p95_ms"]
              and latency.get("p99") is not None and latency.get("p99") <= targets["p99_ms"]
              and sample.get("error_rate") is not None and sample["error_rate"] <= targets["error_rate_max"])
    results.append({
        "profile": meta["profile"], "status": "PASS" if passed else "FAIL",
        "configured_concurrency": meta["threads"], "ramp_seconds": meta["ramp_seconds"],
        "duration_seconds": meta["duration_seconds"], "samples": sample["samples"],
        "requests_per_second": sample["rps"], "error_rate": sample["error_rate"],
        "latency_ms": latency, "by_sampler": sample.get("by_sampler", {}),
        "containers": containers, "max_db_connections": max(dbconn, default=None),
        "max_db_transactions_committed": max(txcommit, default=None),
        "max_db_transactions_rolled_back": max(txrollback, default=None),
        "max_outbox_pending": max(pending, default=None),
        "outbox_delivered_delta": (delivered[-1] - delivered[0]) if len(delivered) > 1 else 0,
        "outbox_failed_count": max(failed, default=None),
        "outbox_deliveries_per_second": ((delivered[-1] - delivered[0]) / meta["duration_seconds"] if len(delivered) > 1 and meta["duration_seconds"] else 0),
        "max_outbox_oldest_pending_seconds": max(oldest, default=None),
        "targets": targets, "evidence_dir": str(directory),
    })
print(json.dumps({"comparison": "P95<=500ms, P99<=1000ms, HTTP errors<=1%", "runs": results}, indent=2))
