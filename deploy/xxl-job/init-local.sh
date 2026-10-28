#!/usr/bin/env bash
# Local-only first install. The legacy init-db.sh continues to apply official seeds on demand.
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
fail() { printf '[xxl-job-init] %s\n' "$*" >&2; exit 1; }
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
db_name="${XXL_JOB_DB_NAME:-xxl_job}"
init_timeout="${LOCAL_INIT_TIMEOUT_SECONDS:-60}"
[[ "$db_name" =~ ^[a-z][a-z0-9_]{0,62}$ ]] || fail 'Invalid scheduler database name.'
[[ "$db_name" != postgres && "$db_name" != template0 && "$db_name" != template1 \
  && "$db_name" != "${POSTGRES_DB:-relayflow}" ]] || fail 'Scheduler must use its own database.'
[[ "$init_timeout" =~ ^[1-9][0-9]{0,3}$ ]] && ((10#$init_timeout <= 3600)) || fail 'Invalid initialization timeout.'
export PGHOST="${POSTGRES_HOST:-postgres}" PGPORT="${POSTGRES_PORT:-5432}"
export PGUSER="$POSTGRES_USER" PGPASSWORD="$POSTGRES_PASSWORD" PGCONNECT_TIMEOUT=5

deadline=$((SECONDS + init_timeout))
until pg_isready -q -d postgres; do
  ((SECONDS < deadline)) || fail 'PostgreSQL readiness timed out.'
  sleep 1
done

# A session lock serializes CREATE DATABASE, which cannot be inside a transaction.
# psql literal/identifier quoting protects supplied names; never interpolate shell values into SQL.
psql -X --no-password -q -d postgres -v ON_ERROR_STOP=1 \
  -v target_db="$db_name" -v db_owner="$POSTGRES_USER" -v init_timeout="${init_timeout}s" <<'SQL'
\o /dev/null
SET lock_timeout TO :'init_timeout';
SET statement_timeout TO :'init_timeout';
SELECT pg_advisory_lock(hashtextextended('relayflow.local.create:' || :'target_db', 0));
SELECT format('CREATE DATABASE %I OWNER %I', :'target_db', :'db_owner')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'target_db')
\gexec
SQL

psql -X --no-password -q -d "$db_name" -v ON_ERROR_STOP=1 \
  -v init_timeout="${init_timeout}s" -f "$script_dir/init-local.sql"
