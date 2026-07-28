## ADDED Requirements

### Requirement: 管理端企业信息导航与页面

管理端壳层 SHALL 在具备相应 permission 时展示「企业信息」导航项，并路由至 `/admin` 前缀下的企业信息页面（含企业信息管理与企业地址管理两个 Tab）。

#### Scenario: 有权限可见

- **WHEN** 当前用户 permission 包含企业信息查询码
- **THEN** 侧栏显示企业信息入口且可进入页面

#### Scenario: 无权限隐藏

- **WHEN** 当前用户不具备企业信息相关 permission
- **THEN** 侧栏不显示该入口
- **AND** 直接访问路由时被前端权限门禁拦截或提示无权限
