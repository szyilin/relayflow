# API 契约：workspace-create-hub

> **状态**：integrate（前端已接）  
> **变更**：`workspace-create-hub-web` → `workspace-create-hub-integrate`  
> **母 change**：`workspace-plus-create-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

工作台 Rail 头像旁「+」菜单：创建群组、添加企业成员、添加外部联系人、创建文档。组织邀请四渠道见 [`org-invite`](../org-invite/contract.md)；外部发起申请见 [`workspace-contacts-enrich`](../workspace-contacts-enrich/contract.md)。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`） |

## 前端路径（已联调）

| 动作 | UI | 数据真源 |
|------|-----|----------|
| 「+」菜单 | `WorkspaceRailHeader` | `createHub`；`canInvite` ← `orgInvite.loadCapability` |
| 创建群组 | `WorkspaceCreateGroupModal` + `WorkspaceMemberPicker` | `POST /app-api/im/group/create` |
| 邀请入群 | `ImInviteMembersModal` + 同 picker + `excludeUserIds` | `POST /app-api/im/group/members/add` |
| 添加企业成员 | `WorkspaceOrgInviteModal`；加入 `/app/invite/join` | `/app-api/system/org-invite/*` |
| 添加外部联系人 | `WorkspaceAddExternalContactModal` | `search-account` / `POST request` |
| 创建文档 | 跳转 `/app/docs` + `docs.createPage` | `POST /app-api/docs/library/nodes` |
| 我管理的群组 | `memberPicker.loadManagedGroups` | V1：会话列表 + `group/members` 前端聚合（专用 `GET …/group/managed` **可选延后**） |

## App API

### GET /app-api/system/org-invite/capability

**Response `data`**

```json
{ "canInvite": true, "inviteMode": "ADMIN_ONLY" }
```

前端：`stores/orgInvite` → `createHub.canInvite`；`inviteMode=OFF` 时菜单隐藏「添加企业成员」。

### GET /app-api/im/group/managed（草案 · 可选）

当前用户为 `owner`/`admin` 的群组（含成员摘要）。**V1 不阻塞**：前端聚合 `conversation/list`（group）→ 并行 `GET …/group/members` → 过滤角色；上限约 30 群。真 API 落地后替换 `stores/memberPicker.loadManagedGroups`。

详见 [`im-group-chat`](../im-group-chat/contract.md) managed 章。

## 既有复用

| 端点 | 用途 |
|------|------|
| `GET /app-api/system/dept/tree` | 选人器组织内 |
| `GET /app-api/system/user/list-by-dept` | 选人器成员 |
| `GET /app-api/system/contact/external/list` | 选人器外部 |
| `GET /app-api/system/contact/search-account` | 添加外部搜索 |
| `POST /app-api/system/contact/request` | 发起外部申请 |
| `GET /app-api/im/conversation/list` | managed 群源（V1 聚合） |
| `GET /app-api/im/group/members` | 判定 owner/admin + 展开成员 |
| `POST /app-api/im/group/create` | 建群 |
| `POST /app-api/im/group/members/add` | 邀请入群 |
| `POST /app-api/docs/library/nodes` | 建文档 |

## 浏览器验收路径

| 路径 | 验证 |
|------|------|
| `/app` | Rail「+」四项（邀请项受 capability） |
| `/app/contacts` | 外部入口打开同一添加弹窗；申请可同意/拒绝 |
| `/app/messages` | 建群 / 邀请入群共用 picker |
| `/app/docs` | 「+」创建文档落地并打开 |
| `/app/invite/join` | 消费链接/码 |
| `/admin/system/invite-settings` | 三种 invite_mode |

## curl（建群 · 已有）

```bash
TOKEN="<jwt>"
curl -s -X POST "http://localhost:8080/app-api/im/group/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"产品讨论","memberUserIds":["201","202"]}'
```
