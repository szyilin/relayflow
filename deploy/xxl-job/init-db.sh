#!/bin/bash
# Create database xxl_job on the shared Postgres instance, then apply schema + official job seeds.
# Runs as a Compose oneshot (safe to re-run; SQL is idempotent).
set -euo pipefail

HOST="${POSTGRES_HOST:-postgres}"
PORT="${POSTGRES_PORT:-5432}"
USER="${POSTGRES_USER:-relayflow}"
PASSWORD="${POSTGRES_PASSWORD:-relayflow}"
DB_NAME="${XXL_JOB_DB_NAME:-xxl_job}"

export PGPASSWORD="${PASSWORD}"

echo "Waiting for Postgres at ${HOST}:${PORT}..."
until pg_isready -h "${HOST}" -p "${PORT}" -U "${USER}" >/dev/null 2>&1; do
  sleep 1
done

EXISTS="$(psql -h "${HOST}" -p "${PORT}" -U "${USER}" -d postgres -Atc \
  "SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}'")"
if [[ "${EXISTS}" != "1" ]]; then
  echo "Creating database ${DB_NAME}..."
  psql -h "${HOST}" -p "${PORT}" -U "${USER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE DATABASE ${DB_NAME} OWNER ${USER};"
else
  echo "Database ${DB_NAME} already exists."
fi

echo "Applying XXL-JOB schema..."
psql -h "${HOST}" -p "${PORT}" -U "${USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 \
  -f /xxl-job/schema/01_tables_xxl_job.sql

echo "Applying official executor + job seeds..."
psql -h "${HOST}" -p "${PORT}" -U "${USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 \
  -f /xxl-job/seed/01_executor.sql
psql -h "${HOST}" -p "${PORT}" -U "${USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 \
  -f /xxl-job/seed/02_jobs.sql

echo "XXL-JOB Postgres init done."
