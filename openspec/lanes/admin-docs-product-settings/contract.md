# API 契约：admin-docs-product-settings（Wave B）

> **状态**：api ready（web 本地草稿仍可用作回退）；待 integrate 去草稿  
> **变更**：`admin-docs-product-settings-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

云文档组织策略（B）：成员分享 / Markdown 导出 / 云盘上传三开关。文件管理（A1）、存储配额（A2）另见同母 change design。

## 鉴权

| 面 | 路径 | 鉴权 |
|----|------|------|
| Admin | `/admin-api/docs/product-settings` | JWT + 管理身份 + `docs:product-settings:query` / `update` |
| App | `/app-api/docs/settings/effective` | JWT + 有效成员 |

前端：`api/admin/docsProductSettings.ts` → `stores/docsProductSettings.ts` → `/admin/products/docs/permissions`。

## Admin

### GET `/admin-api/docs/product-settings`

响应 `data`：

```json
{
  "memberShareEnabled": true,
  "markdownExportEnabled": true,
  "driveUploadEnabled": true
}
```

无行时返回上表默认值（均为 `true`）。

### PUT `/admin-api/docs/product-settings`

请求 body 同 GET 三字段；upsert `doc_tenant_settings`；响应同 GET。

## App

### GET `/app-api/docs/settings/effective`

返回与强制一致的三布尔值（字段名同 Admin）。

## 强制点（B-api）

| 开关 | 为 false 时 |
|------|-------------|
| `memberShareEnabled` | 拒绝 share 写（POST/PUT/DELETE grants）；已有授权仍可读 |
| `markdownExportEnabled` | 拒绝 `format=md` 导出 |
| `driveUploadEnabled` | 拒绝注册新云盘 FILE |

## 前端路由

| 路径 | 说明 |
|------|------|
| `/admin/products/docs` | redirect → permissions |
| `/admin/products/docs/permissions` | B 三开关（本契约） |
| `/admin/products/docs/files` | A1 占位 |
| `/admin/products/docs/storage` | A2 占位 |

## curl 草案

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/docs/product-settings

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"memberShareEnabled":true,"markdownExportEnabled":true,"driveUploadEnabled":false}' \
  http://localhost:8080/admin-api/docs/product-settings
```
