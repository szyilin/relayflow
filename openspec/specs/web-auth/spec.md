# web-auth Specification

## Purpose
TBD - created by archiving change unified-login-slice. Update Purpose after archive.
## Requirements
### Requirement: 统一 Web 登录入口

系统 SHALL 仅提供 **一个** 产品级登录页；管理员与普通员工使用同一登录接口与同一 JWT 会话。

#### Scenario: 登录成功后进入工作台

- **WHEN** 用户在 `/app/login` 提交有效凭据
- **THEN** 前端保存 access token
- **AND** 默认跳转至 `/app/messages`

#### Scenario: 未登录访问管理端

- **WHEN** 用户未持有 token 访问 `/admin` 下受保护路由
- **THEN** 重定向至 `/app/login` 并携带 `redirect` 查询参数

#### Scenario: 旧管理端登录 URL 兼容

- **WHEN** 用户访问 `/admin/login`
- **THEN** 重定向至 `/app/login`（保留 query）

#### Scenario: 禁止双登录入口

- **WHEN** 产品导航或首页展示登录入口
- **THEN** MUST NOT 同时提供独立的「管理员登录」与「员工登录」两个入口

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

