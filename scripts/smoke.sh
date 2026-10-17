#!/usr/bin/env bash
set -euo pipefail

env_file=.env
if [[ ! -f "$env_file" ]]; then
  echo "Run bash scripts/setup.sh first." >&2
  exit 1
fi
read_env() {
  awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$env_file"
}
port=$(read_env SERVER_PORT)
demo_password=$(read_env CLAIMS_DEMO_PASSWORD)
base="http://127.0.0.1:${port:-8080}"
# Suppress response bodies and curl diagnostics so credentials and tokens never appear.
curl --silent --show-error --fail --output /dev/null --user "clerk:${demo_password}" \
  "$base/actuator/health" 2>/dev/null || { echo "Authenticated health check failed." >&2; exit 1; }
curl --silent --show-error --fail --output /dev/null --user "clerk:${demo_password}" \
  "$base/api/v1/claims?page=0&size=1" 2>/dev/null || { echo "Authenticated claims API check failed." >&2; exit 1; }
echo "Authenticated health and claims API checks passed."
