# 任务：system-schema-v1

## 1. 文档与规格

- [x] 1.1 审阅 proposal.md、design.md、spec delta
- [x] 1.2 运行 `openspec validate system-schema-v1 --strict`

## 2. Flyway 迁移（V1.0.0.2）

- [x] 2.1 `sys_tenant_user` 增加 `status` 列；已有记录默认 `ACTIVE`
- [x] 2.2 （可选）`sys_tenant` 增加 `owner_user_id` 列
- [x] 2.3 创建 `sys_user`（全局账号，无 `tenant_id`、无 lifecycle status）
- [x] 2.4 创建 `sys_dept`、`sys_user_dept`（含 `primary_flag`）
- [x] 2.5 创建 `sys_role`（含 `parent_id`、`role_type`、`data_scope`、`can_delegate`、`code`）
- [x] 2.6 创建 `sys_permission`、`sys_role_permission`、`sys_role_dept`
- [x] 2.7 创建 `sys_menu`（含可选 `permission_id`）
- [x] 2.8 种子：system 域最小权限点树 + `super_admin` 角色绑定全部权限

## 3. 系统模块 DO / Mapper（relayflow-module-system-biz）

> **DO / 基础 Mapper 改由 `scaffold-system-codegen` 实现**（见 `docs/dev/codegen.md`）。此前 AI 手写 DO/Mapper 已回滚。

- [x] 3.1 枚举：`TenantUserStatus`、`RoleType`、`DataScope`（手写，保留）
- [ ] 3.2 DO + Mapper：`SysUser`、`SysTenantUser`（含 status）— codegen
- [ ] 3.3 DO + Mapper：`SysDept`、`SysUserDept` — codegen
- [ ] 3.4 DO + Mapper：`SysRole`、`SysUserRole`、`SysRolePermission`、`SysRoleDept` — codegen
- [ ] 3.5 DO + Mapper：`SysPermission`、`SysMenu` — codegen

## 4. 验证

- [ ] 4.1 `./mvnw -pl relayflow-server -am compile`（codegen 落地后）
- [ ] 4.2 （可选）本地启动 Flyway 迁移，确认表与种子存在

## 5. 归档（实现完成后）

- [ ] 5.1 运行 `openspec archive system-schema-v1` 合并 spec 至 `openspec/specs/system/`
