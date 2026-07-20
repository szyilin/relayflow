# API 契约：workspace-docs-drive

> **状态**：integrate done（`workspace-docs-drive-integrate`；含跨容器移动）  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **母 change**：[`workspace-docs-drive-v1`](../../changes/workspace-docs-drive-v1/proposal.md)  
> **前置 schema**：`docs-drive-schema-v1`（`doc_drive_folder` / `doc_drive_item` / `FILE` + `storage_file_id`）

## 背景

员工工作台 `/app/docs` **云盘**：「我的文件夹」树 + 上传/下载 `FILE`（字节在 infra/MinIO）。V1 无共享文件夹、无 Wiki、无在云盘内新建 `RICH_DOC`。跨容器移动见后续子切片（本契约预留 `placements/move`）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |
| 数据范围 | 仅当前用户 `owner_user_id` 的 folder / item / object |

## ID 与 JSON

- 雪花 ID 在 JSON 中以 **string** 传递
- 时间：`TIMESTAMPTZ` → ISO-8601
- `folderId` / `parentId`：根目录为 `null`

## REST 前缀

`/app-api/docs/drive`

上传字节仍走既有：

- `POST /app-api/infra/file/upload-session`
- `POST /app-api/infra/file/upload-confirm`
- `GET /app-api/infra/file/download?fileId=`

---

## 文件夹

### GET /folders?parentId=

列出指定父级下的**子文件夹**（不含文件）。`parentId` 省略或空 = 根。

**Response `data`**：

```json
{
  "folders": [
    {
      "folderId": "100",
      "parentId": null,
      "name": "项目资料",
      "sortOrder": 0,
      "updateTime": "2026-07-20T12:00:00+08:00"
    }
  ]
}
```

### GET /folders/tree

可选：整棵文件夹树（嵌套 `children`）。V1 实现可二选一；前端 web Mock 用按层 `parentId` 导航。

### POST /folders

```json
{ "parentId": null, "name": "新建文件夹" }
```

**Response `data`**：创建后的 folder 摘要（同 list 项）。

### PUT /folders/{folderId}

```json
{ "name": "改名", "parentId": null, "sortOrder": 1 }
```

字段均可选；`parentId` 变更须防环且仅限本人树。

### DELETE /folders/{folderId}

软删。若仍有子文件夹或 drive items → **拒绝**（`DOC_DRIVE_FOLDER_NOT_EMPTY`）。

---

## 列表（文件夹 + 对象）

### GET /items?folderId=

`folderId` 省略或空 = 根。返回**当前层**子文件夹 + 放置的对象摘要。

**Response `data`**：

```json
{
  "folders": [
    {
      "folderId": "100",
      "parentId": null,
      "name": "项目资料",
      "sortOrder": 0,
      "updateTime": "2026-07-20T12:00:00+08:00"
    }
  ],
  "items": [
    {
      "itemId": "200",
      "folderId": null,
      "objectId": "300",
      "type": "FILE",
      "title": "产品说明.pdf",
      "storageFileId": "400",
      "sizeBytes": 102400,
      "mimeType": "application/pdf",
      "sortOrder": 0,
      "updateTime": "2026-07-20T12:00:00+08:00"
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `type` | `FILE` \| `RICH_DOC`（后者来自跨容器移入） |
| `storageFileId` | 仅 `FILE`；下载用 |
| `sizeBytes` / `mimeType` | `FILE` 可选摘要（可来自 FileApi） |

---

## 登记上传文件

### POST /files

前置：客户端已完成 infra upload-confirm，持有 `fileId`。

```json
{
  "folderId": null,
  "fileId": "400",
  "title": "产品说明.pdf"
}
```

**行为**：`FileApi` 校验文件归属 → 建 `doc_object(type=FILE, storage_file_id)` → 建 `doc_drive_item`。

**Response `data`**：item 摘要（同 list `items[]` 元素）。

---

## Item 变更

### PUT /items/{itemId}

```json
{ "title": "新文件名.pdf", "folderId": "100", "sortOrder": 1 }
```

改 `title` 同步 `doc_object.title`；改 `folderId` = 云盘内移动。

### DELETE /items/{itemId}

软删 item + object。V1 **不**级联删 infra 对象存储（仅解绑 docs）。

---

## 跨容器移动

### POST /placements/move

```json
{
  "objectId": "300",
  "target": "DRIVE",
  "folderId": null
}
```

或 `target: "LIBRARY"` + `parentId`（文档库父节点，根为 `null`）。

**行为**：

- `DRIVE`：要求当前在 Library；软删 `doc_library_node`；新建 `doc_drive_item`；`objectId` 不变
- `LIBRARY`：要求当前在 Drive 且类型为 `RICH_DOC`；软删 `doc_drive_item`；新建 `doc_library_node`；`FILE` → Library 拒绝（`DOC_TYPE_UNSUPPORTED`）
- 已在目标容器 / 无源 Placement → `DOC_PLACEMENT_INVALID`

**Response `data`**：

```json
{
  "objectId": "300",
  "target": "DRIVE",
  "placementId": "200"
}
```

`placementId`：Drive 为 `itemId`，Library 为 `nodeId`。

---

## 错误码

| 常量 | 码 | 含义 |
|------|-----|------|
| `DOC_DRIVE_FOLDER_NOT_EMPTY` | `1_006_001_007` | 删非空文件夹 |
| `DOC_DRIVE_FOLDER_INVALID` | `1_006_001_008` | 父文件夹不存在 / 越权 / 成环 |
| `DOC_STORAGE_FILE_INVALID` | `1_006_001_009` | fileId 无效或不属于当前用户 |
| `DOC_PLACEMENT_INVALID` | `1_006_001_010` | 放置无效或不支持该移动 |
| 复用 | | `DOC_NOT_FOUND` / `DOC_FORBIDDEN` / `DOC_TYPE_UNSUPPORTED` |

---

## curl 草图

```bash
# 根目录列表
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/docs/drive/items"

# 建文件夹
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"parentId":null,"name":"项目资料"}' \
  "$BASE/app-api/docs/drive/folders"

# 登记文件（fileId 来自 infra upload-confirm）
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"folderId":null,"fileId":"400","title":"产品说明.pdf"}' \
  "$BASE/app-api/docs/drive/files"

# Library → Drive
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"objectId":"300","target":"DRIVE","folderId":null}' \
  "$BASE/app-api/docs/drive/placements/move"

# Drive → Library
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"objectId":"300","target":"LIBRARY","parentId":null}' \
  "$BASE/app-api/docs/drive/placements/move"

# 下载（infra）
curl -s -L -H "Authorization: Bearer $TOKEN" \
  -o out.bin \
  "$BASE/app-api/infra/file/download?fileId=400"
```

---

## 前端对接要点

| 项 | 说明 |
|----|------|
| 页面 | `/app/docs` → 左栏「云盘」 |
| Store | `stores/docsDrive.ts` 走真实 `/app-api/docs/drive/**`（无 Mock） |
| 上传 | `uploadPrivateFile` → `POST /drive/files` |
| 下载 | `downloadAuthenticatedFile('/app-api/infra/file/download?fileId=' + id, title)` |
| RICH_DOC | 列表中打开走既有 `?docId=` / `openDocument`；可移回文档库 |
| 移动 | 库树「移动到云盘」/ 云盘「移回文档库」→ `POST /placements/move` |
