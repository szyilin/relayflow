# API 契约：account-register-v2（前端 lane）

> **母 change**：`multi-tenant-account-v2`  
> **子切片**：`account-register-v2-web` / `account-register-v2-api`  
> **前置**：`tenant-bootstrap-api`（后端 register 实现时）

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

### 成功响应

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

## 前端联调（`-integrate`）

- Store 直接调用 `POST /auth/register`，**无 Mock 回退**
- 注册成功后 `establishSession(accessToken, tenantId, mobile, nickname)`

| 路径 | 说明 |
|------|------|
| `/app/register` | 注册页 |
| `/app/login` | 入口「没有账号？注册」 |
| `/app/invite/accept` | redirect → `/app/register?mobile=` |

## 验收

```bash
docker compose -f deploy/compose.yml up -d
./mvnw -pl relayflow-server -am spring-boot:run
cd web && pnpm dev
# 浏览器：/app/register → 注册 → /app/messages
```
