#!/usr/bin/env bash
set -euo pipefail

env_file=.env
example=.env.example
if [[ ! -f "$env_file" ]]; then
  cp "$example" "$env_file"
  chmod 600 "$env_file"
fi

replace_dummy() {
  local key=$1 value
  value=$(awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; found=1; exit } END { if (!found) exit 1 }' "$env_file") || {
    echo "Missing $key in .env" >&2
    return 1
  }
  if [[ "$value" == CHANGE_ME_* || -z "$value" ]]; then
    value=$(openssl rand -hex 32)
    sed -i "s|^${key}=.*|${key}=${value}|" "$env_file"
  fi
}

replace_dummy CLAIMS_BOOTSTRAP_DB_PASSWORD
replace_dummy CLAIMS_RUNTIME_DB_PASSWORD
replace_dummy CLAIMS_MIGRATION_DB_PASSWORD
replace_dummy CLAIMS_DEMO_PASSWORD
replace_dummy CLAIMS_SYSTEM_PASSWORD
chmod 600 "$env_file"

compose=(docker compose --env-file "$env_file" -f docker/compose.yml)
"${compose[@]}" config --quiet
"${compose[@]}" up -d --build --wait --wait-timeout 180
echo "Claims services are healthy. Run bash scripts/smoke.sh to verify the API."
