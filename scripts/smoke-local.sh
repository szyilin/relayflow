#!/usr/bin/env bash
# Creates a fresh, isolated project. Never operates on the default user project.
# Keeps its config/data for diagnosis; prints the exact stop command on completion.
set -Eeuo pipefail
umask 077
ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
project="relayflow-smoke-$(date +%s)-$$"
export COMPOSE_PROJECT_NAME="$project"
export RELAYFLOW_LOCAL_ENV_FILE="$ROOT/.relayflow/smoke/$project/local.env"
mkdir -p "$(dirname "$RELAYFLOW_LOCAL_ENV_FILE")"
case "${1:-}" in
  --no-build|'') ;;
  *) printf 'Usage: ./scripts/smoke-local.sh [--no-build]\n' >&2; exit 1 ;;
esac
[[ $# -le 1 ]]
"$ROOT/scripts/local-config.sh" ${1:+--no-pull}
# Alternative ports avoid the ordinary local project. A conflict fails safely.
config_tmp="$RELAYFLOW_LOCAL_ENV_FILE.ports"
sed -e 's/^LOCAL_WEB_PORT=.*/LOCAL_WEB_PORT=38080/' \
    -e 's/^MINIO_API_PORT=.*/MINIO_API_PORT=39000/' \
    -e 's/^LIVEKIT_PORT=.*/LIVEKIT_PORT=37880/' \
    -e 's/^LIVEKIT_RTC_PORT=.*/LIVEKIT_RTC_PORT=37881/' \
    -e 's/^LIVEKIT_UDP_PORT=.*/LIVEKIT_UDP_PORT=37882/' "$RELAYFLOW_LOCAL_ENV_FILE" > "$config_tmp"
mv "$config_tmp" "$RELAYFLOW_LOCAL_ENV_FILE"
cp "$RELAYFLOW_LOCAL_ENV_FILE" "$RELAYFLOW_LOCAL_ENV_FILE.before"
source "$ROOT/scripts/lib/local-compose.sh"
trap rf_cleanup EXIT
trap 'rf_failed "$?"' ERR
rf_require_docker
rf_load
RF_STAGE=smoke
if ! docker image inspect "$RF_NODE_IMAGE" >/dev/null 2>&1; then
  [[ "${1:-}" != --no-build ]] || rf_die '缺少固定 Node 验收工具镜像；--no-build 不下载工具。'
  docker pull "$RF_NODE_IMAGE"
fi
run_checks() {
  docker run --rm --pull never --network "${RF_PROJECT}_default" \
    --mount "type=bind,src=$ROOT/scripts/tests/fixtures,dst=/fixture,readonly" \
    --mount "type=bind,src=$(dirname "$RF_CONFIG"),dst=/state" \
    "$RF_NODE_IMAGE" node /fixture/local-smoke.mjs "$1" 2>&1 | rf_redact | tee -a "$RF_LOG"
}
scheduler() {
  rf_compose exec -T postgres sh -ec 'exec psql -X --no-password -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d xxl_job -At' "$@"
}
"$ROOT/scripts/start-local.sh" "$@"
run_checks create
scheduler <<'SQL'
UPDATE xxl_job_info SET schedule_conf='0 17 3 * * ?', trigger_status=0 WHERE id=1;
UPDATE xxl_job_group SET address_type=1,address_list='http://smoke-preserved:9999/' WHERE id=1;
SQL
scheduler <<'SQL' > "$RF_CONFIG.scheduler.before"
SELECT id,schedule_conf,trigger_status FROM xxl_job_info ORDER BY id;
SELECT id,address_type,address_list FROM xxl_job_group ORDER BY id;
SQL
"$ROOT/scripts/start-local.sh" --no-build
run_checks verify
"$ROOT/scripts/stop-local.sh"
cmp "$RF_CONFIG" "$RF_CONFIG.before"
"$ROOT/scripts/start-local.sh" --no-build
run_checks verify
scheduler <<'SQL' > "$RF_CONFIG.scheduler.after"
SELECT id,schedule_conf,trigger_status FROM xxl_job_info ORDER BY id;
SELECT id,address_type,address_list FROM xxl_job_group ORDER BY id;
SQL
cmp "$RF_CONFIG" "$RF_CONFIG.before"
cmp "$RF_CONFIG.scheduler.before" "$RF_CONFIG.scheduler.after"
printf 'ok - repeat startup and stop/recreate preserve config, files, messages and scheduler settings\n'
"$ROOT/scripts/stop-local.sh"
printf '[smoke] Passed; retained only this test project data/config: %s\n' "$RF_CONFIG"
