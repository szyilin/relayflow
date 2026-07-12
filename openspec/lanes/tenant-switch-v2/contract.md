# API 契约：tenant-switch-v2

> **母 change**：`multi-tenant-account-v2`  
> **子切片**：`tenant-switch-v2-web` / `tenant-switch-v2-api`  
> **前置**：`account-register-v2-integrate` 完成

## 我的企业列表

```http
GET /app-api/system/tenant/my-list
Authorization: Bearer <token>
```

```json
{
  "code": 0,
  "data": [
    { "tenantId": 200001, "tenantName": "张三的工作室", "owner": true },
    { "tenantId": 1, "tenantName": "默认企业", "owner": false }
  ]
}
```

## 切换企业

```http
POST /app-api/system/tenant/switch
Authorization: Bearer <token>
Content-Type: application/json

{ "tenantId": 1 }
```

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "tenantId": 1
  }
}
```

## 登录扩展

```http
POST /admin-api/system/auth/login
Content-Type: application/json

{
  "username": "13900001234",
  "password": "pass1234",
  "tenantId": 200001
}
```

多企业且未传 `tenantId`：

```json
{
  "code": 1001003003,
  "msg": "请选择要进入的企业",
  "data": {
    "tenants": [
      { "tenantId": 200001, "tenantName": "张三的工作室" },
      { "tenantId": 1, "tenantName": "默认企业" }
    ]
  }
}
```

**错误码**：`TENANT_SELECTION_REQUIRED` = `1_001_003_003`（1001003003）

## 前端（`-integrate`）

| 能力 | 行为 |
|------|------|
| `fetchMyTenants` | 调用真实 `GET /tenant/my-list` |
| `switchTenant` | 调用真实 `POST /tenant/switch`，更新 token 后 `fetchPermissionInfo` |
| 登录选企业 | 由 `TENANT_SELECTION_REQUIRED`（`1001003003`）驱动 |
| WebSocket | `useImWebSocket` 监听 `auth.token` 变化自动重连 |

## 浏览器验收

```bash
cd web && pnpm build && pnpm dev
```

1. 注册或登录后，Rail 品牌下方显示当前企业名；多企业时出现切换器
2. DEV：`13900009126` + `pass1234`（先被邀请再注册）→ 登录页出现企业选择 → 选企业后进入工作台

## curl（`-api` 阶段）

```bash
curl -s http://localhost:8080/app-api/system/tenant/my-list \
  -H "Authorization: Bearer $TOKEN" | jq

curl -s -X POST http://localhost:8080/app-api/system/tenant/switch \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":1}' | jq
```
