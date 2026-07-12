## ADDED Requirements

### Requirement: 租户产品模式配置

系统 SHALL 提供 `relayflow.tenant.enabled` 配置项，用于控制是否暴露多租户 SaaS 产品能力。

#### Scenario: 默认单租户模式

- **WHEN** 未配置或 `relayflow.tenant.enabled=false`
- **THEN** 系统以单租户产品模式运行
- **AND** 所有 API 与 WebSocket 请求隐式绑定 `relayflow.tenant.default-id` 指定的租户（默认 `1`）
- **AND** 不要求客户端传递租户标识 Header

#### Scenario: 多租户产品模式预留

- **WHEN** `relayflow.tenant.enabled=true`（将来版本启用）
- **THEN** 系统从 JWT claim `tenant_id` 或配置的 `relayflow.tenant.header-name` Header 解析当前租户
- **AND** 解析失败时拒绝请求（除白名单公开接口）

### Requirement: 默认租户环境变量

自部署安装 SHALL 支持通过环境变量覆盖租户模式相关配置。

#### Scenario: Docker Compose 部署

- **WHEN** 运维通过 `deploy/compose.yml` 启动应用
- **THEN** 可通过 `RELAYFLOW_TENANT_ENABLED` 与 `RELAYFLOW_TENANT_DEFAULT_ID` 配置租户行为
- **AND** 未设置时使用单租户默认值
