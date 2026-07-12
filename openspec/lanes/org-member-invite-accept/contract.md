# API 契约：org-member-invite-accept

> **变更**：`org-member-invite-accept`  
> **前置**：[`org-member-invite`](../org-member-invite/contract.md) 已部署

## 预览邀请

```http
GET /app-api/system/member-invite/preview?mobile=13900008888
```

响应 `code=0`：

```json
{
  "tenantId": 1,
  "tenantName": "默认企业",
  "nickname": "浏览器验证"
}
```

## 接受邀请

```http
POST /app-api/system/member-invite/accept
Content-Type: application/json

{
  "mobile": "13900008888",
  "password": "join1234"
}
```

响应 `code=0`（同登录）：

```json
{
  "accessToken": "<jwt>",
  "tenantId": 1
}
```

## curl 验收

```bash
# 1. 管理端邀请
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')
curl -s -X POST http://localhost:8080/admin-api/system/user/invite \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"mobile":"13900007777","nickname":"待加入","deptId":1}'

# 2. 预览
curl -s 'http://localhost:8080/app-api/system/member-invite/preview?mobile=13900007777' | jq

# 3. 接受
curl -s -X POST http://localhost:8080/app-api/system/member-invite/accept \
  -H 'Content-Type: application/json' \
  -d '{"mobile":"13900007777","password":"join1234"}' | jq

# 4. 用新密码登录
curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"13900007777","password":"join1234"}' | jq
```
