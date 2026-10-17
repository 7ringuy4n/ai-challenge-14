#!/bin/sh
set -eu

: "${DB_PASSWORD:?DB_PASSWORD must be provided by Compose}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be provided by Compose}"

# Local socket authentication is managed by the official Postgres image. Bind both
# current secrets as psql variables so values never become executable SQL or output.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=runtime_password="$DB_PASSWORD" \
  --set=migration_password="$POSTGRES_PASSWORD" \
  --set=ON_ERROR_STOP=1 <<'SQL'
SELECT format('CREATE ROLE claims_app LOGIN PASSWORD %L', :'runtime_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'claims_app')
\gexec
ALTER ROLE claims_app PASSWORD :'runtime_password';
ALTER ROLE claims_owner PASSWORD :'migration_password';
SQL
