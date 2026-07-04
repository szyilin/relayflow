#!/usr/bin/env bash
# RelayFlow selective MyBatis-Plus codegen -> temp directory (for human/AI diff before merge).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/relayflow-tools/relayflow-codegen/target/relayflow-codegen.jar"

build_jar() {
  echo "[codegen] building relayflow-codegen fat JAR (-Pcodegen)..." >&2
  "$ROOT/mvnw" -Pcodegen package -DskipTests -q
}

if [[ ! -f "$JAR" ]]; then
  build_jar
fi

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  exec java -jar "$JAR" --repo-root "$ROOT" --help
fi

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 --module <system|infra|im> --tables <t1,t2,...> [--output dir] [--migrate]" >&2
  echo "Run $0 --help for details." >&2
  exit 1
fi

exec java -jar "$JAR" --repo-root "$ROOT" "$@"
