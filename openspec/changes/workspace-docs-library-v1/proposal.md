# 提案：工作台云文档 · 我的文档库 V1（workspace-docs-library-v1 · 母 change）

## Why

README 定位含「即时通讯、**文档**与轻量工作流」，但 `/app/docs` 仍是空壳占位。消息 / 任务 / 日历已可用，文档是产品叙事最大空洞。

飞书云文档由 **三种存放容器** + **多种内容形态** 组成。我们按同一心智规划产品，但 **实现必须小步**：本 change 只落地最简单的容器——**我的文档库**；云盘与知识库仅作路线图占位，各自另开母 change。

编辑与存储已拍板：**Block JSON 为真源**（TipTap），**Markdown / DOCX / PDF 为导出适配**——避免用原生 MD/Mermaid 硬扛流程图等重型块，同时保留导出互通。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。`[平台]` 可先行。

### 产品总图（三大容器 · 粗规划，本 change 不实现后两者）

| 容器 | 心智 | 组织方式 | 本 change |
|------|------|----------|-----------|
| **我的文档库** | 个人知识树 | 页面树（父子页）；每人每租户一份 | **本 change 实现** |
| **云盘** | 网盘 / 文件夹 | 文件夹树；上传/下载二进制为主 | 后续母 change（本 change 只留边界） |
| **知识库** | 团队 Wiki | 多个知识空间 + 页面树 + 空间成员 | 更后母 change |

统一约定（对齐飞书官方）：

- **内容本体**（一篇在线文档等）同一时刻只归属一个容器；跨容器靠 **移动**，不是双向同步。
- 「云文档」是产品入口总称；侧栏三种容器是入口，不是三种互不相关的产品。

### 本 change 实际交付（我的文档库 MVP）

1. **新 Maven 域** `relayflow-module-docs`（`*-api` + `*-biz`），表前缀 `doc_`
2. **内容形态仅一种**：在线块文档（`RICH_DOC`）；不新建独立「表格文件 / 幻灯片 / 文件夹」等 object type
3. **真源与编辑**：`body` = TipTap/ProseMirror JSON（`body_format` 标注版本）；前端锁定 **TipTap**；防抖自动保存 + 乐观锁
4. **导出（V1）**：提供 **Markdown 导出**；Word / PDF **本 change 不做**
5. **容器**：租户内当前用户的「我的文档库」页面树（空树即可，无根行 ensure）；新建 / 重命名 / 移动（树内）/ 删除（逻辑删）
6. **工作台 UI** `/app/docs`：侧栏「我的文档库」树 + 「最近」；「与我共享 / 星标 / 云盘 / 知识库」占位或 disabled
7. **看板**：登记 `workspace-docs-library` 切片与 lane contract

## Capabilities

### New Capabilities

- `docs`：云文档域总规格——三大容器边界、Block JSON 真源与导出边界、以及 **我的文档库** V1 行为（本 change 归档时写入主 specs）

### Modified Capabilities

- （无强制修改；`workspace-search` 后续可加 `doc` 分组，不在本 change 范围）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| Maven | 根 `pom.xml`、`relayflow-server` | 引入 `relayflow-module-docs-biz` |
| DB | Flyway `V0.1.0.x` | `doc_object`、`doc_library_node`（**无** `doc_embed`，见 design） |
| 后端 | `relayflow-module-docs-*` | 新建；依赖 `system-api`；MD 导出适配 |
| 前端 | `web/` `/app/docs`、store、`api/app/docs` | TipTap + 树 + MD 导出入口 |
| npm | `web/` | 引入 TipTap 相关依赖（已拍板） |
| 文档 | `database.md` 的 `doc_`；`api-integration-board` | 已登记 / 随实现更新 |
| 迁移 | 仅增量 Flyway | 可回滚卸模块依赖；表可保留空 |

## 非目标（本 change / 文档库 V1）

- 云盘（文件夹、上传入口、共享文件夹）
- 知识库（多空间、空间成员、从库迁入 Wiki）
- 多种 object type（独立表格文件、幻灯片、问卷等）；文件夹仅属云盘
- 重型块（流程图、画板、多维表嵌入等）与 `doc_embed` 表
- Word / PDF 导出（后续切片）
- 文档分享 / 协作者 / 「与我共享」真数据
- 星标、快捷方式、回收站 UI（逻辑删即可，恢复 UI 可后置）
- IM 发文档卡片、任务挂文档、全局搜索 `doc` 分组
- 实时多人光标协作（CRDT）；V1 以乐观锁为准
- 管理端文档治理
- **Markdown 作主存** 或 MD↔JSON 双真源同步

## 拍板结论

| 项 | 结论 |
|----|------|
| 三大容器都规划 | **要**（总图写在本母 change） |
| 实现顺序 | **我的文档库 → 云盘 → 知识库** |
| 本 change 实现范围 | **仅我的文档库 + RICH_DOC** |
| 内容真源 | **B：Block JSON（TipTap）为真源**；MD/DOCX/PDF 为导出 |
| 编辑器 | **锁定 TipTap**（允许引入 npm 依赖） |
| 导出 V1 | **仅 Markdown**；Word/PDF 下一刀 |
| `doc_embed` | **V1 不建表**；等第一个重型块切片再加（形态预留在 design） |
| 云盘 / 知识库详细 tasks | **不做进本 change** |
| 分享协作 | **文档库 V1 不做**（仅所有者可读写自己的树） |

## 子 change 切片（实现顺序）

```text
workspace-docs-library-v1              ← 本 change（规划母版）
├── docs-schema-v1                     [平台] Flyway + Maven + codegen
├── workspace-docs-library-web         UI + TipTap + 临时 store + contract + MD 导出 UI
├── workspace-docs-library-api         App API：树 CRUD + 正文读写 + MD 导出
└── workspace-docs-library-integrate   去临时数据联调
```

## 后续母 change（本 change 不写 tasks）

```text
workspace-docs-drive-v1                云盘：文件夹 + 上传/下载（复用 MinIO）
workspace-docs-wiki-v1                 知识库：知识空间 + 页面树 + 成员
workspace-docs-export-office-v1        （示意）DOCX/PDF 导出
workspace-docs-embed-flowchart-v1      （示意）重型块 + doc_embed
```
