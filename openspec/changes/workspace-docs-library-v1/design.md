# 设计：工作台云文档 · 我的文档库 V1

## Context

- 产品参考：飞书「云文档」= 内容形态 + **三种存放容器**；官方明确「本体只住一处」。飞书文档是 **块编辑器载体**（可插流程图等），不是 MD 编辑器。
- 现状：`/app/docs` 空壳；无 `docs` 域模块；已有 `infra` MinIO（供云盘后续复用，本 change 不接上传）
- 约束：前端优先；`doc_` 前缀；本设计只覆盖 **我的文档库 + RICH_DOC**
- **已拍板（2026-07-20）**：真源方案 B；编辑器 TipTap；V1 仅 MD 导出；`doc_embed` 延后到首个重型块

## Goals / Non-Goals

**Goals:**

- 钉死三大容器边界与「Object type vs Block type」模型
- 交付个人文档库：页面树 + TipTap 编辑 + 乐观锁保存 + 「最近」+ **MD 导出**
- 为后续重型块 / DOCX·PDF / 云盘·知识库留扩展点且 V1 不提前建空表

**Non-Goals:**

- 云盘、知识库实现；分享；重型块；Word/PDF；MD 主存

## Decisions

### D0：三大容器（产品总图 · 本 change 只实现 Library）

```text
DocObject（内容本体）
    │ 同一时刻仅一处 Placement
    ├─ LIBRARY  → 我的文档库（页面树）     ← V1 本 change
    ├─ DRIVE    → 云盘（文件夹）           ← 后续母 change
    └─ WIKI     → 知识库（空间 + 节点树） ← 更后母 change
```

| 容器 | 权限心智 | 组织 | V1 |
|------|----------|------|-----|
| 我的文档库 | 仅所有者 | `parent_id` 页面树；无「文件夹」实体 | **做** |
| 云盘 | 文件夹协作者（后） | 文件夹树 + 上传 blob | 不做 |
| 知识库 | 空间成员 + 节点继承（后） | `space` + `node` 挂载 `object` | 不做 |

跨容器：**移动** 换 Placement；**快捷方式** 后置。禁止默认双向同步。  
**文件夹** 不是 `RICH_DOC`，仅出现在云盘域。

### D1：Maven 与表前缀

```text
relayflow-module-docs/
  ├── relayflow-module-docs-api/
  └── relayflow-module-docs-biz/
```

- 表前缀 **`doc_`**（`docs/dev/database.md` 已登记）
- `docs-biz` → `system-api`（如需）；禁止直查他域表
- 本 change **不**依赖 `infra-api` / MinIO

### D2：两层类型（防跑偏）

| 层 | 字段/位置 | V1 | 后续例子 |
|----|-----------|-----|----------|
| **Object type** | `doc_object.type` | 仅 `RICH_DOC` | `SHEET`、`SLIDES`、`FILE`；`FOLDER` 属云盘表 |
| **Block type** | `body` JSON 内节点 `type` | 基础文本块 | `flowchart`、`bitable_ref`、…（插入菜单扩展） |

飞书「+ 插入」长列表 = **Block type 注册表**，不是每个都新建一种 object type。  
「新建表格 / 幻灯片」= 新 **object type**（另切片），与「文档里插一个表格块」可并存，勿混为一谈。

### D3：真源方案 B（Block JSON + 导出适配）

```text
编辑 / 协作 / 还原          导出 / 备份 / 互通
─────────────────          ─────────────────
TipTap JSON  = 真源    →   Markdown（V1）
                           DOCX / PDF（后续）
```

- **禁止** MD 作主存；**禁止** MD↔JSON 双写同步
- 复杂块（流程图等）将来：`body` 内占位节点 + `doc_embed.data` 存可编辑图数据；**不是** Mermaid 文本真源
- 导出 MD **允许有损**（规则见 D7）；规格须写明

### D4：数据模型（V1 两表；embed 延后）

```text
doc_object
  id, tenant_id
  type              -- V1: RICH_DOC
  title
  body              -- JSONB，TipTap doc JSON
  body_format       -- 如 'tiptap_json_v1'（换引擎时做迁移判据）
  content_version   -- 乐观锁，成功保存 +1
  owner_user_id
  last_opened_at
  + 公共字段

doc_library_node
  id, tenant_id
  owner_user_id
  parent_id         -- NULL = 根下
  object_id         -- UNIQUE → doc_object
  sort_order
  + 公共字段
```

- 无 `doc_library` 头表；空树 = B 类默认（无行即空）
- 删节点 → 级联软删 node + object
- 云盘/Wiki 后续另表 Placement，**保留同一 `doc_object.id`**

#### D4.1 `doc_embed`（V1 不建 · 形态预留）

**决定：等第一个重型块切片再加 Flyway**，避免空表无代码路径。

预定形态（实现时再落库，可微调）：

```text
doc_embed
  id, tenant_id, object_id
  embed_type        -- flowchart | whiteboard | …
  data              -- JSONB（编辑器专用载荷）
  data_version
  + 公共字段
```

`body` 中对应块示例：`{ "type": "flowchart", "attrs": { "embedId": "…" } }`。  
导出 MD：快照图或 `relayflow://embed/{id}` 链接占位。

### D5：权限（文档库 V1）

- `/app-api/docs/**`：JWT + 有效成员
- 仅 `owner_user_id = 当前用户` 读写
- 无分享、无管理面 permission

### D6：编辑器 TipTap（已锁定）

| 项 | 选择 |
|----|------|
| 框架 | **TipTap**（ProseMirror）；`web/` 引入官方 Vue 包与所需 extension |
| 加载 | `/app/docs` 路由 **懒加载**编辑器 chunk，控制体积 |
| V1 Block 最小集 | 段落、H1–H3、有序/无序/任务列表、粗/斜体、代码块、引用、分割线、链接；（图片可 `-web` 评估，无则占位禁用） |
| 插入菜单 | 可做「+」菜单骨架；未支持块 **disabled**，名称可预留 |
| 保存 | 防抖 1–2s + 失焦；`PUT` 带 `contentVersion`；冲突 `DOC_VERSION_CONFLICT` |
| 空文档 | 合法空 TipTap doc；标题默认「未命名文档」 |

扩展惯例：新 Block = TipTap `Node`/`NodeView` +（若重型）`doc_embed` + 类型注册常量。

### D7：导出适配

| 格式 | V1 | 说明 |
|------|-----|------|
| Markdown | **做** | `tiptap_json` → MD；基础块尽量无损；未知/重型块 → 注释或链接占位 |
| DOCX / PDF | **不做** | 后续 `workspace-docs-export-office-v1`（示意名） |

API 草图：`GET /documents/{objectId}/export?format=md`（或 `Accept`/`format` 按 contract）。  
前端：编辑器工具栏或「…」菜单「导出 Markdown」下载文件。

导入 MD：V1 **不做**（可后置）；避免半吊子双向。

### D8：App API（草图）

前缀：`/app-api/docs/`

| 能力 | 方法（示意） |
|------|----------------|
| 库树 | `GET /library/tree` |
| 新建页 | `POST /library/nodes` `{ parentId?, title? }` |
| 改节点 | `PUT /library/nodes/{nodeId}` `{ title?, parentId?, sortOrder? }` |
| 删节点 | `DELETE /library/nodes/{nodeId}` |
| 读文档 | `GET /documents/{objectId}`（含 `body`、`bodyFormat`、`contentVersion`） |
| 存正文 | `PUT /documents/{objectId}/body` `{ body, contentVersion }` |
| 最近 | `GET /recent?limit=` |
| 导出 MD | `GET /documents/{objectId}/export?format=md` |

错误码：`DOC_NOT_FOUND`、`DOC_FORBIDDEN`、`DOC_VERSION_CONFLICT`、`DOC_PARENT_INVALID`、`DOC_TYPE_UNSUPPORTED`、`DOC_EXPORT_FORMAT_UNSUPPORTED`。

契约：`openspec/lanes/workspace-docs-library/contract.md`（`-web` 起草）。

### D9：前端信息架构

```text
/app/docs
  左栏：我的文档库（树）| 最近 | 与我共享/星标/云盘/知识库（占位）
  右栏：标题 + TipTap；导出 MD
深链：-web 时在 contract 写死 ?docId= 或 /:docId
```

遵循 [`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)。

### D10：子 change 与验证

```text
docs-schema-v1                     两表 + 模块（无 doc_embed）
workspace-docs-library-web         TipTap + 树 + MD 导出 UI + contract
workspace-docs-library-api         REST + MD 序列化
workspace-docs-library-integrate   联调
```

验证：compile；`pnpm build` + `typecheck`；浏览器新建/编辑/刷新/树操作；导出 `.md` 可打开。

### D11：云盘 / 知识库 / 重型块边界

| 后续 | 要点 |
|------|------|
| `workspace-docs-drive-v1` | 文件夹 + 上传；与 library **移动** |
| `workspace-docs-wiki-v1` | 空间 + 节点挂 object |
| 首个重型块切片 | 建 `doc_embed` + TipTap Node + 专用编辑器（如 flowchart） |
| Office 导出切片 | DOCX/PDF 适配器 |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| TipTap 包体积 | 路由懒加载；V1 扩展集收敛 |
| 用户以为「存的是 MD」 | 产品/开发文档明确真源；导出入口文案「导出为 Markdown」 |
| MD 导出有损被误解为 bug | contract + UI 注明基础块保证；高级块后续规则 |
| 延后 `doc_embed` 导致 body 被塞大 JSON | 规格禁止 V1 实现重型块；上块前必须加 embed 表 |
| 无分享 | 文案标明个人库 |

## Migration Plan

- 仅增量 Flyway（两表）
- 回滚：卸 `docs-biz` + 前端回空态
- 将来加 `doc_embed`：新迁移，不改历史脚本；`body_format` 变更走版本迁移任务

## Open Questions（已关闭 / 残留）

| # | 状态 |
|---|------|
| 真源 B / TipTap / 仅 MD 导出 / embed 延后 | **已拍板** |
| 深链 `?docId=` vs `/:docId` | `-web` 定稿进 contract |
| 图片块是否进 V1 最小集 | `-web` 评估；无上传链路则可禁用 |
| 删除回收站 UI | V1 不做 |
