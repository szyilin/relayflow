#!/usr/bin/env bash
# Trusted repository code only; user dotenv files are validated, never sourced.
RF_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RF_TOOL_IMAGE='alpine@sha256:5b10f432ef3da1b8d4c7eb6c487f2f5a8f096bc91145e68878dd4a5019afde11'
RF_NODE_IMAGE='node@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53'
RF_CONFIG="${RELAYFLOW_LOCAL_ENV_FILE:-$RF_ROOT/.relayflow/local.env}"
[[ "$RF_CONFIG" == /* ]] || RF_CONFIG="$RF_ROOT/$RF_CONFIG"
RF_RULES=''
RF_LOCK=''
RF_PROJECT=''
RF_LOG=''
RF_MAVEN_SETTINGS="${RELAYFLOW_MAVEN_SETTINGS_FILE:-}"

rf_die() { printf '[local] %s\n' "$*" >&2; exit 1; }
rf_value() {
  local line
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == "$1="* ]]; then printf '%s' "${line#*=}"; return 0; fi
  done < "$RF_CONFIG"
  return 1
}
rf_require_docker() {
  command -v docker >/dev/null || rf_die '请先安装 Docker 和 Compose。'
  docker info >/dev/null 2>&1 || rf_die 'Docker daemon 不可用，请启动 Docker 后重试。'
  local version
  version="$(docker compose version --short)" || rf_die 'Docker Compose 不可用。'
  if [[ "$version" =~ ^v?([0-9]+)\.([0-9]+) ]]; then
    ((BASH_REMATCH[1] > 2 || (BASH_REMATCH[1] == 2 && BASH_REMATCH[2] >= 24))) || rf_die '需要 Compose 2.24 或更新版本。'
  else rf_die '无法识别 Compose 版本。'; fi
}
rf_load() {
  "$RF_ROOT/scripts/local-config.sh" --check >/dev/null
  RF_CONFIG="$(cd "$(dirname "$RF_CONFIG")" && pwd -P)/$(basename "$RF_CONFIG")"
  RF_PROJECT="$(rf_value COMPOSE_PROJECT_NAME)"
  RF_TIMEOUT="$(rf_value RELAYFLOW_LOCAL_START_TIMEOUT_SECONDS)"
  RF_WEB_PORT="$(rf_value LOCAL_WEB_PORT)"
  if [[ -n "$RF_MAVEN_SETTINGS" ]]; then
    [[ -f "$RF_MAVEN_SETTINGS" && -r "$RF_MAVEN_SETTINGS" ]] || rf_die 'Maven settings 文件不可读。'
    RF_MAVEN_SETTINGS="$(cd "$(dirname "$RF_MAVEN_SETTINGS")" && pwd -P)/$(basename "$RF_MAVEN_SETTINGS")"
  fi
  mkdir -p "$RF_ROOT/.relayflow/logs"
  RF_LOG="$RF_ROOT/.relayflow/logs/$RF_PROJECT.log"
  RF_RULES="$(mktemp "$RF_ROOT/.relayflow/redact.XXXXXX")"
  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    key="${line%%=*}"; value="${line#*=}"
    case "$key" in
      *PASSWORD|*SECRET|*TOKEN|RELAYFLOW_ENCRYPT_AES_KEY|MINIO_ROOT_USER|RELAYFLOW_LIVEKIT_API_KEY)
        printf 's|%s|[REDACTED]|g\n' "$value" >> "$RF_RULES" ;;
    esac
  done < "$RF_CONFIG"
}
rf_compose() (
  # Compose shell interpolation takes precedence over --env-file. Remove inherited
  # values so the validated persisted file is authoritative for this project.
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    unset "${line%%=*}"
  done < "$RF_CONFIG"
  unset COMPOSE_FILE COMPOSE_ENV_FILES COMPOSE_PROFILES
  args=(--env-file "$RF_CONFIG" --project-name "$RF_PROJECT" -f "$RF_ROOT/deploy/compose.local.yml")
  if [[ -n "$RF_MAVEN_SETTINGS" ]]; then
    export RELAYFLOW_MAVEN_SETTINGS_FILE="$RF_MAVEN_SETTINGS"
    args+=(-f "$RF_ROOT/deploy/compose.maven-settings.yml")
  fi
  exec docker compose "${args[@]}" "$@"
)
rf_redact() {
  sed -f "$RF_RULES" | sed -E \
    -e 's/("(apiKey|apiSecret|password|accessToken|secret)"[[:space:]]*:[[:space:]]*")[^"]*/\1[REDACTED]/g' \
    -e 's/([?&](token|access_token|X-Amz-[A-Za-z-]+)=)[^ &"]+/\1[REDACTED]/g' \
    -e 's/Bearer [A-Za-z0-9._-]+/Bearer [REDACTED]/g' \
    -e 's/[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/[REDACTED-JWT]/g'
}
rf_logged() { rf_compose "$@" 2>&1 | rf_redact | tee -a "$RF_LOG"; }
rf_cleanup() {
  [[ -z "$RF_RULES" ]] || rm -f -- "$RF_RULES"
  [[ -z "$RF_LOCK" ]] || rmdir -- "$RF_LOCK"
}
rf_lock() {
  mkdir -p "$RF_ROOT/.relayflow/operation-locks"
  local path="$RF_ROOT/.relayflow/operation-locks/$RF_PROJECT.lock"
  mkdir "$path" 2>/dev/null || rf_die '本项目已有启动/停止操作；请等待完成，或确认进程结束后清理遗留锁。'
  RF_LOCK="$path"
}
rf_diagnostics() {
  rf_compose ps -a 2>&1 | rf_redact
  rf_compose logs --no-color --tail 60 2>&1 | rf_redact
}
rf_failed() {
  local status="$1"
  trap - ERR
  set +e
  printf '[local] 阶段失败：%s。数据卷与密钥保留。\n' "${RF_STAGE:-preflight}" >&2
  if [[ -n "$RF_RULES" ]]; then rf_diagnostics | tee -a "$RF_LOG" >&2; fi
  exit "$status"
}
