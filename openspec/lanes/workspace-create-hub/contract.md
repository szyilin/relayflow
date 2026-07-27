# API 契约：workspace-create-hub

> **状态**：draft（`-web`）  
> **变更**：`workspace-create-hub-web`  
> **母 change**：`workspace-plus-create-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

工作台 Rail 头像旁「+」菜单：创建群组、添加企业成员、添加外部联系人、创建文档。本契约描述前端路径与后续 capability / 选人相关端点；组织邀请四渠道细节见 [`org-invite`](../org-invite/contract.md)（待立）；外部发起申请见 contacts enrich 扩展。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`） |

## 前端路径（本切片）

| 动作 | UI | 数据 |
|------|-----|------|
| 「+」菜单 | `WorkspaceRailHeader` | — |
| 创建群组 | `WorkspaceCreateGroupModal` + `WorkspaceMemberPicker` | 既有 `POST /app-api/im/group/create`；「我管理的群组」见下方 managed |
| 邀请入群 | `ImInviteMembersModal` + 同 picker + `excludeUserIds` | 既有 `POST /app-api/im/group/members/add` |
| 添加企业成员 | `WorkspaceOrgInviteModal` | `-web` 骨架；真 API → org-invite |
| 添加外部联系人 | `WorkspaceAddExternalContactModal` | `-web` 临时搜索；真 API → contact search/request |
| 创建文档 | 跳转 `/app/docs` | 既有 `POST /app-api/docs/library/nodes` |

## App API（后续）

### GET /app-api/system/org-invite/capability

**Response `data`**

```json
{ "canInvite": true, "inviteMode": "ADMIN_ONLY" }
```

`-web`：`canInvite = auth.isAdmin`（默认 ADMIN_ONLY）。

### GET /app-api/im/group/managed（草案 · 供选人器）

当前用户为 `owner`/`admin` 的群组（含成员摘要）。`-web` 前端聚合：`conversation/list`（group）→ 并行 `GET …/group/members` → 过滤角色；上限约 30 群。真 API 落地后替换 `stores/memberPicker.loadManagedGroups`。

**Response `data`**（建议）

```json
[
  {
    "conversationId": "301",
    "title": "产品讨论组",
    "memberCount": 4,
    "members": [
      { "userId": "100", "nickname": "张三", "avatarText": "张" },
      { "userId": "102", "nickname": "李晓明", "avatarText": "李" }
    ]
  }
]
```

仅含 `subjectType=user` 成员；不含 bot。详见 [`im-group-chat`](../im-group-chat/contract.md) managed 章。

### 外部搜索 / 发起申请

见 contacts enrich 契约：`GET …/contact/search-account`、`POST …/contact/request`。

## 既有复用

| 端点 | 用途 |
|------|------|
| `GET /app-api/system/dept/tree` | 选人器组织内 |
| `GET /app-api/system/user/list-by-dept` | 选人器成员 |
| `GET /app-api/system/contact/external/list` | 选人器外部（可空） |
| `GET /app-api/im/conversation/list` | `-web` 聚合 managed 群源 |
| `GET /app-api/im/group/members` | `-web` 判定 owner/admin + 展开成员 |
| `POST /app-api/im/group/create` | 建群 |
| `POST /app-api/im/group/members/add` | 邀请入群 |
| `POST /app-api/docs/library/nodes` | 建文档 |

## curl（建群 · 已有）

```bash
TOKEN="<jwt>"
curl -s -X POST "http://localhost:8080/app-api/im/group/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"产品讨论","memberUserIds":["201","202"]}'
```
