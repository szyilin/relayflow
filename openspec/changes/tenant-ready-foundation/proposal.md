# 提案：租户就绪基础（Tenant-Ready Foundation）

## Why

RelayFlow V1 产品定位为自托管单企业部署，默认不暴露 SaaS / 多租户能力；但若底层数据模型从第一天起不含租户维度，未来启用 SaaS 工作平台或多组织切换将引发全库重构。本变更在 **产品层默认单租户、数据层租户就绪** 的前提下，为后续无缝升级预留架构空间。

## What Changes

- 引入 **租户（tenant）** 作为全局数据隔离维度；V1 仅存在一条 **默认租户**（`id=1`）
- 新增配置项 `relayflow.tenant.enabled`（默认 `false`）：关闭时不展示租户相关 UI/流程，所有请求隐式绑定默认租户
- 所有业务表（`sys_`、`infra_`、`im_`）必须包含 `tenant_id` 字段及联合索引
- 新增 `sys_tenant`、`sys_tenant_user` 表及 Flyway 种子数据
- 框架层增加 **TenantContext** 与 MyBatis-Plus 租户插件，自动注入 `tenant_id` 过滤
- Redis 缓存 key、MinIO 对象路径、JWT claim 预留 `tenant_id`
- 修正 V1 边界表述：「不做 SaaS 产品能力」≠「不做租户数据模型」
- 更新架构文档、AGENTS.md、openspec/config.yaml

## Capabilities

### New Capabilities

（无新增独立域；租户能力归入 system 与 deployment 域）

### Modified Capabilities

- `deployment`：新增租户模式配置、默认单租户部署行为
- `system`：新增租户实体、默认租户种子、用户-租户关系、全表 `tenant_id` 规范、租户上下文与自动过滤

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-framework` | 新增 `tenant` Starter 或并入 security/mybatis Starter |
| `relayflow-module-system` | 租户表、TenantContext、租户插件、JWT claim |
| `relayflow-module-infra` / `im` | 所有 DO/表含 `tenant_id` |
| Flyway 迁移 | 首版迁移即含租户表与默认种子 |
| `web/` | V1 无租户 UI；JWT/请求无需传 tenant（由后端注入） |
| `deploy/` | `.env.example` 增加 `RELAYFLOW_TENANT_ENABLED` |
| `openspec/specs/` | 合并 deployment、system 域增量 |
| 归档 design | 修正「V1 不做多租户」为「V1 不开启 SaaS 产品能力」 |

## Rollback

本变更首阶段为规格与架构文档。实现后若需回滚租户插件，须保留 `tenant_id` 列（仅停用自动过滤），避免数据丢失。默认租户 `id=1` 不可删除。
