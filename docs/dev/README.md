# 开发者文档

面向贡献者与 AI 编码代理的实现约定。**行为规格**（系统应做什么）以 `openspec/specs/` 为准；本目录描述 **怎么做**。

| 文档 | 内容 |
|------|------|
| [**product-permission-model.md**](product-permission-model.md) | **双产品面**、登录门禁、管理身份 vs 组织成员、RBAC 适用范围 |
| [architecture.md](architecture.md) | **V1 微服务就绪单体**、内核 vs 业务域、四条耦合铁律、Phase 2 拆分 |
| [**im-messaging-architecture-draft.md**](im-messaging-architecture-draft.md) | **IM 架构（已拍板）**：Bot 统一业务触达；真源见 OpenSpec `im-bot-notify-foundation` |
| [**im-bot-interactive-card.md**](im-bot-interactive-card.md) | **可交互卡片**：进程内 SPI、`open_url`/`callback(+form)`；实现待 `im-bot-interactive-card` |
| [**cross-domain-messaging.md**](cross-domain-messaging.md) | **跨域同步/异步评判**、领域消息契约、当前 Redis → 目标 MQ |
| [api.md](api.md) | 统一响应、错误码、分页、HTTP 状态；与前端路由对齐 |
| [database.md](database.md) | 表前缀、V1 单库分域、公共字段、雪花 ID、Flyway 命名 |
| [code-style.md](code-style.md) | Java 分层、跨域 `*-api`、领域消息落点、VO/DTO、JWT/RBAC、前端路由 |
| [codegen.md](codegen.md) | **DO/Mapper/XML**：CLI 临时参照 → diff 合入 `src/`；自定义 SQL 用 ExtMapper |
| [vertical-slice-workflow.md](vertical-slice-workflow.md) | **纵向切片**：后端 API + `web/` 同批交付、tasks 模板、推荐路线 |
| [git-and-idea.md](git-and-idea.md) | **Git 纳入/忽略清单**、IntelliJ IDEA 导入与 Maven Wrapper |

协作与 Git 流程见根目录 [CONTRIBUTING.md](../../CONTRIBUTING.md)。
