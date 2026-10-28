#!/usr/bin/env bash
# Isolated behavior tests. The fake Docker never contacts a real daemon.
set -euo pipefail
umask 077
SOURCE_ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/relayflow-config-test.XXXXXX")"
trap 'rm -rf -- "$TEST_DIR"' EXIT
trap 'printf "Failed at test line %s\n" "$LINENO" >&2; cat "$TEST_DIR/output" >&2' ERR
mkdir -p "$TEST_DIR/repo with spaces/scripts" "$TEST_DIR/repo with spaces/deploy" "$TEST_DIR/bin"
cp "$SOURCE_ROOT/scripts/local-config.sh" "$TEST_DIR/repo with spaces/scripts/"
cp "$SOURCE_ROOT/deploy/local.env.example" "$TEST_DIR/repo with spaces/deploy/"
SCRIPT="$TEST_DIR/repo with spaces/scripts/local-config.sh"
export DOCKER_CALLS="$TEST_DIR/docker-calls"
export FIXTURE="$TEST_DIR/fixture"
export TEST_SIGNAL_READY="$TEST_DIR/signal-ready"
export TEST_PUBLISH_TARGET=''
unset COMPOSE_PROJECT_NAME RELAYFLOW_LOCAL_ENV_FILE || true
for key in POSTGRES_PASSWORD MINIO_ROOT_PASSWORD RELAYFLOW_JWT_SECRET XXL_JOB_ACCESS_TOKEN RELAYFLOW_LIVEKIT_API_SECRET; do
  printf '%s=%064d\n' "$key" 0
done > "$FIXTURE"
printf 'MINIO_ROOT_USER=rf0000000000000000\nRELAYFLOW_LIVEKIT_API_KEY=%032d\n' 0 >> "$FIXTURE"
printf 'RELAYFLOW_ENCRYPT_AES_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\n' >> "$FIXTURE"

cat > "$TEST_DIR/bin/docker" <<'DOCKER'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$DOCKER_CALLS"
case "$1" in
  info) [[ "${DOCKER_MODE:-}" != unavailable ]] ;;
  volume)
    [[ "${DOCKER_MODE:-}" != list-fails ]] || exit 1
    if [[ "${DOCKER_MODE:-}" == old-volume ]]; then printf 'isolated-project-data\n'; fi ;;
  ps)
    if [[ "${DOCKER_MODE:-}" == old-container ]]; then printf 'isolated-container\n'; fi ;;
  image) [[ "${DOCKER_MODE:-}" != missing-image ]] ;;
  pull) exit 1 ;;
  run)
    case "${DOCKER_MODE:-}" in
      partial-fails) printf 'POSTGRES_PASSWORD=partial\n'; exit 1 ;;
      signal) touch "$TEST_SIGNAL_READY"; sleep 2; cat "$FIXTURE" ;;
      slow) sleep 1; cat "$FIXTURE" ;;
      publish-race) printf 'external-file\n' > "$TEST_PUBLISH_TARGET"; cat "$FIXTURE" ;;
      publish-directory) mkdir "$TEST_PUBLISH_TARGET"; cat "$FIXTURE" ;;
      publish-symlink-directory) ln -s "$(dirname "$TEST_PUBLISH_TARGET")" "$TEST_PUBLISH_TARGET"; cat "$FIXTURE" ;;
      *) cat "$FIXTURE" ;;
    esac ;;
  *) exit 1 ;;
esac
DOCKER
chmod +x "$TEST_DIR/bin/docker"
export PATH="$TEST_DIR/bin:$PATH"
passed=0
ok() { passed=$((passed + 1)); printf 'ok %s - %s\n' "$passed" "$1"; }
expect_failure() {
  if "$@" > "$TEST_DIR/output" 2>&1; then printf 'Expected failure\n' >&2; exit 1; fi
}
assert_clean() {
  [[ ! -e "$RELAYFLOW_LOCAL_ENV_FILE" ]]
  [[ -z "$(find "$(dirname "$RELAYFLOW_LOCAL_ENV_FILE")" -name '*.tmp.*' -print)" ]]
  [[ ! -d "$TEST_DIR/repo with spaces/.relayflow/config-locks/$COMPOSE_PROJECT_NAME.lock" ]]
}
new_case() {
  export COMPOSE_PROJECT_NAME="test-$1"
  export RELAYFLOW_LOCAL_ENV_FILE="$TEST_DIR/cases/$1/local.env"
  export DOCKER_MODE=''
  : > "$DOCKER_CALLS"
}

new_case first
(cd / && "$SCRIPT" --no-pull) > "$TEST_DIR/output"
[[ -f "$RELAYFLOW_LOCAL_ENV_FILE" && ! -L "$RELAYFLOW_LOCAL_ENV_FILE" ]]
[[ "$(ls -l "$RELAYFLOW_LOCAL_ENV_FILE" | cut -c1-10)" == '-rw-------' ]]
"$SCRIPT" --check > /dev/null
if rg -q 'POSTGRES_PASSWORD=|RELAYFLOW_ENCRYPT_AES_KEY=' "$TEST_DIR/output"; then exit 1; fi
cp "$RELAYFLOW_LOCAL_ENV_FILE" "$TEST_DIR/original"
rg -q 'run --rm --network none --read-only --cap-drop ALL --security-opt no-new-privileges alpine@sha256:' "$DOCKER_CALLS"
rg -q "label=com.docker.compose.project=$COMPOSE_PROJECT_NAME" "$DOCKER_CALLS"
ok 'first generation, permissions, pinned isolated tool and path with spaces'

export DOCKER_MODE=unavailable
: > "$DOCKER_CALLS"
"$SCRIPT" > /dev/null
cmp "$RELAYFLOW_LOCAL_ENV_FILE" "$TEST_DIR/original"
[[ ! -s "$DOCKER_CALLS" ]]
ok 'existing complete configuration is reused byte for byte without random regeneration'

cp "$TEST_DIR/original" "$TEST_DIR/invalid.env"
export RELAYFLOW_LOCAL_ENV_FILE="$TEST_DIR/invalid.env"
printf 'POSTGRES_PASSWORD=invalid\n' >> "$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT"
rg -q '无效|重复' "$TEST_DIR/output"
ok 'invalid configuration is rejected without repairing secrets'

cp "$TEST_DIR/original" "$RELAYFLOW_LOCAL_ENV_FILE"
printf 'LOCAL_WEB_PORT=18080\n' >> "$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT" --check
rg -q '重复' "$TEST_DIR/output"
ok 'duplicate dotenv keys are rejected'

cp "$TEST_DIR/original" "$RELAYFLOW_LOCAL_ENV_FILE"
printf 'UNTRUSTED=$(touch %s)\n' "$TEST_DIR/must-not-exist" >> "$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT" --check
[[ ! -e "$TEST_DIR/must-not-exist" ]]
ok 'dotenv shell/interpolation content is never executed'

sed '/^RELAYFLOW_JWT_SECRET=/d' "$TEST_DIR/original" > "$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT"
rg -q '缺少 RELAYFLOW_JWT_SECRET' "$TEST_DIR/output"
ok 'missing secret never triggers automatic regeneration'

for scenario in old-volume old-container unavailable list-fails partial-fails missing-image; do
  new_case "$scenario"
  export DOCKER_MODE="$scenario"
  expect_failure "$SCRIPT" --no-pull
  assert_clean
  ok "$scenario fails closed without partial configuration or stale lock"
done

new_case symlink
mkdir -p "$(dirname "$RELAYFLOW_LOCAL_ENV_FILE")"
ln -s "$TEST_DIR/original" "$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT"
cmp "$TEST_DIR/original" "$TEST_DIR/cases/first/local.env"
ok 'symlink is rejected and its target is not modified'

new_case concurrent
export DOCKER_MODE=slow
"$SCRIPT" --no-pull > "$TEST_DIR/concurrent-1" 2>&1 &
first_pid=$!
"$SCRIPT" --no-pull > "$TEST_DIR/concurrent-2" 2>&1 &
second_pid=$!
wait "$first_pid"
wait "$second_pid"
[[ "$(rg -c '^run ' "$DOCKER_CALLS")" == 1 ]]
"$SCRIPT" --check > /dev/null
ok 'concurrent same-project generation runs the random tool only once'

export RELAYFLOW_LOCAL_ENV_FILE="$TEST_DIR/cases/another-path/local.env"
expect_failure "$SCRIPT"
rg -q '另一配置路径' "$TEST_DIR/output"
[[ ! -e "$RELAYFLOW_LOCAL_ENV_FILE" ]]
ok 'same project cannot silently acquire a second configuration path'

new_case publish-race
export DOCKER_MODE=publish-race TEST_PUBLISH_TARGET="$RELAYFLOW_LOCAL_ENV_FILE"
expect_failure "$SCRIPT" --no-pull
[[ "$(cat "$RELAYFLOW_LOCAL_ENV_FILE")" == external-file ]]
[[ -z "$(find "$(dirname "$RELAYFLOW_LOCAL_ENV_FILE")" -name '*.tmp.*' -print)" ]]
ok 'atomic publication never overwrites a file created during generation'

for scenario in publish-directory publish-symlink-directory; do
  new_case "$scenario"
  export DOCKER_MODE="$scenario" TEST_PUBLISH_TARGET="$RELAYFLOW_LOCAL_ENV_FILE"
  expect_failure "$SCRIPT" --no-pull
  [[ -z "$(find "$(dirname "$RELAYFLOW_LOCAL_ENV_FILE")" -name '*.tmp.*' -print)" ]]
  ok "$scenario is rejected without publishing a secret into the directory"
done

new_case unignored
export RELAYFLOW_LOCAL_ENV_FILE="$TEST_DIR/repo with spaces/deploy/private-settings"
expect_failure "$SCRIPT" --no-pull
[[ ! -e "$RELAYFLOW_LOCAL_ENV_FILE" && ! -s "$DOCKER_CALLS" ]]
ok 'custom in-repository secret paths outside .relayflow are rejected'

new_case ignored
export RELAYFLOW_LOCAL_ENV_FILE="$TEST_DIR/repo with spaces/.relayflow/custom.env"
"$SCRIPT" --no-pull > /dev/null
"$SCRIPT" --check > /dev/null
ok 'custom in-repository configuration remains inside the ignored runtime directory'

new_case signal
export DOCKER_MODE=signal
"$SCRIPT" --no-pull > "$TEST_DIR/signal-output" 2>&1 &
signal_pid=$!
for ((attempt=0; attempt<50; attempt++)); do
  [[ ! -e "$TEST_SIGNAL_READY" ]] || break
  sleep 0.1
done
[[ -e "$TEST_SIGNAL_READY" ]]
kill -TERM "$signal_pid"
if wait "$signal_pid"; then exit 1; fi
assert_clean
ok 'termination cleans owned temporary configuration and project lock'

printf 'Passed %s isolated configuration checks.\n' "$passed"
