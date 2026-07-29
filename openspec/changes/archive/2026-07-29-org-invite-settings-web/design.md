## Context

母 change design D3：`sys_tenant_invite_settings` 字段与飞书全局设置对齐。管理端 UI 参考存储设置页（表单 + 保存）。

## Goals / Non-Goals

**Goals:** 邀请设置页可编辑可保存（store 临时）；导航可见；contract settings 章；看板 ui_ready。

**Non-Goals:** 真 API、Flyway、四渠道弹窗、审批列表页、改 Create Hub canInvite 接本 store（可轻量同步，非必须）。

## Decisions

1. 路由 `/admin/system/invite-settings`；nav label「邀请设置」。
2. 权限：`system:user:list` 查看、`system:user:create` 保存（贴近邀请）。
3. Store `inviteSettings`：默认 `ADMIN_ONLY`；`save` 写内存并 toast。
4. 指定部门：`USelectMenu` multiple 基于 `deptStore.list`。
5. 有效期选项：1 / 3 / 7 / 30 天（对齐飞书常见档）。

## Risks / Trade-offs

- [Risk] 刷新丢失设置 → 预期 `-web`；integrate 后走 API
- [Risk] 与 Create Hub `isAdmin` 不同步 → settings-web 文档注明；channels 切片统一接 capability
