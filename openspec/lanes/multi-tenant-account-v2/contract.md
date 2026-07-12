# API 契约：account-register-v2

> **母 change**：`multi-tenant-account-v2`  
> **子切片**：`account-register-v2-api`  
> **前置**：`tenant-bootstrap-api`

## 注册

```http
POST /app-api/system/auth/register
Content-Type: application/json

{
  "mobile": "13900001234",
  "password": "pass1234",
  "nickname": "张三",
  "tenantName": "张三的工作室"
}
```

### 成功响应（同 AuthLoginResp + 可选 tenants）

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "tenantId": 200001,
    "tenants": [
      { "tenantId": 200001, "tenantName": "张三的工作室", "owner": true }
    ]
  }
}
```

### 错误码

| code | 含义 |
|------|------|
| `USER_MOBILE_EXISTS` | 手机号已注册 |
| `AUTH_REGISTER_PASSWORD_WEAK` | 密码 < 6 |

**鉴权**：permitAll  
**配置**：`tenant.enabled=true` AND `allow-open-register=true`

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
  "code": 1001003001,
  "msg": "请选择要进入的企业",
  "data": {
    "tenants": [
      { "tenantId": 200001, "tenantName": "张三的工作室" },
      { "tenantId": 1, "tenantName": "默认企业" }
    ]
  }
}
```

## 验收 curl

```bash
# 1. 注册
curl -s -X POST http://localhost:8080/app-api/system/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"mobile":"13900009999","password":"pass1234","nickname":"测试","tenantName":"测试企业"}' | jq

# 2. 管理端邀请同一手机号到 tenant 1（需 admin token）
# 3. 再注册/登录验证 NOT_JOINED 已激活

# 4. my-list
curl -s http://localhost:8080/app-api/system/tenant/my-list \
  -H "Authorization: Bearer $TOKEN" | jq
```
