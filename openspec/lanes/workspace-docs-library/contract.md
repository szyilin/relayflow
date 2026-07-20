# API 契约：workspace-docs-library

> **状态**：integrate 完成（`workspace-docs-library-integrate`）；store 无 Mock  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **母 change**：[`workspace-docs-library-v1`](../../changes/workspace-docs-library-v1/proposal.md)

## 背景

员工工作台 `/app/docs`：**我的文档库**页面树 + `RICH_DOC`（TipTap JSON 真源）+ 最近 + Markdown 导出。V1 无分享、无云盘/知识库、无 `doc_embed`。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |
| 数据范围 | 仅当前用户 `owner_user_id` 的库与文档 |

## ID 与 JSON

- 雪花 ID 在 JSON 中以 **string** 传递（与其它 app-api 一致）
- `body`：TipTap/ProseMirror JSON 对象（非 HTML 字符串）
- `bodyFormat`：V1 固定 `tiptap_json_v1`
- 时间：`TIMESTAMPTZ` → ISO-8601

## 深链

- `/app/docs?docId=<objectId>` 打开对应文档（object id，非 node id）

## REST 前缀

`/app-api/docs`

---

## 库树

### GET /library/tree

**Response `data`**：

```json
{
  "nodes": [
    {
      "nodeId": "1",
      "parentId": null,
      "objectId": "10",
      "title": "未命名文档",
      "sortOrder": 0,
      "children": []
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `nodes` | 根节点列表（可含嵌套 `children`）；也可扁平返回由前端组树——**实现采用嵌套 `children`** |
| `parentId` | 根为 `null` |

### POST /library/nodes

```json
{ "parentId": null, "title": "未命名文档" }
```

**Response `data`**：

```json
{
  "nodeId": "1",
  "objectId": "10",
  "parentId": null,
  "title": "未命名文档",
  "sortOrder": 0,
  "contentVersion": 0,
  "bodyFormat": "tiptap_json_v1"
}
```

创建时服务端创建 `doc_object`（空 TipTap doc）+ `doc_library_node`。

### PUT /library/nodes/{nodeId}

```json
{ "title": "周报", "parentId": null, "sortOrder": 1 }
```

字段均可选；改 `title` 同步 `doc_object.title`。`parentId` 变更须防环。

**Response `data`**：更新后的节点摘要（同 create 字段子集即可）。

### DELETE /library/nodes/{nodeId}

软删 node + 关联 object。无 body。

---

## 文档

### GET /documents/{objectId}

打开时更新 `last_opened_at`。

**Response `data`**：

```json
{
  "objectId": "10",
  "title": "周报",
  "type": "RICH_DOC",
  "body": { "type": "doc", "content": [{ "type": "paragraph" }] },
  "bodyFormat": "tiptap_json_v1",
  "contentVersion": 3,
  "lastOpenedAt": "2026-07-20T12:00:00+08:00"
}
```

### PUT /documents/{objectId}/body

```json
{
  "body": { "type": "doc", "content": [{ "type": "paragraph", "content": [{ "type": "text", "text": "hi" }] }] },
  "contentVersion": 3
}
```

成功：`contentVersion` +1。

**Response `data`**：

```json
{ "contentVersion": 4 }
```

冲突（版本不匹配）：业务错误 `DOC_VERSION_CONFLICT`。

### GET /documents/{objectId}/export?format=md

- `format=md`：返回 Markdown 文本  
  - 推荐：`Content-Type: text/markdown; charset=utf-8`，或统一 `CommonResult` 包一层 `{ "markdown": "..." }`  
  - **本契约采用 CommonResult**：`data.markdown` 为 string
- `format=docx` / `pdf`：`DOC_EXPORT_FORMAT_UNSUPPORTED`

```json
{ "markdown": "# 周报\n\nhi\n" }
```

---

## 最近

### GET /recent?limit=

| 参数 | 默认 | 说明 |
|------|------|------|
| `limit` | 20 | 最大 50 |

按 `last_opened_at` 降序（null 靠后）。

**Response `data`**：

```json
[
  {
    "objectId": "10",
    "title": "周报",
    "lastOpenedAt": "2026-07-20T12:00:00+08:00"
  }
]
```

---

## 错误码

| code 名 | 数字码 | 场景 |
|---------|--------|------|
| `DOC_NOT_FOUND` | `1006001001` | 节点/文档不存在或已删 |
| `DOC_FORBIDDEN` | `1006001002` | 非所有者 |
| `DOC_VERSION_CONFLICT` | `1006001003` | 正文版本冲突 |
| `DOC_PARENT_INVALID` | `1006001004` | parent 非法 / 成环 / 跨用户 |
| `DOC_TYPE_UNSUPPORTED` | `1006001005` | 非 RICH_DOC |
| `DOC_EXPORT_FORMAT_UNSUPPORTED` | `1006001006` | 非 md 导出 |

> **状态**：integrate 完成；前端 store 已接 API。

## curl 示例

```bash
# 树
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/app-api/docs/library/tree

# 新建
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"parentId":null,"title":"未命名文档"}' \
  http://localhost:8080/app-api/docs/library/nodes

# 读文档
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/app-api/docs/documents/10

# 保存
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"body":{"type":"doc","content":[]},"contentVersion":0}' \
  http://localhost:8080/app-api/docs/documents/10/body

# 导出 MD
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/documents/10/export?format=md"

# 最近
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/app-api/docs/recent?limit=20"
```
