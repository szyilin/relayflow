# 提案：工作台云文档 · 云盘 V1（workspace-docs-drive-v1 · 母 change）

## Why

「我的文档库」已交付，但 `/app/docs` 侧栏「云盘」仍为占位。飞书心智里，**上传本地文件默认进云盘（文件夹模型）**，与个人文档库（页面树）并列。补齐云盘后，三大容器中第二块可用，并兑现「本体只住一处、跨容器靠移动」。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。依赖已有 `relayflow-module-docs` 与 `infra` 工作台文件上传/下载。

### 本 change 实际交付（云盘 MVP）

1. **Drive Placement**：`doc_drive_folder` + `doc_drive_item`（或等价表，见 design）；每人每租户隐式「我的文件夹」树（B 类，无头表 ensure）
2. **文件夹**：新建 / 重命名 / 树内移动 / 删除（空文件夹或级联策略见 design）
3. **上传文件**：`object type = FILE`；二进制走已有 **`/app-api/infra/file/upload-*` + download**；docs 域只建 `doc_object` + drive 放置并绑定 `infra_file` id
4. **浏览**：按文件夹列出子文件夹 + 文件项；下载 FILE
5. **跨容器移动（本母 change 内单独子切片）**：Library ↔ Drive **移动**（保留同一 `doc_object.id`）；禁止默认同步双份
6. **UI**：`/app/docs` 启用「云盘」面板（我的文件夹）；「共享文件夹 / 知识库」仍占位
7. **看板**：登记 `workspace-docs-drive` 切片与 lane contract

## Capabilities

### New Capabilities

- （无新 capability 名；行为并入 `docs` 域增量）

### Modified Capabilities

- `docs`：增加云盘容器（我的文件夹）、`FILE` object type、与 Library 的移动规则；共享文件夹 / 知识库仍非目标

## Impact

| 层 | 变更 |
|----|------|
| DB | Flyway：`doc_drive_*`；`doc_object` 扩展 `FILE`（如 `storage_file_id` 等，见 design） |
| 后端 | `docs-biz` 增 Drive API；依赖 `infra-api`（`FileApi` / 既有 app 上传下载） |
| 前端 | `/app/docs` 云盘面板 + 上传 UX；扩展 store / `api/app/docs` |
| 文档 | 看板登记；contract 新 lane |

## 非目标（本 change / 云盘 V1）

- **共享文件夹**、文件夹协作者、链接分享整夹
- 知识库
- 在云盘内「新建 RICH_DOC / 表格 / 幻灯片」（V1 以 **上传 FILE + 从文档库迁入** 为主；新建在线文档仍走文档库，可后置「新建到云盘」）
- `doc_embed` / 流程图等重型块
- Word/PDF 导出、星标、与我共享真数据
- 管理端网盘治理、容量配额 UI（可后置简单上限）

## 拍板结论（立项默认 · 实现前可再确认）

| 项 | 结论 |
|----|------|
| 范围 | **仅「我的文件夹」** + 上传 FILE + 浏览/下载/树操作 |
| 共享文件夹 | **V1 不做** |
| 跨容器移动 | **要**（Library ↔ Drive），单独子切片，避免与上传 UI 缠死 |
| 存储 | 复用 MinIO + 既有 app-api infra 上传下载；**不**新建第二套对象存储 |
| 文件夹实体 | **是**（云盘专属）；**不是** `RICH_DOC` |

## 子 change 切片（实现顺序）

```text
workspace-docs-drive-v1                 ← 本 change（规划母版）
├── docs-drive-schema-v1                [平台] Flyway + codegen（drive 表 + FILE 字段）
├── workspace-docs-drive-web            UI + 临时 store + contract（文件夹/上传/列表）
├── workspace-docs-drive-api            Drive REST + 绑定 infra_file
├── workspace-docs-drive-integrate      去临时数据联调
├── workspace-docs-drive-move-web       （可选拆分）跨容器移动 UI + contract 增量
├── workspace-docs-drive-move-api
└── workspace-docs-drive-move-integrate
```

若单人节奏紧，**移动** 三切片可合并为一刀 `workspace-docs-drive-move`（仍建议 web→api→integrate 顺序写在 tasks）。

## 后续（本 change 不写实现 tasks）

```text
workspace-docs-wiki-v1                  知识库
workspace-docs-drive-share-v1           共享文件夹（示意）
```
