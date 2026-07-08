## ADDED Requirements

### Requirement: 产品面未登录强制跳转登录

系统 SHALL 在用户未持有有效 token 时，禁止访问 `/app/**` 下除 `/app/login` 以外的路由；须重定向至 `/app/login` 并携带 `redirect` 查询参数。

#### Scenario: 未登录访问工作台

- **WHEN** 用户未持有 token 访问 `/app/messages`
- **THEN** 前端重定向至 `/app/login`
- **AND** `redirect` 查询参数包含原路径

#### Scenario: 未登录不渲染产品面壳层

- **WHEN** 用户未持有 token 访问任意 `/app/**` 受保护路由
- **THEN** 前端 MUST NOT 渲染 workspace 壳层后再提示未登录

### Requirement: 管理面未登录强制跳转登录

系统 SHALL 在用户未持有有效 token 时，禁止访问 `/admin/**`；须重定向至 `/app/login` 并携带 `redirect` 查询参数（**不**使用独立管理员登录页）。

#### Scenario: 未登录访问管理后台

- **WHEN** 用户未持有 token 访问 `/admin/system/user`
- **THEN** 前端重定向至 `/app/login`
- **AND** `redirect` 查询参数包含原路径

#### Scenario: 未登录不渲染管理端壳层

- **WHEN** 用户未持有 token 访问任意 `/admin/**` 路由
- **THEN** 前端 MUST NOT 渲染 admin 壳层后再提示未登录
