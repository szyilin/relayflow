# 代码与分层约定

Java 后端与 Vue 前端的命名、分层、认证与日志习惯。

## Java 后端

### 基础

| 项 | 值 |
|----|-----|
| 包名 | `com.relayflow` |
| Java | 21 |
| Spring Boot | 3.4.x |
| 注入 | 构造器注入 |
| 异常 | 统一 `@ControllerAdvice`；禁止空 `catch` |
| DTO 转换 | MapStruct |
| 参数校验 | Jakarta Validation |

### 模块依赖与跨域

- `relayflow-server` 仅依赖各 `*-biz`
- `*-biz` 可依赖其他 `*-api`，**不得**依赖其他 `*-biz`
- `*-api` 仅含接口、DTO、枚举、错误码；**不含** Controller

**微服务就绪边界**（详见 [`architecture.md`](architecture.md)）：

| 规则 | V1 | Phase 2 |
|------|-----|---------|
| 跨域调用 | 仅 `*-api`（本地 `XxxApiImpl`） | 同接口，换 OpenFeign |
| 数据访问 | 本域表前缀；禁止跨域 Mapper | 分库，规则不变 |
| 运行时 | 仅 `relayflow-server` | Gateway + `*-server` |

- 跨模块只传 DTO，不传 VO；Controller **不返回 DO**
- `admin` 与 `app` 的 VO 分离；Service 层可共用

### biz 包结构

```text
com.relayflow.module.{domain}/
├── api/                 # XxxApiImpl
├── controller/
│   ├── admin/
│   └── app/
├── service/
├── dal/
│   ├── mysql/           # 手写 ExtMapper（*ExtMapper.java）
│   └── redis/
├── enums/               # 手写业务枚举
├── convert/             # MapStruct
└── framework/           # 模块内配置（可选）

target/generated-sources/mybatis/.../dal/   # 生成：*DO、*Mapper（见 codegen.md）
src/main/resources/mapper/                  # 手写：*ExtMapper.xml
```

DO / 基础 Mapper **不得**放在 `src/` 下手写；须按 [codegen.md](codegen.md) 使用 CLI 生成到临时目录，diff 后合并至 `target/generated-sources/mybatis/`。

### 对象命名

| 类型 | 命名 | 位置 | 用途 |
|------|------|------|------|
| DO | `UserDO` | `target/generated-sources/.../dal/dataobject` | 数据库实体（生成） |
| ExtMapper | `UserExtMapper` | biz / dal / mysql | 自定义 SQL 接口（手写） |
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

### RBAC

- 权限字符串：`{module}:{resource}:{action}`，如 `system:user:query`
- V1 粒度：菜单 + 按钮级 API 权限
- 内置超级管理员角色可拥有 `*:*:*` 或等价 bypass（仅默认租户）

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

### 目录

```text
web/src/
├── api/
│   ├── admin/         # 封装 /admin-api/*
│   └── app/           # 封装 /app-api/*
├── stores/
├── views/
│   ├── admin/         # 管理端页面（路由须 /admin/*）
│   └── ...            # 用户端页面（路由命名暂不限）
├── layouts/
│   ├── AdminLayout.vue
│   └── ...
├── components/
├── composables/
├── router/
├── utils/
└── types/
```

### 路由

与后端 **管理面 / 产品面** 对齐（常见协作产品中的「企业管理后台 vs 成员使用端」权限域划分）：

| 类型 | path 规则 | 示例 | 对应 API |
|------|-----------|------|----------|
| **管理端** | **必须以 `/admin` 开头** | `/admin/login`、`/admin/system/user` | `/admin-api/**` |
| **用户端** | 暂不限前缀，语义清晰即可 | `/login`、`/im`、`/chat/:id` | `/app-api/**` |

管理端路由约定：

- 所有管理后台页面挂在 `/admin` 下，使用嵌套路由 + `AdminLayout`
- 页面文件放在 `views/admin/`，路径与 URL 对应（如 `views/admin/system/user/index.vue` → `/admin/system/user`）
- 管理端 API 调用走 `api/admin/`，**禁止**在 admin 页面直接请求 `/app-api`

用户端路由约定（V1）：

- 不强制 `/app` 前缀；后续若做独立用户端壳层，再评估是否统一加前缀
- 用户端 API 调用走 `api/app/` → `/app-api/**`

公共页（如安装向导、404）可不在 `/admin` 下，按实际产品命名。

### 规则

- `<script setup lang="ts">`；禁止 `any`
- API 层统一处理 `{ code, msg, data }`；`code === 0` 为成功
- 401 → 跳转登录；403 → 无权限页
- 权限码不散落在组件 magic string 中
- UI 以 Nuxt UI 组件为主；禁止 Element Plus 作为主 UI 层
- 禁止 React；前端代码不得放在 `web/` 外

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
