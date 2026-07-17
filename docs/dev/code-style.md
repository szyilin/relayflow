# 代码与分层约定

Java 后端与 Vue 前端的命名、分层、认证与日志习惯。

## Java 后端

### 基础

| 项 | 值 |
|----|-----|
| 包名 | `com.relayflow` |
| Java | 21 |
| Spring Boot | 3.4.x |
| 注入 | 构造器注入 + `@RequiredArgsConstructor`（见下节） |
| 异常 | 统一 `@ControllerAdvice`；禁止空 `catch` |
| DTO 转换 | MapStruct |
| 参数校验 | Jakarta Validation |
| 样板代码 | **Lombok**（见下节）；禁止手写 getter/setter |

### 模块依赖与跨域

- `relayflow-server` 仅依赖各 `*-biz`
- `*-biz` 可依赖其他 `*-api`，**不得**依赖其他 `*-biz`
- `*-api` 仅含接口、DTO、枚举、错误码；**不含** Controller

**微服务就绪边界**（详见 [`architecture.md`](architecture.md)）：

| 规则 | V1 | Phase 2 |
|------|-----|---------|
| 跨域同步 | 仅 `*-api`（本地 `XxxApiImpl`） | 同接口，换 OpenFeign |
| 跨域异步 | 领域消息（当前 Redis Streams，目标独立 MQ） | 同契约，换 MQ 传输 |
| 数据访问 | 本域表前缀；禁止跨域 Mapper | 分库，规则不变 |
| 运行时 | 仅 `relayflow-server` | Gateway + `*-server` |

- 跨模块只传 DTO，不传 VO；Controller **不返回 DO**
- `admin` 与 `app` 的 VO 分离；Service 层可共用
- **何时同步 / 何时异步**：必须遵循 [`cross-domain-messaging.md`](cross-domain-messaging.md) 评判标准；不得凭感觉「顺便异步」

### 领域消息（Java 落点）

- 事件类型名、`payload` DTO 放在**产出域** `*-api`（与同步跨域 DTO 同等地位）
- 业务只注入框架 `DomainEventPublisher`（名称以实现为准），**禁止**在 `*-biz` 中直接操作 Redis Stream / MQ 客户端
- 消费方在本域注册 Listener；处理须**幂等**（按 `eventId` 或业务唯一键）
- **禁止**用 Spring `ApplicationEvent` 作为跨域长期契约（进程内演示可以，跨模块正式集成须走领域消息抽象，便于换 MQ）
- WebSocket 实时推送继续走现有 infra 通道，**不要**与领域消息总线混用

### biz 包结构

```text
com.relayflow.module.{domain}/
├── api/                 # XxxApiImpl
├── controller/
│   ├── admin/
│   └── app/
├── service/
├── dal/
│   ├── dataobject/      # *DO.java（Git；codegen 参照合并，禁止从零手写字段）
│   ├── mapper/          # *Mapper.java（基础）+ *ExtMapper.java（自定义 SQL）
│   └── redis/
├── enums/               # 手写业务枚举
├── convert/             # MapStruct
└── framework/           # 模块内配置（可选）

src/main/resources/mapper/
├── *Mapper.xml          # 基础 XML（Git；跟表结构，codegen 参照合并）
└── *ExtMapper.xml       # 自定义 SQL（手写；codegen diff 不碰）
```

DO / 基础 Mapper / 基础 XML 由 [codegen.md](codegen.md) CLI 生成到临时目录，**diff 后增量合并进 `src/` 并提交 Git**。自定义 SQL 只放 ExtMapper（或等价 PublicMapper），与生成的 `*Mapper.xml` **分文件**。

### Lombok

数据载体类 **必须** 使用 Lombok，**禁止** 手写 getter/setter（枚举、接口实现中的匿名类等除外）。

| 类型 | 注解 | 示例 |
|------|------|------|
| DO / DTO / VO / ReqVO / RespVO | `@Data` | `UserRespVO`、`UserCreateReqDTO` |
| 抽象 DO 基类 | `@Getter` `@Setter` | `BaseDO`、`TenantBaseDO` |
| `@ConfigurationProperties` | `@Data` | `JwtProperties`、`TenantProperties` |
| 仅构造注入 + 只读字段（非 Spring Bean） | `@Getter` `@RequiredArgsConstructor` | `LoginUser` |
| Spring Bean 依赖注入 | `@RequiredArgsConstructor` | `AuthController`、`AuthServiceImpl` |
| 异常（含 `code` 等 final 字段） | `@Getter` | `ServiceException` |
| 枚举字段 | `@Getter` | `ErrorCodeConstants` |

约定：

- 依赖：`org.projectlombok:lombok`，`scope` 为 `provided`（版本由 BOM / Spring Boot 管理）
- 生成 DO 由 codegen CLI 输出 Lombok 注解（见 [codegen.md](codegen.md)）；手写 POJO 同样遵循上表
- MapStruct `Convert` 与 Lombok 共存；编译期需启用 Lombok 注解处理
- **禁止** 为普通数据类手写 `getXxx` / `setXxx`；IDE 请安装 Lombok 插件（见 [git-and-idea.md](git-and-idea.md)）

### 构造器注入

Spring Bean（`Controller`、`Service`、`ApiImpl`、`Filter`、Starter 内组件等）**必须**使用构造器注入：

- 依赖声明为 `private final`
- 类上添加 Lombok `@RequiredArgsConstructor`，**禁止**手写仅做字段赋值的构造器
- **禁止**字段注入（`@Autowired` 写在字段上）

`@RequiredArgsConstructor` 只为「未在声明处初始化的 `final` 实例字段」生成构造器参数，因此与下列字段共存无冲突：

| 字段类型 | 示例 | 是否进入构造器 |
|----------|------|----------------|
| 常量 | `private static final String ADMIN = "admin"` | 否 |
| 声明处已初始化 | `private final DateTimeFormatter FMT = ISO_OFFSET_DATE_TIME` | 否 |
| 可变状态 | `private volatile boolean ready` | 否 |
| Spring 依赖 | `private final UserService userService` | 是 |

例外：构造器内除赋值外还有**派生逻辑**（如由配置计算 `SecretKey`）时，保留手写构造器（例：`JwtTokenService`）。

依赖过多（如 `final` 注入字段 ≥ 4）时，优先**拆分 Service / 提取 Facade**，而不是改用字段注入。

### 对象命名

| 类型 | 命名 | 位置 | 用途 |
|------|------|------|------|
| DO | `UserDO` | biz / dal / dataobject | 数据库实体（codegen 合入 `src/`，进 Git） |
| Mapper | `UserMapper` | biz / dal / mapper | 基础 Mapper（生成合入） |
| Mapper XML | `UserMapper.xml` | biz / resources / mapper | 基础 XML（生成合入） |
| ExtMapper | `UserExtMapper` | biz / dal / mapper | 自定义 SQL 接口（手写） |
| ExtMapper XML | `UserExtMapper.xml` | biz / resources / mapper | 自定义 SQL（手写） |
| ReqVO | `UserCreateReqVO` | biz / controller | Controller 入参 |
| RespVO | `UserRespVO` | biz / controller | Controller 出参 |
| ReqDTO / RespDTO | `UserRespDTO` | api | 跨模块契约 |
| Convert | `UserConvert` | biz / convert | 对象转换 |

### 对象命名规则

### JWT

| 项 | V1 建议 |
|----|---------|
| 密码存储 | BCrypt（cost 10–12） |
| Access Token | 管理端约 2h；用户端约 7d（按端区分） |
| Claims | `sub`（userId）、`tenant_id`、`user_type`（admin/app）、`exp` |
| Refresh Token | V1 可选；若引入，放 Redis 白名单或 HttpOnly Cookie |
| 登出 | Redis JWT 黑名单或短 TTL |

管理端（`/admin-api`）与用户端（`/app-api`）通过 `user_type` 区分，Security 配置分路径校验。

### RBAC（仅管理面）

- **适用范围**：`/admin/**`、`/admin-api/**`；**不**用于限制 `/app/**` 产品面准入或主导航
- 权限字符串：`{module}:{resource}:{action}`，如 `system:user:query`
- V1 粒度：菜单 + 按钮级 API 权限
- 内置超级管理员角色可拥有 `*:*:*` 或等价 bypass（仅默认租户）
- 管理身份判定见 [`product-permission-model.md`](product-permission-model.md) §2.2

### 登录安全

- 连续失败 N 次（如 5 次）临时锁定（如 15 分钟）
- 错误响应使用统一错误码，不泄露「用户是否存在」

## 日志与追踪

| 项 | 规则 |
|----|------|
| traceId | Filter 生成，日志 MDC 与响应头 `X-Trace-Id` |
| 级别 | 生产默认 INFO；SQL DEBUG 仅 local/dev |
| 敏感信息 | 密码、token、证件号打码 |
| 操作审计 | 管理端写操作记录至 `infra_operate_log` |

V1 不强制 OpenTelemetry / Prometheus；V1.1 可按需引入。

## 配置

| 前缀 | 用途 |
|------|------|
| `RELAYFLOW_*` | 业务自定义配置 |
| `SPRING_*` | Spring Boot 标准配置 |

Profile：`local` / `dev` / `prod`。JWT secret、MinIO 密钥等 **禁止** 写入代码默认值，部署时必须覆盖。

## 前端（`web/`）

技术栈：Vue 3、TypeScript（严格模式）、Vite、**Nuxt UI v4**、Pinia、Vue Router。

**业务功能**按 [vertical-slice-workflow.md](vertical-slice-workflow.md) 与后端同批交付；有 UI 的 change 必须包含 `web/` tasks 与 `pnpm build` + `pnpm typecheck` 验证。

### 目录

```text
web/src/
├── api/
│   ├── admin/         # 封装 /admin-api/*
│   └── app/           # 封装 /app-api/*
├── stores/
├── pages/             # 文件路由（管理端须在 pages/admin/，URL /admin/*）
├── layouts/
├── components/
├── composables/
├── utils/
└── types/
```

> 历史文档中的 `views/admin/` 已统一为 **`pages/admin/`**（与 dashboard-vue 模板一致）。

### 路由

与后端 **管理面 / 产品面** 对齐；**产品权限语义**见 [`product-permission-model.md`](product-permission-model.md)（必读）。

| 类型 | path 规则 | 示例 | 对应 API |
|------|-----------|------|----------|
| **管理端** | **必须以 `/admin` 开头** | `/admin/system/user` | `/admin-api/**` |
| **用户端** | **必须以 `/app` 开头** | `/app/login`、`/app/messages` | `/app-api/**` |
| **统一登录** | **`/app/login` 为唯一产品登录页** | 登录成功 → `/app/messages` | `POST /admin-api/system/auth/login` |

认证约定：

- **同一账号、同一 JWT**；**不** 使用不同登录入口
- **未登录**：访问 `/app/**`（除 login）、`/admin/**` **必须** redirect `/app/login`；**禁止**未登录渲染 app/admin 壳层
- **产品面** `/app/**`：有效组织成员即可；**不** 用 `sys_permission` 区分路由/主导航
- **管理面** `/admin/**`：须 **管理身份**（绑定且角色含 ≥1 个 `sys_permission`）；进入后菜单/API 再按 RBAC 细粒度控制
- 工作台「管理后台」入口：**目标态**仅管理身份可见（见 `product-permission-model.md` §6 现状 gap）
- `/admin/login` 仅作兼容重定向至 `/app/login`；**禁止** 并列「员工登录 / 管理员登录」

管理端路由约定：

- 所有管理后台页面挂在 `/admin` 下，使用嵌套路由 + `admin` layout
- 页面文件放在 `pages/admin/`，路径与 URL 对应（如 `pages/admin/system/user.vue` → `/admin/system/user`）
- 管理端 API 调用走 `api/admin/`，**禁止**在 admin 页面直接请求 `/app-api`

用户端路由约定：

- 员工工作台页面挂在 `/app` 下，使用 `workspace` layout
- 页面文件放在 `pages/app/`
- 用户端 API 调用走 `api/app/` → `/app-api/**`

### HTTP 与数据层

| 项 | 约定 |
|----|------|
| HTTP 客户端 | **axios**（`web/src/api/request.ts` 统一封装） |
| 响应格式 | `{ code, msg, data }`；`code === 0` 成功 |
| 页面数据 | **Page → Pinia Store → `api/admin/*` 或 `api/app/*`**（类型可从 `api/*` 直接 import；下载等一次性工具函数可在页面调用，业务列表/写操作仍走 store） |
| 临时 Mock | 仅 `-web` 阶段、且只在 **store 内**；**integrate 后删除**。仓库不再保留常驻 `web/src/mocks/` |
| 列表分页 | 有 `total` 的列表 MUST 显式分页或「加载更多」；默认 `pageSize` 建议 20，不超过服务端上限 100；**禁止**静默截断 |
| 租户切换 | `tenantId`/`userId` 变更时，租户范围 store MUST `reset` 并按路由 refetch（见 [workspace-ui-patterns.md](workspace-ui-patterns.md)） |
| 禁止 | 页面直接 `import mocks/`；发明全局 `isApiUnavailable` 自动回退（已废弃） |

公共页（如入口 `/`、404）可不在 `/admin` 或 `/app` 下。

### 规则

- `<script setup lang="ts">`；禁止 `any`
- API 层统一处理 `{ code, msg, data }`；`code === 0` 为成功
- **401**（HTTP 或业务码）→ 清会话并跳转 `/app/login`（由 `request.ts` 统一处理）
- **403**（HTTP 或业务码）→ 跳转 `/app/forbidden`（由 `request.ts` 统一处理；管理身份不足仍用 `/app/no-admin-access`）
- 会话键：`relayflow:access-token` 等（见 `web/src/utils/session-storage.ts`）；同一 JWT 服务工作台与管理端
- Account Dock（`relayflow:account-dock`）V1 可缓存多 JWT；威胁模型与目标态见 [workspace-ui-patterns.md](workspace-ui-patterns.md) § Account Dock
- 超管权限码含 `*:*:*` 时，前端 `hasPermission` 应放行
- 权限码不散落在组件 magic string 中
- UI 以 Nuxt UI 组件为主；禁止 Element Plus 作为主 UI 层
- 单文件建议 ≤ ~500 行；超大页/store 拆 `composables/` + 展示组件
- 禁止 React；前端代码不得放在 `web/` 外
- 含 `web/` 的切片验证：`pnpm build` **且** `pnpm typecheck`（`auto-imports.d.ts` / `components.d.ts` 为本地生成、gitignore；typecheck 会按需生成）

### 管理端 UI（已定调）

管理端页面须遵循 UI 定调文档（B · Clean Enterprise）：

| 文档 | 用途 |
|------|------|
| [admin-ui-workflow.md](admin-ui-workflow.md) | 定调 → 规则 → 接 API 流程 |
| [admin-ui-tokens.md](admin-ui-tokens.md) | 色、字、圆角、主题 |
| [admin-ui-patterns.md](admin-ui-patterns.md) | 壳层、登录、列表、表单等页面模式 |

Cursor 规则：`.cursor/rules/admin-ui-patterns.mdc`（编辑 `web/` 管理端时生效）。

### 员工工作台 UI（已定调）

| 文档 | 用途 |
|------|------|
| [workspace-ui-tokens.md](workspace-ui-tokens.md) | 卡片分层、`--ws-*` token |
| [workspace-ui-patterns.md](workspace-ui-patterns.md) | 壳层、消息/任务页模式、数据层 |

Cursor 规则：`.cursor/rules/workspace-ui-patterns.mdc`（编辑 `/app/*` 时生效）。

### UI 预览

UI 定调与签字验收走本机 **`pnpm dev`** → `http://localhost:5173`（Vite 将 `/admin-api` 代理到后端 `:8080`）。

### 环境变量

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | REST 基址 |
| `VITE_WS_URL` | WebSocket 地址 |

## 测试

| 层级 | 范围 | 位置 |
|------|------|------|
| 单元测试 | Service、Convert、工具 | `*-biz/src/test` |
| 集成测试 | Controller + DB（Testcontainers） | `relayflow-server` 或各 biz |

新功能至少覆盖核心 Service 单测；认证、租户插件等基础设施须有集成测试。

## 安全基线

- 生产 CORS 使用域名白名单
- 文件上传：大小限制 + MIME 白名单
- 依赖漏洞：CI 或定期 `dependency-check` / Dependabot
- 首次安装强制修改默认密码；默认凭据不得硬编码在源码
