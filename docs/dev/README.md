# 开发者文档

面向贡献者与 AI 编码代理的实现约定。**行为规格**（系统应做什么）以 `openspec/specs/` 为准；本目录描述 **怎么做**。

| 文档 | 内容 |
|------|------|
| [architecture.md](architecture.md) | **V1 微服务就绪单体**、四条耦合铁律、Phase 2 演进 |
| [api.md](api.md) | 统一响应、错误码、分页、HTTP 状态；与前端路由对齐 |
| [database.md](database.md) | 表前缀、V1 单库分域、公共字段、雪花 ID、Flyway 命名 |
| [code-style.md](code-style.md) | Java 分层、跨域 `*-api`、VO/DTO、JWT/RBAC、前端路由 |
| [git-and-idea.md](git-and-idea.md) | **Git 纳入/忽略清单**、IntelliJ IDEA 导入与 Maven Wrapper |

协作与 Git 流程见根目录 [CONTRIBUTING.md](../../CONTRIBUTING.md)。
