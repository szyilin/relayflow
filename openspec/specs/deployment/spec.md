# 部署规格（deployment）

## Purpose

定义 RelayFlow 的部署形态、仓库结构与前端技术栈要求。

## Requirements

### Requirement: 本地基础设施 Compose 编排

`deploy/compose.yml` SHALL 定义 PostgreSQL、Redis、MinIO 服务，供本地开发与自部署使用。

#### Scenario: Compose 语法有效

- **WHEN** 开发者执行 `docker compose -f deploy/compose.yml config`
- **THEN** 配置解析成功且无语法错误
- **AND** 包含 postgres、redis、minio 三个服务定义

### 需求：自托管单节点部署

系统应支持在单台服务器上通过 Docker Compose 部署 PostgreSQL、Redis、MinIO 与一个应用容器。

#### 场景：全新安装

- 给定 一台已安装 Docker 的空 Linux 服务器
- 当 运维执行 `docker compose -f deploy/compose.yml up -d`
- 那么 PostgreSQL、Redis、MinIO 与 RelayFlow 应用均成功启动
- 并且 不依赖外部 SaaS 服务

### 需求：离线（内网）配置

镜像与依赖预加载后，系统必须能在无互联网访问环境下运行。

#### 场景：离线启动

- 给定 所有容器镜像已在本地可用
- 当 应用在有效环境变量下启动
- 那么 各核心服务仅通过配置的主机名连接
- 并且 正常运行无需外网请求

### 需求：仓库根目录 Maven 模块化布局

仓库根目录应为 Maven 父工程；前端代码仅位于 `web/`。

#### 场景：仓库结构校验

- 给定 开发者克隆本仓库
- 当 查看根目录
- 那么 可见 `pom.xml` 与后端 Maven 模块
- 并且 前端资源仅存在于 `web/`

### 需求：单体应用入口

V1 应以名为 `relayflow-server` 的单一 Spring Boot 模块交付。

#### 场景：单 JAR 启动

- 给定 后端已通过 Maven 构建
- 当 运维运行 `relayflow-server` JAR
- 那么 system、infra、im 模块在同一进程中加载
- 并且 单一 HTTP 端口提供全部 REST 与 WebSocket 端点

### 需求：微服务就绪的模块化单体（V1 运行时）

V1 应以单一 `relayflow-server` 进程交付，并加载 `relayflow-module-system-biz`、`relayflow-module-infra-biz`、`relayflow-module-im-biz`；各域须保持独立的 `*-api` 与 `*-biz` Maven 模块。

#### 场景：Maven 模块边界

- 给定 仓库 Maven 结构已搭建
- 当 查看各业务域模块
- 那么 存在 system、infra、im 各 `*-api` 与 `*-biz` 子模块
- 并且 `relayflow-server` 仅依赖各 `*-biz`，不直接依赖其他模块实现类

### 需求：跨域调用仅经 API 契约

跨业务域调用必须仅通过 `*-api` 模块暴露的接口；`*-biz` 模块不得依赖其他域的 `*-biz` 模块。

#### 场景：IM 查询用户

- 给定 im 域需要展示用户信息
- 当 im-biz 获取用户数据
- 那么 通过 system-api 中的接口（如 `AdminUserApi`）获取
- 并且 im-biz 不依赖 system-biz 的 Service 或 Mapper

#### 场景：禁止 biz 互依赖

- 给定 新增 Maven 依赖
- 当 配置 `relayflow-module-im-biz` 的 pom
- 那么 不包含 `relayflow-module-system-biz`
- 并且 可包含 `relayflow-module-system-api`

### 需求：V1 逻辑数据分域

V1 应使用单一 PostgreSQL 数据库；各域表须使用约定前缀（`sys_`、`infra_`、`im_`）实现逻辑隔离；各域 Mapper 不得访问其他域前缀的表。

#### 场景：表前缀

- 给定 system 域创建用户表
- 当 执行 Flyway 迁移
- 那么 表名为 `sys_user` 等 `sys_` 前缀
- 并且 im 域 Mapper 的 SQL 不出现 `sys_` 表名

### 需求：分布式部署为演进目标（Phase 2，非 V1 阻塞项）

系统架构应预留演进至 Gateway + 多 `*-server` + 分库的能力；V1 实现不得破坏 `*-api` 契约与表前缀分域，以便 Phase 2 仅更换部署与 Feign 配置。

#### 场景：API 路径与未来网关一致

- 给定 V1 暴露 REST 端点
- 当 客户端请求管理端或用户端 API
- 那么 路径使用 `/admin-api/{module}/` 与 `/app-api/{module}/`
- 并且 Phase 2 可由 Gateway 按相同前缀路由至对应 `*-server` 而无需改客户端 URL

### 需求：Vue 前端技术栈

`web/` 下前端子工程应使用 Vue 3、TypeScript 与 Vite。

#### 场景：前端工程结构

- 给定 开发者初始化 `web/` 目录
- 当 创建前端工程
- 那么 使用 Vue 3 Composition API
- 并且 启用 TypeScript 严格模式
- 并且 使用 Vite 作为构建工具

### 需求：Nuxt UI 组件库

前端应以 Nuxt UI v4 作为主 UI 组件库。

#### 场景：UI 依赖

- 给定 `web/` 前端工程已初始化
- 当 查看 `web/package.json`
- 那么 `@nuxt/ui` 列为 UI 依赖
- 并且 Element Plus 不是主 UI 库

### 需求：前端隔离

全部前端源码必须位于 `web/`，且不得使用 React 或其他非 Vue 的 SPA 框架。

#### 场景：仓库中不含 React

- 给定 RelayFlow 仓库
- 当 查看 `web/package.json` 中的前端依赖
- 那么 `vue` 为核心依赖之一
- 并且 不含 `react` 依赖

### 需求：租户产品模式配置

系统 SHALL 提供 `relayflow.tenant.enabled` 与 `relayflow.tenant.allow-open-register` 配置项，用于控制多租户 SaaS 产品能力与开放注册，无需改代码即可切换部署模式。

#### 场景：默认单租户模式

- 当 未配置或 `relayflow.tenant.enabled=false`
- 那么 系统以单租户产品模式运行
- 并且 所有 API 与 WebSocket 请求隐式绑定 `relayflow.tenant.default-id` 指定的租户（默认 `1`）
- 并且 不要求客户端传递租户标识 Header
- 并且 不要求企业切换器或开放注册

#### 场景：多租户产品模式

- 当 `relayflow.tenant.enabled=true`
- 那么 已认证请求从 JWT claim `tenant_id` 解析当前租户
- 并且 解析失败时拒绝请求（除白名单公开接口）
- 并且 当 `relayflow.tenant.allow-open-register=true` 时开放 `POST /app-api/system/auth/register`

#### 场景：环境变量文档

- 当 运维通过 Docker Compose 或环境变量部署
- 那么 文档列出 `RELAYFLOW_TENANT_ENABLED` 与 `RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER`（或等价属性名）供 V2 模式配置

### 需求：默认租户环境变量

自部署安装 SHALL 支持通过环境变量覆盖租户模式相关配置。

#### 场景：Docker Compose 部署

- 当 运维通过 `deploy/compose.yml` 启动应用
- 那么 可通过 `RELAYFLOW_TENANT_ENABLED`、`RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER` 与 `RELAYFLOW_TENANT_DEFAULT_ID` 配置租户行为
- 并且 未设置时使用单租户默认值
