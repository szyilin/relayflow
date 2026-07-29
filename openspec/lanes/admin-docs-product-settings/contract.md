# API 契约：admin-docs-product-settings（B + A1 草案）

> **状态**：B = integrate 完成；A1 = web UI ready（本地草稿），api 未开工  
> **变更**：`admin-docs-product-settings-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

- **B**：组织策略三开关（成员分享 / MD 导出 / 云盘上传）
- **A1**：企业文件运营（列表 / 转移 / 删 / 恢复 / 销毁）
- **A2**：按人用量（后置）

---

## Wave B（已交付）

### 鉴权

| 面 | 路径 | 鉴权 |
|----|------|------|
| Admin | `/admin-api/docs/product-settings` | `docs:product-settings:query` / `update` |
| App | `/app-api/docs/settings/effective` | JWT + 有效成员 |

### Admin GET/PUT `/admin-api/docs/product-settings`

```json
{
  "memberShareEnabled": true,
  "markdownExportEnabled": true,
  "driveUploadEnabled": true
}
```

### App GET `/app-api/docs/settings/effective`

字段同 Admin。强制：share 写 / md export / drive file 注册。

---

## Wave A1（web 草案）

前端：`api/admin/docsFiles.ts` → `stores/docsFiles.ts` → `/admin/products/docs/files`。  
**禁止**返回文档正文；**禁止**用 `/admin-api/infra/file` 冒充。

### 鉴权（A1-api 种子）

| 码 | 用途 |
|----|------|
| `docs:files:query` | 列表 |
| `docs:files:transfer` | 转移所有者 |
| `docs:files:delete` | 软删 |
| `docs:files:restore` | 恢复 |
| `docs:files:purge` | 销毁 |

### GET `/admin-api/docs/files`

Query：`pageNo`、`pageSize`、`keyword`（标题）、`type`=`RICH_DOC|FILE`、`lifecycle`=`ACTIVE|DELETED`、`ownerUserId`。

响应：

```json
{
  "list": [
    {
      "objectId": "9001",
      "title": "产品路线图",
      "type": "RICH_DOC",
      "lifecycle": "ACTIVE",
      "ownerUserId": "100",
      "ownerDisplayName": "管理员",
      "updatedAt": "2026-07-29T12:00:00Z",
      "sizeBytes": null,
      "deletedAt": null,
      "daysUntilPurge": null
    }
  ],
  "total": 1,
  "pageNo": 1,
  "pageSize": 20
}
```

不含 `body`。列表含 ACTIVE + DELETED（未 purge）；不含 PURGED。

### POST `/admin-api/docs/files/transfer-owner`

```json
{ "objectIds": ["9001"], "newOwnerUserId": "101" }
```

仅 `ACTIVE`；新主须本企业 ACTIVE 成员。V1：**整批事务**——任一失败则全部回滚（写死）。转移后 placement 迁到新主「我的文档库」或「我的文件夹」根（A1-api contract 二选一写死）。

### POST `/admin-api/docs/files/delete`

```json
{ "objectIds": ["9001"] }
```

→ `DELETED` + `deletedAt`。

### POST `/admin-api/docs/files/restore`

```json
{ "objectIds": ["9003"], "newOwnerUserId": null }
```

`newOwnerUserId` 省略/null = 原主；否则指定新主。仅 `DELETED` 且自 `deletedAt` 起 ≤90 天。

### POST `/admin-api/docs/files/purge`

```json
{ "objectIds": ["9003"] }
```

→ `PURGED`，不可恢复。

### 前端路由

| 路径 | 说明 |
|------|------|
| `/admin/products/docs/permissions` | B |
| `/admin/products/docs/files` | A1（本波） |
| `/admin/products/docs/storage` | A2 占位 |

### curl 草案（A1）

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/docs/files?pageNo=1&pageSize=20&lifecycle=ACTIVE"

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"objectIds":["9001"],"newOwnerUserId":"101"}' \
  http://localhost:8080/admin-api/docs/files/transfer-owner
```
