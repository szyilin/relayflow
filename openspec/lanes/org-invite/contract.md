# API 契约：org-invite

> **状态**：settings **integrated**（管理端 store 已接）；channels **integrated**（工作台 store 已接）  
> **变更**：`org-invite-settings-integrate` / `org-invite-channels-integrate`  
> **母 change**：`workspace-plus-create-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 鉴权

| 面 | 路径前缀 | 鉴权 |
|----|----------|------|
| 管理端 | `/admin-api/system/invite-settings` | JWT + 管理身份 + `system:user:list`（读）/ `system:user:create`（写） |
| 工作台 | `/app-api/system/org-invite/*` | JWT + 有效组织成员 |

## 邀请设置（已集成）

`GET` / `PUT /admin-api/system/invite-settings`（全量 PUT）。无行时返回默认：`ADMIN_ONLY` / `INVITER_DEPT` / `[]` / `3` / `false`。

前端：`api/admin/inviteSettings.ts` → `stores/inviteSettings` → `/admin/system/invite-settings`。

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/admin-api/system/invite-settings" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PUT "http://localhost:8080/admin-api/system/invite-settings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"inviteMode":"ALL_MEMBERS","nonAdminScope":"INVITER_DEPT","nonAdminDeptIds":[],"nonAdminExpireDays":3,"nonAdminRequireApproval":false}'
```

## 工作台渠道（已集成）

`GET …/capability` · `GET …/shared` · `POST …/shared/reset` · `POST …/mobile` · `POST …/consume`。

前端：`api/app/orgInvite.ts` → `stores/orgInvite` / `stores/createHub.canInvite` → `WorkspaceOrgInviteModal` · `/app/invite/join`。

### GET /app-api/system/org-invite/capability

**Response `data`**

```json
{ "canInvite": true, "inviteMode": "ADMIN_ONLY" }
```

| `inviteMode` | `canInvite` |
|--------------|------------|
| `OFF` | false |
| `ADMIN_ONLY` | 仅管理身份 |
| `ALL_MEMBERS` | 有效成员 true |

### GET /app-api/system/org-invite/shared

确保租户共享邀请（链接/二维码/邀请码同一行 `channel=SHARED`）。须 `canInvite`。

**Response `data`**

```json
{
  "token": "t_…",
  "inviteCode": "AB12CD",
  "expiresAt": "2026-07-30T12:00:00Z"
}
```

前端拼链接：`{origin}/app/invite/join?t={token}`；二维码内容同链接。

### POST /app-api/system/org-invite/shared/reset

旧 token/code → `REVOKED`；返回新凭证。须 `canInvite`。

### POST /app-api/system/org-invite/mobile

**Request**

```json
{ "mobile": "13900001111" }
```

**Response `data`**

```json
{ "id": "…", "mobile": "13900001111", "status": "ACTIVE", "createdAt": "…" }
```

非管理员且 `nonAdminRequireApproval=true` → `PENDING_APPROVAL`（不可 consume）。

### POST /app-api/system/org-invite/consume

**Request**（二选一）

```json
{ "token": "t_…" }
```

```json
{ "code": "AB12CD" }
```

**Response `data`**

```json
{ "joined": true, "alreadyMember": false, "tenantId": "1" }
```

- 共享渠道：可多次消费；行保持 `ACTIVE` 直至过期/重置  
- 手机号：成功后 `CONSUMED`  
- 已是 ACTIVE 成员：`alreadyMember=true` 幂等成功  

错误码：`ORG_INVITE_FORBIDDEN` / `CLOSED` / `NOT_FOUND` / `EXPIRED` / `PENDING_APPROVAL` / `MOBILE_INVALID` / `CONSUME_INVALID`。

## curl

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/app-api/system/org-invite/capability" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/org-invite/shared" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/system/org-invite/shared/reset" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/system/org-invite/mobile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13900001111"}'

curl -s -X POST "http://localhost:8080/app-api/system/org-invite/consume" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"token":"<shared-token>"}'
```

## 前端路径

| 路径 | 说明 |
|------|------|
| Create Hub → 添加企业成员 | `WorkspaceOrgInviteModal` |
| `/admin/system/invite-settings` | 管理端邀请设置 |
| `/app/invite/join` | 加入页（token / code） |

审批列表 UI、Bot 卡推送、admin `user/invite` 委托 — 后续切片 / integrate。
