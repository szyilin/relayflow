## ADDED Requirements

### Requirement: Mock 原型模式

在 `admin-ui-prototype` change 完成前，管理端前端 MUST 以 Mock 数据运行，MUST NOT 调用 `/admin-api/*` 或依赖 `relayflow-server` 启动；页面数据 MUST 来自 `web/src/mocks/` 或读取 Mock 的 Pinia store。

#### Scenario: 无后端启动

- **WHEN** 仅执行 `cd web && pnpm dev`
- **THEN** 用户 MUST 能完成 Mock 登录并浏览管理端占位页
- **AND** 浏览器网络面板 MUST NOT 出现对 `/admin-api/` 的请求

#### Scenario: Mock 登录

- **WHEN** 用户在 `/admin/login` 输入任意非空用户名与密码并提交
- **THEN** 系统 MUST 写入 Mock 登录态并跳转至 `/admin`
- **AND** MUST 展示 toast 或等效成功反馈

---

### Requirement: 管理端完整路由占位

管理端 MUST 提供以下可访问路由及对应页面模式（内容可为占位）：登录、概览、用户列表、用户表单、部门占位、文件占位、设计预览；各页 MUST 共用 `admin` 壳层（登录页除外）。

#### Scenario: 信息架构可浏览

- **WHEN** 已 Mock 登录用户通过 sidebar 导航
- **THEN** MUST 能依次访问概览、用户管理、部门管理、文件管理
- **AND** MUST NOT 出现 404 或跳出 `/admin` 壳层（登录页除外）

#### Scenario: 登录页独立布局

- **WHEN** 用户访问 `/admin/login`
- **THEN** MUST NOT 渲染管理端 sidebar
- **AND** MUST 使用左右分栏认证布局（B · Clean Enterprise）

---

### Requirement: Mock 路由守卫

管理端 MUST 实现基于 Mock token 的路由守卫：未登录访问受保护路由 MUST 重定向至登录页；已登录访问登录页 MUST 重定向至概览。

#### Scenario: 未登录拦截

- **WHEN** Mock token 不存在且用户访问 `/admin/system/user`
- **THEN** MUST 重定向至 `/admin/login`
- **AND** SHOULD 携带 `redirect` 查询参数

#### Scenario: 已登录跳过登录页

- **WHEN** Mock token 存在且用户访问 `/admin/login`
- **THEN** MUST 重定向至 `/admin`

---

### Requirement: 代表型页面模式

原型 MUST 至少实现以下 UI 模式各一例：概览统计卡片、数据列表（表格+分页+筛选）、表单页（分组卡片+提交反馈）、空状态占位、组件/token 预览板。

#### Scenario: 用户列表模式

- **WHEN** 用户打开 `/admin/system/user`
- **THEN** MUST 展示 Mock 用户表格与分页
- **AND** 筛选 MUST 在本地过滤 Mock 数据（无需服务端）

#### Scenario: 设计预览板

- **WHEN** 用户打开 `/admin/design-preview`
- **THEN** MUST 展示主色、按钮、表单、表格、空状态等 Nuxt UI 组件示例

---

### Requirement: 视觉基调一致

Mock 原型 MUST 遵循 `admin-ui-design-direction` 已确认的 **B · Clean Enterprise**：teal 主色、Inter 字体栈、color mode 跟随系统、品牌文案 RelayFlow。

#### Scenario: Token 集中定义

- **WHEN** 开发者查看 `web/src/assets/css/main.css`
- **THEN** MUST 存在与 B 方向一致的 `@theme` 定义
- **AND** 占位页 MUST 使用 semantic 色 class 而非散落 hex

---

### Requirement: 模板演示页隔离

dashboard-vue 模板自带的非 RelayFlow 演示路由（如 customers、inbox、settings）MUST NOT 作为产品入口暴露；MUST 从 auto-routes 中移除或隔离。

#### Scenario: 无 demo 菜单项

- **WHEN** 用户查看管理端 sidebar
- **THEN** MUST NOT 出现 Customers、Inbox 等模板演示菜单
- **AND** 仅展示 RelayFlow 模块占位入口

---

### Requirement: UI 定调人工验收

`admin-ui-prototype` 全部代表页与壳层在浏览器走查通过后，产品负责人 MUST 在 `admin-ui-design-direction/design.md`「验收记录」或等效 PR 评论中签字确认「UI 定调通过」，并记录定稿 commit；**未签字前** MUST NOT 开始 `admin-ui-design-direction` 规则沉淀终稿或 `admin-login-slice` 正式 UI 对接。

#### Scenario: 签字门闩

- **WHEN** 原型 `pnpm build` 通过且浏览器走查完成，但验收记录未填写或未确认通过
- **THEN** `admin-ui-design-direction` 阶段 2（规则抽取）MUST NOT 作为完成态归档
- **AND** `admin-login-slice` MUST 保持暂缓

#### Scenario: 签字后进入规则沉淀

- **WHEN** 验收记录结论为「UI 定调通过」且 commit 已记录
- **THEN** 团队 MUST 从该 commit 的 `web/` 代码抽取规则至 `docs/dev/` 与 `.cursor/rules/`
- **AND** 后续纵向切片 MUST 保留原型页面结构，仅替换 Mock 数据层
