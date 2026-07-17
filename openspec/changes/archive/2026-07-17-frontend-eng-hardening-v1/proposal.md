## Why

工作台前端已过脚手架与多域联调，但存在五类工程债：超大 SFC/Store、企业切换状态清不干净（串租风险）、列表硬编码 `pageSize: 100` 静默截断、用户偏好仍以 localStorage 先行并静默 sync、Account Dock 在 localStorage 堆多份 JWT。现在收敛可避免后续切片在错误约定上继续堆积。

## What Changes

- **拆分 God Page / God Store**：日历页、IM store、消息页、任务 store 按职责拆成 composable + 展示组件；建立软上限约定（写入 `docs/dev`）。
- **企业/账号切换必须重置域状态**：`useTenantSwitchReload`（及等价路径）对 `tasks` / `calendar` / `userPreference` 等租户相关 store 执行 `reset` + 按路由 refetch，消除串租展示。
- **列表不得静默截断于 100**：任务与管理端角色/用户等列表改为显式分页或无限加载；逾期角标等聚合不得只依赖「第一页 100 条」。
- **用户偏好：服务端为真源**：登录/切租户后以 GET preference 为准；localStorage 仅作短时缓存；统一 API/store 类型；同步失败须可感知（toast），去掉 integrate 期空 `catch` 主路径。
- **Account Dock JWT 威胁模型与收紧**：文档化「localStorage 多 JWT = XSS 放大」；本 change 做可落地的收紧（重复 token 策略、登出清理、注释/文档），**不**在本 change 实现 httpOnly Cookie / 服务端会话列表（记为目标态，单独立项）。

无 **BREAKING** API 变更；后端分页上限仍为 100（见 `docs/dev/api.md`）。

## Capabilities

### New Capabilities

- `frontend-eng`: 工作台/管理端前端工程约定——模块体量、租户切换状态生命周期、列表分页 UX、偏好客户端真源策略、Account Dock 凭证存储威胁模型（V1 过渡 vs 目标态）。

### Modified Capabilities

- `user-preference`: 前端客户端 MUST 以 preference API 为设置真源；localStorage 不得再作为跨会话权威写路径。
- `web-auth`: Account Dock 多会话持久化要求补充威胁模型与 V1 过渡约束；目标态（opaque session / httpOnly）记为后续，不改变「可一切换」产品行为。

## Impact

- **代码**：仅 `web/`（stores、composables、pages、components、api 类型对齐）；不改 Java / Flyway / deploy。
- **文档**：更新 `docs/dev/workspace-ui-patterns.md`、`docs/dev/code-style.md`（前端节）、必要时 `admin-ui-patterns.md` / `api-integration-board.md` / `default-data-provisioning.md` 中与偏好真源矛盾的表述。
- **回滚**：纯前端；回滚即还原 `web/` 与文档；无数据迁移。
- **自部署**：无服务端配置变更；用户侧可能需重新登录以刷新偏好缓存。
