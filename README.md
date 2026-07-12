# RelayFlow

自部署企业协作平台 — 即时通讯、文档与轻量工作流。数据留在你的服务器上。

## 产品定位

RelayFlow 是**可自托管（self-hosted）**的企业协作平台：可在自有服务器、私有云或内网环境部署，数据与访问策略由组织自行掌控，并支持离线安装与运维。

当前版本（V1）提供 **组织权限、基础设施与即时通讯**；架构上支持按模块扩展与后续分布式演进，部署规模由基础设施与运维方案决定，产品本身不对组织人数设硬性上限。

## 设计参考

RelayFlow 在**产品形态与交互思路**上参考了 [飞书](https://www.feishu.cn/) 等企业协作类产品，面向**可自托管**场景独立设计与实现。

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

## 开发（日常）

与多数开源项目一致：**Docker 只跑依赖，应用在宿主机跑**（HMR、断点、本机代理均可用）。

```bash
# 1. 基础设施
docker compose -f deploy/compose.yml up -d

# 2. 后端
./mvnw -pl relayflow-server -am spring-boot:run

# 3. 前端（另开终端）
cd web && pnpm install && pnpm dev
```

浏览器打开 **http://localhost:5173/app/login**（Vite 已将 `/admin-api` 代理到 `:8080`）。

默认开发账号：**`19988888888` / `123456`**（Flyway 种子数据；生产环境请修改密码）。

### 租户产品模式（V1 / V2）

| 模式 | 配置 | 行为 |
|------|------|------|
| **V1 自托管（默认）** | `RELAYFLOW_TENANT_ENABLED=false` | 全员落在默认租户 `id=1`；成员通过管理端邀请 + 注册/接受流程加入 |
| **V2 多租户** | `enabled=true` + `allow-open-register=true` | 开放注册建企、一账号多企业、工作台企业切换器 |

- **本地开发**：`application-dev.yml` 默认启用 V2（`spring.profiles.active=dev`）。
- **Docker 部署**：在 `deploy/.env`（参考 [`deploy/.env.example`](deploy/.env.example)）中设置：

```bash
RELAYFLOW_TENANT_ENABLED=true          # V2 多租户
RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER=true
RELAYFLOW_TENANT_DEFAULT_ID=1          # 种子租户，不可删
```

`compose.prod.yml` 会将上述变量传入 `relayflow-server` 容器。详见 [`docs/dev/product-permission-model.md`](docs/dev/product-permission-model.md) §2.4。

## 部署（Docker 后端 · 可选）

机器上无 Java 时，可用 Docker 跑 **基础设施 + relayflow-server**；**前端始终在宿主机** `pnpm dev`：

```bash
# 基础设施（开发/部署均需要）
docker compose -f deploy/compose.yml up -d

# 可选：后端也进容器（会构建 JAR，较慢）
docker compose -f deploy/compose.prod.yml up -d --build server
```

浏览器仍打开 **http://localhost:5173/app/login**（另开终端 `cd web && pnpm dev`）。

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

[MIT License](LICENSE) — 允许免费商用、修改与分发，仅需保留版权声明。
