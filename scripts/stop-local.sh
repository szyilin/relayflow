#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
source "$(cd "$(dirname "$0")" && pwd -P)/lib/local-compose.sh"
trap rf_cleanup EXIT
trap 'rf_failed "$?"' ERR
[[ $# -eq 0 ]] || rf_die 'Usage: ./scripts/stop-local.sh（默认保留数据，不接受清卷参数）'
rf_require_docker
rf_load
rf_lock
RF_STAGE=stop
rf_logged down --remove-orphans
printf '[local] 已停止；本地配置、密钥及数据卷均已保留。\n'
