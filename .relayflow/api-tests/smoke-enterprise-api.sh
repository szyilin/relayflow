#!/usr/bin/env bash
# Smoke test: admin enterprise profile + address APIs.
# Usage: BASE_URL=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=admin123 bash .relayflow/api-tests/smoke-enterprise-api.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-19988888888}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"

login_json=$(curl -sS -X POST "${BASE_URL}/admin-api/system/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")

TOKEN=$(python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("code")==0, d; print(d["data"]["accessToken"])' <<<"$login_json")
AUTH="Authorization: Bearer ${TOKEN}"

echo "== GET profile =="
curl -sS "${BASE_URL}/admin-api/system/enterprise/profile" -H "$AUTH" | python3 -m json.tool

echo "== PUT profile =="
curl -sS -X PUT "${BASE_URL}/admin-api/system/enterprise/profile" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"name":"RelayFlow 演示企业","industry":"INTERNET_IT","scale":"SIZE_10_49","contactName":"张经理","contactEmail":"contact@example.com","contactMobile":"13800138000","contactLandline":"010-88886666"}' \
  | python3 -m json.tool

echo "== geo children (root) =="
geo_json=$(curl -sS "${BASE_URL}/admin-api/infra/geo/children" -H "$AUTH")
echo "$geo_json" | python3 -m json.tool | head -40
REGION_ID=$(python3 -c 'import json,sys; d=json.load(sys.stdin); kids=d.get("data") or []; assert kids, d; print(kids[0]["id"])' <<<"$geo_json")

# drill toward a leaf (province -> city -> district)
for _ in 1 2 3; do
  next=$(curl -sS "${BASE_URL}/admin-api/infra/geo/children?parentId=${REGION_ID}" -H "$AUTH")
  child=$(python3 -c 'import json,sys; d=json.load(sys.stdin); kids=d.get("data") or []; print(kids[0]["id"] if kids else "")' <<<"$next")
  if [[ -z "$child" ]]; then
    break
  fi
  REGION_ID="$child"
done

PATH_LABEL="中国 / 冒烟测试区划"

echo "== create address regionId=${REGION_ID} =="
create_json=$(curl -sS -X POST "${BASE_URL}/admin-api/system/enterprise/address/create" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"types\":[\"JOB\",\"STORE\"],\"regionId\":\"${REGION_ID}\",\"regionPath\":\"${PATH_LABEL}\",\"detail\":\"冒烟测试地址 1 号\"}")
echo "$create_json" | python3 -m json.tool
ADDR_ID=$(python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("code")==0, d; print(d["data"]["id"])' <<<"$create_json")

echo "== list addresses =="
curl -sS "${BASE_URL}/admin-api/system/enterprise/address/list?status=ENABLED" -H "$AUTH" | python3 -m json.tool | head -50

echo "== disable address =="
curl -sS -X PUT "${BASE_URL}/admin-api/system/enterprise/address/status?id=${ADDR_ID}" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"status":"DISABLED"}' | python3 -m json.tool

echo "OK enterprise smoke passed"
