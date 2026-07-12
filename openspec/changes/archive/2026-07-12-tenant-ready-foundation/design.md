# 设计：租户就绪基础（Tenant-Ready Foundation）

## Context

RelayFlow V1 面向自托管单企业部署，产品上不暴露 SaaS 注册、租户切换、计费等能力。用户期望：若未来有人将本项目用作 SaaS 工作平台，或支持「一账号多组织」，应能 **在不大改表结构** 的前提下升级。

当前 `bootstrap-v1-foundation` 设计将「多租户 SaaS」列为 V1 不做项，易被误解为 **数据层也不预留 tenant**。本设计明确：**V1 默认单租户模式（`enabled=false`），数据层从首版迁移起即 tenant-ready**。

## Goals / Non-Goals

**Goals:**

- 所有业务数据归属某一 `tenant_id`；V1 仅存在默认租户 `id=1`
- 配置关闭 SaaS 能力时，请求链自动绑定默认租户，开发者无需手写 `tenant_id`
- Flyway 首版迁移创建租户表与默认种子
- MyBatis-Plus 租户插件、Redis/MinIO/JWT/WebSocket 全链路携带 tenant 上下文
- 为 V2「多租户产品能力 / 用户多组织」预留 `sys_tenant_user` 关系表

**Non-Goals（V1 仍不做）:**

- 租户自助注册、订阅计费、平台超管控制台
- 前端租户切换 UI、子域名解析租户
- 跨租户数据查询（除将来专用平台 API）
- 一用户多租户登录切换（V2；表结构预留）

## Decisions

### D1：术语 — 使用 tenant（租户），UI 上 V1 可称「企业」

| 术语 | 含义 |
|------|------|
| tenant | 数据隔离边界，对应 SaaS 中的一个「企业/组织」 |
| 默认租户 | V1 安装后唯一租户，`id=1`，`code=default` |
| department | system 模块内组织架构，**从属于** tenant，二者不混用 |

**理由**：避免 `org` 与部门树混淆；与行业惯例（tenant_id）一致。

### D2：产品模式 vs 数据模式分离

```yaml
relayflow:
  tenant:
    enabled: false      # V1 默认：单租户产品模式
    default-id: 1
    header-name: X-Tenant-Id   # enabled=true 时从 Header 解析
```

| `enabled=false` | `enabled=true`（将来） |
|-----------------|------------------------|
| 隐式 `tenant_id=1` | JWT / Header 解析当前 tenant |
| UI 不展示租户概念 | 租户切换、注册、隔离审计 |
| 安装向导可设置默认租户名称 | 多租户运营能力 |

**理由**：同一套 SQL 与插件，仅 tenant 来源不同。

### D3：表设计

**全局表（含 tenant 元数据，不带 tenant_id）：**

- `sys_tenant` — 租户主表
- `sys_tenant_user` — 用户与租户多对多（V1 每人一条，指向 tenant 1）

**业务表（必须 `tenant_id BIGINT NOT NULL`）：**

- 所有 `sys_*`（除 tenant 元数据表）、`infra_*`、`im_*`
- 将来 `bpm_*` 同理

**索引：**

- 联合唯一：`UNIQUE(tenant_id, username)` 等租户内唯一约束
- 查询索引：`(tenant_id, ...)` Leading column

**Flyway 种子（`V1.0.0.1__init_tenant.sql` 或合并在 `V1.0.0.2__init_system.sql`）：**

```sql
INSERT INTO sys_tenant (id, code, name, status, create_time)
VALUES (1, 'default', '默认企业', 0, NOW());
-- id=1 受保护，不可删除
```

### D4：TenantContext 与 MyBatis-Plus 租户插件

```text
HTTP / WS 请求
  → TenantWebFilter / WebSocketHandshakeInterceptor
  → TenantContextHolder（ThreadLocal）
  → MyBatis TenantLineHandler 自动追加 AND tenant_id = ?
  → INSERT 自动填充 tenant_id
```

- `enabled=false`：Filter 固定 `TenantContextHolder.set(1L)`
- `enabled=true`：从 JWT claim `tenant_id` 或 `X-Tenant-Id` 解析
- 平台级接口（将来）：`@TenantIgnore` 或白名单表跳过插件

**备选**：手写 SQL 拼接 — 拒绝，易漏。

### D5：JWT

Access Token payload 预留：

```json
{ "sub": "userId", "tenant_id": 1, ... }
```

V1 签发时固定 `tenant_id=1`；校验时写入 TenantContext。

### D6：Redis / MinIO

| 存储 | 约定 |
|------|------|
| Redis | `t:{tenantId}:{业务key}` |
| MinIO | 对象 key 前缀 `tenant/{tenantId}/` 或 bucket 策略（V1 可用前缀） |

### D7：WebSocket

握手时从 JWT 解析 `tenant_id` 绑定 Session；消息路由、在线状态均带 tenant 维度，禁止跨 tenant 推送。

### D8：框架模块归属

新增 `relayflow-spring-boot-starter-tenant`（或在 `starter-mybatis` + `starter-security` 中聚合）：

- `TenantProperties`
- `TenantContextHolder`
- `TenantWebFilter`
- `RelayflowTenantLineHandler`（MyBatis-Plus）
- 自动配置 `@AutoConfiguration`

**理由**：tenant 横切 system/infra/im，放 framework 层，biz 模块仅声明依赖。

### D9：前端 V1

- 不展示租户选择器、注册新企业
- API 不传 `X-Tenant-Id`
- 安装向导（将来）可配置默认租户 **显示名称**（改 `sys_tenant.name`）

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 开发者绕过插件手写 SQL 导致串租 | Code Review + 集成测试断言 tenant 隔离；禁止 biz 层裸 SQL 无 tenant |
| 默认租户被误删 | 应用层 + DB 约束保护 `id=1` |
| 所有表加列增加迁移成本 | 首版尚无生产数据，现在是最佳时机 |
| `enabled=false` 与 `true` 行为差异 | 单测覆盖两种模式；文档明确 |
| 性能：每条 SQL 多一个条件 | `tenant_id` 走联合索引；单租户时优化器影响可忽略 |

## Migration Plan

1. **规格阶段**（本 change）：更新 OpenSpec、design、AGENTS
2. **Scaffold 阶段**：首版 Flyway 即含 `sys_tenant`、各表 `tenant_id`
3. **已有环境**：若未来已有无 tenant 列的库，需专门 Flyway 迁移脚本补列并回填 `1`（V1 尚无生产库，风险低）
4. **回滚**：可关闭插件，**不可**删 `tenant_id` 列

## Open Questions

- 安装向导是否在 V1 提供「企业名称」配置？（建议 V1.0 可选，默认「默认企业」）
- `enabled=true` 的 JWT 刷新策略（切换 tenant 是否 re-login）留 V2 设计

## 路线图衔接

```text
V1.0  tenant-ready + enabled=false（本设计）
V1.x  安装向导配置默认租户名称
V2    enabled=true、sys_tenant_user 多对多、前端组织切换
V3    SaaS 云运营（计费、注册）— 在 V2 数据模型上扩展
```
