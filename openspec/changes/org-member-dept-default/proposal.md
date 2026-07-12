# 提案：组织成员默认主部门（org-member-dept-default）

## Why

工作台通讯录与飞书式「成员与部门」管理，都依赖**每个有效组织成员至少有一个主部门**。飞书 Open API 创建用户时 `department_ids` 必填；未指定子部门时成员落在**企业根部门**（[创建用户文档](https://open.feishu.cn/document/server-docs/contact-v3/user/create)）。

RelayFlow 当前 `UserServiceImpl.assignDept` 在 `deptId == null` 时跳过写入，种子 `admin` 等用户无 `sys_user_dept`，管理端用户列表部门列显示 `-`。在此状态下无法可靠实现按部门浏览的通讯录。

须在 `workspace-contacts` 与 `admin-user-by-dept` 之前，统一**成员必有主部门 + 默认根部门**规则并回填历史数据。

## What Changes

- **根部门语义**：每租户唯一根部门（`parent_id = 0`）；展示名对齐租户名（`sys_tenant.name`），作为未指定部门时的默认落点
- **创建用户**：`deptId` 省略或为空 → 自动写入根部门主归属（`primary_flag = 1`）
- **更新主部门**：禁止清空；`deptId` 为空时拒绝或回落根部门（规格取「拒绝」以保持显式操作）
- **根部门保护**：禁止删除租户根部门
- **Flyway 回填**：为所有无 `sys_user_dept` 的 `sys_tenant_user` 补根部门关联；将现有根部门名称同步为租户名
- **DeptService**：提供 `getOrCreateRootDept(tenantId)` 供 UserService 与未来「邀请入企」复用

## Capabilities

### New Capabilities

（无新 spec 域；行为归入 `system`）

### Modified Capabilities

- `system`：组织架构与成员管理 — 补充「成员必有主部门」「默认根部门」「根部门不可删」等需求

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-system-biz` | `UserServiceImpl`、`DeptServiceImpl` / `DeptService` |
| `relayflow-module-system-api` | 可选错误码（如 `USER_DEPT_REQUIRED`、`DEPT_ROOT_DELETE_FORBIDDEN`） |
| Flyway | `V0.1.0.7__org_member_dept_default.sql`（回填 + 根部门改名） |
| `web/` | **不改**（本 change 纯后端 + 数据；管理端按部门 UI 为后续 change） |
| 下游 |  unblock `admin-user-by-dept`、`workspace-contacts` |

## 不在本 change

- 管理端用户页左侧部门树（`admin-user-by-dept`）
- 工作台通讯录 `/app/contacts`（`workspace-contacts`）
- 用户多部门 UI、批量改部门
- 邀请入企完整流程（仅预留 `getOrCreateRootDept` 钩子）

## 前置

- 表：`sys_dept`、`sys_user_dept`、`sys_tenant_user`（`V0.1.0.2` / `V0.1.0.4`）
- 根部门种子：`V0.1.0.4__seed_root_dept.sql`（id=1, parent_id=0, name=总部）

## 迁移与回滚

- 迁移为**追加** `sys_user_dept` 行与 `UPDATE sys_dept.name`；不删用户数据
- 回滚：手动删除迁移插入的 `sys_user_dept` 行并恢复部门名（开发环境可 `./mvnw flyway:repair` + 重做）；生产需备份后逆向 SQL
