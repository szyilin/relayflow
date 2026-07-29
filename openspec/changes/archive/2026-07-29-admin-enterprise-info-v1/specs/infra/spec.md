## ADDED Requirements

### Requirement: 平台共享参考表允许无 tenant_id

基础设施模块 MAY 提供平台共享参考数据表（如行政区划），此类表使用 `infra_` 前缀但 MUST NOT 强制 `tenant_id`；迁移与文档 MUST 标明平台共享语义。其余租户隔离的 infra 业务表仍 MUST 含 `tenant_id`。

#### Scenario: 行政区划表

- **GIVEN** 需要存储全国共享的行政区划
- **WHEN** 应用 Flyway 创建 `infra_geo_region`（或等价表名）
- **THEN** 表名以 `infra_` 开头
- **AND** 表结构可不含 `tenant_id`
- **AND** 注释或规格标明平台共享
