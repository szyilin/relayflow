# RelayFlow 架构概览（V1）

本文档面向使用者与管理员，说明 V1 后端架构。开发细节见 [`docs/dev/architecture.md`](../dev/architecture.md)，行为规格见 `openspec/specs/`。

## 部署形态

V1 采用 **微服务就绪的模块化单体**：运行时只有一个 `relayflow-server` 进程，内部按域加载组织权限（system）、基础设施（infra）与即时通讯（im）。代码已按未来拆分写好边界（各域 `*-api` / `*-biz`、跨域只走 API、表前缀分域），日后 IM 与运维成熟时可演进为 Gateway + 多服务，**无需重写业务代码**。

通过 Docker Compose 可一键启动 PostgreSQL、Redis、MinIO 与应用。

## 前端

用户界面位于 `web/` 子项目，采用 **Vue 3 + TypeScript + Vite + Nuxt UI**，通过 REST 与 WebSocket 与后端通信。管理端页面路由以 `/admin` 开头。

## 核心模块

| 模块 | 说明 |
|------|------|
| system | 用户登录、角色权限、部门组织 |
| infra | 文件存储、操作日志、WebSocket 长连接 |
| im | 单聊、群聊、频道广播 |

## API 约定

- 管理端接口：`/admin-api/...`
- 用户端接口：`/app-api/...`
- 实时消息：`/infra/ws`（WebSocket）

## 数据存储

- 业务数据：PostgreSQL（V1 单库，表前缀 `sys_` / `infra_` / `im_` 分域）
- 缓存与会话：Redis
- 附件与文件：MinIO

## 环境要求

- Java 21+
- Docker（推荐用于本地与生产部署）
- **单机部署参考配置**（可按实际并发与数据量水平扩展）：2 核 CPU、4 GB 内存起（含数据库与缓存）
