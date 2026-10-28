#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
source "$(cd "$(dirname "$0")" && pwd -P)/lib/local-compose.sh"
trap rf_cleanup EXIT
trap 'rf_failed "$?"' ERR
trap 'exit 130' INT
trap 'exit 143' TERM HUP
no_build=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build) no_build=true; shift ;;
    --maven-settings) [[ $# -ge 2 ]] || rf_die '缺少 Maven settings 路径。'; RF_MAVEN_SETTINGS="$2"; shift 2 ;;
    --help|-h) printf 'Usage: %s [--no-build] [--maven-settings FILE]\n' "$0"; exit 0 ;;
    *) rf_die '未知参数；使用 --help 查看用法。' ;;
  esac
done
RF_STAGE=preflight
rf_require_docker
if $no_build; then
  "$RF_ROOT/scripts/local-config.sh" --no-pull
else
  "$RF_ROOT/scripts/local-config.sh"
fi
rf_load
rf_lock
rf_logged config --quiet

RF_STAGE=ports
docker image inspect "$RF_TOOL_IMAGE" >/dev/null 2>&1 || rf_die '缺少配置/端口检查工具镜像；请先在线启动一次。'
owned=''
for container in $(docker ps --quiet --filter "label=com.docker.compose.project=$RF_PROJECT"); do
  owned="$owned
$(docker inspect "$container" --format '{{range $port, $bindings := .NetworkSettings.Ports}}{{range $bindings}}{{printf "%s/%s\n" .HostPort $port}}{{end}}{{end}}')"
done
tcp_ports="$(rf_value LOCAL_WEB_PORT) $(rf_value MINIO_API_PORT) $(rf_value LIVEKIT_PORT) $(rf_value LIVEKIT_RTC_PORT)"
seen=' '
for port in $tcp_ports; do
  [[ "$seen" != *" $port "* ]] || rf_die '本地配置中 TCP 端口重复。'
  seen="$seen$port "
done
for mapping in $tcp_ports "$(rf_value LIVEKIT_UDP_PORT)/udp"; do
  protocol=tcp; port="$mapping"
  if [[ "$mapping" == */udp ]]; then protocol=udp; port="${mapping%/udp}"; fi
  if printf '%s\n' "$owned" | grep -Eq "^$port/[0-9]+/$protocol$"; then continue; fi
  if ! docker run --rm --pull never -p "127.0.0.1:$port:$port/$protocol" "$RF_TOOL_IMAGE" true >/dev/null 2>&1; then
    rf_die "端口 ${port}/${protocol} 被其他进程占用或无法绑定；请修改本地配置后重试。"
  fi
done

if $no_build; then
  RF_STAGE=images
  while IFS= read -r image; do
    docker image inspect "$image" >/dev/null 2>&1 || rf_die "缺少镜像 ${image}；--no-build 不构建或拉取镜像。"
  done < <(rf_compose config --images)
else
  RF_STAGE=pull
  rf_logged pull --ignore-buildable --policy missing
  RF_STAGE=build
  rf_logged --progress plain build minio minio-init server web
fi
RF_STAGE=startup
rf_logged up -d --no-build --pull never --wait --wait-timeout "$RF_TIMEOUT"
RF_STAGE=readiness
docker run --rm --pull never --network "${RF_PROJECT}_default" "$RF_TOOL_IMAGE" sh -ec '
  page="$(wget -q -T 5 -O- http://web/app/login)"
  printf "%s" "$page" | grep -qi "<html"
  result="$(wget -q -T 5 -O- http://web/admin-api/system/tenant/default)"
  printf "%s" "$result" | grep -Eq "\"code\"[[:space:]]*:[[:space:]]*0[[:space:]]*[,}]"
  wget -q -T 5 -O- http://livekit:7880/ >/dev/null
' 2>&1 | rf_redact | tee -a "$RF_LOG"
printf '\n[local] 已就绪：http://localhost:%s/app/login\n' "$RF_WEB_PORT"
printf '[local] 本地体验账号：19988888888 / 123456\n'
if [[ "$RF_CONFIG" == "$RF_ROOT/.relayflow/local.env" ]]; then
  printf '[local] 状态/日志：./scripts/local-status.sh --logs\n[local] 保留数据停止：./scripts/stop-local.sh\n'
else
  printf '[local] 状态/日志：RELAYFLOW_LOCAL_ENV_FILE=%q ./scripts/local-status.sh --logs\n' "$RF_CONFIG"
  printf '[local] 保留数据停止：RELAYFLOW_LOCAL_ENV_FILE=%q ./scripts/stop-local.sh\n' "$RF_CONFIG"
fi
