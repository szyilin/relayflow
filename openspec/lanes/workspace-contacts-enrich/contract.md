# API 契约：workspace-contacts-enrich

> **状态**：草案（`-web` lane）  
> **起草**：`workspace-contacts-enrich-web`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **组织内既有**：[`workspace-contacts`](../workspace-contacts/contract.md)（`dept/tree` · `list-by-dept`）  
> **群组**：[`im-group-chat`](../im-group-chat/contract.md) · `GET /app-api/im/conversation/list`

## 背景

工作台 `/app/contacts` 扩展为飞书式目录：组织内按部门钻取；外部联系人、好友申请、星标；我的群组复用 IM 群会话。本契约描述 **后续 `-api`** 需补齐的端点；`-web` 阶段外部/申请/星标用 store 临时数据。

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

## 拟新增 REST（`-api`）

### GET /app-api/system/contact/external/list

外部联系人列表。

**Response `data`**：`ExternalContactItem[]`

```json
[
  {
    "id": "e1",
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
| `sortLetter` | 服务端计算的首字母（A–Z 或 `#`）；前端亦可本地兜底 |
| `orgLabel` | 可选副标题（对方组织名等） |

排序：按 `sortLetter`，同组内按 `displayName`（`zh-CN`）。

### GET /app-api/system/contact/request/list

好友 / 加联系人申请。

**Response `data`**：`ContactRequestItem[]`

```json
[
  {
    "id": "r1",
    "fromUserId": "201",
    "displayName": "Audrey",
    "subtitle": "想添加你为联系人",
    "avatarText": "A",
    "status": "pending",
    "createTime": "2026-07-26T10:00:00Z"
  }
]
```

`status`：`pending` | `accepted` | `rejected`（V1 可仅实现 pending/accepted）。

### POST /app-api/system/contact/request/{id}/accept

同意申请。成功后该条 `status=accepted`，并可出现在外部联系人列表。

### GET /app-api/system/contact/starred/list

星标联系人。

**Response `data`**：与 `ContactItem` 同构或含 `userId` + 显示字段。

### PUT /app-api/system/contact/starred/{userId}

Body：`{ "starred": true | false }`  
取消星标：`starred: false`。

## 我的群组

不新增专用 API：`GET /app-api/im/conversation/list`，前端过滤 `type === "group"`。点击 → `/app/messages?conversationId=` 或 `im.selectConversation`。

## curl 草图（后续 `-api`）

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/app-api/system/contact/external/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/contact/request/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/system/contact/request/r1/accept" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/contact/starred/list" \
  -H "Authorization: Bearer $TOKEN"
```

## 前端映射（`-web`）

| UI | Store | 数据 |
|----|-------|------|
| 分类导航 | `stores/contacts.ts` `category` | 本地 |
| 组织内 | 同上 + `deptPath` | 既有 dept/user API |
| 外部 / 申请 / 星标 | 同上临时数组 | integrate 删除 |
| 我的群组 | `stores/im` conversations | IM list |

## V1 不在范围

- 邮箱联系人、服务台
- 添加外部联系人完整流程（仅申请列表同意）
- 拼音库；无 `sortLetter` 时前端拉丁 A–Z、其余 `#`
