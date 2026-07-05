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

