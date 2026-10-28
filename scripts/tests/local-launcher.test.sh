#!/usr/bin/env bash
# Fault injection at the Docker boundary; no daemon or user resources are used.
set -euo pipefail
umask 077
ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
temp="$(mktemp -d "${TMPDIR:-/tmp}/relayflow-launcher-test.XXXXXX")"
trap 'rm -rf -- "$temp"' EXIT
trap 'cat "$temp/output" >&2' ERR
mkdir -p "$temp/repo/scripts/lib" "$temp/repo/deploy" "$temp/bin"
cp "$ROOT/scripts/"{local-config,start-local,stop-local,local-status}.sh "$temp/repo/scripts/"
cp "$ROOT/scripts/lib/local-compose.sh" "$temp/repo/scripts/lib/"
cp "$ROOT/deploy/local.env.example" "$temp/repo/deploy/"
export TEST_CALLS="$temp/calls" TEST_FIXTURE="$temp/secrets"
for key in POSTGRES_PASSWORD MINIO_ROOT_PASSWORD RELAYFLOW_JWT_SECRET XXL_JOB_ACCESS_TOKEN RELAYFLOW_LIVEKIT_API_SECRET; do
  printf '%s=%064d\n' "$key" 0
done > "$TEST_FIXTURE"
printf 'MINIO_ROOT_USER=rf0000000000000000\nRELAYFLOW_LIVEKIT_API_KEY=%032d\nRELAYFLOW_ENCRYPT_AES_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\n' 0 >> "$TEST_FIXTURE"
cat > "$temp/bin/docker" <<'DOCKER'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$TEST_CALLS"
case "$1" in
 info) [[ "${MODE:-}" != daemon ]] ;;
 volume) : ;;
 ps) [[ "${MODE:-}" != owned ]] || printf 'own-container\n' ;;
 inspect) printf '18080/80/tcp\n19000/9000/tcp\n7880/7880/tcp\n7881/7881/tcp\n7882/7882/udp\n' ;;
 image) [[ "${MODE:-}" != missing-image || "$3" != missing-application ]] ;;
 run)
   if [[ "$*" == *'/dev/urandom'* ]]; then cat "$TEST_FIXTURE"
   elif [[ "$*" == *' -p '* ]]; then [[ "${MODE:-}" != conflict && "${MODE:-}" != owned ]]
   fi ;;
 compose)
   if [[ "$2" == version ]]; then printf '%s\n' "${VERSION:-2.24.0}"; exit 0; fi
   [[ -z "${POSTGRES_PASSWORD:-}${LOCAL_WEB_PORT:-}${COMPOSE_FILE:-}" ]] || exit 91
   case "$*" in
     *'config --images'*) printf 'missing-application\n' ;;
     *'up -d'* )
       if [[ "${MODE:-}" == init-fails || "${MODE:-}" == timeout ]]; then
         printf 'service minio-init failed password=%064d Bearer eyJabcdefghijk.abcdefghijk.abcdefghijk https://localhost/?X-Amz-Signature=secret\n' 0
         exit 12
       fi ;;
     *'logs --no-color'*) printf '{"apiKey":"fixture-key","password":"fixture-password"}\n' ;;
   esac ;;
 *) exit 95 ;;
esac
DOCKER
chmod +x "$temp/bin/docker"
export PATH="$temp/bin:$PATH"
unset COMPOSE_PROJECT_NAME RELAYFLOW_LOCAL_ENV_FILE RELAYFLOW_MAVEN_SETTINGS_FILE || true
start="$temp/repo/scripts/start-local.sh"
stop="$temp/repo/scripts/stop-local.sh"
passed=0
ok() { passed=$((passed+1)); printf 'ok %s - %s\n' "$passed" "$1"; }
reject() { if "$start" --no-build > "$temp/output" 2>&1; then cat "$temp/output"; exit 1; fi; grep -q "$1" "$temp/output" || { cat "$temp/output" >&2; tail -10 "$TEST_CALLS" >&2; return 1; }; }
export MODE=daemon
reject 'Docker daemon 不可用'; ok 'daemon unavailable fails before mutation'
export MODE='' VERSION=2.23.0
reject '需要 Compose'; ok 'unsupported Compose version rejected'
export VERSION=2.24.0
"$start" --no-build > "$temp/output" 2>&1
config="$temp/repo/.relayflow/local.env"
cp "$config" "$temp/original"
ok 'first start and bounded wait contract'
grep -q -- '--wait --wait-timeout 900' "$TEST_CALLS"
export MODE=conflict
reject '端口'; ok 'external port conflict rejected'
export MODE=owned
"$start" --no-build > "$temp/output" 2>&1
ok 'own published ports accepted on repeat startup'
export MODE=missing-image
reject '缺少镜像 missing-application'; ok 'no-build lists missing image without pulling/building'
export MODE=init-fails
reject '阶段失败：startup'
! grep -q '00000000000000000000000000000000\|fixture-key\|fixture-password\|Signature=secret\|Bearer eyJ' "$temp/output"
grep -q 'minio-init' "$temp/output"
ok 'failed init returns nonzero with redacted service diagnostics'
export MODE=timeout
reject '阶段失败：startup'; ok 'readiness timeout is a failure and retains configuration'
export MODE=''
POSTGRES_PASSWORD=host-secret LOCAL_WEB_PORT=12345 COMPOSE_FILE=host.yml "$start" --no-build > "$temp/output" 2>&1
ok 'shell interpolation cannot override validated persisted configuration'
"$stop" > "$temp/output" 2>&1
cmp "$config" "$temp/original"
grep -q 'down --remove-orphans' "$TEST_CALLS"
! grep -Eq 'down .* (-v|--volumes)' "$TEST_CALLS"
ok 'stop preserves exact env and never requests volume removal'
printf 'Passed %s isolated launcher checks.\n' "$passed"
