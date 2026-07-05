## ADDED Requirements

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
