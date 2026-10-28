#!/usr/bin/env bash
set -euo pipefail
umask 077
source "$(cd "$(dirname "$0")" && pwd -P)/lib/local-compose.sh"
trap rf_cleanup EXIT
rf_require_docker
rf_load
case "${1:-}" in
  '') rf_compose ps -a | rf_redact ;;
  --logs) rf_diagnostics ;;
  *) rf_die 'Usage: ./scripts/local-status.sh [--logs]' ;;
esac
