# web-auth Specification

## Purpose
TBD - created by archiving change unified-login-slice. Update Purpose after archive.
## Requirements
### Requirement: 统一 Web 登录入口

系统 SHALL 仅提供 **一个** 产品级登录页；管理员与普通员工使用同一登录接口与同一 JWT 会话。

#### Scenario: 登录成功后进入工作台

- **WHEN** 用户在 `/app/login` 提交有效凭据
- **THEN** 前端保存 access token
- **AND** 当仅解析出一个活跃租户时跳转至 `/app/messages`
- **AND** 当存在多个活跃租户且须选择时，在登录页或弹层中让用户选择企业后再进入工作台

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

### Requirement: 账号注册页

当多租户开放注册启用时，系统 SHALL 在工作台提供 `/app/register` 注册页，用于创建账号与企业。

#### Scenario: 注册表单字段

- **WHEN** 用户打开 `/app/register`
- **THEN** 页面展示手机号、密码、确认密码、昵称与企业名称（tenantName）字段
- **AND** 使用 workspace 认证布局

#### Scenario: 注册成功进入工作台

- **WHEN** 用户提交有效注册数据且 API 成功
- **THEN** 前端保存 access token 与活跃租户
- **AND** 跳转至 `/app/messages`

#### Scenario: 登录页注册入口

- **WHEN** 用户查看 `/app/login`
- **THEN** 提供指向 `/app/register` 的注册链接（如「没有账号？注册」）
- **AND** MUST NOT 以「收到邀请？设置密码加入」类文案作为唯一主入口

#### Scenario: 旧邀请接受页重定向

- **WHEN** 用户访问 `/app/invite/accept`
- **THEN** 前端重定向至 `/app/register`
- **AND** 可从 query 预填手机号

### Requirement: 工作台企业切换器

当用户拥有多个 ACTIVE 租户成员关系时，工作台壳层 SHALL 暴露企业切换器。

#### Scenario: 展示当前企业

- **WHEN** 已认证用户拥有一个或多个 ACTIVE 企业
- **THEN** 工作台头部展示当前企业名称

#### Scenario: 切换企业

- **WHEN** 用户从切换器选择另一企业
- **THEN** 前端调用企业切换 API、更新 token，并刷新租户范围 UI 状态（含 WebSocket 重连）

#### Scenario: 登录时企业选择

- **WHEN** 登录 API 返回 `TENANT_SELECTION_REQUIRED` 及企业列表
- **THEN** 登录页或弹层让用户选择企业后再进入工作台

