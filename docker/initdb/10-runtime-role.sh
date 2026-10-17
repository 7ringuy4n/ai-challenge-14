#!/bin/sh
set -eu

# The official image runs init scripts only on first database creation.
# psql variable quoting safely handles generated passwords without logging them.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=migration_user="$CLAIMS_MIGRATION_DB_USER" \
  --set=migration_password="$CLAIMS_MIGRATION_DB_PASSWORD" \
  --set=runtime_user="$CLAIMS_RUNTIME_DB_USER" \
  --set=runtime_password="$CLAIMS_RUNTIME_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', :'migration_user', :'migration_password')
\gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', :'runtime_user', :'runtime_password')
\gexec
ALTER DATABASE claims OWNER TO :"migration_user";
ALTER SCHEMA public OWNER TO :"migration_user";
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE claims TO :"migration_user", :"runtime_user";
GRANT USAGE, CREATE ON SCHEMA public TO :"migration_user";
SQL

