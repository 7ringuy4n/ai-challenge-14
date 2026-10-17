#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
if [[ ! -f .env ]]; then
  printf 'Missing .env. Run bash scripts/setup.sh first.\n' >&2
  exit 1
fi

python3 - .env <<'PY'
import base64
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

values = {}
for line in Path(sys.argv[1]).read_text(encoding="utf-8").splitlines():
    stripped = line.strip()
    if not stripped or stripped.startswith("#"):
        continue
    key, separator, value = stripped.partition("=")
    if separator:
        values[key.strip()] = value.strip()

port = values.get("HOST_PORT", "18080")
if not port.isdecimal() or not 1 <= int(port) <= 65535:
    raise SystemExit("Smoke check failed: HOST_PORT is invalid")
base = f"http://127.0.0.1:{port}"


def get(path, authorization=None):
    headers = {"Accept": "application/json"}
    if authorization:
        token = base64.b64encode(authorization.encode("utf-8")).decode("ascii")
        headers["Authorization"] = f"Basic {token}"
    request = urllib.request.Request(base + path, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=8) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"HTTP status {error.code} for {path}") from None
    except (urllib.error.URLError, TimeoutError) as error:
        raise RuntimeError(f"request failed for {path}: {error.reason if isinstance(error, urllib.error.URLError) else 'timeout'}") from None


try:
    status, body = get("/actuator/health")
    if status != 200 or json.loads(body).get("status") != "UP":
        raise RuntimeError("health endpoint did not report UP")
    print("PASS health endpoint")

    status, _ = get("/")
    if status != 200:
        raise RuntimeError("home page did not return HTTP 200")
    print("PASS home page")

    password = values.get("DEMO_PASSWORD", "")
    if not password or "CHANGEME" in password:
        raise RuntimeError("DEMO_PASSWORD is missing or still a placeholder")
    status, body = get("/api/session", f"clerk:{password}")
    if status != 200:
        raise RuntimeError("authenticated session request did not return HTTP 200")
    session = json.loads(body)
    if not isinstance(session, dict) or not session.get("userId") or not session.get("role"):
        raise RuntimeError("authenticated session response is missing userId or role")
    print("PASS authenticated session")
except (RuntimeError, ValueError, json.JSONDecodeError) as error:
    print(f"FAIL smoke check: {error}", file=sys.stderr)
    raise SystemExit(1)
PY
