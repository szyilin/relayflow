## Context

母 change `workspace-plus-create-v1` D2/D3 定义 `sys_tenant_invite_settings`；`-web` 已冻结 contract settings 章与管理端表单字段。本切片仅落地管理端持久化读写。

## Goals / Non-Goals

**Goals:**

- Flyway 建表；DO/Mapper 经 codegen diff 合入
- `GET/PUT /admin-api/system/invite-settings` 与 contract 字段一致（全量 PUT）
- 无行时 GET 返回代码默认；PUT upsert（C 类）
- 定稿 contract settings 章；编译 + curl 验收；看板 api 状态更新

**Non-Goals:**

- `sys_org_invite`、四渠道、consume、审批列表
- `GET /app-api/system/org-invite/capability`（channels-api）
- 改 `web/` store（integrate）
- 推倒旧 `POST /admin-api/system/user/invite`（channels/迁移切片）

## Decisions

1. **表**：`sys_tenant_invite_settings`；`id` 雪花 PK + `uk (tenant_id) WHERE deleted=0`（对齐 BaseDO / 偏好表模式；相对母稿「tenant_id PK」的可实现微调）。
2. **列**：`invite_mode`、`non_admin_scope`、`non_admin_dept_ids`（JSONB 数组）、`non_admin_expire_days`、`non_admin_require_approval` + 公共字段。
3. **默认**（代码 + GET 无行）：`ADMIN_ONLY` / `INVITER_DEPT` / `[]` / `3` / `false`。不入企灌表。
4. **校验**：`invite_mode` / `non_admin_scope` 枚举；`nonAdminExpireDays` ∈ {1,3,7,30}（与 UI 档一致）；`DESIGNATED_DEPTS` 时允许空部门列表（产品可后续收紧）。
5. **权限**：`@PreAuthorize` 读 `system:user:list`、写 `system:user:create`（与 `-web` 一致）。
6. **租户**：从当前 JWT 租户上下文取 `tenant_id`；禁止跨租户读写。
7. **Flyway**：`V0.1.0.41__sys_tenant_invite_settings.sql`（若序号被占用则顺延）。
8. **端口**：沿用 `relayflow-server` `:8080`；无新环境变量。
9. **离线/内网**：无外呼；仅 DB 持久化。

## Risks / Trade-offs

- [Risk] 多租户并发首次 PUT → 依赖唯一索引 + upsert/冲突重试
- [Risk] OFF 模式不在本切片撤销已发链接 → channels-api 消费时再校验 mode
- [Trade-off] 复用 user permission 而非独立 `system:invite-settings:*` → 与 web 对齐，后续可加细粒度码
