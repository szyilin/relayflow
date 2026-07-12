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

# DO/Mapper（独立 CLI fat JAR；按需表名 -> 临时目录，diff 后再合并）
./mvnw -Pcodegen package -DskipTests   # 仅构建 codegen JAR
./scripts/codegen.sh --module system --tables sys_user,sys_dept
```

## 文档地图

| 读什么 | 路径 | 何时读 |
|--------|------|--------|
| 产品简介、快速开始 | [README.md](README.md) | 了解项目 |
| Git、Commit、版本号 | [CONTRIBUTING.md](CONTRIBUTING.md) | 提交代码前；**改版本号 / Flyway 前缀前必读** |
| 后端架构（V1 单体 / Phase 2 微服务） | [docs/dev/architecture.md](docs/dev/architecture.md) | **写模块依赖、跨域调用前** |
| API 响应、错误码、分页 | [docs/dev/api.md](docs/dev/api.md) | 写 Controller / 前端 API 层 |
| 数据库、Flyway、公共字段 | [docs/dev/database.md](docs/dev/database.md) | 写迁移、DO、Mapper |
| **DO/Mapper 代码生成** | [docs/dev/codegen.md](docs/dev/codegen.md) | **写/改 DO、Mapper 前（禁止手写 DO）** |
| 分层命名、JWT、前端结构 | [docs/dev/code-style.md](docs/dev/code-style.md) | 写业务代码 |
| **产品面 / 权限模型** | [product-permission-model.md](docs/dev/product-permission-model.md) | **写 auth、路由、RBAC、工作台/管理端准入前** |
| Git / IDEA | [docs/dev/git-and-idea.md](docs/dev/git-and-idea.md) | 首次用 IDEA 打开、确认勿提交项 |
| **前端优先工作流** | [frontend-first-workflow.md](docs/dev/frontend-first-workflow.md) | **业务切片默认顺序：先 web UI，后 API** |
| **纵向切片工作流** | [vertical-slice-workflow.md](docs/dev/vertical-slice-workflow.md) | **实现任何用户可见功能前** |
| **并行 Lane（前后端拆分）** | [parallel-lane-workflow.md](docs/dev/parallel-lane-workflow.md) | **`-api`/`-web` 双 Agent 并行时必读** |
| **API 对接看板** | [api-integration-board.md](docs/dev/api-integration-board.md) | **`-web` lane / 前端接 API 前必读** |
| **管理端 UI 定调工作流** | [admin-ui-workflow.md](docs/dev/admin-ui-workflow.md) | **管理端 Mock 原型、规则沉淀、接 API 前** |
| **员工工作台 UI** | [workspace-ui-patterns.md](docs/dev/workspace-ui-patterns.md) | **编辑 `/app/*`、对接 app-api 前** |
| 实现工作流（脚手架、小步 change） | [.cursor/rules/implementation-workflow.mdc](.cursor/rules/implementation-workflow.mdc) | **实现任何代码前** |
| 纵向切片（Cursor 规则摘要） | [.cursor/rules/vertical-slice-workflow.mdc](.cursor/rules/vertical-slice-workflow.mdc) | **业务功能必读** |
| 并行 Lane 前端 | [.cursor/rules/parallel-lane-frontend.mdc](.cursor/rules/parallel-lane-frontend.mdc) | **`*-web` change / 前端 AI** |
| 并行 Lane 后端 | [.cursor/rules/parallel-lane-backend.mdc](.cursor/rules/parallel-lane-backend.mdc) | **`*-api` change / 后端 AI** |
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

脚手架已就绪。**用户可感知功能**按 **纵向切片** 推进，默认 **前端优先**（见 [`frontend-first-workflow.md`](docs/dev/frontend-first-workflow.md)）：先 `-web` UI + Mock + contract 草案，再 `-api`，最后 `-integrate` 去 Mock 联调。

1. 读 `openspec/changes/<active-change>/tasks.md` — 本次唯一范围
2. 读 [`docs/dev/frontend-first-workflow.md`](docs/dev/frontend-first-workflow.md) 与 [`vertical-slice-workflow.md`](docs/dev/vertical-slice-workflow.md)
3. 读 `.cursor/rules/implementation-workflow.mdc` 与 `vertical-slice-workflow.mdc`
4. 实现后验证：`mvn compile`；含 `web/` 时 **`pnpm build`**；有 UI 时写明浏览器路径
5. 未经 change 文件夹，不得直接改 `openspec/specs/`

**认证入口**：唯一登录页 `/app/login`（已接 JWT）；登录后进工作台；管理后台 `/admin` 须管理身份（见 [product-permission-model.md](docs/dev/product-permission-model.md)）。

**下一优先**：`infra-storage-v1`（已完成）→ `tenant-platform-slice`（租户 Redis/WS 前缀，补齐 `tenant-ready-foundation` §5）或按域继续 `im-*` 纵向切片。

脚手架顺序（已完成）：`scaffold-*` → `tenant-ready-foundation` / `system-schema-v1` → 此后走纵向切片。

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
| 产品版本 | **开发期 `0.x`**（当前 `0.1.0-SNAPSHOT`）；**禁止** AI 自行升到 `1.x`；minor/patch 可按进度调整；见 [CONTRIBUTING.md](CONTRIBUTING.md) § 版本号 |
| Commit | 中文 + Conventional Commits |
| 跨域调用 | 仅 `*-api`；禁止 `*-biz → *-biz` |
| 运行时 V1 | 仅 `relayflow-server`（无 Gateway/Nacos） |
| 前端 | 仅 `web/`，Nuxt UI v4，禁止 React |
| DO / 基础 Mapper | Flyway 真源 + `./scripts/codegen.sh --tables …` → 临时目录 → diff 合并至 `target/generated-sources/` |
| Lombok | DO/DTO/VO/Properties 等数据类用 `@Data` 等注解；**禁止**手写 getter/setter（见 `docs/dev/code-style.md` § Lombok） |
| 构造器注入 | Spring Bean 用 `private final` + `@RequiredArgsConstructor`；禁止字段注入（见 `docs/dev/code-style.md` § 构造器注入） |
| 业务功能交付 | **纵向切片 · 前端优先**：`-web` → `-api` → `-integrate`；见 [frontend-first-workflow.md](docs/dev/frontend-first-workflow.md) |
| 统一登录 | `/app/login`；同一 JWT；产品面有效成员可用；管理面须管理身份 + RBAC |

## 边界

- **始终**：遵循 `openspec/specs/` 与 `docs/dev/` 约定
- **先询问**：新增 Maven/npm 依赖、Flyway 迁移、删除文件、扩大 tasks 范围
- **禁止**：无验证声称完成、提交密钥、未经用户要求 push
