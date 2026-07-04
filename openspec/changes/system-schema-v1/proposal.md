# 提案：系统域数据模型 V1（system-schema-v1）

## Why

租户基础（`tenant-ready-foundation`）已建立 `sys_tenant` / `sys_tenant_user` 与租户插件，但 system 域仍缺少用户、组织架构与 RBAC 表结构。结合飞书帮助中心归档（`research/feishu-hc/`，403 篇 P0 文档）与 RelayFlow 约束，需在 **不写完整业务 API** 的前提下，先定型 system 域 ER、Flyway 迁移与预置种子，为后续登录鉴权（`system-auth-minimal`）与管理端 API 提供数据基础。

## What Changes

- 新增 Flyway `V1.0.0.2__init_system.sql`：用户、部门、RBAC、菜单表
- 扩展 `sys_tenant_user`：增加 **成员生命周期状态**（按租户维度，非全局账号状态）
- 明确 **双轴 RBAC**：功能权限（`sys_permission`）+ 数据范围（`sys_role.data_scope` / `sys_role_dept`）
- 权限与菜单解耦：`sys_menu` 仅负责 UI 导航，鉴权走 `sys_role_permission`
- 预置系统角色（`super_admin`）与 system 域最小权限点树
- system-biz 层 DO / Mapper 骨架（无 Controller / 无 JWT 实现）
- OpenSpec system 域规格增量

## Capabilities

### New Capabilities

（无新增独立域）

### Modified Capabilities

- `system`：组织架构、RBAC 双轴模型、成员租户状态、表清单与种子数据约定

## Impact

| 区域 | 影响 |
|------|------|
| Flyway | 新增 `V1.0.0.2__init_system.sql`；`sys_tenant_user` 增列 |
| `relayflow-module-system-biz` | DO、Mapper、枚举常量 |
| `relayflow-framework` | 无（数据权限拦截器留 `system-auth-minimal` 或后续 change） |
| `web/` | 无 |
| `openspec/specs/system` | 归档时合并增量 |

## 不在本 change 范围

- JWT 登录、密码校验、Token 签发（→ `system-auth-minimal`）
- 管理端 CRUD API、前端页面
- 用户组（`sys_user_group`）、审批角色、IM presence（请勿打扰/会议中）
- 沟通协作规则、组织架构可见范围、权限审计工作台
- `tenant-ready-foundation` 未完成项（§4 JWT、§5 infra/im tenantId、§7 测试）

## Rollback

保留 `V1.0.0.1` 不变；若需回滚本迁移，须在新迁移中 DROP 本 change 创建的表并移除 `sys_tenant_user.status` 列（V1 尚无生产数据）。
