# 设计：工作台云文档 · 云盘 V1

## Context

- 前置：[`workspace-docs-library-v1`](../workspace-docs-library-v1/design.md) 已交付 Library + `RICH_DOC`（TipTap JSON 真源）
- 产品：飞书云盘 ≈ 文件夹模型；官方「本体只住一处」；上传偏云盘
- 已有能力：`/app-api/infra/file/upload-session|upload-confirm`、download（IM/资料已用）；`FileApi.getFile` / `bindFile`
- 约束：小步；仅「我的文件夹」；docs-biz 不直查 `infra_` 表

## Goals / Non-Goals

**Goals:**

- 个人云盘文件夹树 + 上传/下载 FILE + 列表浏览
- 与 Library **移动**（同 `doc_object.id` 换 Placement）
- `/app/docs`「云盘」入口可用

**Non-Goals:**

- 共享文件夹、Wiki、在云盘内新建在线文档类型、配额产品化

## Decisions

### D0：容器关系（不变）

```text
DocObject ──唯一 Placement──┬─ LIBRARY (doc_library_node)
                            ├─ DRIVE   (doc_drive_item + folder)
                            └─ WIKI    (后置)
```

### D1：表模型

```text
doc_drive_folder
  id, tenant_id, owner_user_id
  parent_id          -- NULL = 我的文件夹根下
  name
  sort_order
  + 公共字段

doc_drive_item                 -- 对象在云盘中的放置
  id, tenant_id, owner_user_id
  folder_id          -- NULL = 根目录
  object_id          -- UNIQUE（deleted=0）；指向 doc_object
  sort_order
  + 公共字段

doc_object 扩展
  type 增加 FILE（CHECK 含 RICH_DOC | FILE）
  storage_file_id    -- BIGINT NULL；FILE 时必填，指向 infra_file.id
  -- RICH_DOC：body / body_format / content_version 照旧；FILE 时 body 可空或占位
```

- 无 `doc_drive` 头表（B 类空树）
- `FOLDER` **不是** `doc_object.type`，只用 `doc_drive_folder`
- 删文件夹：V1 推荐 **非空则拒绝**（须先移走/删内容）；避免静默级联丢文件（可在 tasks 拍板）

**备选**：单表 path 字符串 — 否，不利于移动与权限后续。

### D2：FILE 与 infra

```text
浏览器 → POST /app-api/infra/file/upload-session
       → PUT MinIO
       → POST /app-api/infra/file/upload-confirm  → infra_file.id
       → POST /app-api/docs/drive/files { folderId, fileId, title? }
            docs-biz: FileApi.getFile 校验归属/存在
                      建 doc_object(FILE, storage_file_id)
                      建 doc_drive_item
```

下载：`GET /app-api/infra/file/download?fileId=`（既有）或 docs 包装 302；优先复用既有 app download，权限以 infra 私有文件 + 当前用户为准（实现时核对 IM 同路径）。

`docs-biz` → `infra-api`（`FileApi`）；禁止 Mapper 扫 `infra_file`。

### D3：权限（云盘 V1）

- JWT + 有效成员
- 仅 `owner_user_id = 当前用户` 的 folder / item / object
- 无分享、无管理面 permission

### D4：App API（草图）

前缀：`/app-api/docs/drive/`（与 `/library` 并列）

| 能力 | 示意 |
|------|------|
| 文件夹树或子列表 | `GET /folders/tree` 或 `GET /folders?parentId=` |
| 建文件夹 | `POST /folders` |
| 改/删文件夹 | `PUT/DELETE /folders/{id}` |
| 列出内容 | `GET /items?folderId=`（文件夹 + FILE/RICH_DOC 摘要） |
| 登记上传文件 | `POST /files` `{ folderId, fileId, title? }` |
| 改 item（改名/移动） | `PUT /items/{id}` 或按 objectId |
| 删 item | `DELETE /items/{id}`（软删 item + object；infra 文件逻辑删策略见实现：V1 可只解绑 docs，infra 行保留） |
| 跨容器移动 | `POST /placements/move` `{ objectId, target: LIBRARY\|DRIVE, parentId\|folderId }` |

错误码新增示例：`DOC_DRIVE_FOLDER_NOT_EMPTY`、`DOC_DRIVE_FOLDER_INVALID`、`DOC_STORAGE_FILE_INVALID`；复用 `DOC_NOT_FOUND` / `DOC_FORBIDDEN` / `DOC_PARENT_INVALID`。

契约：`openspec/lanes/workspace-docs-drive/contract.md`（`-web` 起草）。

### D5：前端信息架构

```text
/app/docs
  左栏：我的文档库 | 最近 | 云盘（启用）| 与我共享/星标/知识库（占位）
  云盘面板：
    面包屑 + 新建文件夹 + 上传
    列表：文件夹 / 文件（图标、大小、时间）
    点击文件夹进入；FILE 下载；RICH_DOC（若迁入）打开既有编辑器
  移动：
    「移动到云盘 / 移动到文档库」对话框（选目标文件夹或库父节点）
```

### D6：子 change 与验证

见 proposal 切片表。验证：compile；`pnpm build` + `typecheck`；浏览器上传 PDF → 刷新仍在 → 下载；建文件夹；从文档库移入云盘后库树消失、云盘可见。

### D7：明确不做

共享文件夹、Wiki、云盘内新建 TipTap 文档（可后置）、双写同步。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| infra 上传权限与 docs 所有者不一致 | confirm 后 `FileApi.getFile` 校验 uploader=当前用户再 bind |
| 删 docs 对象残留 MinIO | V1 文档化「只软删 docs；对象清理后置」 |
| 移动与上传并行复杂度 | 移动独立子切片，先交付纯云盘再互通 |
| 列表混 RICH_DOC+FILE | item 摘要带 `type`；UI 分支打开/下载 |

## Migration Plan

- 增量 Flyway；`doc_object.type` CHECK 扩展需重建约束（PG）
- 回滚：卸 Drive API/UI；表可留空

## Open Questions

1. 删非空文件夹：拒绝 vs 级联 — **默认拒绝**（实现前可改）
2. 删 FILE item 是否调用 infra 逻辑删 — **默认否**（仅 docs 软删）
3. 云盘根是否显示「从文档库移来的 RICH_DOC」可编辑 — **是**（复用既有编辑器路由 `?docId=`）
