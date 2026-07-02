# 设计：RelayFlow V1 后端技术栈与代码组织

## 1. 版本定位

| 维度 | V1 决策 |
|------|---------|
| 部署形态 | 单体：一个 `relayflow-server` 可执行 JAR |
| 目标用户 | 50–2000 人组织，自部署 / 内网 / 离线安装 |
| V1 核心能力 | 组织权限、基础设施、即时通讯（IM） |
| V1.1 扩展 | 嵌入式工作流引擎（审批流） |
| V2 扩展 | 在线文档协同、全文搜索、LDAP/OIDC |

## 2. 技术栈

### 2.1 基础框架

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 21 LTS |
| 应用框架 | Spring Boot | 3.4.x |
| 构建 | Maven 多模块 | 3.9+ |
| 依赖管理 | `relayflow-dependencies`（BOM） | — |
| 基础包名 | `com.relayflow` | — |

### 2.2 数据与中间件

| 类别 | 选型 | 说明 |
|------|------|------|
| 主数据库 | PostgreSQL | 16+ |
| ORM | MyBatis-Plus | 分页、代码生成可选 |
| 数据库迁移 | Flyway | `relayflow-server/src/main/resources/db/migration/` |
| 缓存 | Redis | 7.x；Session、在线状态、WS 广播 |
| 对象存储 | MinIO | 附件、头像；S3 兼容 API |
| 连接池 | HikariCP | Spring Boot 默认 |

### 2.3 安全与 API

| 类别 | 选型 |
|------|------|
| 认证授权 | Spring Security 6 + JWT |
| API 文档 | springdoc-openapi |
| 参数校验 | Jakarta Validation |
| DTO 转换 | MapStruct |
| 工具 | Lombok |

### 2.4 即时通讯

| 类别 | V1 选型 |
|------|---------|
| 长连接 | Spring WebSocket |
| 多实例消息广播 | Redis Pub/Sub（配置项 `sender-type: local \| redis`） |
| 消息格式 | JSON envelope |
| 音视频 | V1 不做 |

### 2.5 工作流

| 阶段 | 方案 |
|------|------|
| V1.0 | 不做通用工作流引擎 |
| V1.1 | 引入 `relayflow-module-bpm`，嵌入式 BPMN 工作流引擎 |

### 2.6 测试与运维

| 类别 | 选型 |
|------|------|
| 单元测试 | JUnit 5 + Mockito |
| 集成测试 | Testcontainers（PostgreSQL、Redis） |
| 日志 | Logback |
| 本地编排 | Docker Compose（`deploy/compose.yml`） |
| 配置 | `application.yml` + 环境变量；不使用外部注册中心 |

### 2.7 V1 明确不引入

- 微服务框架与独立 API 网关进程
- 消息队列（Kafka / RocketMQ 等）
- 多租户 SaaS 隔离
- 自研 Netty IM 协议栈（WebSocket 足够）

## 3. 仓库目录结构

```text
relayflow/                                    # Git 根 = Java Maven 父工程
├── pom.xml
├── mvnw / mvnw.cmd
│
├── relayflow-dependencies/                   # BOM：统一依赖版本
├── relayflow-framework/                        # 框架层
│   ├── relayflow-common/
│   ├── relayflow-spring-boot-starter-web/
│   ├── relayflow-spring-boot-starter-security/
│   ├── relayflow-spring-boot-starter-mybatis/
│   ├── relayflow-spring-boot-starter-redis/
│   ├── relayflow-spring-boot-starter-websocket/
│   └── relayflow-spring-boot-starter-oss/
│
├── relayflow-module-system/                  # 组织、用户、权限
│   ├── relayflow-module-system-api/
│   └── relayflow-module-system-biz/
│
├── relayflow-module-infra/                   # 文件、配置、日志、WS 基础
│   ├── relayflow-module-infra-api/
│   └── relayflow-module-infra-biz/
│
├── relayflow-module-im/                      # 即时通讯
│   ├── relayflow-module-im-api/
│   └── relayflow-module-im-biz/
│
├── relayflow-module-bpm/                     # V1.1 启用
│   ├── relayflow-module-bpm-api/
│   └── relayflow-module-bpm-biz/
│
├── relayflow-server/                         # 唯一启动入口
│   └── src/main/
│       ├── java/com/relayflow/server/RelayflowServerApplication.java
│       └── resources/application.yml
│
├── web/                                      # 前端（Vue 3 + TypeScript + Vite + Nuxt UI v4）
├── db/                                       # 初始化 SQL、种子数据
├── deploy/                                   # Docker Compose
├── docs/user/
├── openspec/
├── AGENTS.md
└── README.md
```

## 4. Maven 模块依赖规则

```text
relayflow-server
  └── 依赖各 *-biz 模块（system-biz, infra-biz, im-biz）

*-biz 模块
  └── 可依赖其他模块的 *-api
  └── 禁止 *-biz 直接依赖其他模块的 *-biz 或内部实现类

*-api 模块
  └── 仅含接口、DTO、枚举、错误码；无 Spring Controller
```

## 5. api / biz 双模块约定

| 子模块 | 职责 | 典型内容 |
|--------|------|----------|
| `*-api` | 跨模块契约 | `XxxApi` 接口、`ReqDTO`/`RespDTO`、枚举、`ErrorCodeConstants` |
| `*-biz` | 域内实现 | `XxxApiImpl`、Controller、Service、Mapper、DO、Convert |

## 6. biz 模块内部包结构

以 `relayflow-module-im-biz` 为例：

```text
com.relayflow.module.im/
├── api/                 # ApiImpl
├── controller/
│   ├── admin/           # 管理端 REST
│   └── app/             # 用户端 REST
├── service/
├── dal/
│   ├── dataobject/      # DO
│   ├── mysql/           # Mapper
│   └── redis/
├── convert/             # MapStruct
└── framework/           # 模块内配置（可选）
```

## 7. 数据库表前缀

| 前缀 | 模块 | 说明 |
|------|------|------|
| `sys_` | system | 用户、角色、部门、菜单 |
| `infra_` | infra | 文件、配置、操作日志 |
| `im_` | im | 会话、消息、群组、频道 |
| `bpm_` | bpm | 工作流扩展表（V1.1） |

## 8. API 与 WebSocket 约定

| 类型 | 路径前缀 | 示例 |
|------|----------|------|
| 管理端 REST | `/admin-api/{module}/` | `/admin-api/system/auth/login` |
| 用户端 REST | `/app-api/{module}/` | `/app-api/im/conversation/list` |
| WebSocket | `/infra/ws` | 长连接入口 |

WebSocket 配置：

```yaml
relayflow:
  websocket:
    enable: true
    path: /infra/ws
    sender-type: local   # 单机 local；多实例 redis
```

## 9. V1 模块启用策略

根 `pom.xml` 默认启用：

- `relayflow-module-system`
- `relayflow-module-infra`
- `relayflow-module-im`

默认注释（V1.1 再开）：

- `relayflow-module-bpm`

`relayflow-server/pom.xml` 仅引入已启用模块的 `*-biz` 依赖。

## 10. V1 功能矩阵

| 能力 | 模块 | V1 |
|------|------|-----|
| 登录 / JWT | system-biz | ✅ |
| 用户 / 角色 / 部门（RBAC） | system-biz | ✅ |
| 多租户 | — | ❌ |
| 文件上传 | infra-biz | ✅ |
| 操作日志 | infra-biz | ✅ 基础 |
| WebSocket 基础设施 | infra-biz | ✅ |
| 单聊 / 群聊 | im-biz | ✅ |
| 频道（广播） | im-biz | ✅ 简化 |
| 好友 / 音视频 | im-biz | ❌ 二期 |
| 审批工作流 | bpm-biz | ❌ V1.1 |
| 在线文档 | — | ❌ V2 |

## 11. 实施顺序

1. `relayflow-dependencies` + `relayflow-framework`（common, web, security, mybatis, redis）
2. `relayflow-module-system`（api + biz）
3. `relayflow-module-infra`（api + biz）
4. `relayflow-module-im`（api + biz）
5. `relayflow-server` + Flyway + `deploy/compose.yml`
6. `web/` 前端脚手架

## 12. 环境变量（自部署）

| 变量 | 说明 | 默认 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC | `jdbc:postgresql://localhost:5432/relayflow` |
| `SPRING_REDIS_HOST` | Redis 主机 | `localhost` |
| `RELAYFLOW_MINIO_ENDPOINT` | MinIO 地址 | `http://localhost:9000` |
| `RELAYFLOW_JWT_SECRET` | JWT 签名密钥 | 安装时必须覆盖 |
