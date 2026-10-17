#!/usr/bin/env python3
"""Capture host, JMeter, target-container, Actuator, and PostgreSQL load signals.

This monitor emits only aggregate resource/metrics data. It never writes credentials,
request bodies, SQL text, session tokens, or claim contents to the evidence file.
Run from a clean 0435e1b worktree while the `docker` Compose project serves port 8080.
"""
from __future__ import annotations

import base64
import json
import os
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen


OUT = Path(sys.argv[1])
DURATION = int(sys.argv[2]) if len(sys.argv) > 2 else 100
INTERVAL = 5
WORKTREE = Path.cwd()
ENV_FILE = WORKTREE / ".env"


def run(args: list[str], timeout: int = 5) -> str:
    try:
        result = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
        return result.stdout.strip() if result.returncode == 0 else ""
    except (OSError, subprocess.TimeoutExpired):
        return ""


def proc_stat() -> list[int]:
    try:
        return [int(x) for x in Path("/proc/stat").read_text().splitlines()[0].split()[1:]]
    except (OSError, ValueError, IndexError):
        return []


def cpu_percent(previous: list[int], current: list[int]) -> float | None:
    if len(previous) < 4 or len(current) < 4:
        return None
    delta = [b - a for a, b in zip(previous, current)]
    total = sum(delta)
    idle = delta[3] + (delta[4] if len(delta) > 4 else 0)
    return round(100 * (total - idle) / total, 2) if total > 0 else None


def meminfo() -> dict[str, int]:
    wanted = {"MemTotal", "MemAvailable", "SwapTotal", "SwapFree", "Dirty", "Writeback"}
    values = {}
    try:
        for line in Path("/proc/meminfo").read_text().splitlines():
            key, value = line.split(":", 1)
            if key in wanted:
                values[key] = int(value.strip().split()[0])
    except (OSError, ValueError):
        pass
    return values


def vmstat() -> dict[str, int]:
    wanted = {"pswpin", "pswpout", "pgmajfault", "allocstall"}
    values = {}
    try:
        for line in Path("/proc/vmstat").read_text().splitlines():
            key, value = line.split()
            if key in wanted:
                values[key] = int(value)
    except (OSError, ValueError):
        pass
    return values


def thermal() -> dict[str, int]:
    values = {}
    root = Path("/sys/devices/system/cpu")
    try:
        for path in root.glob("cpu*/thermal_throttle/*"):
            key = path.name
            if key.endswith(("_count", "_total_time_ms")):
                try:
                    values[str(path)] = int(path.read_text().strip())
                except (OSError, ValueError):
                    pass
    except OSError:
        pass
    return values


def pressure() -> dict[str, str]:
    values = {}
    for name in ("cpu", "memory", "io"):
        try:
            values[name] = Path(f"/proc/pressure/{name}").read_text().strip()
        except OSError:
            pass
    return values


def jmeter_processes() -> list[dict[str, float | int]]:
    output = run(["ps", "-eo", "pid=,pcpu=,pmem=,rss=,comm=,args="], timeout=3)
    result = []
    for line in output.splitlines():
        if "org.apache.jmeter" not in line and "ApacheJMeter" not in line:
            continue
        # Exclude args, which may include file paths or external command details.
        parts = line.split(None, 4)
        if len(parts) >= 4:
            try:
                result.append({"pid": int(parts[0]), "cpu_percent": float(parts[1]),
                               "mem_percent": float(parts[2]), "rss_kib": int(parts[3])})
            except ValueError:
                pass
    return result


def docker_stats() -> list[dict[str, str]]:
    output = run(["docker", "stats", "--no-stream", "--format", "{{json .}}"], timeout=5)
    result = []
    for line in output.splitlines():
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            continue
        if item.get("Name") in {"docker-app-1", "docker-database-1", "docker-otel-collector-1"}:
            result.append({k: item.get(k, "") for k in
                           ("Name", "CPUPerc", "MemUsage", "MemPerc", "PIDs", "BlockIO")})
    return result


def app_metrics() -> dict[str, str]:
    password = ""
    try:
        for line in ENV_FILE.read_text().splitlines():
            if line.startswith("CLAIMS_DEMO_PASSWORD="):
                password = line.split("=", 1)[1].strip()
                break
    except OSError:
        pass
    if not password or password.startswith("CHANGE_ME_"):
        return {"status": "NOT_AVAILABLE_NO_TEST_CREDENTIAL"}
    token = base64.b64encode(f"member-demo:{password}".encode()).decode()
    request = Request("http://127.0.0.1:8080/actuator/prometheus",
                      headers={"Authorization": f"Basic {token}"})
    try:
        with urlopen(request, timeout=3) as response:
            text = response.read().decode(errors="replace")
    except (URLError, TimeoutError, OSError) as exc:
        return {"status": f"NOT_AVAILABLE_{type(exc).__name__}"}
    patterns = (
        "hikaricp_connections_active", "hikaricp_connections_idle",
        "hikaricp_connections_pending", "hikaricp_connections_max",
        "hikaricp_connections_timeout", "jvm_gc_pause_seconds_count",
        "jvm_gc_pause_seconds_sum", "jvm_gc_pause_seconds_max",
        "jvm_memory_used_bytes", "process_cpu_usage", "system_cpu_usage",
        "system_cpu_count", "process_files_open",
    )
    return {"status": "PASS", "samples": [line for line in text.splitlines()
            if any(line.startswith(prefix) for prefix in patterns)]}


DB_QUERY = """SELECT json_build_object(
 'active_connections',(SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()),
 'active_queries',(SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND state='active'),
 'lock_waiters',(SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'),
 'wait_events',(SELECT COALESCE(json_object_agg(wait_event_type,n), '{}'::json) FROM
   (SELECT wait_event_type,count(*) n FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type IS NOT NULL GROUP BY 1) q),
 'max_active_query_seconds',(SELECT COALESCE(max(extract(epoch from(clock_timestamp()-query_start))),0) FROM pg_stat_activity WHERE datname=current_database() AND state='active'),
 'max_transaction_seconds',(SELECT COALESCE(max(extract(epoch from(clock_timestamp()-xact_start))),0) FROM pg_stat_activity WHERE datname=current_database() AND xact_start IS NOT NULL),
 'deadlocks',(SELECT deadlocks FROM pg_stat_database WHERE datname=current_database()),
 'blks_hit',(SELECT blks_hit FROM pg_stat_database WHERE datname=current_database()),
 'blks_read',(SELECT blks_read FROM pg_stat_database WHERE datname=current_database()),
 'temp_bytes',(SELECT temp_bytes FROM pg_stat_database WHERE datname=current_database()),
 'transactions_committed',(SELECT xact_commit FROM pg_stat_database WHERE datname=current_database()),
 'transactions_rolled_back',(SELECT xact_rollback FROM pg_stat_database WHERE datname=current_database()),
 'outbox_pending',(SELECT count(*) FROM local_outbox WHERE delivered_at IS NULL),
 'outbox_failed',(SELECT count(*) FROM local_outbox WHERE last_error IS NOT NULL),
 'outbox_delivered',(SELECT count(*) FROM local_outbox WHERE delivered_at IS NOT NULL)
);"""


def database_metrics() -> dict[str, object]:
    output = run(["docker", "compose", "--env-file", ".env", "-f", "docker/compose.yml",
                  "exec", "-T", "database", "psql", "-U", "claims_bootstrap", "-d", "claims",
                  "-Atc", DB_QUERY], timeout=5)
    try:
        return json.loads(output)
    except json.JSONDecodeError:
        return {"status": "NOT_AVAILABLE"}


def main() -> int:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    end = time.monotonic() + DURATION
    previous_cpu = proc_stat()
    while time.monotonic() < end:
        now = datetime.now(timezone.utc).isoformat()
        current_cpu = proc_stat()
        record = {
            "timestamp_utc": now,
            "host_cpu_busy_percent": cpu_percent(previous_cpu, current_cpu),
            "load_average": os.getloadavg(),
            "host_mem_kib": meminfo(),
            "vmstat_counters": vmstat(),
            "cpu_thermal_throttle_counters": thermal(),
            "pressure_stall_information": pressure(),
            "jmeter_processes": jmeter_processes(),
            "target_container_stats": docker_stats(),
            "application_actuator_metrics": app_metrics(),
            "postgresql": database_metrics(),
        }
        with OUT.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, separators=(",", ":")) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        previous_cpu = current_cpu
        time.sleep(INTERVAL)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
