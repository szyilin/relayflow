# API 契约：workspace-profile-card

> **状态**：已冻结（2026-07-12）  
> **起草**：`workspace-profile-card-web` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

员工工作台左下角个人资料卡片：昵称 inline 编辑、头像上传、企业名展示。头像走 infra 直传；资料读写走 system user profile。

## 端点

### GET /app-api/system/user/profile

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT（任意有效组织成员） |
| 所需权限 | 无（非管理面 RBAC） |

**Response `data`**：

```json
{
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "avatar": "1983123456789012345",
  "tenantId": 1,
  "tenantName": "乐云",
  "tenantVerified": false,
  "isAdmin": true
}
```

| 字段 | 说明 |
|------|------|
| `avatar` | `sys_user.avatar`，存 fileId 字符串；空表示无自定义头像 |
| `tenantVerified` | V1 恒为 `false`（企业认证未实现） |
| `isAdmin` | 当前企业下是否有 ≥1 个 permission code |

### PUT /app-api/system/user/profile

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | 无 |

**Request body**（字段可选，至少传一个）：

```json
{
  "nickname": "新昵称",
  "avatar": "1983123456789012345"
}
```

| 字段 | 约束 |
|------|------|
| `nickname` | 非空，≤ 64 字符 |
| `avatar` | fileId 字符串，≤ 512 字符 |

**Response `data`**：同 GET profile。

**错误码**：

| code | 说明 |
|------|------|
| `1_001_002_009` | 昵称为空 |

### POST /app-api/infra/file/upload-session

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | 无（工作台成员可上传 public 头像） |

**Request body**：

```json
{
  "filename": "avatar.png",
  "size": 102400,
  "mimeType": "image/png",
  "accessLevel": "public"
}
```

**Response `data`**：同 [`infra-file-upload/contract.md`](../infra-file-upload/contract.md) 的 upload-session 形状。

### POST /app-api/infra/file/upload-confirm

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | 无 |

**Request / Response**：同 infra-file-upload confirm；返回 `fileId` 后写入 profile `avatar`。

### GET /app-api/infra/file/public/{fileId}

公开文件 302 重定向；头像展示 URL：`/app-api/infra/file/public/{fileId}`。

## 权限信息补充

### GET /admin-api/system/auth/get-permission-info

Response 增加 `avatar` 字段（与 profile 一致），供会话恢复与 Dock 展示。

## 前端多账号 Dock

localStorage key：`relayflow:account-dock`

```typescript
interface AccountDockEntry {
  key: string          // `${userId}:${tenantId}`
  userId: number
  username: string
  nickname: string
  avatar?: string
  tenantId: number
  tenantName: string
  token: string
  isAdmin?: boolean
}
```

- 登录 / 切换企业 / 更新资料后 upsert 当前 entry
- 同账号多企业：登录后展开 `tenants` 列表，为每个企业创建 Dock entry
- 点击其他 entry：同 userId 走 `switchTenant`；不同 userId 恢复 token
- 退出：移除当前 userId 全部 entry，切到下一个或跳转登录页
- Token 失效：移除 entry 并提示重新登录

## curl 示例

```bash
# 获取资料
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:48080/app-api/system/user/profile | jq

# 更新昵称
curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"新昵称"}' \
  http://localhost:48080/app-api/system/user/profile | jq

# 头像上传（三阶段，见 infra-file-upload）
```
