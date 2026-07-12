# API 契约：workspace-contacts

> **状态**：草案（`-web` lane）  
> **起草**：`workspace-contacts` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **IM 衔接**：[`im-direct-chat`](../im-direct-chat/contract.md)

## 背景

员工工作台 `/app/contacts`：按部门浏览组织内同事，点击名片可发起单聊。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`，**不用** `sys_permission`） |

## REST 端点

### GET /app-api/system/dept/tree

当前租户部门树（只读）。

**Response `data`**：`DeptTreeNode[]`（或扁平列表由前端建树，与 admin 一致）

```json
[
  { "id": "1", "parentId": "0", "name": "默认企业", "sort": 0 },
  { "id": "2", "parentId": "1", "name": "研发部", "sort": 1 }
]
```

### GET /app-api/system/user/list-by-dept

部门**直属**主部门成员。

**Query**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `deptId` | string | 是 | 部门 ID |
| `keyword` | string | 否 | 昵称/用户名模糊 |

**Response `data`**：`ContactItem[]`

```json
[
  {
    "id": "102",
    "nickname": "李晓明",
    "username": "liming",
    "deptId": "2",
    "deptName": "研发部",
    "avatarText": "李"
  }
]
```

### GET /app-api/system/user/profile（V1 可选）

**Query**：`userId`  
**Response**：与 `ContactItem` 同构或扩展（个性签名占位可前端写死）

## 前端 → IM

| 操作 | 行为 |
|------|------|
| 名片「消息」 | `imStore.openDirectChat(peerUserId)` → `/app/messages` |
| 首条发送 | `POST /app-api/im/message/send` 带 `peerUserId`（无 `conversationId`） |

## curl 示例

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/app-api/system/dept/tree" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/system/user/list-by-dept?deptId=1" \
  -H "Authorization: Bearer $TOKEN"
```

## 前端映射

| UI | Store | API |
|----|-------|-----|
| `/app/contacts` | `stores/contacts.ts` | `api/app/contacts.ts` |
| Mock 回退 | store 内 | `mocks/contacts.ts`（仅 store 引用） |

## V1 不在范围

- 外部联系人、星标、群组
- `includeChildren` 递归子部门成员
- 资料编辑 API
