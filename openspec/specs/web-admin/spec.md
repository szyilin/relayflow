# web-admin Specification

## Purpose
TBD - created by archiving change admin-login-slice. Update Purpose after archive.
## Requirements
### Requirement: 管理端登录页

管理端 SHALL 提供 Web 登录界面，调用已有 `/admin-api/system/auth/login` 并维持会话状态。

#### Scenario: 未登录访问管理页

- **WHEN** 用户未持有有效 token 访问 `/admin` 下受保护路由
- **THEN** 前端重定向至 `/admin/login`

#### Scenario: 登录成功

- **WHEN** 用户在 `/admin/login` 提交正确凭据
- **THEN** 前端保存 access token 并跳转至 `/admin`
- **AND** 后续请求携带 `Authorization: Bearer <token>`

#### Scenario: 登录失败

- **WHEN** 用户提交错误凭据
- **THEN** 页面展示后端返回的 `msg`
- **AND** 不写入 token

### Requirement: 管理端壳层租户展示

管理端壳层 SHALL 展示从 API 获取的默认租户名称。

#### Scenario: 登录后展示租户名

- **WHEN** 用户已登录并进入 `/admin` 壳层
- **THEN** navbar 展示 `GET /admin-api/system/tenant/default` 返回的 `data.name`

#### Scenario: 租户 API 失败

- **WHEN** 租户接口不可用或返回非 0
- **THEN** 壳层仍可渲染
- **AND** 租户名使用 fallback 文案

### Requirement: 管理端退出登录

管理端 SHALL 支持从用户菜单退出并清除本地会话。

#### Scenario: 退出登录

- **WHEN** 用户点击「退出登录」
- **THEN** 清除本地 token 与用户信息
- **AND** 跳转至 `/admin/login`

### Requirement: 管理端壳层端到端验收

管理端壳层租户展示与退出 SHALL 在前后端联调环境下可完整走通。

#### Scenario: 端到端租户展示

- **WHEN** 用户通过真实登录进入 `/admin`
- **THEN** navbar 展示的租户名与后端 `/admin-api/system/tenant/default` 一致

#### Scenario: 端到端退出

- **WHEN** 用户从壳层退出登录
- **THEN** 会话清除且受保护路由不可访问

### Requirement: 管理端前端权限门禁

管理端前端 **SHALL** 在登录后加载权限码，并在用户缺少关联 permission code 时隐藏导航项；**且**须在 `isAdmin=false` 时由门户准入要求拦截，不得仅依赖菜单过滤。导航文案中原「角色管理」入口 MUST 展示为「管理员角色」，路由仍为 `/admin/system/role`（或等价路径）。

#### Scenario: Sidebar 隐藏无权限菜单

- **WHEN** 已登录且 `isAdmin=true` 用户的权限集不包含 `system:role:list`
- **THEN** 管理端 sidebar MUST NOT 展示管理员角色入口

#### Scenario: Sidebar 隐藏部门管理

- **WHEN** 已登录且 `isAdmin=true` 用户的权限集不包含 `system:dept:list`
- **THEN** 管理端 sidebar MUST NOT 展示部门管理入口

#### Scenario: 超级管理员可见全部静态导航

- **WHEN** 已登录且 `isAdmin=true` 用户拥有全部 system 预置权限（如 `super_admin`）
- **THEN** 所有按 permission 门禁的 V1 静态 admin 导航项均可见

#### Scenario: 刷新页面重新加载权限

- **WHEN** 用户在 admin 路由携带有效 token 且 `isAdmin=true` 刷新浏览器
- **THEN** 前端在渲染过滤导航前再次请求权限信息

### Requirement: 管理员角色页面

管理端 SHALL 在 `/admin/system/role` 提供「管理员角色」页面，对齐飞书式心智：用户可见信息仅为角色名称、对应管理员、后台功能权限；MUST NOT 展示角色编码或数据范围。

#### Scenario: 列表列

- **WHEN** 具备 `system:role:list` 的管理员打开该页
- **THEN** 列表至少展示角色名称（系统角色须有可识别标签）、管理员摘要、权限摘要与操作
- **AND** MUST NOT 展示角色编码列或数据范围列

#### Scenario: 创建表单字段

- **WHEN** 管理员打开新建/编辑角色表单
- **THEN** 可填写角色名称、可选上级角色、功能权限、可选备注
- **AND** MUST NOT 展示或要求填写角色编码、数据范围
- **AND** 上级角色留空表示创建一级角色（须有说明或等价提示）

#### Scenario: 权限树折叠与级联

- **WHEN** 打开功能权限树
- **THEN** 节点默认折叠
- **AND** 勾选父节点时选中其全部子孙并展开该父节点

#### Scenario: 角色详情成员与权限

- **WHEN** 管理员打开某角色详情
- **THEN** 界面提供「成员」与「权限」两类视图（Tab 或等价）
- **AND** 成员视图支持查看、添加、移除该角色管理员（受权限门禁）

### Requirement: 管理后台门户准入

管理端前端 SHALL 仅在用户具备管理身份（`isAdmin=true`）时允许进入 `/admin/**` 并渲染 admin 壳层；已登录但非管理员 MUST NOT 看到管理端 sidebar/navbar。

#### Scenario: 管理员进入管理后台

- **WHEN** 已登录用户 `isAdmin` 为 true 访问 `/admin`
- **THEN** 前端渲染 admin 壳层
- **AND** 按既有 permission 规则过滤 sidebar

#### Scenario: 非管理员拒绝进入管理后台

- **WHEN** 已登录用户 `isAdmin` 为 false 访问 `/admin` 或任意 `/admin/**` 子路由
- **THEN** 前端 MUST NOT 渲染 admin 壳层
- **AND** 重定向至 `/app/no-admin-access` 或等价无权限引导页

#### Scenario: 刷新后重新校验管理身份

- **WHEN** 用户在 `/admin` 路由携带有效 token 刷新浏览器
- **THEN** 前端在渲染 admin 壳层前再次请求 permission info
- **AND** 若 `isAdmin` 为 false 则按非管理员场景处理

### Requirement: 工作台管理后台入口可见性

员工工作台 SHALL 仅在 `isAdmin=true` 时展示进入管理后台的导航入口。

#### Scenario: 非管理员不显示入口

- **WHEN** 已登录用户 `isAdmin` 为 false 使用工作台
- **THEN** 工作台 footer 或等价导航 MUST NOT 展示「管理后台」链接

#### Scenario: 管理员显示入口

- **WHEN** 已登录用户 `isAdmin` 为 true 使用工作台
- **THEN** 工作台展示「管理后台」入口
- **AND** 点击后进入 `/admin`

### Requirement: 无管理身份引导页

系统 SHALL 提供 `/app/no-admin-access`（或 spec 等价路径）页面，向已登录但无管理身份的用户说明无法访问管理后台。

#### Scenario: 展示引导页

- **WHEN** 已登录非管理员被门户守卫拦截
- **THEN** 用户看到无权限说明
- **AND** 提供返回工作台（如 `/app/messages`）的操作

### Requirement: Invite settings page in admin shell web

管理端壳层 MUST 能导航至邀请设置页；页面使用管理端布局与 `AdminPageHeader`，视觉与其它系统设置页一致。页面数据 MUST 经 `stores/inviteSettings` 调用 admin-api，不得以会话内存为唯一真源。

#### Scenario: Open from sidebar

- **WHEN** 管理员点击「邀请设置」
- **THEN** 渲染邀请设置表单且不离开 `/admin` 前缀路由

#### Scenario: Load settings from API on enter

- **WHEN** 管理员进入邀请设置页且具备 `system:user:list`
- **THEN** 客户端请求 `GET /admin-api/system/invite-settings` 并展示返回值

### Requirement: Overview statistics replace empty placeholder

管理端概览页 MUST 用企业身份与统计卡替换「统计数据将在接入 API 后展示」空状态，并在加载失败时给出可读错误提示。

#### Scenario: Placeholder removed when data loaded

- **WHEN** overview API 成功返回
- **THEN** 不再展示「统计数据将在接入 API 后展示」文案

### Requirement: Enterprise info page logo control

企业信息「基础信息」区 MUST 支持点击/按钮更换企业头像（需 `system:enterprise:update`），上传走管理端文件会话；无更新权限时仅展示。

#### Scenario: Updater can change logo

- **WHEN** 具备 update 权限的管理员在企业信息页选择图片并上传成功
- **THEN** 基础信息区头像立即更新为新图

### Requirement: 管理端企业信息导航与页面

管理端壳层 SHALL 在具备相应 permission 时展示「企业信息」导航项，并路由至 `/admin` 前缀下的企业信息页面（含企业信息管理与企业地址管理两个 Tab）。

#### Scenario: 有权限可见

- **WHEN** 当前用户 permission 包含企业信息查询码
- **THEN** 侧栏显示企业信息入口且可进入页面

#### Scenario: 无权限隐藏

- **WHEN** 当前用户不具备企业信息相关 permission
- **THEN** 侧栏不显示该入口
- **AND** 直接访问路由时被前端权限门禁拦截或提示无权限

### Requirement: Product settings navigation in admin shell

管理端壳层导航 MUST 提供进入「产品设置」的入口，并在用户具备相应 permission 时可见。产品设置子路由 MUST 使用 `/admin` 前缀，且遵循既有 `isAdmin` 门户准入与 permission 过滤规则。

#### Scenario: Sidebar shows product settings

- **WHEN** 已登录且 `isAdmin=true` 且拥有产品设置相关查询权限的用户进入管理端
- **THEN** 导航中可见产品设置入口

#### Scenario: Hide without permission

- **WHEN** 管理员缺少产品设置查询 permission
- **THEN** 导航 MUST NOT 展示产品设置入口（或进入后拒绝，与既有 admin 门禁一致）

