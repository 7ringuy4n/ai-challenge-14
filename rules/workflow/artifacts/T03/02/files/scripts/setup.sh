#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
ENV_FILE="$ROOT_DIR/.env"
EXAMPLE_FILE="$ROOT_DIR/.env.example"

if [[ ! -f "$ENV_FILE" ]]; then
  if [[ ! -f "$EXAMPLE_FILE" ]]; then
    printf 'Missing .env.example; cannot initialize local configuration.\n' >&2
    exit 1
  fi
  cp "$EXAMPLE_FILE" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi

# Fill absent/sample local secrets without sourcing .env as executable shell code.
# Existing non-placeholder values are preserved. Generated hex is safe in Compose dotenv.
python3 - "$ENV_FILE" <<'PY'
import os
import re
import secrets
import sys
from pathlib import Path

path = Path(sys.argv[1])
lines = path.read_text(encoding="utf-8").splitlines()
values = {}
for line in lines:
    stripped = line.strip()
    if not stripped or stripped.startswith("#"):
        continue
    key, separator, value = stripped.partition("=")
    if separator:
        values[key.strip()] = value.strip()

required = ("DB_PASSWORD", "DB_MIGRATION_PASSWORD", "DEMO_PASSWORD", "SYSTEM_PASSWORD")
replacements = {}
for key in required:
    value = values.get(key, "")
    if not value or "CHANGEME" in value or value.startswith("dummy_"):
        replacements[key] = secrets.token_hex(24)
    elif len(value) < 16 or not re.fullmatch(r"[A-Za-z0-9_.-]+", value):
        raise SystemExit(f"{key} must be at least 16 characters using letters, digits, dot, underscore, or hyphen")

if len({replacements.get(key, values.get(key, "")) for key in required}) != len(required):
    raise SystemExit("DB_PASSWORD, DB_MIGRATION_PASSWORD, DEMO_PASSWORD, and SYSTEM_PASSWORD must be distinct")

if not values.get("HOST_PORT"):
    replacements["HOST_PORT"] = "18080"
port = replacements.get("HOST_PORT", values.get("HOST_PORT", ""))
if not port.isdecimal() or not 1 <= int(port) <= 65535:
    raise SystemExit("HOST_PORT must be an integer between 1 and 65535")

seen = set()
output = []
for line in lines:
    stripped = line.strip()
    if stripped and not stripped.startswith("#"):
        key, separator, _ = stripped.partition("=")
        key = key.strip()
        if separator and key in replacements:
            output.append(f"{key}={replacements[key]}")
            seen.add(key)
            continue
    output.append(line)
for key, value in replacements.items():
    if key not in seen and key not in values:
        output.append(f"{key}={value}")
path.write_text("\n".join(output) + "\n", encoding="utf-8")
os.chmod(path, 0o600)
PY

# Reconcile the local database credentials before starting the application. Compose
# updates the DB container environment while retaining its named data volume. The
# reconciliation runs over the container's trusted local socket and emits no secrets.
docker compose --env-file .env -f docker/compose.yml config --quiet
docker compose --env-file .env -f docker/compose.yml up -d --wait db
docker compose --env-file .env -f docker/compose.yml exec -T db sh /opt/claims/reconcile-roles.sh

# Provision/build the app only after current database credentials are installed.
# This does not run Maven tests or scenario checks.
docker compose --env-file .env -f docker/compose.yml up -d --build --wait app
printf 'Local claims services are healthy. Run bash scripts/smoke.sh for the HTTP smoke check.\n'
