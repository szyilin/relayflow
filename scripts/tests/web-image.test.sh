#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
image="${1:-relayflow-web:local}"
node_image='node@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53'
project="relayflow-proxy-test-$$-$(date +%s)"
cleanup() {
  docker rm -f "$project-web" "$project-server" >/dev/null 2>&1 || true
  docker network rm "$project" >/dev/null 2>&1 || true
}
trap cleanup EXIT
docker network create "$project" >/dev/null
docker run -d --name "$project-server" --network "$project" --network-alias server \
  --mount "type=bind,src=$ROOT/scripts/tests/fixtures,dst=/fixture,readonly" \
  "$node_image" node /fixture/proxy-upstream.mjs >/dev/null
docker run -d --name "$project-web" --network "$project" --network-alias web "$image" >/dev/null
for ((attempt=0; attempt<30; attempt++)); do
  state="$(docker inspect "$project-web" --format '{{.State.Health.Status}}')"
  [[ "$state" != healthy ]] || break
  sleep 1
done
[[ "$state" == healthy ]]
docker run --rm --network "$project" \
  --mount "type=bind,src=$ROOT/scripts/tests/fixtures,dst=/fixture,readonly" \
  "$node_image" node /fixture/web-proxy-check.mjs
if docker logs "$project-web" 2>&1 | grep -q test-log-redaction-token; then
  printf 'Web access log leaked WS query token\n' >&2; exit 1
fi
printf 'ok - access logs omit query tokens\n'
