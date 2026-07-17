# frontend-eng Specification

## Purpose

工作台与管理端前端工程约定：模块体量、租户切换状态生命周期、列表分页 UX、偏好客户端真源策略、Account Dock 凭证存储威胁模型（V1 过渡 vs 目标态）。

## Requirements

### Requirement: Frontend module size soft limits

工作台与管理端前端 MUST 避免将无关职责堆进单一 SFC 或 Pinia store。单文件建议不超过约 500 行；超过时 MUST 将逻辑抽到 `composables/` 与/或展示组件，或在文件头注释标明待拆边界。日历页、IM store、消息页、任务 store 在本能力落地时 MUST 完成一轮职责拆分，使主文件体量显著下降且职责边界可评审。

#### Scenario: Calendar page is composed of smaller units

- **WHEN** 开发者打开日历功能实现
- **THEN** 拖拽改期、任务投影图层、日/周/月网格中至少两类逻辑不在同一巨型 SFC 内联全部实现
- **AND** 页面 SFC 主要负责编排与路由深链

#### Scenario: IM and tasks stores expose focused surfaces

- **WHEN** 开发者修改会话发送或任务列表逻辑
- **THEN** 可在独立 composable 或拆分后的 store 模块中定位，而无需通读整份历史巨型 store 才能理解单一路径

### Requirement: Tenant or account switch resets tenant-scoped stores

当 JWT 中的 `tenantId` 或 `userId` 变更（企业切换、Account Dock 切会话）时，前端 MUST 清空所有租户范围的 Pinia 域状态，并按当前路由重新加载所需数据。至少 MUST 覆盖：`profile`、`im`、`contacts`、`tasks`、`calendar`、`userPreference`。MUST NOT 在切换后继续展示上一租户的任务列表、日历事件或偏好。

#### Scenario: Switch enterprise clears tasks and calendar

- **WHEN** 已登录成员从租户 A 切换到租户 B
- **THEN** 任务与日历内存状态被清空
- **AND** 若当前路由为任务或日历页，则按 B 的上下文重新请求数据
- **AND** MUST NOT 短暂或持续展示租户 A 的任务/日程作为 B 的数据

#### Scenario: Preference rehydrates for new tenant

- **WHEN** 成员切换到另一租户
- **THEN** 用户偏好 store 重置并重新 GET 该租户偏好
- **AND** MUST NOT 继续应用上一租户的主题/日历设置作为权威状态

### Requirement: List UIs must not silently truncate at page size

凡调用分页 API 的工作台或管理端列表，前端 MUST 使用显式分页或无限加载（「加载更多」），并使 `pageSize` 与产品默认一致（建议遵循 API 默认 20，且不超过服务端上限 100）。当 `total` 大于当前已加载条数时，UI MUST 提供继续浏览的方式。MUST NOT 固定请求一页 `pageSize: 100` 且无任何翻页/加载更多而假装列表完整。

#### Scenario: Task list beyond one page

- **WHEN** 当前视图下任务 `total` 大于一页 `pageSize`
- **THEN** 用户可通过分页或加载更多看到后续任务
- **AND** MUST NOT 仅展示第一页且无任何「还有更多」指示

#### Scenario: Admin role or user picker lists

- **WHEN** 管理端角色或用户相关列表/选项的数据量超过一页
- **THEN** UI 提供分页或等价加载更多
- **AND** MUST NOT 静默截断于 100 条而无提示

### Requirement: Aggregate badges must not pretend full dataset

依赖列表查询结果的角标或计数（如任务逾期数）MUST 基于完整计数来源，或在仅有分页数据时诚实反映范围（例如仅当前已加载）。MUST NOT 在只拉取第一页时把结果展示为「全部逾期数」且无说明。

#### Scenario: Overdue badge honesty

- **WHEN** 前端刷新任务逾期角标且仅能获得分页列表而非全量 count
- **THEN** 实现选择：调用专用 count/筛选 API，或角标语义不声称全量
- **AND** MUST NOT 用第一页 100 条 TODO 的过滤结果静默当作全量逾期数

### Requirement: User preference client uses API as source of truth

工作台用户偏好客户端 MUST 以 `GET/PUT` 用户偏好 API 的返回为权威状态。localStorage MUST 至多作为带 `(tenantId, userId)` 作用域的短时缓存。UI 变更 MUST 同步到服务端；同步失败时 MUST 给予用户可感知反馈（如 toast）。Store 与 API 模块 MUST 共用同一套设置类型定义，MUST NOT 维护两套易漂移的平行类型。

#### Scenario: Login hydrates preference from server

- **WHEN** 成员登录或切换到某租户后进入工作台
- **THEN** 客户端 GET 该租户偏好并应用到主题等 UI
- **AND** MUST NOT 仅凭无租户作用域的旧 localStorage 覆盖服务端结果作为长期真源

#### Scenario: Save failure is visible

- **WHEN** 成员在设置中修改偏好且 PUT 失败
- **THEN** 用户看到失败提示
- **AND** MUST NOT 仅写入 localStorage 并空 `catch` 吞掉错误

### Requirement: Account dock JWT threat model is documented for V1

V1 工作台 Account Dock 在浏览器 `localStorage` 持久化多会话以便一键切换时，项目文档 MUST 说明威胁模型：页面 XSS 可读取全部 dock 中的 JWT，导致多会话凭证泄露。实现 MUST 在登出时清除对应用户的 dock 条目。同账号跨企业切换 MUST 走租户切换 API 并刷新会话，MUST NOT 依赖过期或错误的拷贝 token 作为切换成功条件。完整的 httpOnly Cookie / 服务端会话列表迁移为本能力目标态，MAY 另立项，不阻塞本要求的文档与 V1 行为核验。

#### Scenario: Docs describe multi-token XSS amplification

- **WHEN** 开发者阅读工作台 UI / 前端会话约定文档
- **THEN** 可见 Account Dock 多 JWT 存储的 XSS 放大风险说明
- **AND** 可见 V1 过渡与目标态（opaque session / httpOnly）区分

#### Scenario: Logout clears dock entries for user

- **WHEN** 成员从资料名片退出登录
- **THEN** 前端移除该 `userId` 下全部 Account Dock 条目（与既有产品行为一致）
- **AND** 激活下一 dock 会话或跳转登录页
