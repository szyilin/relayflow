#!/usr/bin/env bash
# Realign flyway_schema_history after squashing migrations (strategy A).
# - DELETE absorbed version rows
# - UPDATE checksum for the kept version to match the rewritten SQL file
#
# Usage:
#   repair_history.sh \
#     --url jdbc:postgresql://localhost:5432/relayflow \
#     --user relayflow \
#     --password relayflow \
#     --migrations-dir /path/to/db/migration \
#     --absorb 0.1.0.1,0.1.0.2 \
#     --keep 0.1.0.3
#
# Optional: --dry-run

set -euo pipefail

URL=""
USER_NAME=""
PASSWORD=""
MIGRATIONS_DIR=""
ABSORB=""
KEEP=""
DRY_RUN=0

usage() {
  sed -n '2,16p' "$0" | sed 's/^# \?//'
  exit "${1:-1}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url) URL="${2:-}"; shift 2 ;;
    --user) USER_NAME="${2:-}"; shift 2 ;;
    --password) PASSWORD="${2:-}"; shift 2 ;;
    --migrations-dir) MIGRATIONS_DIR="${2:-}"; shift 2 ;;
    --absorb) ABSORB="${2:-}"; shift 2 ;;
    --keep) KEEP="${2:-}"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage 0 ;;
    *) echo "Unknown arg: $1" >&2; usage ;;
  esac
done

[[ -n "$URL" && -n "$USER_NAME" && -n "$MIGRATIONS_DIR" && -n "$KEEP" ]] || usage
[[ -d "$MIGRATIONS_DIR" ]] || { echo "migrations dir not found: $MIGRATIONS_DIR" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKSUM_PY="$SCRIPT_DIR/flyway_checksum.py"

# jdbc:postgresql://host:port/db → psql conn bits
if [[ "$URL" != jdbc:postgresql://* ]]; then
  echo "Only jdbc:postgresql URLs are supported, got: $URL" >&2
  exit 1
fi
REST="${URL#jdbc:postgresql://}"
HOSTPORT="${REST%%/*}"
DBNAME="${REST#*/}"
DBNAME="${DBNAME%%\?*}"
HOST="${HOSTPORT%%:*}"
PORT="${HOSTPORT##*:}"
if [[ "$HOST" == "$PORT" ]]; then
  PORT=5432
fi

KEEP_FILE=""
shopt -s nullglob
for f in "$MIGRATIONS_DIR"/V"${KEEP}"__*.sql; do
  KEEP_FILE="$f"
  break
done
shopt -u nullglob
[[ -n "$KEEP_FILE" && -f "$KEEP_FILE" ]] || {
  echo "Keep migration file not found for version $KEEP under $MIGRATIONS_DIR" >&2
  exit 1
}

CHECKSUM="$(python3 "$CHECKSUM_PY" "$KEEP_FILE")"

# Build IN list for absorb versions
ABSORB_SQL="NULL"
if [[ -n "$ABSORB" ]]; then
  ABSORB_SQL=""
  IFS=',' read -r -a VERS <<< "$ABSORB"
  for v in "${VERS[@]}"; do
    v_trimmed="$(echo "$v" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    [[ -z "$v_trimmed" ]] && continue
    if [[ -n "$ABSORB_SQL" ]]; then
      ABSORB_SQL+=", "
    fi
    ABSORB_SQL+="'${v_trimmed}'"
  done
  [[ -n "$ABSORB_SQL" ]] || ABSORB_SQL="NULL"
fi

echo "Host=$HOST Port=$PORT Db=$DBNAME"
echo "Delete versions: $ABSORB_SQL"
echo "Update checksum: version=$KEEP file=$(basename "$KEEP_FILE") checksum=$CHECKSUM"

SQL=$(cat <<EOF
BEGIN;
DELETE FROM flyway_schema_history
WHERE version IN (${ABSORB_SQL});
UPDATE flyway_schema_history
SET checksum = ${CHECKSUM},
    script = '$(basename "$KEEP_FILE")'
WHERE version = '${KEEP}';
COMMIT;
SELECT version, checksum, success, script
FROM flyway_schema_history
WHERE version = '${KEEP}' OR version IN (${ABSORB_SQL})
ORDER BY installed_rank;
EOF
)

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "----- dry-run SQL -----"
  echo "$SQL"
  exit 0
fi

export PGPASSWORD="$PASSWORD"
psql -h "$HOST" -p "$PORT" -U "$USER_NAME" -d "$DBNAME" -v ON_ERROR_STOP=1 -c "$SQL"
echo "repair_history: done"
