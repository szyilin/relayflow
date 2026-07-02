# 提案：RelayFlow V1 后端基础架构

## Intent

确定 RelayFlow 第一版（V1）后端的技术栈、Maven 多模块代码组织方式，以及各业务域的边界与实施顺序。本变更仅产出架构与规格文档，作为后续实现的唯一依据。

## Scope

### In scope

- V1 技术栈选型（语言、框架、中间件、测试、部署）
- 仓库目录结构（根目录 Java Maven 父工程 + `web/` 前端）
- Maven 模块划分：`dependencies`、`framework`、业务 `module-*`（api/biz 双模块）、`server`
- api/biz 依赖规则与 biz 模块内部包结构
- 数据库表前缀约定、API 路径约定、WebSocket 约定
- V1 功能边界：system、infra、im 必开；bpm 工作流 V1.1 再开
- OpenSpec 域规格初稿：deployment、system、infra、im

### Out of scope

- 具体业务代码实现（由后续 change 完成）
- 前端 UI 实现
- 在线文档 CRDT、音视频通话、多租户 SaaS
- 微服务拆分与独立注册中心

## Approach

采用 **单体部署 + Maven 多模块** 架构：一个 `relayflow-server` 启动入口，业务按域拆分为独立 Maven 模块，每个域使用 `*-api`（契约）与 `*-biz`（实现）双模块。V1 不引入分布式服务治理组件，自部署用户通过 Docker Compose 一键启动 PostgreSQL、Redis、MinIO 与应用。

## Affected areas

| 区域 | 影响 |
|------|------|
| 仓库根目录 | Maven 父工程结构（后续 scaffold） |
| `web/` | 前端子项目位置不变 |
| `deploy/` | Compose 编排约定 |
| `openspec/specs/` | 合并本变更的域规格 |
| `README.md` / `AGENTS.md` | 同步人类与 Agent 文档 |

## Rollback

本变更为纯文档与规格，无运行时代码。若架构决策需调整，通过新的 OpenSpec change 修改规格并 archive 合并。
