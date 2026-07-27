## Why

当前 `/app/docs` 把「最近 / 与我共享 / 星标」拆成侧栏独立入口，「我的文档库」把树塞在中间栏，云盘也缺少飞书式「滑入文件夹浏览器」交互；列表里所有者只用纯文本或「我」，无法点开通讯录名片。产品信息架构与飞书云文档主页差距过大，影响可发现性与一致性。本母 change 对齐飞书式 IA，同时评估并避免不必要的表结构推翻。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

1. **主页（Home）信息架构**：「最近 / 与我共享 / 星标」从中间栏独立入口收回 **主页**；默认进入主页且 Tab 落在「最近访问」；主区顶部快捷操作 + **横向 Tab** + 统一文件列表（列：标题、位置、所有者、创建时间、最近访问、行操作）；主页「上传」= **进入云盘滑入态并立即拉起上传**
2. **我的文档库改展**：中间栏仅保留入口；**右侧主区**以 **树形缩进** 列表展示文档（所有者、创建时间、最近访问、操作；窄宽下标题截断/缩进封顶）；点开进入编辑，**必须有返回**
3. **云盘飞书式交互**：侧栏「云盘」悬停显示向右进入指示；点击后中间栏 **向左滑出并替换** 为 IDE 式文件夹树；右侧展示当前文件夹内容（所有者、修改时间、操作）；**不含**最右详情检视栏（后置，design 留底）
4. **可复用所有者组件**：头像 + 显示名；点击打开既有 `WorkspaceBusinessCard`（通讯录名片）；云文档列表、云盘、任务负责人等凡展示「所有者/负责人」处统一使用，**禁止**仅用「我」或无头像纯文本作为唯一呈现
5. **API / 契约对齐**：统一列表列所需元数据（含 `ownerUserId`、头像可解析字段、时间字段）；**不**为本次 IA 强制推翻 `doc_object` / Placement / share/star/visit 核心表（见 design 结论）
6. **规格修订**：**BREAKING（导航行为）**——侧栏不再以独立一级项呈现「最近 / 与我共享 / 星标」；Library 主交互从「中栏树」改为「右栏树形列表 + 编辑返回」

### 非目标（本母 change V1）

- 飞书「归我所有」独立 Tab（由「我的文档库」承载）
- 模板库完整能力（快捷卡可占位 toast）
- 云盘最右侧「详情检视面板」（图二右侧窄栏）——**后置**；`design.md` 已留简短提案，IA 改版完成后再开执行切片
- 知识库 / Wiki 实装
- 置顶文档区完整产品化（可后置）
- 推翻分享 ACL / 星标 / 访问足迹表模型（除非 design 发现硬冲突；当前结论：**不需要**）
- 实时协作、评论、外链分享

## Capabilities

### New Capabilities

- `workspace-owner-chip`：工作台可复用「用户头像+显示名」芯片；点击打开个人名片；跨 docs / task 等列表复用

### Modified Capabilities

- `docs`：主页 Tab 信息架构；Library 右栏列表；云盘滑入文件夹浏览器；列表列与所有者呈现；导航与深链返回行为
- `workspace-business-card`：明确可作为列表所有者芯片的弹出目标（peer/self），不改名片本体能力边界时可仅增量引用
- `task`：任务列表/详情中负责人展示改为所有者芯片（行为增量；不改任务 ACL）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway | **默认无新核心表**；若需列表列补齐，优先 JOIN/查询扩展既有 `doc_*` + `UserApi`；头像走 system 既有资料，不在 docs 表反范式昵称/头像 |
| 后端 | `relayflow-module-docs-*`、可能 `system-api` `UserBasicDTO` | 列表 VO 字段对齐（shared/starred/drive/library list）；可选批量用户摘要含头像 fileId |
| 前端 | `web/` `/app/docs`、`components/workspace/`、任务页 | 主页 / Library 列表 / Drive 滑入态；`WorkspaceOwnerChip`；任务负责人替换 |
| 契约 | `openspec/lanes/workspace-docs-ia-redesign/contract.md` | `-web` 起草 |
| 看板 | `docs/dev/api-integration-board.md` | 登记切片 |
| 回滚 | 恢复侧栏一级入口与中栏 Library 树；卸滑入态；芯片回退为文本 | 无破坏性 Flyway 时可零迁移回滚 UI |

### 子 change 建议拆分

```text
workspace-docs-ia-redesign-v1          母 change（本目录 · 规划真源）
workspace-owner-chip-web               ① 所有者芯片 + 名片弹出（可先于 docs IA）
workspace-docs-home-ia-web             ② 主页 Tab + Library 右栏列表 + Drive 滑入 UI + contract
workspace-docs-home-ia-api             ③ 按 contract 补齐列表字段 / 可选 UserBasic 头像
workspace-docs-home-ia-integrate       ④ 去临时数据、联调、任务页芯片落地收尾
```

**前置假设**：对象级分享 / 星标 / 最近足迹（`workspace-docs-share-star-*`）已可用或作为基线合入；本母 change **不重做 ACL**，只重做导航与呈现。
