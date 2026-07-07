# 提案：用户创建/编辑（admin-user-mutate-slice）

## Why

部门与角色管理已落地，但用户创建页仍为 Mock，列表缺少编辑与启停能力；`user/page` 未按 data_scope 过滤。

## What Changes

- 用户 CRUD 写 API：create（含 dept/role）、get、update、update-status、update-dept、update-role
- `user/page` 增加 data_scope 过滤
- 前端 `/admin/system/user/create` 与列表编辑弹窗对接 API
- 冻结 [`openspec/lanes/admin-user-mutate/contract.md`](../../lanes/admin-user-mutate/contract.md)

## Capabilities

### Modified Capabilities

- `system`：用户写操作管理端 API、前端用户 mutate 页

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-system-biz` | UserController / UserService 扩展 |
| `web/` | user API、store、create/index 页 |

## 不在本 change

- 重置密码、删除用户
