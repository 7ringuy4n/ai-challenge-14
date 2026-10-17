#!/bin/sh
set -eu

: "${DB_PASSWORD:?DB_PASSWORD must be provided by Compose}"

# Pass the secret as a psql variable; never interpolate it into SQL or log it.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=runtime_password="$DB_PASSWORD" \
  --set=ON_ERROR_STOP=1 <<'SQL'
SELECT format('CREATE ROLE claims_app LOGIN PASSWORD %L', :'runtime_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'claims_app')
\gexec
SQL
