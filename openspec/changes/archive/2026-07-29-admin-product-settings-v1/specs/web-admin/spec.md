## ADDED Requirements

### Requirement: Product settings navigation in admin shell

管理端壳层导航 MUST 提供进入「产品设置」的入口，并在用户具备相应 permission 时可见。产品设置子路由 MUST 使用 `/admin` 前缀，且遵循既有 `isAdmin` 门户准入与 permission 过滤规则。

#### Scenario: Sidebar shows product settings

- **WHEN** 已登录且 `isAdmin=true` 且拥有产品设置相关查询权限的用户进入管理端
- **THEN** 导航中可见产品设置入口

#### Scenario: Hide without permission

- **WHEN** 管理员缺少产品设置查询 permission
- **THEN** 导航 MUST NOT 展示产品设置入口（或进入后拒绝，与既有 admin 门禁一致）
