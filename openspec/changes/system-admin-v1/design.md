# 设计：系统管理 V1 史诗（system-admin-v1）

## Context

- **上游**：`system-schema-v1`（表 + 种子权限点 + `super_admin`）、`tenant-ready-foundation` §2–4（租户插件 + JWT）、已归档登录/壳层/用户列表切片
- **参考**：飞书管理后台 — 管理员角色（功能权限 + 管理范围）、部门组织树、成员生命周期；见已归档 `system-schema-v1/design.md` 飞书映射表
- **现状缺口**：
  - `LoginUser.getAuthorities()` 返回空列表
  - 无 Dept/Role Controller
  - 前端 sidebar 写死、无 `/admin/system/role`
  - `SecurityAutoConfiguration` 对 `user/create` 仍 `permitAll`

本设计为 **史诗总纲**；各子 change 的 API 字段细节写入 `openspec/lanes/{slice}/contract.md`。

## Goals / Non-Goals

**Goals:**

- 闭环 **双轴 RBAC**：功能权限（`permission.code`）+ 数据范围（`data_scope`）
- 可维护的 **部门树** 与 **角色**（含绑权限、管理范围）
- 用户 **创建/编辑** 可绑真实部门与角色
- 前端 **菜单/按钮** 按 permission 显示；API 403 与 spec 一致
- 子 change 可独立 archive、≤10 tasks/change

**Non-Goals（V1 本史诗及近期子 change）:**

- 用户组、审批角色（BPM）、权限审计工作台
- `sys_menu` 动态 CRUD（V1 用静态 nav + permission 过滤）
- 邀请流、`NOT_JOINED` / `PENDING_LEAVE` 完整 UI（字段保留，V1 先用 ACTIVE/SUSPENDED）
- 字段级可见性、协作规则引擎

## 飞书对齐（决策依据）

| 飞书概念 | RelayFlow V1 | 本史诗 |
|----------|--------------|--------|
| 管理员角色 | `sys_role` + `sys_permission` | `admin-role-slice` |
| 管理范围（同角色共享） | `data_scope` + `sys_role_dept` | `admin-role-slice` + kernel 过滤 |
| 部门组织 | `sys_dept` 树 | `admin-dept-slice` |
| 菜单 vs 权限 | 菜单导航，API 看 permission | 静态 nav + code 过滤 |
| 审批角色 / 用户组 | 不做 | — |

## 子 Change 路线图

```text
system-admin-v1（本 change — 仅文档）
  │
  ├─① system-rbac-kernel     [平台] 后端优先
  ├─② admin-dept-slice       web → api → integrate
  ├─③ admin-role-slice       web → api → integrate  （可与②并行）
  └─④ admin-user-mutate-slice  web → api → integrate （依赖 ②③）
```

### ① `system-rbac-kernel`

| 层 | 内容 |
|----|------|
| Framework | `PermissionApi` / `PermissionServiceImpl`：查 user→roles→permission codes |
| Security | `LoginUser` 填充 authorities；`@EnableMethodSecurity`；自定义 `@PreAuthorize` 或 `hasAuthority('system:user:list')` |
| System | `GET /admin-api/system/auth/get-permission-info` |
| DataScope | `DataScopeHelper`：解析当前用户 deptIds（ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM） |
| 回归 | `UserController.page` 加 `system:user:list`；移除 `user/create` 的 permitAll |
| 前端 | 登录后 fetch permissions；`usePermission()` / 过滤 `useAdminNav` |

**响应形状（契约草案）**：

```json
{
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "roles": [{ "id": 100, "code": "super_admin", "name": "超级管理员" }],
  "permissions": ["system:user:list", "system:dept:list", "..."]
}
```

### ② `admin-dept-slice`

| API | permission |
|-----|------------|
| `GET /admin-api/system/dept/list` | `system:dept:list` |
| `GET /admin-api/system/dept/get?id=` | `system:dept:query` |
| `POST /admin-api/system/dept/create` | `system:dept:create` |
| `PUT /admin-api/system/dept/update` | `system:dept:update` |
| `DELETE /admin-api/system/dept/delete?id=` | `system:dept:delete` |

- Flyway 种子：默认根部门「总部」（若库内无部门）
- 前端：`/admin/system/dept` 树形 UI（替换 UEmpty 占位）
- 删除规则：有子部门或关联用户时拒绝

### ③ `admin-role-slice`

| API | permission |
|-----|------------|
| `GET /admin-api/system/role/page` | `system:role:list` |
| `GET /admin-api/system/role/get?id=` | `system:role:query` |
| `POST /admin-api/system/role/create` | `system:role:create` |
| `PUT /admin-api/system/role/update` | `system:role:update` |
| `DELETE /admin-api/system/role/delete?id=` | `system:role:delete` |
| `GET /admin-api/system/permission/list` | `system:role:query`（只读权限树，供勾选） |

- 更新角色时写入 `sys_role_permission`、`sys_role_dept`（CUSTOM 时）
- 校验：`role_type=SYSTEM` 不可删；子角色 permission ⊆ 父角色
- 前端：新建 `/admin/system/role` + sidebar 入口（需 `system:role:list`）

### ④ `admin-user-mutate-slice`

| API | permission |
|-----|------------|
| `POST /admin-api/system/user/create` | `system:user:create` |
| `PUT /admin-api/system/user/update` | `system:user:update` |
| `PUT /admin-api/system/user/update-status` | `system:user:update` |
| `PUT /admin-api/system/user/update-role` | `system:user:update` |
| `PUT /admin-api/system/user/update-dept` | `system:user:update` |

- `GET /user/page` 增加 **data_scope** 过滤（kernel 提供 helper）
- 前端：`/admin/system/user/create` 接 API；部门/角色从 ②③ API 加载

## Decisions

### D1：史诗 vs 单体 change

**决策**：1 个规划史诗 + 4 个 implementation change。  
**理由**：符合 AGENTS「单次 ≤10 tasks」；kernel 可 `[平台]` 后端先行，业务切片仍 `-web → -api → -integrate`。

### D2：菜单 V1 静态 + permission 过滤

**决策**：不改 `sys_menu` 表用法；`useAdminNav` 每项增加 `permission?: string`。  
**理由**：飞书有菜单管理，但非阻塞；静态 nav 与现有 prototype 一致，实现快。  
**替代**：动态 menu API → 留 `admin-menu-slice`（V1.1）。

### D3：权限信息独立 GET vs 扩 login 响应

**决策**：`GET /auth/get-permission-info`（登录后 + 刷新页面调用）。  
**理由**：权限变更可 refresh 而不 re-login；login 响应保持精简。

### D4：data_scope 计算

**决策**：多角色取 **并集**（任一角色 ALL → ALL；否则合并 deptIds）。  
**理由**：与常见 admin 产品一致；飞书同角色内范围相同，多角色并集为扩展点。

```text
ALL → 无 dept 过滤
DEPT → 用户主部门 id
DEPT_AND_CHILD → 主部门 + 递归子部门
SELF → 仅 userId 自身
CUSTOM → sys_role_dept 并集
```

### D5：命名 Supersedes

| 旧 change 名 | 新承接 |
|-------------|--------|
| `system-auth-minimal` | `system-rbac-kernel` |
| `system-admin-api` | ②③④ 拆分 |
| `admin-bootstrap-slice` | `admin-user-mutate-slice` |

## 共享约定

### Permission code

沿用 Flyway 种子（`V0.1.0.2__init_system.sql`）：`system:user:*`、`system:dept:*`、`system:role:*`、`system:menu:*`、`system:auth:login`。

### 前端

| 项 | 约定 |
|----|------|
| Store | `stores/auth.ts` 增加 `permissions: string[]`、`fetchPermissionInfo()` |
| Composable | `usePermission(code)` → boolean |
| Nav | `useAdminNav` 过滤无 permission 的项 |
| 403 | 无 permission 访问路由 → 403 页或 redirect |

### Lanes / 看板

每个子 change 实施前在 `openspec/lanes/{slice}/contract.md` 冻结 API；更新 `docs/dev/api-integration-board.md`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| kernel 与 user/page 回归 | 先保证 super_admin 全权限；集成测试 curl |
| 中英文 spec header 归档失败 | 子 change delta 用 `### Requirement:` 英文标题 |
| tenant-ready 未归档干扰范围 | 史诗明确仅依赖 §2–4；§5/§7 独立 `tenant-platform-slice` |
| 多角色 data_scope 并集过宽 | V1 文档化；后续可加「取最严」策略 |

## Migration Plan

1. 审阅并 validate 本史诗 change
2. 按序 `openspec new change` 创建 4 个子 change
3. 实施 ① → ②∥③ → ④
4. 全部子 change archive 后 archive 本史诗

**无 Flyway 破坏性迁移**（本史诗层）；子 change 仅可能 INSERT 默认部门。

## Open Questions

- `admin-dept-slice` 与 `admin-role-slice` 是否允许并行两个 AI 会话？（contract 冻结后可并行）
- 用户列表 `status` 字段映射：V1 是否仅暴露 ACTIVE/SUSPENDED 两态 UI？（建议是）

## 参考

- 已归档：`openspec/changes/archive/2026-07-05-system-schema-v1/design.md`
- 主 spec：`openspec/specs/system/spec.md`
- Lanes 示例：`openspec/lanes/admin-user-list/contract.md`
