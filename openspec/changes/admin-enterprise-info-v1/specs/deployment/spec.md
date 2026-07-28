## MODIFIED Requirements

### Requirement: 本地基础设施 Compose 编排

`deploy/compose.yml` SHALL 定义 PostgreSQL、Redis、MinIO 与 XXL-JOB Admin（及其文档约定的依赖存储）服务，供本地开发与自部署使用。

#### Scenario: Compose 语法有效

- **WHEN** 开发者执行 `docker compose -f deploy/compose.yml config`
- **THEN** 配置解析成功且无语法错误
- **AND** 包含 postgres、redis、minio、以及 xxl-job-admin（或等价服务名）的服务定义

## ADDED Requirements

### Requirement: XXL-JOB 种子随仓库分发

仓库 MUST 包含 XXL-JOB 表结构与官方任务种子的版本化文件（路径在 design/README 约定，如 `deploy/xxl-job/`），使自托管者初始化后即可获得默认任务列表。

#### Scenario: 文档可定位种子

- **WHEN** 自托管者阅读部署文档或 `deploy/xxl-job/README.md`
- **THEN** 能明确得知如何应用 schema 与 seed，以及默认 Admin 访问方式
- **AND** 官方任务列表不以「到业务代码中搜索注解」为唯一发现途径
