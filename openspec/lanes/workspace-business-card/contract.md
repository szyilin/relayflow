# API 契约：workspace-business-card

> **状态**：已冻结（2026-07-16）  
> **起草**：`workspace-business-card-web`  
> **实现**：`workspace-business-card-api`  
> **惯例**：签名/封面跟用户资料；备注 C 类（读空 / 写 upsert）

## 背景

飞书式个人名片。本人与他人同一 UI（`self` / `peer`）。

| 数据 | 作用域 | 存储 |
|------|--------|------|
| `signature`、`coverFileId` | 用户公开资料 | `sys_user.signature` / `cover_file_id` |
| `remarkName`、`description` | `(owner → target)` 私有 | `sys_contact_remark` |

## 端点

### GET /app-api/system/user/profile

本人资料（含 `signature`、`coverFileId`）。

### PUT /app-api/system/user/profile

可选字段：`nickname`、`avatar`、`signature`（≤120）、`coverFileId`（public fileId）。仅本人。

### GET /app-api/system/user/profile/{userId}

租户内 ACTIVE 成员只读公开资料（含签名/封面/头像）。

### GET /app-api/system/contact-remark/{targetUserId}

当前用户对目标的备注；无行 → 空字符串字段，不 INSERT。

### PUT /app-api/system/contact-remark/{targetUserId}

upsert 备注名/描述。

## curl

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/app-api/system/user/profile

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"signature":"hello","coverFileId":"<fileId>"}' \
  http://localhost:8080/app-api/system/user/profile

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/app-api/system/user/profile/2

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"remarkName":"Audrey","description":"设计"}' \
  http://localhost:8080/app-api/system/contact-remark/2
```
