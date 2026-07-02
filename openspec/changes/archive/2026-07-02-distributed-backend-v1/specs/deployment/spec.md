## ADDED Requirements

### Requirement: 微服务就绪的模块化单体（V1 运行时）

V1 SHALL 以单一 `relayflow-server` 进程交付，并加载 `relayflow-module-system-biz`、`relayflow-module-infra-biz`、`relayflow-module-im-biz`；各域 MUST 保持独立的 `*-api` 与 `*-biz` Maven 模块。

#### Scenario: 单 JAR 启动

- **WHEN** 运维运行 `relayflow-server` JAR
- **THEN** system、infra、im 在同一进程中加载
- **AND** 对外单一 HTTP 端口提供 `/admin-api`、`/app-api` 与 `/infra/ws`

#### Scenario: Maven 模块边界

- **WHEN** 查看仓库 Maven 结构
- **THEN** 存在 system、infra、im 各 `*-api` 与 `*-biz` 子模块
- **AND** `relayflow-server` 仅依赖各 `*-biz`，不直接依赖其他模块实现类

### Requirement: 跨域调用仅经 API 契约

跨业务域调用 MUST 仅通过 `*-api` 模块暴露的接口；`*-biz` 模块 MUST NOT 依赖其他域的 `*-biz` 模块。

#### Scenario: IM 查询用户

- **WHEN** im-biz 需要用户展示信息
- **THEN** 通过 system-api 中的接口（如 `AdminUserApi`）获取
- **AND** im-biz 不依赖 system-biz 的 Service 或 Mapper

#### Scenario: 禁止 biz 互依赖

- **WHEN** 新增 Maven 依赖
- **THEN** `relayflow-module-im-biz` 的 pom 不包含 `relayflow-module-system-biz`
- **AND** 可包含 `relayflow-module-system-api`

### Requirement: V1 逻辑数据分域

V1 SHALL 使用单一 PostgreSQL 数据库；各域表 MUST 使用约定前缀（`sys_`、`infra_`、`im_`）实现逻辑隔离；各域 Mapper MUST NOT 访问其他域前缀的表。

#### Scenario: 表前缀

- **WHEN** system 域创建用户表
- **THEN** 表名为 `sys_user` 等 `sys_` 前缀
- **AND** im 域 Mapper 的 SQL 不出现 `sys_` 表名

### Requirement: 分布式部署为演进目标（Phase 2，非 V1 阻塞项）

系统架构 SHALL 预留演进至 Gateway + 多 `*-server` + 分库的能力；V1 实现 MUST NOT 破坏 *-api 契约与表前缀分域，以便 Phase 2 仅更换部署与 Feign 配置。

#### Scenario: API 路径与未来网关一致

- **WHEN** V1 暴露 REST 端点
- **THEN** 路径使用 `/admin-api/{module}/` 与 `/app-api/{module}/`
- **AND** Phase 2 可由 Gateway 按相同前缀路由至对应 `*-server` 而无需改客户端 URL
