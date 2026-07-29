## ADDED Requirements

### Requirement: Tenant invite settings persistence

系统 MUST 以每租户至多一行持久化邀请全局设置（`sys_tenant_invite_settings`），字段至少包含：`invite_mode`（`OFF` | `ADMIN_ONLY` | `ALL_MEMBERS`）、`non_admin_scope`（`INVITER_DEPT` | `DESIGNATED_DEPTS`）、`non_admin_dept_ids`、`non_admin_expire_days`、`non_admin_require_approval`。默认语义 MUST 为 `ADMIN_ONLY`、`INVITER_DEPT`、空部门列表、`non_admin_expire_days=3`、`non_admin_require_approval=false`。无持久行时 MUST NOT 要求入企预插行（C 类：读合并默认、写 upsert）。

#### Scenario: Default when no row

- **WHEN** 租户尚无邀请设置行且管理员请求读取设置
- **THEN** 系统返回上述默认值且不必先插入数据库行

#### Scenario: Upsert on save

- **WHEN** 管理员保存邀请设置
- **THEN** 系统按当前租户 upsert 一行，后续读取返回已保存值

### Requirement: Admin invite settings API

系统 MUST 提供管理端 `GET /admin-api/system/invite-settings` 与 `PUT /admin-api/system/invite-settings`（全量 body，字段与契约一致）。读 MUST 要求管理身份且具备 `system:user:list`；写 MUST 要求管理身份且具备 `system:user:create`。成功响应 MUST `code=0`，`data` 为完整设置对象。非法枚举或非法有效期天数 MUST 拒绝（非 0 业务错误或校验错误，按项目统一错误约定）。

#### Scenario: Get settings as admin

- **WHEN** 具备 `system:user:list` 的管理员调用 GET
- **THEN** 返回当前租户设置（或默认）

#### Scenario: Put all-members settings

- **WHEN** 具备 `system:user:create` 的管理员 PUT `inviteMode=ALL_MEMBERS` 及非管理员相关字段
- **THEN** 持久化成功并在响应 `data` 中回显

#### Scenario: Forbidden without permission

- **WHEN** 已登录但缺少对应用户权限的调用方请求 GET 或 PUT
- **THEN** 系统拒绝访问（403 或项目统一无权限码）
