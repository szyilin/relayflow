# API 契约：workspace-contacts-enrich

> **状态**：done（`-integrate` 完成）  
> **起草**：`workspace-contacts-enrich-web`  
> **实现**：`workspace-contacts-enrich-api` · `workspace-contacts-enrich-integrate`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **组织内既有**：[`workspace-contacts`](../workspace-contacts/contract.md)（`dept/tree` · `list-by-dept`）  
> **群组**：[`im-group-chat`](../im-group-chat/contract.md) · `GET /app-api/im/conversation/list`

## 背景

工作台 `/app/contacts` 扩展为飞书式目录：组织内按部门钻取；外部联系人、好友申请、星标；我的群组复用 IM 群会话。本契约描述 enrich 端点；`-web` 阶段外部/申请/星标用 store 临时数据，`-integrate` 接本 API。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`） |

## 组织内（既有，不变）

见 [`workspace-contacts/contract.md`](../workspace-contacts/contract.md)：

- `GET /app-api/system/dept/tree`
- `GET /app-api/system/user/list-by-dept?deptId=&keyword=`

前端主区按树节点钻取：面包屑 = 祖先路径；子部门 = `parentId === currentDeptId`；成员 = 当前 `deptId` 直属列表。

## REST（`-api`）

### GET /app-api/system/contact/external/list

外部联系人列表（当前租户、当前用户为 owner）。

**Response `data`**：`ExternalContactItem[]`

```json
[
  {
    "id": "1001",
    "userId": "201",
    "displayName": "Audrey",
    "orgLabel": "用户 687476 的组织",
    "avatarText": "A",
    "sortLetter": "A"
  }
]
```

| 字段 | 说明 |
|------|------|
| `id` | 关系行雪花 ID（JSON 字符串） |
| `userId` | 目标用户 ID |
| `sortLetter` | 服务端计算的首字母（A–Z 或 `#`）；前端亦可本地兜底 |
| `orgLabel` | 可选副标题（对方组织名等）；可为空串 |

排序：按 `sortLetter`，同组内按 `displayName`（`zh-CN` Collator）。

### GET /app-api/system/contact/request/list

好友 / 加联系人申请（指向当前用户）。

**Response `data`**：`ContactRequestItem[]`

```json
[
  {
    "id": "2001",
    "fromUserId": "201",
    "displayName": "Audrey",
    "subtitle": "想添加你为联系人",
    "avatarText": "A",
    "status": "pending",
    "createTime": "2026-07-26T10:00:00Z"
  }
]
```

`status`：`pending` | `accepted` | `rejected`（V1 实现 list 全量；写路径仅 `accept`）。

### POST /app-api/system/contact/request/{id}/accept

同意申请。成功后该条 `status=accepted`，并为当前用户 upsert 外部联系人（`fromUserId`）。

- 仅 `to_user_id` = 当前用户且 `status=pending` 可同意
- 其它情况返回业务错误

**Response `data`**：可为空对象或更新后的申请项（实现返回更新后的 `ContactRequestItem`）。

### GET /app-api/system/contact/starred/list

星标联系人。

**Response `data`**：`StarredContactItem[]`（含 `id`、`userId`、`displayName`、可选 `orgLabel`/`avatarText`/`username`）。

### PUT /app-api/system/contact/starred/{userId}

Body：`{ "starred": true | false }`  
取消星标：`starred: false`（逻辑删除关系行）。

**Response `data`**：可为空。

## 我的群组

不新增专用 API：`GET /app-api/im/conversation/list`，前端过滤 `type === "group"`。点击 → `/app/messages?conversationId=` 或 `im.selectConversation`。

## curl

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/app-api/system/contact/external/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/contact/request/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/system/contact/request/<id>/accept" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/contact/starred/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PUT "http://localhost:8080/app-api/system/contact/starred/<userId>" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"starred":true}'
```

验收样例 SQL 见 change `workspace-contacts-enrich-api` tasks / design（不写入 Flyway seed）。

## 发起添加 / 拒绝（`workspace-contact-add-external-*`）

> `-web`：前端 `contactAdd` store 草稿搜索 + outgoing；拒绝为 contacts 本地改状态。  
> `-api`：实现下列端点后 integrate 去草稿。

### GET /app-api/system/contact/search-account

Query：`mobile` **或** `email`（二选一，确切匹配）。

**Response `data`**：`ContactAccountSearchHit[]`

```json
[
  {
    "userId": "201",
    "displayName": "样依琳",
    "orgLabel": "合作方企业",
    "avatarText": "样",
    "alreadyAdded": false
  }
]
```

同一手机号/邮箱绑定多账号时全部返回。无匹配 → `[]`。

### POST /app-api/system/contact/request

```json
{ "targetUserId": "201", "message": "可选附言" }
```

成功后目标用户的 `request/list` 出现 `pending`。已是外部联系人或已有 pending → 业务错误或幂等（`-api` 定稿）。

### POST /app-api/system/contact/request/{id}/reject

仅 `to_user_id` = 当前用户且 `pending` 可拒绝 → `status=rejected`，不写 external。

## 前端映射

| UI | Store | 数据 |
|----|-------|------|
| 分类导航 | `stores/contacts.ts` `category` | 本地 |
| 组织内 | 同上 + `deptPath` | 既有 dept/user API |
| 外部 / 申请 / 星标 | 同上 | `GET/POST/PUT /app-api/system/contact/*`（integrate 后无 store seed） |
| 添加外部 | `stores/contactAdd` + Hub/通讯录入口 | `-web` 草稿；`-api` search + create request |
| 我的群组 | `stores/im` conversations | IM list（`type=group`） |

## V1 不在范围

- 邮箱联系人目录、服务台
- 拼音库；无 `sortLetter` 时前端拉丁 A–Z、其余 `#`
- 扫码加外部联系人实装（UI 可占位）
