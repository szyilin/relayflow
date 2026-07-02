# RelayFlow — AI 编码说明

> **人类读者请读 [`README.md`](README.md)**。本文档仅面向 Cursor 等 AI 编码代理，作为实现时的入口导引。

自部署企业协作平台。**仓库根目录 = Java Maven 父工程；前端代码仅位于 `web/`。**

## 常用命令

```bash
# 后端（在仓库根目录执行）
./mvnw -pl relayflow-server -am spring-boot:run
./mvnw test
./mvnw verify

# 前端
cd web && pnpm install && pnpm dev

# 基础设施
docker compose -f deploy/compose.yml up -d

# OpenSpec
openspec list
openspec validate <change-name> --strict
openspec archive <change-name>
```

## 文档地图

| 读什么 | 路径 | 何时读 |
|--------|------|--------|
| 产品简介、快速开始 | [README.md](README.md) | 了解项目 |
| Git、Commit、版本号 | [CONTRIBUTING.md](CONTRIBUTING.md) | 提交代码前 |
| 后端架构（V1 单体 / Phase 2 微服务） | [docs/dev/architecture.md](docs/dev/architecture.md) | **写模块依赖、跨域调用前** |
| API 响应、错误码、分页 | [docs/dev/api.md](docs/dev/api.md) | 写 Controller / 前端 API 层 |
| 数据库、Flyway、公共字段 | [docs/dev/database.md](docs/dev/database.md) | 写迁移、DO、Mapper |
| 分层命名、JWT、前端结构 | [docs/dev/code-style.md](docs/dev/code-style.md) | 写业务代码 |
| Git / IDEA | [docs/dev/git-and-idea.md](docs/dev/git-and-idea.md) | 首次用 IDEA 打开、确认勿提交项 |
| 实现工作流（脚手架、小步 change） | [.cursor/rules/implementation-workflow.mdc](.cursor/rules/implementation-workflow.mdc) | **实现任何代码前** |
| 前端 UI（Nuxt UI v4） | [.cursor/rules/frontend-nuxt-ui.mdc](.cursor/rules/frontend-nuxt-ui.mdc) | 写 `web/` |
| 系统行为规格 | [openspec/specs/](openspec/specs/) | 实现前对齐需求 |
| 当前变更任务 | [openspec/changes/](openspec/changes/) | 本次工作范围 |
| V1 架构设计（参考） | [openspec/changes/archive/2026-06-30-bootstrap-v1-foundation/design.md](openspec/changes/archive/2026-06-30-bootstrap-v1-foundation/design.md) | 查模块/栈细节 |
| 架构演进（Phase 2 微服务） | [openspec/changes/distributed-backend-v1/design.md](openspec/changes/distributed-backend-v1/design.md) | 查拆分目标态 |

## 仓库结构（概要）

```text
relayflow/
├── pom.xml                    # Maven 父工程
├── relayflow-dependencies/    # BOM
├── relayflow-framework/       # Starter
├── relayflow-module-*/        # 业务域（*-api + *-biz）
├── relayflow-server/          # V1 唯一运行时入口（加载各 *-biz）
├── web/                       # Vue 3 + Nuxt UI
├── deploy/                    # Docker Compose
├── docs/dev/                  # 开发约定（API、DB、代码风格）
├── openspec/                  # 规格与变更
├── CONTRIBUTING.md            # Git 与协作
└── AGENTS.md                  # 本文件
```

模块依赖：`server → *-biz`；`*-biz → 其他 *-api`；**禁止** `*-biz → *-biz`。跨域只走 `*-api`（见 [architecture.md](docs/dev/architecture.md)）。

**V1 部署**：仅 `relayflow-server`。**Phase 2**（以后）：Gateway + Nacos + `*-server`，业务仍在 `*-biz`，不重写。

## 实现工作流（摘要）

当前阶段：**规格已定，代码骨架未搭建**。禁止一次性生成整套 V1。

1. 读 `openspec/changes/<active-change>/tasks.md` — 本次唯一范围
2. 读 `.cursor/rules/implementation-workflow.mdc` — 脚手架与验证门禁
3. 实现后运行验证（`mvn compile` / `pnpm build` / `openspec validate` 等）
4. 未经 change 文件夹，不得直接改 `openspec/specs/`

推荐顺序：`scaffold-maven-parent` → `scaffold-framework` → `scaffold-server` → `scaffold-deploy-compose` → `scaffold-web-nuxt-ui` → `tenant-ready-foundation` → 各业务域 change。

Phase 2 微服务脚手架（Gateway、Nacos、`*-server`）**不在 V1 脚手架阶段实现**；见 `distributed-backend-v1`。

## OpenSpec（摘要）

1. `/opsx:propose <name>` → 审阅 → `/opsx:apply` → `/opsx:archive`
2. 行为真源：`openspec/specs/<domain>/spec.md`
3. 任务真源：`openspec/changes/<change>/tasks.md`

## 硬约束（速查）

| 项 | 规则 |
|----|------|
| 包名 | `com.relayflow` |
| 管理端 API | `/admin-api/{module}/` |
| 用户端 API | `/app-api/{module}/` |
| 管理端前端路由 | 必须以 `/admin` 开头 |
| WebSocket | `/infra/ws` |
| 表前缀 | `sys_`、`infra_`、`im_`、`bpm_` |
| API 成功码 | `code = 0`，HTTP 200 |
| 主键 | 雪花 ID |
| Flyway | `V{major}.{minor}.{patch}.{seq}__{desc}.sql` |
| Commit | 中文 + Conventional Commits |
| 跨域调用 | 仅 `*-api`；禁止 `*-biz → *-biz` |
| 运行时 V1 | 仅 `relayflow-server`（无 Gateway/Nacos） |
| 前端 | 仅 `web/`，Nuxt UI v4，禁止 React |

## 边界

- **始终**：遵循 `openspec/specs/` 与 `docs/dev/` 约定
- **先询问**：新增 Maven/npm 依赖、Flyway 迁移、删除文件、扩大 tasks 范围
- **禁止**：无验证声称完成、提交密钥、未经用户要求 push
