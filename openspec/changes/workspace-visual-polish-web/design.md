## Context

员工工作台已有卡片分层壳层（`--ws-*`）与 Nuxt UI 主色 teal，默认头像多为中性灰圆 + 首字，标签偏灰，「即将推出」像注释，Rail 图标未选中几乎全灰，空区大块冷底。问题不在「背景再花一点」，而在缺少可复用的 **身份色 / 状态色 / 空态氛围**。

约束：仅改 `web/`；禁止新 UI 库；禁止混用 `--admin-*`；规格与文案禁止点名第三方商业产品品牌；色用在「人 / 态 / 空」，不染画布与正文框。

## Goals / Non-Goals

**Goals:**

- 默认字母头像：稳定哈希双色渐变，同人跨页一致，明暗可读
- 语义标签 + 对照色 token（同 hue，明暗调亮度）
- Rail 模块微 tint（未选中可辨）
- 统一 `WorkspaceEmptyState` 与「即将推出」活力标签
- 文档 token/patterns 与实现同步
- `pnpm build` + `pnpm typecheck` 通过；浏览器走查关键表面

**Non-Goals:**

- 后端 API、存用户「头像色」、改偏好 schema
- 管理端整套换肤 / 与 `--admin-*` 合并
- 壳层大面积 mesh、霓虹 glow、全图标强制彩色
- 重做业务功能或改信息架构
- 为每个空页定制独立插画体系（本期 1～2 套线描即可）

## Decisions

### D1. 色分层：chrome 冷静 / content 生动

| 层 | 上色？ | 说明 |
|----|--------|------|
| 画布、列卡片、分割线、正文 | 否 | 继续灰阶 `--ws-*` |
| 默认头像、语义标签、模块图标 tint、空态插画、状态点 | 是 | 走 token / 工具函数 |
| 工具栏线框图标、溢出菜单 | 否 | 保持 muted |

**备选**：给整列卡片上轻渐变 → 拒绝（破坏分层、易与暗色冲突）。

### D2. 默认头像：前端哈希渐变，不落库

在 `avatar.ts`（或并列 util）扩展：

- 输入优先 `userId`，否则昵称字符串
- 复用现有 `hashId`，映射到 **8～12 组** 双色渐变（浅色/深色可用同一组 + `color-mix` 进 panel，或 `.dark` 下调亮度）
- 输出 CSS `background-image: linear-gradient(...)` + 首字；有 `src` 时不套渐变
- 接线：`WorkspaceOwnerChip`、`WorkspaceRailHeader`、IM/通讯录字母头像、名片等统一入口

已有 `tenantTileColor` 证明「哈希纯色」可行；头像升级为 **对角/135° 双色**，避免单色块。

**备选**：服务端存 `avatarHue` → 本期不做（无 API、无迁移价值）。

### D3. 对照语义色写入 `--ws-tint-*`

在 `workspace.css` 的 `:root` / `.dark` 增加：

- `--ws-tint-info|success|warning|danger`（及 `-muted` 底）
- 身份类：`--ws-tint-official|bot|agent|external|coming-soon`（及 muted）

规则：明暗 **同 hue**；深色用更高透明 / 更低 chroma 底 + 更亮字，避免切换时「换色眨眼」。业务页禁止散落 hex；标签组件只消费这些 token。

用户设置里的 **主题色色点**（`--ui-primary`）继续管按钮/选中；**模块 tint** 与主题色独立。

### D4. 标签：薄封装 + soft 为主

新增 `WorkspaceSemanticBadge`（或对 `UBadge` 的薄包装）：`kind` 枚举（official / bot / agent / external / role / comingSoon / success / warning / danger / info）。默认 `variant=soft`（浅底深字）。「即将推出」MUST 走 `comingSoon`，禁止纯 muted 文字冒充标签。

### D5. Rail 模块 tint

在 `useWorkspaceNav`（或 Rail 数据）为每项配置 `tint` CSS 变量名；未选中图标用该 tint（约 55–70% 观感），选中保留 active 底 + tint/primary 协调。文字仍 `--ws-text` / muted。

建议映射（可微调，须写入 tokens 文档）：

| 模块 | tint 方向 |
|------|-----------|
| 消息 | info 蓝 |
| 通讯录 | 青 |
| 云文档 | 绿 / teal 系 |
| 日历 | 琥珀 |
| 任务 | 靛 |
| 视频会议 | 青蓝 |
| AI 智能体 | 紫 |

### D6. 空态组件

`WorkspaceEmptyState`：可选插画 slot（默认线描 SVG，主色 + 一辅色）、标题、描述、可选 CTA。列表空 / 主区无内容 / 设置占位分类优先接入。列下半「骨架灰条」类占位改为文案引导或极淡氛围，禁止假数据填空。

### D7. 交付顺序（实现任务对应）

1. Token + util（渐变、tint）  
2. 头像接线（OwnerChip / Rail / 高频列表）  
3. Badge + 模块 tint  
4. EmptyState + 关键空页  
5. 文档 + 验证  

可一次 change 内按 tasks 勾选推进，不必拆多个 OpenSpec。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 渐变对比度不足（浅字浅底） | 调色板人工验收；字强制近白；对比抽检明暗 |
| 哈希碰撞导致多人同色 | 8～12 组可接受；优先 userId 降低昵称碰撞 |
| 模块 tint + 用户主题色冲突 | 文档写清：主题色管 CTA/选中底，tint 只管图标识别 |
| 扫页面遗漏字母头像 | tasks 列清单 + grep `UAvatar` / `avatarText` |
| 过度上色 | Non-Goals + design 边界；评审对照「不染框」 |

## Migration Plan

1. 合入 `web/` 与 docs；无 Flyway、无偏好迁移  
2. 回滚：revert 该 change 提交即可  
3. 离线/内网：仅静态资源，无新端口/环境变量  

## Open Questions

- 渐变组数最终 8 还是 12：实现时按对比度验收定，写入 tokens 表即可  
- 管理端是否复用同一套字母渐变：OwnerChip 已在管理端文档场景复用 → **字母渐变随组件走**；管理端壳层仍不引入 `--ws-tint` 染框  
