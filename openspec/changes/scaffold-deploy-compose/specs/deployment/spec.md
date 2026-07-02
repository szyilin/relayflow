## ADDED Requirements

### Requirement: 本地基础设施 Compose 编排

`deploy/compose.yml` SHALL 定义 PostgreSQL、Redis、MinIO 服务，供本地开发与自部署使用。

#### Scenario: Compose 语法有效

- **WHEN** 开发者执行 `docker compose -f deploy/compose.yml config`
- **THEN** 配置解析成功且无语法错误
- **AND** 包含 postgres、redis、minio 三个服务定义
