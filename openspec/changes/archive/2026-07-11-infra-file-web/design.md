# 设计：文件管理 Web — infra-file-web

## Context

- ⑤ `infra-file-upload-api`：`upload-session` / `confirm` 已 ready
- 占位页 `/admin/infra/file`；列表/删除 API 未单独立项，本 change contract 定义并同期实现

## Goals / Non-Goals

**Goals:**

- `useDirectUpload`：session → PUT presigned → confirm → 返回 `fileId`
- 分页列表：`GET /admin-api/infra/file/page`（`infra:file:list`）
- 逻辑删除：`DELETE /admin-api/infra/file/{id}`（`infra:file:delete`）
- 上传后刷新列表；`pnpm build` + 浏览器直传验收

**Non-Goals:**

- 下载（→ ⑦ `infra-file-download-api`）
- 分片上传、批量上传 UI

## Decisions

### D1：直传 composable

```text
useDirectUpload.upload(file, { accessLevel })
  → createUploadSession
  → fetch(uploadUrl, PUT, body=file, headers)
  → confirmUpload({ uploadId, etag, size })
```

页面 **禁止** 引入 MinIO SDK。

### D2：列表 API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/admin-api/infra/file/page` | `infra:file:list` |

Query：`pageNo`, `pageSize`, `keyword`（匹配 `original_name`）

### D3：删除 API

| Method | Path | Permission |
|--------|------|------------|
| DELETE | `/admin-api/infra/file/{id}` | `infra:file:delete` |

MyBatis 逻辑删除 `infra_file`；V1 不删 MinIO 对象（与 session 孤儿对象策略一致）。

### D4：UI

- `AdminPageHeader` + 上传按钮 + 关键词筛选
- `UTable` + `UPagination`
- 删除 `UModal` 确认
- 权限：`infra:file:upload` 控制上传；`infra:file:delete` 控制删除

## 验证

```bash
cd web && pnpm build
# 浏览器 /admin/infra/file：上传 → 列表出现 → 删除
```
