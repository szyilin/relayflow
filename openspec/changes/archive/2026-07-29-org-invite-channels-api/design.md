## Context

Settings 持久化已完成；channels-web 字段对齐 store：`token` / `inviteCode` / `expiresAt` / mobile drafts；母 change D2–D4 定义实体与状态机。

## Goals / Non-Goals

**Goals:**

- `sys_org_invite` + codegen DO/Mapper
- capability、shared ensure/reset、mobile create、consume（token|code）
- 创建受 `invite_mode` / isAdmin / 非管理员 expire 约束
- 消费写 ACTIVE membership；定稿 contract；curl 验收

**Non-Goals:**

- 审批队列 API、Bot 邀请卡（可沿用旧 admin invite）
- `web/` integrate、改 Create Hub UI
- 推倒删除旧 `member-invite/*`（保留 deprecated）

## Decisions

1. **共享行**：每租户至多一条 `channel=SHARED` 且 `status=ACTIVE`；同存 `token` + `invite_code`；reset → 旧行 `REVOKED`，新建 ACTIVE。
2. **token** 全局唯一（`WHERE deleted=0`）；`invite_code` 租户内唯一（ACTIVE）。
3. **expires_at**：管理员创建共享可用较长默认（如 30 天）或按 settings `nonAdminExpireDays`；V1 统一用 settings 天数（与 web 一致）。
4. **capability**：读 settings + `PermissionService.isAdmin`；`OFF`→false；`ADMIN_ONLY`→isAdmin；`ALL_MEMBERS`→true。
5. **mobile**：仅 `canInvite` 时可建；`PENDING_APPROVAL` 当非管理员且 `require_approval`；否则 ACTIVE。不预插 `NOT_JOINED`。
6. **consume**：登录用户；按 token/code 定位邀请；校验未过期、非 REVOKED、mode≠OFF；已是 ACTIVE 成员 → `alreadyMember=true`；否则 upsert membership ACTIVE + ensure bot；MOBILE → CONSUMED；SHARED 保持 ACTIVE。
7. **Flyway**：`V0.1.0.42__sys_org_invite.sql`；可选 `V0.1.0.43__migrate_not_joined_to_org_invite.sql`。
8. **端口**：`:8080`；无新环境变量（链接由前端拼 origin）。

## Risks / Trade-offs

- [Risk] 旧 NOT_JOINED 与新 invite 双轨 → 迁移 SQL + 后续改 register 扫 invite
- [Trade-off] 本切片不推 Bot 卡 → 手机号邀请靠加入页/注册后续衔接
