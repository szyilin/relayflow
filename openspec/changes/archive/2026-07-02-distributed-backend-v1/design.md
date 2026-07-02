# 设计：架构演进 — V1 微服务就绪单体 + Phase 2 分布式

## 1. 决策摘要

| 维度 | V1（现在开发） | Phase 2（以后拆分） |
|------|----------------|---------------------|
| 部署 | 1× `relayflow-server` | Gateway + 3× `*-server` |
| Maven 域模块 | system / infra / im（api+biz） | 同左，不变 |
| 跨域调用 | `*-api` 本地 ApiImpl | OpenFeign 同接口 |
| 数据库 | 单库 `relayflow`，表前缀分域 | 分库 `relayflow_system` 等 |
| 服务治理 | 无 | Nacos |
| 对外入口 | server :8080 | Gateway :8080 |

**核心原则**：Phase 2 **不重写业务**；仅增加薄启动壳、注册中心、网关、拆库与 Feign 配置。

## 2. V1 逻辑架构

```text
              web / App / 小程序
                      │
                      ▼
            ┌─────────────────────┐
            │   relayflow-server   │  :8080
            │  ┌─────────────────┐ │
            │  │ system-biz      │ │
            │  │ infra-biz       │ │
            │  │ im-biz          │ │
            │  └─────────────────┘ │
            └──────────┬──────────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    sys_* 表      infra_* 表     im_* 表
         └─────────────┼─────────────┘
                       ▼
            PostgreSQL · Redis · MinIO
```

### 跨域调用（V1）

```text
im-biz 需要用户昵称
  → 注入 AdminUserApi（system-api）
  → system-biz 内 AdminUserApiImpl
  → 禁止 im-biz import UserService / UserMapper(sys_*)
```

`*-api` 接口签名 **即未来 Feign 方法**。

## 3. Phase 2 逻辑架构（目标态）

```text
                    relayflow-gateway :8080
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
  system-server      infra-server       im-server
         │                 │                 │
         └─────────────────┼─────────────────┘
                    OpenFeign (*-api)
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
  relayflow_system   relayflow_infra    relayflow_im
```

技术栈：Spring Cloud Gateway、Nacos 2.x、OpenFeign、LoadBalancer；可选 Resilience4j、Micrometer Tracing。V1 **不**引入 Kafka/RocketMQ。

Gateway 路由与现 API 前缀一致：`/admin-api/system/**` → system-server，等。

## 4. Maven 结构

### V1

```text
relayflow-gateway/          # 不存在
relayflow-*-server/         # 不存在
relayflow-server/           # 唯一启动入口
relayflow-module-*/         # api + biz
```

### Phase 2 新增（biz 模块不动）

```text
relayflow-gateway/
relayflow-system-server/    # 仅依赖 system-biz
relayflow-infra-server/
relayflow-im-server/
relayflow-server/           # 废弃或仅 dev 聚合（默认不用）
```

新增 `relayflow-spring-boot-starter-rpc`（Feign 约定）。

## 5. 模块职责（与是否微服务无关）

| 域 | 职责 | 表前缀 |
|----|------|--------|
| system | 用户、组织、RBAC、JWT、租户元数据 | `sys_` |
| infra | 文件、审计、WS、工作台应用注册（将来） | `infra_` |
| im | 会话、消息、群、频道 | `im_` |

## 6. Phase 2 触发条件

满足 **多项** 再启动 Phase 2 实现 change：

- IM 连接数/消息量需独立扩缩容
- 需独立发布某一域而不全量回归
- 运维就绪：Nacos、多容器 Compose、监控
- 单库瓶颈，需分库

## 7. 实施顺序

### V1（当前）

```text
1. scaffold-maven-parent
2. scaffold-framework
3. scaffold-server              ← relayflow-server 空壳可 run
4. scaffold-deploy-compose
5. scaffold-web-nuxt-ui
6. tenant-ready-foundation
7. system-auth-minimal
8. infra-* / im-* 按域 change
```

### Phase 2（将来独立 change 或本 change 第二章节）

```text
1. starter-rpc + Nacos compose
2. scaffold-gateway
3. scaffold-*-server（三业务）
4. 分库迁移 + Feign 切换
5. 下线单体部署路径（或保留 dev profile）
```

## 8. 防微服务成灾

- V1～Phase 2 业务 JVM **≤ 3**（system / infra / im）
- 新官方能力 → 先 `module-*`，默认并入最近域 server
- 第三方应用 → 开放平台，不增 JVM
- 禁止按表/按侧边栏菜单拆服务
