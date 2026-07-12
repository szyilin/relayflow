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

## 前端 Mock（`-web` 阶段）

Store 在 API 未就绪（404 / 网络错误）时 Mock 成功：

- `accessToken`: `mock-register-token`
- `tenantId`: `200001`
- 跳转 `/app/messages`

## 路由

| 路径 | 说明 |
|------|------|
| `/app/register` | 注册页 |
| `/app/login` | 入口「没有账号？注册」 |
| `/app/invite/accept` | redirect → `/app/register?mobile=` |

## 验收

```bash
cd web && pnpm build
# 浏览器：/app/login → 注册 → Mock 成功 → /app/messages
```
