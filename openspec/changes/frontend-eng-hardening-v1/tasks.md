## 1. 文档约定（本会话先落地）

- [x] 1.1 更新 `docs/dev/workspace-ui-patterns.md`：模块软上限、租户切换 reset 清单、偏好 API 真源、Account Dock JWT 威胁模型与目标态
- [x] 1.2 更新 `docs/dev/code-style.md` 前端节、`docs/dev/api.md` 分页节、`docs/dev/admin-ui-patterns.md` 列表节；看板将 `user-preference-integrate` 并入本 change 说明

## 2. 租户切换与偏好真源

- [x] 2.1 `tasks` / `calendar` / `userPreference` 增加 `resetForTenantSwitch`；扩展 `useTenantSwitchReload` 统一调用并按路由 refetch
- [x] 2.2 偏好 store：API 类型单源、登录/切租户后 GET 水合、PUT 失败 toast；localStorage 仅作带 tenant+user 作用域缓存

## 3. 列表分页与角标诚实

- [x] 3.1 引入列表默认 `pageSize` 常量；任务列表与管理端用户/角色相关列表改为显式分页或加载更多（禁止静默 `pageSize: 100`）
- [x] 3.2 逾期角标：接 count/筛选 API 或诚实语义；禁止用第一页假充全量

## 4. God Page / Store 拆分

- [x] 4.1 拆分日历：至少抽出 drag、任务投影图层、一处网格展示为 composable/组件
- [x] 4.2 拆分 IM store / 消息页与任务 store：会话列表与发送、任务列表与详情边界可独立阅读

## 5. Account Dock 收紧与验证

- [x] 5.1 核验同账号跨企业走 `tenant/switch`；登出清 dock；`accountDock` 注释指向威胁模型文档（不做 Cookie 迁移）
- [x] 5.2 `cd web && pnpm build && pnpm typecheck`；`openspec validate frontend-eng-hardening-v1 --strict`；浏览器冒烟：切企业无串租、设置偏好、任务翻页、日历拖拽/消息发送
