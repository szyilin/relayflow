## ADDED Requirements

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
