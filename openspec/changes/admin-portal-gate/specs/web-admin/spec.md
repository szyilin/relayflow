## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: 管理端前端权限门禁

管理端前端 **SHALL** 在登录后加载权限码，并在用户缺少关联 permission code 时隐藏导航项；**且**须在 `isAdmin=false` 时由门户准入要求拦截，不得仅依赖菜单过滤。

#### Scenario: Sidebar 隐藏无权限菜单

- **WHEN** 已登录且 `isAdmin=true` 用户的权限集不包含 `system:role:list`
- **THEN** 管理端 sidebar MUST NOT 展示角色管理入口

#### Scenario: 超级管理员可见全部静态导航

- **WHEN** 已登录且 `isAdmin=true` 用户拥有全部 system 预置权限（如 `super_admin`）
- **THEN** 所有按 permission 门禁的 V1 静态 admin 导航项均可见

#### Scenario: 刷新页面重新加载权限

- **WHEN** 用户在 admin 路由携带有效 token 且 `isAdmin=true` 刷新浏览器
- **THEN** 前端在渲染过滤导航前再次请求权限信息
