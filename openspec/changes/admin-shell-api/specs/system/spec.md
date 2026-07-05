## ADDED Requirements

### Requirement: 默认租户查询 API

系统 SHALL 提供匿名可访问的管理端接口，返回 V1 默认租户信息供壳层展示。

#### Scenario: 查询默认租户成功

- **WHEN** 客户端请求 `GET /admin-api/system/tenant/default`
- **THEN** 响应 `code` 为 0
- **AND** `data` 包含 `id`、`code`、`name`、`status`、`createTime`

#### Scenario: 未携带 JWT 可访问

- **WHEN** 请求未带 `Authorization` 头
- **THEN** 仍返回成功结果（permitAll）
