# RelayFlow

自部署企业协作平台 — 即时通讯、文档与轻量工作流。数据留在你的服务器上。

## 产品定位

RelayFlow 是**可自托管（self-hosted）**的企业协作平台：可在自有服务器、私有云或内网环境部署，数据与访问策略由组织自行掌控，并支持离线安装与运维。

当前版本（V1）提供 **组织权限、基础设施与即时通讯**；架构上支持按模块扩展与后续分布式演进，部署规模由基础设施与运维方案决定，产品本身不对组织人数设硬性上限。

## 仓库结构

```text
relayflow/                          # Git 根 = Java Maven 父工程
├── pom.xml
├── relayflow-dependencies/           # 依赖版本 BOM
├── relayflow-framework/              # 框架 Starter
├── relayflow-module-system/          # 用户、角色、部门
├── relayflow-module-infra/           # 文件、配置、日志、WebSocket
├── relayflow-module-im/              # 即时通讯
├── relayflow-server/                 # V1 唯一运行时入口（加载各 *-biz）
├── web/                              # 前端（Vue 3 + TypeScript + Vite + Nuxt UI）
├── db/                               # 初始化 SQL
├── deploy/                           # Docker Compose
├── openspec/                         # 规格与变更
├── docs/
│   ├── dev/                          # 开发者约定（贡献者阅读）
│   └── user/                         # 用户与运维文档
└── CONTRIBUTING.md                   # 贡献指南
```

> 使用 Cursor 等 AI 编码工具时，见 [`AGENTS.md`](AGENTS.md)（**给 Agent 看的导引，非人类主文档**）。

## V1 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.x |
| 构建 | Maven 多模块 |
| 数据库 | PostgreSQL 16+ |
| ORM | MyBatis-Plus |
| 迁移 | Flyway |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 认证 | Spring Security + JWT |
| 实时通讯 | Spring WebSocket |
| 前端 | Vue 3、TypeScript、Vite、Nuxt UI v4、Pinia、Vue Router |
| 部署 | Docker Compose |

## 模块说明

| 模块 | 职责 | V1 |
|------|------|-----|
| system | 登录、用户、角色、部门 | ✅ |
| infra | 文件、操作日志、WebSocket 基础 | ✅ |
| im | 单聊、群聊、频道 | ✅ |
| bpm | 审批工作流 | V1.1 |

每个业务模块拆分为 `*-api`（跨域契约）与 `*-biz`（实现）。**跨域只走 `*-api`**，禁止 `*-biz` 互依赖；V1 单进程部署，Phase 2 可拆为 Gateway + 多 `*-server`（见 [`docs/dev/architecture.md`](docs/dev/architecture.md)）。

## 快速开始（开发）

### 依赖服务

```bash
docker compose -f deploy/compose.yml up -d
```

### 后端

```bash
./mvnw -pl relayflow-server -am spring-boot:run
```

### 前端

```bash
cd web && pnpm install && pnpm dev
```

### 前端（Docker，Nginx 静态部署）

适合演示 / 预览 Mock 管理端，无需本机安装 Node：

```bash
docker compose -f deploy/compose.yml up -d web
# 浏览器打开 http://localhost:8081/admin/login
```

重新构建镜像（改完 web/ 代码后）：

```bash
docker compose -f deploy/compose.yml up -d --build web
```

> 当前原型阶段为零后端 Mock；接真 API 后需在 `web/nginx.conf` 中启用 `/admin-api/` 反向代理，并将 `relayflow-server` 加入 Compose。

> 后端 Maven 骨架第一步已完成（`scaffold-maven-parent`）；server 与 framework 见 OpenSpec `scaffold-*` changes。

## 文档

| 读者 | 文档 |
|------|------|
| 使用者 / 运维 | [`docs/user/`](docs/user/) |
| 贡献者 | [`CONTRIBUTING.md`](CONTRIBUTING.md)、[`docs/dev/`](docs/dev/)（含 [Git/IDEA](docs/dev/git-and-idea.md)） |
| 架构与需求规格 | [`openspec/specs/`](openspec/specs/)、[V1 架构设计](openspec/changes/archive/2026-06-30-bootstrap-v1-foundation/design.md) |

本项目使用 [OpenSpec](https://openspec.dev/) 管理需求变更；日常开发流程见 [`CONTRIBUTING.md`](CONTRIBUTING.md)。

## 当前进度

- [x] OpenSpec 初始化
- [x] V1 后端架构规格（`bootstrap-v1-foundation`）
- [x] 项目开发约定（`docs/dev/`、`CONTRIBUTING.md`）
- [x] 两阶段架构文档（`docs/dev/architecture.md`、`distributed-backend-v1`）
- [x] Maven 多模块骨架（`scaffold-maven-parent`：根 POM、BOM、三域 api/biz）
- [ ] relayflow-framework / relayflow-server
- [ ] system / infra / im 业务实现
- [ ] 前端 `web/` 脚手架
- [ ] Docker Compose 编排

## 许可证

待定
