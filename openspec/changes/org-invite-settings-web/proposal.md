## Why

母 change `workspace-plus-create-v1` 要求管理端在「成员与部门」下提供飞书式邀请全局设置。需先交付可交互 UI + 契约草案，再接 `-api`。

## What Changes

- 管理端路由 `/admin/system/invite-settings`：邀请功能三态、非管理员范围/有效期/审批
- 侧栏「邀请设置」挂在系统（成员与部门）导航
- Store 临时持久（内存）；保存后可供工作台 `canInvite` 后续对接预留
- 起草 `openspec/lanes/org-invite/contract.md`（settings 章）；更新看板

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `org-invite`：本切片落地管理端邀请设置 UI（相对母 change 的可演示子集）
- `web-admin`：成员与部门下增加邀请设置导航与页面

## Impact

- 仅 `web/` + OpenSpec lane/看板；不改 Java
- 权限临时用既有 `system:user:create`（更新）/ `system:user:list`（查看），`-api` 可改种子 permission
