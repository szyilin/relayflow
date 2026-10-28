#!/bin/sh
# Local managed MinIO only. Never deletes objects or changes anonymous policies.
set -eu
umask 077

fail() { printf '[minio-init] %s\n' "$*" >&2; exit 1; }
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
endpoint="${RELAYFLOW_MINIO_ENDPOINT:-http://minio:9000}"
region="${RELAYFLOW_MINIO_REGION:-us-east-1}"
web_port="${LOCAL_WEB_PORT:-18080}"
init_timeout="${LOCAL_INIT_TIMEOUT_SECONDS:-60}"
case "$web_port" in ''|*[!0-9]*) fail 'LOCAL_WEB_PORT must be a port number.' ;; esac
[ "${#web_port}" -le 5 ] && [ "$web_port" -gt 0 ] && [ "$web_port" -le 65535 ] || fail 'Invalid web port.'
case "$init_timeout" in ''|*[!0-9]*) fail 'Invalid initialization timeout.' ;; esac
[ "${#init_timeout}" -le 4 ] && [ "$init_timeout" -gt 0 ] && [ "$init_timeout" -le 3600 ] || fail 'Invalid initialization timeout.'

MC_CONFIG_DIR="$(mktemp -d)"
export MC_CONFIG_DIR
trap 'rm -rf -- "$MC_CONFIG_DIR"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

# Bound every network command, including authentication and the readiness wait.
mc_local() { timeout "$init_timeout" mc --quiet --no-color "$@"; }
printf '[minio-init] Waiting for managed storage…\n'
mc_local alias set managed "$endpoint" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 \
  || fail 'Storage authentication/connection failed; credentials were not printed.'
mc_local ready managed >/dev/null 2>&1 || fail 'Storage readiness timed out.'
mc_local mb --ignore-existing --region "$region" managed/relayflow >/dev/null \
  || fail 'Could not ensure relayflow bucket; retry after fixing storage.'

# Community MinIO has server-wide CORS, not the commercial bucket-CORS API.
# Only this managed origin setting converges on each run; objects and policies remain untouched.
origins="http://localhost:$web_port,http://127.0.0.1:$web_port"
mc_local admin config set managed api "cors_allow_origin=$origins" >/dev/null \
  || fail 'Could not configure local CORS origins.'
cors_config="$(mc_local admin config get managed api)" || fail 'Could not verify CORS configuration.'
case " $cors_config " in
  *" cors_allow_origin=$origins "*|*" cors_allow_origin=\"$origins\" "*) ;;
  *) fail 'CORS configuration did not match the managed local origins.' ;;
esac
printf '[minio-init] Bucket and local CORS ready; existing objects and access policies preserved.\n'
