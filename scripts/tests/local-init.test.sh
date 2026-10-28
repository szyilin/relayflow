#!/usr/bin/env bash
# Real Docker integration tests; only resources created by this invocation are removed.
set -euo pipefail
umask 077
ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
PG_IMAGE='postgres@sha256:972eeb4e0a5fee4c3046cf868896719227e845aa9e38ff79a353efb3b2b2c10a'
MINIO_IMAGE='relayflow-minio:local'
MC_IMAGE='relayflow-mc:local'
scratch="$(mktemp -d "${TMPDIR:-/tmp}/relayflow-init-test.XXXXXX")"
project="relayflow-init-test-$(date +%s)-$$"
pg_name="$project-pg"
minio_name="$project-minio"
network="$project-net"
pg_volume="$project-pg-data"
minio_volume="$project-minio-data"
passed=0

cleanup() {
  docker rm -f "$pg_name" "$minio_name" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
  docker volume rm "$pg_volume" "$minio_volume" >/dev/null 2>&1 || true
  rm -rf -- "$scratch"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP
trap 'printf "Initialization test failed at line %s; project %s\n" "$LINENO" "$project" >&2; tail -n 12 "$scratch/latest.log" >&2' ERR
ok() { passed=$((passed + 1)); printf 'ok %s - %s\n' "$passed" "$1"; }
for required in docker curl sed grep; do command -v "$required" >/dev/null; done
for image in "$PG_IMAGE" "$MINIO_IMAGE" "$MC_IMAGE"; do
  docker image inspect "$image" >/dev/null || { printf 'Preload fixed integration image: %s\n' "$image" >&2; exit 1; }
done
: > "$scratch/latest.log"
docker network create --label "com.relayflow.test=$project" "$network" >/dev/null
docker volume create --label "com.relayflow.test=$project" "$pg_volume" >/dev/null
docker volume create --label "com.relayflow.test=$project" "$minio_volume" >/dev/null
start_servers() {
  docker run -d --name "$pg_name" --network "$network" --network-alias postgres \
  -e POSTGRES_USER=init_test -e POSTGRES_PASSWORD=init-test-password -e POSTGRES_DB=relayflow \
  -v "$pg_volume:/var/lib/postgresql/data" "$PG_IMAGE" >/dev/null
  docker run -d --name "$minio_name" --network "$network" --network-alias minio -p 127.0.0.1::9000 \
  -e MINIO_ROOT_USER=init_test -e MINIO_ROOT_PASSWORD=init-test-password \
  -v "$minio_volume:/data" "$MINIO_IMAGE" server /data >/dev/null
}
start_servers

wait_ready() {
  local attempt port
  for ((attempt=0; attempt<60; attempt++)); do
    port="$(docker port "$minio_name" 9000/tcp | cut -d: -f2)"
    if docker exec "$pg_name" pg_isready -q -U init_test -d relayflow \
      && curl --silent --fail "http://127.0.0.1:$port/minio/health/ready" >/dev/null; then return 0; fi
    sleep 1
  done
  return 1
}
run_minio() {
  docker run --rm --network "$network" \
    -e MINIO_ROOT_USER=init_test -e "MINIO_ROOT_PASSWORD=${1:-init-test-password}" \
    -e "LOCAL_WEB_PORT=${2:-18080}" -e LOCAL_INIT_TIMEOUT_SECONDS=10 \
    -v "$ROOT/deploy/minio:/init:ro" --entrypoint /bin/sh "$MC_IMAGE" /init/init-local.sh
}
run_xxl() {
  docker run --rm --network "$network" -e POSTGRES_HOST=postgres \
    -e POSTGRES_USER=init_test -e POSTGRES_PASSWORD=init-test-password -e POSTGRES_DB=relayflow \
    -e "XXL_JOB_DB_NAME=$1" -e LOCAL_INIT_TIMEOUT_SECONDS=20 \
    -v "${2:-$ROOT/deploy/xxl-job}:/xxl-job:ro" --entrypoint /bin/bash "$PG_IMAGE" /xxl-job/init-local.sh
}
sql() { docker exec "$pg_name" psql -X -qAt -v ON_ERROR_STOP=1 -U init_test -d "$1" -c "$2"; }
mc_run() {
  docker run --rm -i --network "$network" --entrypoint /bin/sh "$MC_IMAGE" -ec '
    mc alias set managed http://minio:9000 init_test init-test-password >/dev/null
    exec mc --no-color "$@"
  ' sh "$@"
}
snapshot() {
  sql "$1" "SELECT md5((SELECT jsonb_agg(to_jsonb(t) ORDER BY id)::text FROM xxl_job_info t)
    || (SELECT jsonb_agg(to_jsonb(t) ORDER BY id)::text FROM xxl_job_group t)
    || (SELECT jsonb_agg(to_jsonb(t) ORDER BY id)::text FROM xxl_job_user t)
    || (SELECT jsonb_agg(to_jsonb(t) ORDER BY component)::text FROM relayflow_local_bootstrap t)
    || (SELECT last_value::text FROM xxl_job_info_id_seq));"
}
expect_failure() { if "$@" > "$scratch/latest.log" 2>&1; then printf 'Expected failure\n' >&2; return 1; fi; }

wait_ready
mkdir -p "$scratch/fault"
cat > "$scratch/fault/mc" <<'MC'
#!/bin/sh
case " $* " in *' admin config set '*) exit 42 ;; esac
exec "$RELAYFLOW_TEST_MC_BINARY" "$@"
MC
chmod +x "$scratch/fault/mc"
run_minio_fault() {
  docker run --rm --network "$network" -e MINIO_ROOT_USER=init_test -e MINIO_ROOT_PASSWORD=init-test-password \
    -e LOCAL_INIT_TIMEOUT_SECONDS=10 -v "$ROOT/deploy/minio:/init:ro" -v "$scratch/fault:/fault:ro" \
    --entrypoint /bin/sh "$MC_IMAGE" -ec '
      RELAYFLOW_TEST_MC_BINARY="$(command -v mc)"; export RELAYFLOW_TEST_MC_BINARY
      PATH="/fault:$PATH"; export PATH
      exec /bin/sh /init/init-local.sh
    '
}
expect_failure run_minio_fault
mc_run stat managed/relayflow > /dev/null
printf 'partial-initialization-object\n' | mc_run pipe managed/relayflow/partial.txt > /dev/null
run_minio > "$scratch/latest.log" 2>&1
[[ "$(mc_run cat managed/relayflow/partial.txt)" == partial-initialization-object ]]
ok 'MinIO CORS-stage failure after bucket creation can retry without losing an existing object'
mc_run stat managed/relayflow > /dev/null
ok 'MinIO first install creates the private managed bucket'

printf 'preserve-object-content\n' | mc_run pipe managed/relayflow/existing.txt > /dev/null
mc_run tag set managed/relayflow preservation=keep > /dev/null
mc_run admin config set managed api requests_max=37 > /dev/null
run_minio > "$scratch/latest.log" 2>&1
[[ "$(mc_run cat managed/relayflow/existing.txt)" == preserve-object-content ]]
[[ "$(mc_run tag list managed/relayflow)" == *preservation* ]]
[[ "$(mc_run admin config get managed api)" == *requests_max=37* ]]
ok 'repeated bucket/CORS initialization preserves objects, bucket tags and other API settings'

minio_port="$(docker port "$minio_name" 9000/tcp | cut -d: -f2)"
for origin in http://localhost:18080 http://127.0.0.1:18080; do
  for method in PUT GET HEAD; do
    curl --silent --fail -D "$scratch/headers" -o /dev/null -X OPTIONS \
      -H "Origin: $origin" -H "Access-Control-Request-Method: $method" \
      -H 'Access-Control-Request-Headers: content-type' "http://127.0.0.1:$minio_port/relayflow/existing.txt"
    grep -qi "^Access-Control-Allow-Origin: $origin" "$scratch/headers"
    grep -qi "^Access-Control-Allow-Methods: $method" "$scratch/headers"
  done
done
curl --silent -D "$scratch/headers" -o /dev/null -X OPTIONS -H 'Origin: https://untrusted.example' \
  -H 'Access-Control-Request-Method: PUT' "http://127.0.0.1:$minio_port/relayflow/existing.txt"
if grep -qi '^Access-Control-Allow-Origin:' "$scratch/headers"; then exit 1; fi
[[ "$(curl --silent -o /dev/null -w '%{http_code}' "http://127.0.0.1:$minio_port/relayflow/existing.txt")" == 403 ]]
ok 'CORS preflights permit local PUT/GET/HEAD, reject foreign origins and keep anonymous reads denied'

share_output="$(mc_run share download --expire 1h managed/relayflow/existing.txt)"
share_url="$(printf '%s\n' "$share_output" | sed -n 's/^Share: *//p')"
[[ "$share_url" == http://minio:9000/* ]]
curl --silent --fail --noproxy '*' --connect-to "minio:9000:127.0.0.1:$minio_port" \
  -H 'Origin: http://localhost:18080' -D "$scratch/headers" "$share_url" > "$scratch/download"
[[ "$(cat "$scratch/download")" == preserve-object-content ]]
grep -qi '^Access-Control-Expose-Headers:.*ETag' "$scratch/headers"
ok 'signed object download retains exact bytes and exposes ETag to the browser origin'

expect_failure run_minio wrong-test-password
run_minio > "$scratch/latest.log" 2>&1
[[ "$(mc_run cat managed/relayflow/existing.txt)" == preserve-object-content ]]
ok 'failed MinIO authentication can be retried without deleting existing data'

run_xxl xxl_job > "$scratch/latest.log" 2>&1
[[ "$(sql xxl_job "SELECT count(*) FROM xxl_job_info;")" == 2 ]]
[[ "$(sql xxl_job "SELECT count(*) FROM relayflow_local_bootstrap WHERE component='xxl-job' AND version=1;")" == 1 ]]
sql xxl_job "UPDATE xxl_job_info SET schedule_conf='0 17 * * * ?', trigger_status=0, executor_param='keep-me';
 UPDATE xxl_job_group SET title='Custom', address_type=1, address_list='http://custom-executor:9999';
 UPDATE xxl_job_user SET password='changed-by-user';
 INSERT INTO xxl_job_info (job_group, job_desc, glue_type, executor_handler) VALUES (1,'custom task','BEAN','custom-handler');" >/dev/null
before="$(snapshot xxl_job)"
run_xxl xxl_job > "$scratch/latest.log" 2>&1
[[ "$before" == "$(snapshot xxl_job)" ]]
ok 'XXL repeat startup preserves cron, disabled state, executor settings, users and custom jobs'

mkdir -p "$scratch/broken-xxl"
cp -R "$ROOT/deploy/xxl-job/." "$scratch/broken-xxl/"
printf '\nSELECT 1 / 0;\n' >> "$scratch/broken-xxl/seed/02_jobs.sql"
expect_failure run_xxl retry_job "$scratch/broken-xxl"
[[ "$(sql retry_job "SELECT count(*) FROM pg_tables WHERE schemaname='public';")" == 0 ]]
run_xxl retry_job > "$scratch/latest.log" 2>&1
[[ "$(sql retry_job "SELECT count(*) FROM xxl_job_info;")" == 2 ]]
ok 'failed seed transaction leaves no partial tables or completion marker and retries successfully'

run_xxl concurrent_job > "$scratch/concurrent-1" 2>&1 & first_pid=$!
run_xxl concurrent_job > "$scratch/concurrent-2" 2>&1 & second_pid=$!
wait "$first_pid"
wait "$second_pid"
[[ "$(sql concurrent_job "SELECT count(*) FROM xxl_job_info;")" == 2 ]]
[[ "$(sql concurrent_job "SELECT count(*) FROM xxl_job_group;")" == 1 ]]
ok 'parallel first starts serialize database creation and seed initialization'

sql relayflow 'CREATE DATABASE existing_job;' >/dev/null
sql existing_job 'CREATE TABLE xxl_job_existing (note text); INSERT INTO xxl_job_existing VALUES ('\''preserve'\'');' >/dev/null
expect_failure run_xxl existing_job
[[ "$(sql existing_job 'SELECT note FROM xxl_job_existing;')" == preserve ]]
[[ "$(sql existing_job "SELECT to_regclass('relayflow_local_bootstrap') IS NULL;")" == t ]]
ok 'unmarked pre-existing scheduler data is refused instead of adopted or overwritten'

sql retry_job "UPDATE relayflow_local_bootstrap SET version=99 WHERE component='xxl-job';" >/dev/null
expect_failure run_xxl retry_job
[[ "$(sql retry_job "SELECT version FROM relayflow_local_bootstrap WHERE component='xxl-job';")" == 99 ]]
ok 'unsupported marker versions fail instead of silently reseeding'

sql concurrent_job 'ALTER TABLE xxl_job_log RENAME TO xxl_job_log_backup;' >/dev/null
expect_failure run_xxl concurrent_job
[[ "$(sql concurrent_job "SELECT to_regclass('xxl_job_log') IS NULL;")" == t ]]
ok 'missing initialized tables cause failure instead of automatic destructive repair'

expect_failure run_xxl relayflow
[[ "$(sql relayflow "SELECT count(*) FROM pg_tables WHERE schemaname='public';")" == 0 ]]
ok 'business database is rejected as a scheduler initialization target'

docker rm -f "$pg_name" "$minio_name" >/dev/null
start_servers
wait_ready
run_minio > "$scratch/latest.log" 2>&1
run_xxl xxl_job > "$scratch/latest.log" 2>&1
[[ "$before" == "$(snapshot xxl_job)" ]]
[[ "$(mc_run cat managed/relayflow/existing.txt)" == preserve-object-content ]]
[[ "$(mc_run admin config get managed api)" == *requests_max=37* ]]
ok 'container recreation with persistent volumes retains objects, API settings and modified schedules'
printf 'Passed %s real initialization checks.\n' "$passed"
