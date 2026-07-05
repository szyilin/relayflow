# 提案：系统管理 V1 史诗（system-admin-v1）

## Why

`system-schema-v1` 已定型 system 域表结构、权限点种子与 `super_admin` 角色，登录与用户列表分页也已落地，但 **RBAC 运行时、部门/角色管理 API、前端权限门禁** 尚未实现。在此状态下继续堆用户创建、菜单控制或业务模块，会导致 Mock 部门、硬编码角色、API 仅校验 JWT 而无 `permission.code` 鉴权。

本 change 为 **规划型史诗**：定义系统管理 V1 的拆分路线、技术决策与子 change 清单；**不在本 change 内写业务代码**。实现按子 change 顺序推进。

## What Changes

- 发布史诗级 `design.md`：依赖图、飞书对齐、API 总览、共享约定
- 在 `system` 域 spec 增量中补充 **运行时行为**（鉴权链路、管理 API、前端门禁）
- 定义 4 个 implementation change 及顺序：
  1. `system-rbac-kernel` — 权限加载 + API 鉴权 + 前端 permission store
  2. `admin-dept-slice` — 部门树 CRUD + `/admin/system/dept`
  3. `admin-role-slice` — 角色 CRUD + 绑权限/数据范围 + `/admin/system/role`
  4. `admin-user-mutate-slice` — 用户创建/编辑/绑部门角色 + data_scope 过滤列表
- **Supersedes**（命名替换，不再单独立项）：
  - `system-auth-minimal` → 由 `system-rbac-kernel` 承接 RBAC 部分
  - `system-admin-api` → 由上述 4 个子 change 拆分
  - `admin-bootstrap-slice` → 由 `admin-user-mutate-slice` 承接用户创建
- V1 菜单策略：**静态 `useAdminNav` + permission code 过滤**（不做 `sys_menu` CRUD）

## Capabilities

### New Capabilities

（无独立新域；行为增量写入 `system`）

### Modified Capabilities

- `system`：RBAC 运行时鉴权、权限信息 API、部门/角色/用户写操作管理端 API、前端权限门禁（史诗级需求摘要；细节在子 change spec delta）

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-framework` | Security：`LoginUser` authorities、`@PreAuthorize`、DataScope 辅助 |
| `relayflow-module-system-biz` | Dept/Role/Auth Controller + Service |
| `web/` | `stores/auth` permissions、`useAdminNav` 过滤、部门/角色/用户页 |
| Flyway | 子 change 可能增默认根部门种子（无表结构变更） |
| `openspec/lanes/` | 新增 `system-rbac-kernel`、`admin-dept`、`admin-role`、`admin-user-mutate` contract |
| `docs/dev/api-integration-board.md` | 子 change 实施时更新 |

**本 change 不写 Java / web 代码。**

## 不在本 change 范围

- 用户组、审批角色、权限审计 UI、`sys_menu` 动态 CRUD
- `tenant-ready-foundation` §5 平台项（Redis/MinIO/WS 租户前缀）
- 员工工作台 `/app/*` app-api

## 前置条件

- `system-schema-v1` 已归档（表 + DO + 主 spec 已合并）
- 登录、租户壳层、用户列表分页已联调（`admin-user-list-integrate` 等已归档）
