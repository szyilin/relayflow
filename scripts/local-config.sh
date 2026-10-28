#!/usr/bin/env bash
# Generate/validate local configuration. Never source the dotenv file.
set -euo pipefail
umask 077

ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
TEMPLATE="$ROOT/deploy/local.env.example"
# Alpine 3.23.4; pin the image digest rather than a mutable latest tag.
TOOL_IMAGE='alpine@sha256:5b10f432ef3da1b8d4c7eb6c487f2f5a8f096bc91145e68878dd4a5019afde11'
CHECK_ONLY=false
NO_PULL=false
LOCK=''
TEMP_FILE=''
REGISTRY_TEMP=''
KEYS=()
VALUES=()
REQUIRED_KEYS=(LOCAL_CONFIG_VERSION COMPOSE_PROJECT_NAME LOCAL_WEB_PORT MINIO_API_PORT
  LIVEKIT_PORT LIVEKIT_RTC_PORT LIVEKIT_UDP_PORT POSTGRES_DB POSTGRES_USER
  RELAYFLOW_MINIO_REGION RELAYFLOW_TENANT_ENABLED RELAYFLOW_TENANT_DEFAULT_ID
  RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER RELAYFLOW_LOCAL_START_TIMEOUT_SECONDS
  POSTGRES_PASSWORD MINIO_ROOT_USER MINIO_ROOT_PASSWORD RELAYFLOW_JWT_SECRET
  RELAYFLOW_ENCRYPT_AES_KEY XXL_JOB_ACCESS_TOKEN RELAYFLOW_LIVEKIT_API_KEY
  RELAYFLOW_LIVEKIT_API_SECRET)

fail() { printf '[local-config] %s\n' "$*" >&2; exit 1; }
cleanup() {
  if [[ -n "$TEMP_FILE" ]]; then rm -f -- "$TEMP_FILE"; fi
  if [[ -n "$REGISTRY_TEMP" ]]; then rm -f -- "$REGISTRY_TEMP"; fi
  if [[ -n "$LOCK" ]]; then rmdir -- "$LOCK"; fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

for arg in "$@"; do
  case "$arg" in
    --check) CHECK_ONLY=true ;;
    --no-pull) NO_PULL=true ;;
    --help|-h)
      printf 'Usage: %s [--check] [--no-pull]\n' "$0"
      printf 'RELAYFLOW_LOCAL_ENV_FILE selects the env file (default: .relayflow/local.env).\n'
      printf 'COMPOSE_PROJECT_NAME selects a new project; existing configuration must match.\n'
      exit 0 ;;
    *) fail "未知参数；使用 --help 查看用法。" ;;
  esac
done

get_value() {
  local index
  for ((index=0; index<${#KEYS[@]}; index++)); do
    if [[ "${KEYS[$index]}" == "$1" ]]; then printf '%s' "${VALUES[$index]}"; return 0; fi
  done
  return 1
}

validate_value() {
  local key="$1" value="$2"
  case "$key" in
    LOCAL_CONFIG_VERSION) [[ "$value" == 1 ]] ;;
    COMPOSE_PROJECT_NAME) [[ "$value" =~ ^[a-z0-9][a-z0-9_-]*$ && ${#value} -le 63 ]] ;;
    LOCAL_WEB_PORT|MINIO_API_PORT|LIVEKIT_PORT|LIVEKIT_RTC_PORT|LIVEKIT_UDP_PORT)
      [[ "$value" =~ ^[1-9][0-9]{0,4}$ ]] && ((10#$value <= 65535)) ;;
    POSTGRES_DB|POSTGRES_USER) [[ "$value" =~ ^[a-z][a-z0-9_]{0,62}$ ]] ;;
    RELAYFLOW_MINIO_REGION) [[ "$value" =~ ^[a-z0-9][a-z0-9-]*$ ]] ;;
    RELAYFLOW_TENANT_ENABLED|RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER)
      [[ "$value" == true || "$value" == false ]] ;;
    RELAYFLOW_TENANT_DEFAULT_ID) [[ "$value" =~ ^[1-9][0-9]{0,17}$ ]] ;;
    RELAYFLOW_LOCAL_START_TIMEOUT_SECONDS)
      [[ "$value" =~ ^[1-9][0-9]{0,3}$ ]] && ((10#$value <= 3600)) ;;
    MINIO_ROOT_USER) [[ "$value" =~ ^rf[0-9a-f]{16}$ ]] ;;
    RELAYFLOW_LIVEKIT_API_KEY) [[ "$value" =~ ^[0-9a-f]{32}$ ]] ;;
    RELAYFLOW_ENCRYPT_AES_KEY) [[ "$value" =~ ^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw048]=$ ]] ;;
    POSTGRES_PASSWORD|MINIO_ROOT_PASSWORD|RELAYFLOW_JWT_SECRET|XXL_JOB_ACCESS_TOKEN|RELAYFLOW_LIVEKIT_API_SECRET)
      [[ "$value" =~ ^[0-9a-f]{64}$ ]] ;;
    *) return 1 ;;
  esac
}

load_config() {
  local file="$1" line key value number=0 required
  [[ -f "$file" && ! -L "$file" ]] || fail '配置必须是普通文件，不能是符号链接；请恢复原配置。'
  KEYS=()
  VALUES=()
  while IFS= read -r line || [[ -n "$line" ]]; do
    number=$((number + 1))
    [[ "$line" != *$'\r'* ]] || fail "配置第 $number 行使用 CRLF；请转换为 LF。"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || fail "配置第 $number 行格式无效；只支持字面 KEY=VALUE。"
    key="${line%%=*}"
    value="${line#*=}"
    # Strict per-key grammars also reject Compose interpolation and shell syntax.
    validate_value "$key" "$value" || fail "配置第 $number 行的键或值无效；不支持未知键、引号、插值或空值。"
    if get_value "$key" >/dev/null; then fail "配置第 $number 行的键重复。"; fi
    KEYS+=("$key")
    VALUES+=("$value")
  done < "$file"
  for required in "${REQUIRED_KEYS[@]}"; do
    get_value "$required" >/dev/null || fail "配置缺少 ${required}；请恢复完整原配置，不会自动补写密钥。"
  done
}

config_file="${RELAYFLOW_LOCAL_ENV_FILE:-$ROOT/.relayflow/local.env}"
[[ "$config_file" != *$'\n'* && "$config_file" != *$'\r'* ]] || fail '配置路径不能包含换行。'
[[ "$config_file" == /* ]] || config_file="$ROOT/$config_file"
[[ ! -L "$config_file" ]] || fail '配置不能是符号链接。'
if $CHECK_ONLY; then
  load_config "$config_file"
  if [[ -n "${COMPOSE_PROJECT_NAME:-}" && "$COMPOSE_PROJECT_NAME" != "$(get_value COMPOSE_PROJECT_NAME)" ]]; then
    fail '项目名与现有配置不一致；请复用原项目名或选择独立配置。'
  fi
  printf '[local-config] 配置校验通过（未修改文件）。\n'
  exit 0
fi

mkdir -p -- "$(dirname "$config_file")"
config_file="$(cd "$(dirname "$config_file")" && pwd -P)/$(basename "$config_file")"
# Custom in-repository credentials must stay inside the ignored runtime directory.
case "$config_file" in
  "$ROOT"/.relayflow/*) ;;
  "$ROOT"/*) fail '仓库内自定义配置必须位于 .relayflow/；也可选择仓库外的绝对路径。' ;;
esac
if [[ -e "$config_file" ]]; then
  load_config "$config_file"
  project="$(get_value COMPOSE_PROJECT_NAME)"
  [[ -z "${COMPOSE_PROJECT_NAME:-}" || "$COMPOSE_PROJECT_NAME" == "$project" ]] || fail '项目名与现有配置不一致。'
else
  project="${COMPOSE_PROJECT_NAME:-relayflow-local}"
  validate_value COMPOSE_PROJECT_NAME "$project" || fail '项目名无效。'
fi

state_dir="$ROOT/.relayflow"
mkdir -p -- "$state_dir/config-locks" "$state_dir/config-projects"
lock_path="$state_dir/config-locks/$project.lock"
attempt=0
until mkdir -- "$lock_path" 2>/dev/null; do
  attempt=$((attempt + 1))
  ((attempt < 30)) || fail '同项目配置生成锁等待超时；请检查其他启动进程，确认结束后再清理遗留锁。'
  sleep 1
done
LOCK="$lock_path"
registry="$state_dir/config-projects/$project.path"
[[ ! -L "$registry" ]] || fail '项目配置关联不能是符号链接。'
if [[ -e "$registry" ]]; then
  [[ -f "$registry" ]] || fail '项目配置关联文件无效。'
  bound_path="$(cat "$registry")"
  [[ "$bound_path" == "$config_file" ]] || fail '本项目已关联另一配置路径；请复用原路径或选择独立项目名。'
fi

if [[ -e "$config_file" || -L "$config_file" ]]; then
  load_config "$config_file"
  [[ "$(get_value COMPOSE_PROJECT_NAME)" == "$project" ]] || fail '等待期间配置项目名发生变化。'
  chmod 600 "$config_file"
else
  command -v docker >/dev/null 2>&1 || fail '未找到 Docker；请先安装并启动 Docker。'
  docker info >/dev/null 2>&1 || fail '无法连接 Docker daemon；不会在未知数据状态下生成密钥。'
  old_volumes="$(docker volume ls --quiet --filter "label=com.docker.compose.project=$project")" || fail '无法检查旧数据卷；配置未生成。'
  old_containers="$(docker ps --all --quiet --filter "label=com.docker.compose.project=$project")" || fail '无法检查已有项目容器；配置未生成。'
  [[ -z "$old_volumes" && -z "$old_containers" ]] || fail '发现本项目已有数据卷或容器但配置缺失；请恢复原配置，禁止生成新密钥连接旧数据。'
  if ! docker image inspect "$TOOL_IMAGE" >/dev/null 2>&1; then
    $NO_PULL && fail '缺少固定版本配置工具镜像；--no-pull 模式不会下载镜像。'
    printf '[local-config] 准备固定版本配置工具镜像…\n' >&2
    docker pull "$TOOL_IMAGE" >&2 || fail '配置工具镜像准备失败。'
  fi

  TEMP_FILE="$(mktemp "$config_file.tmp.XXXXXX")"
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == COMPOSE_PROJECT_NAME=* ]]; then
      printf 'COMPOSE_PROJECT_NAME=%s\n' "$project"
    else
      printf '%s\n' "$line"
    fi
  done < "$TEMPLATE" > "$TEMP_FILE"
  if ! docker run --rm --network none --read-only --cap-drop ALL \
    --security-opt no-new-privileges "$TOOL_IMAGE" sh -euc '
      set -o pipefail
      hex() { od -An -tx1 -N "$1" /dev/urandom | tr -d " \n"; }
      for key in POSTGRES_PASSWORD MINIO_ROOT_PASSWORD RELAYFLOW_JWT_SECRET XXL_JOB_ACCESS_TOKEN RELAYFLOW_LIVEKIT_API_SECRET; do
        printf "%s=%s\n" "$key" "$(hex 32)"
      done
      printf "MINIO_ROOT_USER=rf%s\n" "$(hex 8)"
      printf "RELAYFLOW_LIVEKIT_API_KEY=%s\n" "$(hex 16)"
      printf "RELAYFLOW_ENCRYPT_AES_KEY=%s\n" "$(head -c 32 /dev/urandom | base64)"
    ' >> "$TEMP_FILE"; then fail '随机密钥生成失败；未发布配置文件。'; fi
  load_config "$TEMP_FILE"
  [[ "$(get_value COMPOSE_PROJECT_NAME)" == "$project" ]] || fail '生成的项目配置不一致。'
  # Same-directory hard link publishes a complete 0600 file atomically, without overwrite.
  link "$TEMP_FILE" "$config_file" || fail '配置目标已存在或无法发布；不会覆盖任何现有文件。'
  rm -f -- "$TEMP_FILE"
  TEMP_FILE=''
fi

if [[ ! -e "$registry" ]]; then
  REGISTRY_TEMP="$(mktemp "$registry.tmp.XXXXXX")"
  printf '%s\n' "$config_file" > "$REGISTRY_TEMP"
  link "$REGISTRY_TEMP" "$registry" || fail '无法保存项目与配置路径关联。'
  rm -f -- "$REGISTRY_TEMP"
  REGISTRY_TEMP=''
fi
printf '[local-config] 配置已就绪：%s（项目 %s；密钥未输出）。\n' "$config_file" "$project"
