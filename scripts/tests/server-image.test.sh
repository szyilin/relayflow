#!/usr/bin/env bash
# Verify the built image without starting the application or contacting infrastructure.
set -euo pipefail
image="${1:-relayflow-server:local}"
scratch="$(mktemp -d "${TMPDIR:-/tmp}/relayflow-server-image-test.XXXXXX")"
trap 'rm -rf -- "$scratch"' EXIT
chmod 755 "$scratch"

docker run --rm --network none --entrypoint /bin/sh "$image" -ec '
  test "$(id -u)" = 10001
  test -r /app/app.jar
  test -w /app/logs/xxl-job/jobhandler
  command -v wget >/dev/null
  command -v grep >/dev/null
  java -version 2>&1 | grep -q "version \"21\."
  unzip -p /app/app.jar BOOT-INF/classes/application-local-docker.yml > /tmp/local-profile.yml
  grep -q "http://livekit:7880" /tmp/local-profile.yml
  grep -q "http://server:" /tmp/local-profile.yml
  test ! -e /workspace
  test ! -e /root/.m2/settings.xml
'
printf 'ok 1 - non-root Java 21 runtime, writable logs and packaged local profile\n'

health_command="$(docker image inspect "$image" --format '{{index .Config.Healthcheck.Test 1}}')"
cat > "$scratch/wget" <<'SH'
#!/bin/sh
printf '%s' "$HEALTH_BODY"
exit "$WGET_STATUS"
SH
chmod 755 "$scratch/wget"
check_health() {
  docker run --rm --network none --mount "type=bind,src=$scratch,dst=/fixture,readonly" \
    -e "HEALTH_BODY=$1" -e "WGET_STATUS=$2" -e "HEALTH_COMMAND=$health_command" \
    --entrypoint /bin/sh "$image" -ec 'export PATH="/fixture:$PATH"; exec /bin/sh -c "$HEALTH_COMMAND"'
}
check_health '{"code":0,"data":{}}' 0
printf 'ok 2 - health check accepts business success\n'
for body in '{"code":500,"msg":"failed"}' '{"code":10}' '<html>error</html>'; do
  if check_health "$body" 0; then printf 'Health check accepted an error response\n' >&2; exit 1; fi
done
printf 'ok 3 - health check rejects business errors and HTML\n'
if check_health '{"code":0}' 1; then printf 'Health check ignored HTTP transport failure\n' >&2; exit 1; fi
printf 'ok 4 - health check rejects transport failure even with a success-looking body\n'
