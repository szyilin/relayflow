## ADDED Requirements

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
