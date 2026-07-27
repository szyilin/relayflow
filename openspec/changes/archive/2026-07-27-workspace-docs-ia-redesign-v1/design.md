## Context

工作台 `/app/docs` 已具备：

- 三栏壳层（Rail + 中栏 panel + 主区）
- Library 树（中栏）+ TipTap 编辑（主区）
- 最近 / 与我共享 / 星标（中栏独立入口；仅「最近」接近飞书表格列）
- 云盘：中栏提示 + 主区 `DocsDriveBrowser`（非 IDE 树滑入）
- 分享 ACL / 星标 / `doc_user_visit`（基线能力；本母 change **不重做**）
- 通讯录名片：`WorkspaceBusinessCard` + `useClickAnchoredPopover`（可复用）

产品对照飞书后，当前痛点是 **信息架构与呈现**，不是文档本体模型。

```text
现状 IA                         目标 IA（飞书对齐）
─────────────                   ──────────────────
中栏:                           中栏（云文档根）:
  最近 ──► 右列表                 主页 ──► 右: 快捷卡 + Tab + 统一表
  与我共享 ──► 右列表             我的文档库 ──► 右: 文档列表（非中栏树）
  星标 ──► 右列表                 云盘 ──悬停 ›──► 中栏滑入文件夹树
  我的文档库 ──► 中栏树             └ 右: 当前夹文件表（所有者/改时）
  云盘 ──► 右浏览器               知识库（占位）
  知识库（占位）
```

开发期允许推翻表结构（`docs/dev/database.md`），但须先证明 **现有模型无法支撑**；下文结论是 **不必推翻**。

## Goals / Non-Goals

**Goals:**

- 主页承载「最近 / 与我共享 / 星标」横向 Tab + 统一列表列
- Library 内容进主区列表；打开编辑可返回列表
- 云盘：悬停进入指示 + 中栏滑入 IDE 树 + 右侧内容表
- 跨模块 `WorkspaceOwnerChip`（头像+名 → 名片）
- 契约与 VO 字段对齐列表列；看板登记子切片

**Non-Goals（本母 change V1 不实现）：**

- 「归我所有」独立 Tab；模板库实装；Wiki；置顶区产品化
- 云盘最右侧「详情检视栏」实装（**后置**；见文末「后置提案 · 云盘详情检视栏」留底，改版完成后再开执行 change）
- 重做 share/star/visit / Placement 模型
- CRDT、评论、外链

## Decisions

### D1：Schema 结论 — **不推翻核心表**

| 表 | 是否改 | 理由 |
|----|--------|------|
| `doc_object` | 否（结构） | 已有 `owner_user_id`、`create_time`、`last_opened_at`（对象级）；列表可查 |
| `doc_library_node` | 否 | 树仍是权威结构；右栏列表 = **树的另一种呈现**（扁平/缩进），不改 Placement |
| `doc_drive_folder` / `doc_drive_item` | 否 | 已支持文件夹树与 listing；滑入 UI 是前端状态机 |
| `doc_share` / `doc_star` / `doc_user_visit` | 否 | 与主页 Tab 数据源一一对应 |
| 反范式 `owner_name` / `avatar` 进 docs 表 | **否** | 违反跨域；用 `UserApi` / profile 解析 |

**何时才会推翻：** 若产品要把 Library 从「个人树」改成「空间/团队库」或引入跨用户共享文件夹继承 ACL——那是另一母 change，**不是**本次飞书 IA。

**备选否决：** 为「列表好看」新建 `doc_home_tab` / 物化视图表——无必要，应用层聚合即可。

### D2：主页 Tab 与侧栏导航

```text
中栏一级（云文档根 navMode=root）:
  [主页] [我的文档库] [云盘 ›] [知识库·占位]
  （可选后置：置顶文档）

主页主区:
  [新建] [上传] [模板库·toast 占位]
  Tabs: 最近访问 | 与我共享 | 星标
  Table: 标题 | 位置 | 所有者(chip) | 创建时间 | 最近访问 | ⋯
```

- **不**做飞书「归我所有」Tab：由「我的文档库」入口承担「我拥有的页面」心智
- **默认落地（已拍板）：** `panel=home` + tab=`recent`（取代今日默认 `library`）
- **主页「上传」（已拍板 · 方案 A）：** 进入 `navMode=driveExplorer`（当前文件夹=根或上次上下文可定根）并 **立即拉起上传**（文件选择 / 既有上传流程）；不是仅 toast 指路
- 深链 `?docId=` 仍打开编辑器；关闭/返回时回到进入前的 home tab / library / drive 上下文

**与现行 spec 冲突处理：**

| 现行 | 处理 |
|------|------|
| 「最近 / 与我共享 / 星标」为侧栏一级 | **MODIFIED**：改为主页 Tab；能力与 API 保留 |
| 「Workspace shared and starred navigation」 | 改写为 Home tabs 场景 |
| store「hydrate sidebar panels」 | 改为 hydrate home + library list + drive |

### D3：Library — 右栏**树形缩进**列表，中栏不再塞满树

```text
选「我的文档库」:
  中栏: 仅入口高亮（可保留极简说明或快捷「新建」）
  主区: DocsLibraryList（树形缩进，非平铺）
        列 = 标题（按 depth 缩进 + 展开/折叠可选）| 所有者 | 创建时间 | 最近访问 | ⋯
  点行 → 主区切编辑器 + 返回按钮（回列表，非回 Rail）
```

- **呈现（已拍板）：树形缩进**，保留父子层级；行菜单仍可移动/重命名/删除
- **窄宽适配：** 标题列优先 `min-w-0` + 文本截断（ellipsis）；深层级缩进设上限（例如 max padding，再深用「…」或更小步长），避免把所有者/时间列挤没；中栏拖窄时列表仍可读，不出现横向整表失控滚动为默认（单列内截断优先）
- **数据：** V1 优先基于既有 `GET /library/tree` 在客户端渲染缩进表（带 `createTime`/`lastOpenedAt` 需 API 补齐时走 `-api`）
- **若树很大再考虑** 懒加载子节点或分页 API——**不**作为默认推翻点；contract 可预留
- **不**要求中栏再渲染完整 `DocsTreeNodes`（可删或降级为可选「大纲」后置）

**冲突：** 现行「Library tree in middle + empty right」→ **BREAKING UI**；规格改为「列表在主区」。`doc_library_node` 语义不变（仍 owner-only 树）。

### D4：云盘 — 滑入态（`navMode=driveExplorer`）

```text
状态机:

  navMode=root          navMode=driveExplorer
  ┌──────────┐   click  ┌────────────────────┐
  │ 云文档导航 │ ──────► │ 中栏: 文件夹树(IDE) │
  │ 云盘行 + › │  hover   │ 主区: 当前夹 listing│
  └──────────┘  显示 ›   │ 顶栏: 返回云文档     │
        ▲                └────────────────────┘
        │  返回 / 面包屑「云文档」
        └──────────────────────────────────────
```

- 动画：中栏内容 **translateX 滑出替换**（CSS transition；不改 `WorkspaceShell` 三栏骨架亦可，优先在 docs 页 panel 内做）
- 树数据：既有 `listDriveFolders`（可递归或懒加载子层）；右侧 `listDriveItems`
- listing VO 补齐：`ownerUserId` + 可解析显示名/头像、`updateTime`（已有）
- V1 **不做**图二最右「详情检视」窄栏（已拍板后置；见文末留底提案）

**冲突：** 现行「中栏 hint + 右侧一体浏览器」→ 拆成「中栏树 + 右侧表」；Drive 域模型不变。

### D5：所有者芯片 `WorkspaceOwnerChip`

```text
WorkspaceOwnerChip
  props: userId, nickname?, avatarUrl?, size?
  click → UPopover + useClickAnchoredPopover
       → WorkspaceBusinessCard (self | peer)
```

| 规则 | 说明 |
|------|------|
| 显示名 | **真实昵称/用户名**；当前用户可用角标「我」，**禁止**单独用「我」替代姓名作为唯一文字 |
| 头像 | 优先 URL；无则字头像 |
| 数据 | 列表已有 `ownerUserId` 时即可；昵称/头像：① 响应内带 `ownerDisplayName`+`ownerAvatarFileId` 或 ② 前端 contacts/profile 解析。推荐 **API 批量摘要**（`UserBasicDTO` 扩 `avatarFileId`）避免 N+1 与 contacts 未加载 |
| 复用面 | docs 各表、drive 表、任务负责人（`assigneeId(s)`） |
| 点击冒泡 | chip 点击 `stopPropagation`，避免触发行打开 |

**跨域：** docs-biz / task-biz 继续只调 `system-api`；禁止查 `sys_user` Mapper。

### D6：API / contract 增量（非新域）

建议 lane：`openspec/lanes/workspace-docs-ia-redesign/contract.md`

| 端点 | 增量 |
|------|------|
| `GET /recent`（已有） | 列已齐；所有者禁止仅返回「我」——改为真实 displayName + `ownedByMe`；可选 `ownerAvatarFileId` |
| `GET /shared`、`GET /starred` | 对齐列：`createTime?`、`lastOpenedAt?`、`locationLabel`、owner 头像字段 |
| Drive `GET .../items` | `ownerUserId`、`ownerDisplayName`、`ownerAvatarFileId?`（文件夹可显示所有者=当前用户） |
| Library tree 或新 list | 节点/行带 `createTime`、`lastOpenedAt`（visit 左连当前用户）、`ownerUserId` |
| `UserBasicDTO`（可选同切片） | `avatarFileId`；docs 组装列表时批量 `getUserBasic` |

**无 Flyway** 为默认路径。若发现 `create_time` 未暴露仅属 VO 缺口，不是表缺口。

### D7：切片顺序与依赖

```text
0. 合入/稳定 share-star 基线（若本地未提交，先收口）
1. workspace-owner-chip-web     — 芯片可演示（contacts 数据亦可撑）
2. workspace-docs-home-ia-web   — 主页/Library/Drive 滑入 + contract 草案
3. workspace-docs-home-ia-api   — 按 contract 补 VO / UserBasic
4. workspace-docs-home-ia-integrate — 联调；任务页全面换芯片
```

母 change **本身不写业务代码**；`tasks.md` 只含规划验收与开子 change 检查项。实现任务落在子 change。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 与未提交的 share-star 代码交织冲突 | 母 change 标明基线依赖；实现前先合入 share-star |
| Library 大树前端扁平卡顿 | V1 接受；超限再加分页 list API，不提前建表 |
| 滑入动画与 `WorkspaceShell` resize 打架 | 动画限定 panel 内容层；保留 resize handle |
| UserBasic 无头像导致芯片退化 | `-api` 扩 DTO 或前端走 profile；芯片无 URL 时字头像仍可点名片 |
| 用户期望「归我所有」Tab | design 明确映射到文档库；若强烈需要可后加 Tab 调同一 list |
| 规格 archive 时 MODIFIED 丢细节 | delta 必须整段拷贝再改 |

## Migration Plan

1. 无强制 DB migrate（默认）
2. 前端先上（`-web` Mock/临时 store 字段）→ contract 冻结 → `-api` → integrate
3. **回滚：** 恢复侧栏一级入口与中栏树组件；滑入态 feature flag 可整段删；VO 多数字段可留（向前兼容）
4. 若错误加了反范式列：开发期 `0.x` 可破坏性删列（须征得同意，见 database.md）

## Resolved Decisions（产品拍板 · 2026-07-27）

| # | 议题 | 结论 |
|---|------|------|
| R1 | 默认入口 | **主页**，且主页内默认 Tab = **最近访问** |
| R2 | 主页「上传」 | **方案 A**：进入云盘滑入态并 **立即拉起上传**（非仅 toast） |
| R3 | 文档库列表 | **树形缩进**；注意窄宽截断与缩进上限，避免列挤压别扭 |
| R4 | 云盘详情检视栏 | **V1 后置**；本文件保留简短提案，IA 改版完成后再开执行 change |

## Open Questions（实现期可再定，不阻塞开子 change）

1. **云盘返回手势**：仅顶栏「返回云文档」，还是中栏树顶再加返回？建议：两者都有。
2. **任务多负责人**：最多几个 chip +「+K」溢出？建议：最多 3 chip + 溢出。
3. **上传落点文件夹**：主页上传默认落到 Drive **根**，还是记住上次浏览的文件夹？建议：V1 固定根，简单可预期。

---

## 后置提案 · 云盘详情检视栏（不实装 · 仅留底）

> 目的：改版（本母 change 子切片）完成后再展开具体执行方案；此处只钉住「是什么 / 为何后置 / 将来怎么切」，避免遗忘。

### 是什么

飞书云盘主区在「文件列表」之外，最右侧还有一条 **窄详情栏**：选中文件夹/文件后展示大图标、名称、编辑入口、「已分享」状态、描述、所有者等——**预览元数据，不等于打开编辑器**。

```text
中栏文件夹树 | 主区文件表 | 【后置】详情检视栏
```

### 为何 V1 不做

- 本改版瓶颈在 IA（主页 Tab / Library 右栏 / 滑入树 / OwnerChip）；详情栏是增量面板，不阻塞主路径
- 需选定态、空态、多选策略、描述字段是否落库——单独设计更干净

### 将来执行时建议范围（草案）

| 项 | 建议 |
|----|------|
| 触发 | 单击选中行 → 打开/刷新详情栏；双击或「打开」仍进编辑器/下载 |
| 内容 V1 | 名称、类型图标、所有者 chip、修改时间、分享状态（若 RICH_DOC）；描述可先只读占位 |
| 壳层 | `WorkspaceShell` `#aside` 或 docs 页内第三列；默认可关闭 |
| 数据 | 优先复用 listing / get document 已有字段；描述若要持久化再开小 migrate |
| 切片名（预留） | `workspace-docs-drive-inspector-web` → `-api`（若需）→ `-integrate` |

### 闸门

- **前置：** `workspace-docs-home-ia-*` 归档（滑入云盘 + 右侧文件表已稳定）
- **禁止：** 在本母 change 子切片 tasks 中夹带详情栏实装
