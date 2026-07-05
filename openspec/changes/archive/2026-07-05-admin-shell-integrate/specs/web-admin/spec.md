## ADDED Requirements

### Requirement: 管理端壳层端到端验收

管理端壳层租户展示与退出 SHALL 在前后端联调环境下可完整走通。

#### Scenario: 端到端租户展示

- **WHEN** 用户通过真实登录进入 `/admin`
- **THEN** navbar 展示的租户名与后端 `/admin-api/system/tenant/default` 一致

#### Scenario: 端到端退出

- **WHEN** 用户从壳层退出登录
- **THEN** 会话清除且受保护路由不可访问
