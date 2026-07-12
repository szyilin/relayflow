# 提案：管理端按部门管人（admin-user-by-dept）

## Why

[`org-member-dept-default`](../org-member-dept-default/proposal.md) 落地后，每个成员必有主部门。当前 `/admin/system/user` 为**全量平铺表**，无法像飞书「成员与部门」那样**左侧部门树 + 右侧部门成员**浏览与新建，管理员难以按组织维度管人。

须在通讯录（`workspace-contacts`）之前，补齐**管理端组织视角的用户管理**，并与既有用户 CRUD（`admin-user-mutate`）兼容。

## What Changes

### Lane `-web`

- `/admin/system/user`：左侧部门树（选中高亮）+ 右侧成员表（随选中部门刷新）
- 新建/编辑用户：默认部门 = 当前树选中节点
- 起草 [`openspec/lanes/admin-user-by-dept/contract.md`](../../lanes/admin-user-by-dept/contract.md)（扩展用户分页 `deptId`）

### Lane `-api`

- `GET /admin-api/system/user/page` 增加 `deptId`（V1：**仅直属主部门**成员，不含子部门）
- 权限与 data_scope 过滤行为不变

### Lane `-integrate`

- store 去 Mock（若 `-web` 有）；联调部门树与分页
- 看板 `admin-user-by-dept` → done

## Capabilities

### New Capabilities

（无新 spec 域）

### Modified Capabilities

- `system`：用户分页 API 支持按部门过滤
- `web-admin`：用户管理页布局改为部门树 + 成员列表

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `pages/admin/system/user/*`、`stores/user.ts`、`api/admin/user.ts` |
| `relayflow-module-system-biz` | `UserPageReqVO`、`UserServiceImpl.getUserPage` |
| Flyway | **无** |
| 前置 | `org-member-dept-default` 已实现并归档（或至少已部署） |

## 不在本 change

- 批量导入/导出、批量改部门（飞书高级能力）
- 子部门成员递归汇总（`includeChildren`，Phase 1.1）
- 工作台通讯录（`workspace-contacts`）
- 部门 CRUD 页改造（仍用 `/admin/system/dept`）

## 实施顺序（本 change 内）

```text
admin-user-by-dept-web → admin-user-by-dept-api → admin-user-by-dept-integrate
```

（单 change 文档；实现会话可按 lane 拆分，见 `tasks.md`）
