## Context

工作台 `web/` 已完成登录、IM、任务、日历、偏好等纵向切片联调。审查发现五类工程债：超大 SFC/Store、租户切换未清任务/日历/偏好、列表硬编码 `pageSize: 100`、偏好 localStorage 先行、Account Dock 多 JWT。本 change 仅前端与文档，无后端 API/表结构变更。

当前关键落点：

| 主题 | 现状 |
|------|------|
| 体量 | `calendar/index.vue` ~1381、`stores/im.ts` ~698、`messages/index.vue` ~652、`stores/tasks.ts` ~463 |
| 切租户 | `useTenantSwitchReload` 只 reset `profile` / `im` / `contacts` |
| 分页 | 任务与部分 admin 列表固定 `pageSize: 100`，无翻页 UI |
| 偏好 | `userPreference` 先写 localStorage，再静默 `syncToServer` |
| Dock | `relayflow:account-dock` 持久化多份 `token` |

## Goals / Non-Goals

**Goals:**

1. 按职责拆分日历 / IM / 消息 / 任务的大文件，建立软上限约定并写入 `docs/dev`。
2. 企业或账号切换时清空并按需重载所有租户相关 Pinia 状态（含 tasks、calendar、userPreference）。
3. 列表在 `total > pageSize` 时提供分页或无限加载；角标/计数不得假装「全量」。
4. 偏好以 GET/PUT preference API 为真源；统一类型；同步失败可感知。
5. 文档化 Dock JWT 威胁模型并做 V1 可落地收紧（清理策略、同账号多企业不重复无意义拷贝说明）；产品「一键切会话」行为保留。

**Non-Goals:**

- 引入 Vitest/Playwright 或测试流程强制门禁（快速开发阶段明确不做）。
- 实现 httpOnly Cookie、refresh token 轮换、服务端会话列表（目标态另立项）。
- 扩大后端 `pageSize` 上限或改分页协议。
- 重写日历交互或 IM 协议；仅拆分与状态生命周期。
- 一次拆到「完美」：日历优先拆出 drag / task-layer / 网格展示；IM 优先拆出会话列表与发送路径。

## Decisions

### D1 — 拆分策略：composable 优先，再抽展示组件

- **选择**：先把逻辑迁到 `composables/calendar/*`、`composables/im/*`、`composables/tasks/*`，页面保留编排；大块 UI 再抽 `components/workspace|calendar|im/*`。
- **替代**：按「特性文件夹」整页搬家 — 改动面过大，本 change 不做。
- **软上限（文档约定，非 CI 硬拦）**：单 SFC/store 建议 ≤500 行；超过须有 composable 边界或注明「待拆」。

### D2 — 租户切换：统一 `resetForTenantSwitch` 钩子

- **选择**：各租户相关 store 暴露 `resetForTenantSwitch()`（或复用已有命名）；`useTenantSwitchReload` 集中调用，再按 `route.path` refetch。
- **必须纳入**：`tasks`、`calendar`、`userPreference`（已有 im/contacts/profile）。
- **替代**：路由 `onBeforeRouteUpdate` 各自清 — 易漏；集中 composable 更安全。

### D3 — 列表分页：UI 显式 + 默认 pageSize 常量

- **选择**：引入 `DEFAULT_LIST_PAGE_SIZE`（建议 20，与 `api.md` 默认一致）；任务列表与 admin 用户/角色列表用 `UPagination` 或「加载更多」；`total` 驱动可见性。
- **逾期角标**：若无独立 count API，MUST 标明「基于当前已加载页」或后续加 count 接口（本 change 优先：分页拉全量角标不现实时，用后端已有 count/筛选能力；若无则文档诚实展示，禁止静默当全量）。
- **替代**：一次请求 `pageSize: 100` 假装够用 — 否决。

### D4 — 偏好：hydrate from API，localStorage 为 cache

- **选择**：登录成功 / 切租户 reset 后 `fetchPreference()`；UI 变更 debounce PUT；失败 toast；localStorage 仅缓存最近成功 GET 结果（可选，键带 `tenantId+userId`）。
- **类型**：`api/app/userPreference.ts` 为源；store re-export / import，禁止平行定义两套。
- **替代**：继续 local-first — 否决（多端漂移）。

### D5 — Dock JWT：文档 + V1 收紧，不做 Cookie 迁移

- **选择**：
  - `docs/dev` 写明威胁模型（XSS → 多会话泄露）。
  - 代码注释指向文档；登出继续清除该 `userId` 全部 dock 条目。
  - 同账号多企业：切换走 `tenant/switch` 的条目不强制为每个租户复制同一 JWT 字符串作为长期权威（实现上可保留 entry，但切换同 user 必须走 switch API 刷新 token，避免过期拷贝误用）——与现有「同 user 不同 tenant 调 switch」对齐并核验。
- **目标态（另 change）**：opaque session id 或 httpOnly；本 change 只改文档 + 行为核验/小收紧。
- **替代**：本 change 就上 Cookie — 牵涉后端 Security，超出范围。

### D6 — 文档落点

| 约定 | 写入 |
|------|------|
| 模块体量、租户 reset、偏好真源、Dock 威胁模型 | `workspace-ui-patterns.md` |
| 前端分层补充（分页、store reset） | `code-style.md` 前端节 |
| 管理端列表须分页 | `admin-ui-patterns.md` 列表节 |
| 偏好 integrate 状态 | `api-integration-board.md`（标记本 change） |
| API pageSize 上限与前端义务 | `api.md` 分页节补一句「前端不得静默截断」 |

## Risks / Trade-offs

- [拆分引入回归] → 按域小步拆；每步 `pnpm build` + `pnpm typecheck`；浏览器冒烟日历拖拽与消息发送。
- [分页后首次加载条目变少] → 默认 20，可接受；UI 提供翻页。
- [偏好改服务端真源后短暂闪烁] → 先用缓存再 hydrate，或设置窗打开时 loading。
- [Dock 仍存 JWT] → 威胁模型已文档化；完整缓解依赖后续安全 change。

## Migration Plan

1. 文档先合入（约定可见），再按 tasks 顺序改代码。
2. 无 DB/配置迁移；用户可能需重新登录以刷新偏好缓存键。
3. 回滚：还原 `web/` 与 `docs/dev` 对应提交。

## Open Questions

- 逾期角标是否已有 count API？实现时先查 task API；若无，本 change 用不诚实全量或加最小 count（若加 API 则需扩 scope，优先前端诚实展示）。
- 日历拆分深度：是否本 change 必须拆完所有 view mode？默认：至少拆出 drag + task-layer + 一个网格组件，主文件显著下降即可。
