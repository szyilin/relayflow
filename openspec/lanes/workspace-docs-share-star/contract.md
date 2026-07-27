# API 契约：workspace-docs-share-star

> **状态**：archived / done（主规格已同步 `openspec/specs/docs`、`openspec/specs/im`）  

> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **母 change**：[`workspace-docs-share-star-v1`](../../changes/workspace-docs-share-star-v1/proposal.md)  
> **前端 change**：[`workspace-docs-share-star-web`](../../changes/workspace-docs-share-star-web/proposal.md)

## 背景

工作台 `/app/docs`：对象级分享（`VIEW`|`EDIT`）、与我共享、星标、个人访问足迹「最近」（含 `accessState` 墓碑）。仅 `RICH_DOC`。触达：`docs-bot` + `ImBotApi`（`-api` 实现；本契约标明字段）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |
| 分享管理 | 仅 object **owner** |
| 文档读写 | owner **或** 有效 share（`VIEW` 读/导出；`EDIT` 写 body） |

## ID 与 JSON

- 雪花 ID JSON 为 **string**
- 时间：ISO-8601
- `permission`：`VIEW` \| `EDIT`
- `accessState`：`OK` \| `DELETED` \| `NO_ACCESS`
- `locationLabel`：展示用文案（如 `我的文档库` / `云盘` / `与我共享` / `已删除`）

## 深链

- `/app/docs?docId=<objectId>`（Bot 卡片同此）

## REST 前缀

`/app-api/docs`

---

## 分享（owner）

### GET /documents/{objectId}/shares

协作者列表。

**Response `data`**：

```json
{
  "shares": [
    {
      "shareId": "1",
      "granteeUserId": "20",
      "granteeDisplayName": "张三",
      "permission": "EDIT",
      "grantedByUserId": "10",
      "createTime": "2026-07-27T12:00:00+08:00"
    }
  ]
}
```

### POST /documents/{objectId}/shares

```json
{ "granteeUserId": "20", "permission": "VIEW" }
```

**Response `data`**：创建后的 share 项（同上）。  
副作用（best-effort）：`ImBotApi.send(docs-bot, SINGLE)`，深链 `/app/docs?docId={objectId}`；失败不回滚。

### PUT /documents/{objectId}/shares/{shareId}

```json
{ "permission": "EDIT" }
```

### DELETE /documents/{objectId}/shares/{shareId}

所有者撤销。

---

## 与我共享

### GET /shared?limit=

**Response `data`**：

```json
{
  "items": [
    {
      "objectId": "10",
      "title": "周报",
      "type": "RICH_DOC",
      "permission": "EDIT",
      "ownerUserId": "10",
      "ownerDisplayName": "李四",
      "grantedByUserId": "10",
      "grantedByDisplayName": "李四",
      "sharedAt": "2026-07-27T12:00:00+08:00",
      "starred": false
    }
  ]
}
```

### DELETE /shared/{objectId}

接收方 **leave**（放弃自己的授权）。

---

## 星标

### GET /starred?limit=

```json
{
  "items": [
    {
      "objectId": "10",
      "title": "周报",
      "type": "RICH_DOC",
      "starredAt": "2026-07-27T12:00:00+08:00",
      "accessState": "OK",
      "ownerUserId": "10",
      "ownerDisplayName": "李四",
      "locationLabel": "与我共享"
    }
  ]
}
```

仅返回当前仍可读的；失权项可省略或 `accessState!=OK`（V1：**省略**）。

### PUT /documents/{objectId}/star

加星（需读权限）。无 body。

### DELETE /documents/{objectId}/star

取消星标。

---

## 最近（visit）

### GET /recent?limit=

**BREAKING 相对 library V1**：由「仅自有存活 object」改为个人 visit 列表。

```json
[
  {
    "objectId": "10",
    "title": "周报",
    "titleSnapshot": "周报",
    "type": "RICH_DOC",
    "accessState": "OK",
    "locationLabel": "我的文档库",
    "ownerUserId": "10",
    "ownerDisplayName": "我",
    "ownedByMe": true,
    "createTime": "2026-07-20T10:00:00+08:00",
    "lastOpenedAt": "2026-07-27T12:00:00+08:00",
    "starred": true
  }
]
```

| `accessState` | 行为 |
|---------------|------|
| `OK` | 可打开 |
| `DELETED` | 列表可见；GET document 拒绝；UI 提示已删除 |
| `NO_ACCESS` | 列表可见；GET document 拒绝；UI 提示无权限 |

打开文档成功（GET document）时服务端 upsert visit。

### DELETE /recent/{objectId}

**从最近移除**（只删 visit，不删文档）。

---

## 文档读写（ACL 修订）

既有：

- `GET /documents/{objectId}`
- `PUT /documents/{objectId}/body`
- `GET /documents/{objectId}/export?format=md`

授权：owner 或足够 share。响应可增：

```json
{
  "objectId": "10",
  "title": "周报",
  "type": "RICH_DOC",
  "body": { "type": "doc", "content": [] },
  "bodyFormat": "tiptap_json_v1",
  "contentVersion": 3,
  "lastOpenedAt": "…",
  "ownerUserId": "10",
  "permission": "EDIT",
  "ownedByMe": false
}
```

`permission`：调用方有效权限（`OWNER` 可映射为响应里 `ownedByMe=true` + `permission=EDIT`，或单独 `OWNER`——**实现采用** `ownedByMe` + `permission`∈`VIEW|EDIT`）。

---

## 错误码

| code（业务） | 含义 |
|--------------|------|
| `DOC_NOT_FOUND` | 不存在或不可见 |
| `DOC_FORBIDDEN` | 无权限（含 VIEW 写、非 owner 管分享） |
| `DOC_VERSION_CONFLICT` | 既有 |
| `DOC_SHARE_SELF` | 分享给自己 |
| `DOC_SHARE_TYPE_UNSUPPORTED` | 非 RICH_DOC（如 FILE） |
| `DOC_GRANTEE_INVALID` | 非本租户 ACTIVE 成员 |

（具体数字码由 `-api` 写入 `ErrorCodeConstants`，与现有 docs 段连续。）

---

## curl 示例

```bash
# 分享
curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"granteeUserId":"20","permission":"EDIT"}' \
  "http://localhost:8080/app-api/docs/documents/10/shares"

# 与我共享
curl -sS -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/shared?limit=50"

# 最近
curl -sS -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/recent?limit=20"

# 从最近移除
curl -sS -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/recent/10"

# 星标
curl -sS -X PUT -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/documents/10/star"
```
