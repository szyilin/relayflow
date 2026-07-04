# 任务：tenant-ready-foundation

## 1. 文档与规格

- [x] 1.1 审阅 proposal.md、design.md、spec delta
- [x] 1.2 运行 `openspec validate tenant-ready-foundation --strict`
- [ ] 1.3 运行 `openspec archive tenant-ready-foundation` 合并至 `openspec/specs/`
- [ ] 1.4 更新 `AGENTS.md`、`openspec/config.yaml`、归档 design 中「V1 不做多租户」表述

## 2. 框架层（relayflow-framework）

- [x] 2.1 新增 `relayflow-spring-boot-starter-tenant` 模块（或并入 mybatis/security）
- [x] 2.2 实现 `TenantProperties`（`enabled`、`default-id`、`header-name`）
- [x] 2.3 实现 `TenantContextHolder` 与 `TenantWebFilter`
- [x] 2.4 实现 MyBatis-Plus `TenantLineHandler` 自动注入 `tenant_id`
- [x] 2.5 注册 Spring Boot AutoConfiguration

## 3. 数据库（Flyway）

- [x] 3.1 迁移脚本：创建 `sys_tenant`、`sys_tenant_user`
- [x] 3.2 迁移脚本：插入默认租户 `id=1, code=default`
- [x] 3.3 迁移规范：所有 `sys_`/`infra_`/`im_` 业务表含 `tenant_id NOT NULL`
- [x] 3.4 迁移规范：租户内联合唯一索引（如 `UNIQUE(tenant_id, username)`）

## 4. 系统模块（relayflow-module-system）

- [x] 4.1 租户 DO / Mapper / 只读 API（默认租户查询）
- [x] 4.2 用户创建时写入 `sys_tenant_user`（默认 tenant_id=1）
- [x] 4.3 JWT 签发与校验写入 `tenant_id` claim
- [x] 4.4 登录流程绑定 TenantContext

## 5. 基础设施与 IM 模块

- [ ] 5.1 infra 模块所有 DO 含 `tenantId` 字段
- [ ] 5.2 im 模块所有 DO 含 `tenantId` 字段
- [ ] 5.3 Redis 缓存 key 前缀 `t:{tenantId}:`
- [ ] 5.4 MinIO 对象路径前缀 `tenant/{tenantId}/`
- [ ] 5.5 WebSocket 握手绑定 `tenant_id`，消息路由带租户过滤

## 6. 部署配置

- [x] 6.1 `application.yml` 增加 `relayflow.tenant.*` 默认值
- [x] 6.2 `deploy/.env.example` 增加 `RELAYFLOW_TENANT_ENABLED`、`RELAYFLOW_TENANT_DEFAULT_ID`

## 7. 测试

- [ ] 7.1 单租户模式：所有查询自动过滤 `tenant_id=1`
- [ ] 7.2 集成测试：两租户数据互不可见（为将来 enabled=true 预留）
- [ ] 7.3 默认租户 `id=1` 不可删除
