# org-invite Specification

## Purpose
TBD - created by archiving change org-invite-channels-web. Update Purpose after archive.
## Requirements
### Requirement: Workspace org invite channels web

工作台「添加企业成员」MUST 提供企业二维码、企业链接、企业邀请码、手机号四种方式的可交互 UI。共享凭证的加载与重置、手机号邀请 MUST 调用 `/app-api/system/org-invite/*`；MUST NOT 以客户端随机生成 token/code 作为持久真源。复制链接/码 MUST 可用。Create Hub「添加企业成员」显隐 MUST 以 `GET …/org-invite/capability` 的 `canInvite` 为准。

#### Scenario: Reset invite link

- **WHEN** 有邀请权的用户在企业链接 Tab 点击重置并成功
- **THEN** UI 展示服务端返回的新 token/链接，旧 token 经 `POST …/consume` MUST 失败（如 NOT_FOUND / 已撤销）

#### Scenario: Mobile invite via API

- **WHEN** 有邀请权的用户输入合法手机号并发送邀请成功
- **THEN** 客户端调用 `POST …/org-invite/mobile` 并提示成功；列表/反馈与响应 `status` 一致

#### Scenario: Hub menu respects capability

- **WHEN** `capability.canInvite` 为 false（如 inviteMode=OFF 或非管理员且 ADMIN_ONLY）
- **THEN** Create Hub MUST NOT 展示或打开「添加企业成员」

### Requirement: Join page for invite token or code web

系统 MUST 提供加入页 `/app/invite/join`，支持查询参数中的邀请 token 或页面内输入邀请码；确认加入 MUST 调用 `POST …/org-invite/consume`。成功后 UI MUST 提示已加入（或已是成员）；MUST NOT 仅用本地草稿校验冒充加入成功。

#### Scenario: Join with valid shared token

- **WHEN** 已登录且非本企业 ACTIVE 成员的用户打开带有效 `t` 的加入页并确认
- **THEN** 客户端 `POST …/consume` 成功且 UI 显示加入成功；该用户成为当前租户有效成员（或 `alreadyMember=true` 时提示已是成员）

#### Scenario: Join with revoked or invalid code

- **WHEN** 用户输入已重置或无效的邀请码并确认
- **THEN** UI 显示失败原因（与 API 错误一致），MUST NOT 提示加入成功

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

### Requirement: Org invite capability API

系统 MUST 提供 `GET /app-api/system/org-invite/capability`，返回当前登录成员在当前租户是否可发起组织邀请及 `inviteMode`。`invite_mode=OFF` 时 `canInvite` MUST 为 false；`ADMIN_ONLY` 时仅管理身份为 true；`ALL_MEMBERS` 时有效成员为 true。响应 MUST NOT 泄露完整非管理员指定部门列表以外的敏感配置（本切片可仅返回 `canInvite` + `inviteMode`）。

#### Scenario: Admin-only mode for non-admin

- **WHEN** 设置 `inviteMode=ADMIN_ONLY` 且当前用户非管理身份
- **THEN** `canInvite` 为 false

#### Scenario: All-members mode

- **WHEN** 设置 `inviteMode=ALL_MEMBERS` 且当前用户为有效成员
- **THEN** `canInvite` 为 true

### Requirement: Shared invite channel API

系统 MUST 支持确保与重置租户共享邀请（链接/二维码/邀请码共用）：`GET` 或 `POST /app-api/system/org-invite/shared` 返回当前有效 `token`、`inviteCode`、`expiresAt`；`POST /app-api/system/org-invite/shared/reset` MUST 使旧凭证失效并颁发新凭证。仅 `canInvite` 为 true 时可 ensure/reset。关闭邀请（`OFF`）时 MUST 拒绝创建/重置。

#### Scenario: Ensure creates shared invite

- **WHEN** 有权用户首次请求 shared
- **THEN** 返回非空 token 与 inviteCode，且再次 GET 得到同一有效凭证（直至 reset）

#### Scenario: Reset invalidates old token

- **WHEN** 有权用户 reset 后用旧 token 消费
- **THEN** 消费失败

### Requirement: Mobile org invite create API

系统 MUST 提供 `POST /app-api/system/org-invite/mobile`，在 `canInvite` 为 true 时按手机号创建邀请实体；非法手机号 MUST 拒绝。非管理员且设置要求审批时，新建状态 MUST 为待审批（不可消费直至通过——本切片若未做审批 API，则创建为 `PENDING_APPROVAL` 且 consume 拒绝）。

#### Scenario: Create mobile invite as admin

- **WHEN** 管理员提交合法 11 位手机号
- **THEN** 创建 ACTIVE（或不需审批的）邀请并返回 id/mobile/status

### Requirement: Consume org invite API

系统 MUST 提供 `POST /app-api/system/org-invite/consume`，接受 `token` 或 `code` 之一。已登录用户成功消费后 MUST 成为目标租户 ACTIVE 成员（若尚不是）；已是成员 MUST 幂等成功并标明已加入。失效、撤销、过期、关闭邀请、待审批 MUST 拒绝。共享渠道消费后邀请可保持可用直至过期/重置；手机号渠道成功后 MUST 标记已消费。

#### Scenario: Consume shared token joins tenant

- **WHEN** 已登录且非该租户成员的用户携带有效 shared token 调用 consume
- **THEN** 写入/激活 membership 为 ACTIVE，且响应表示已加入

#### Scenario: Consume revoked token fails

- **WHEN** 用户使用已 reset 的旧 token
- **THEN** 返回业务错误且不改变 membership

### Requirement: Admin invite settings page web

管理端 MUST 提供「邀请设置」页（`/admin/system/invite-settings`），可编辑邀请功能（关闭 / 仅管理员 / 全员）、非管理员范围（本部门 / 指定部门）、非管理员有效期天数、是否需管理员审批。保存与加载 MUST 以 `GET`/`PUT /admin-api/system/invite-settings` 为唯一真源；MUST NOT 仅用会话内存作为持久真源。

#### Scenario: Save all-members mode persists after refresh

- **WHEN** 管理员选择「全员均可发送邀请」、指定部门并保存成功后硬刷新页面
- **THEN** 页面再次展示已保存值（与 GET 响应一致）

#### Scenario: Non-admin fields hidden when admin-only

- **WHEN** 邀请功能为「仅管理员」或「关闭」
- **THEN** 非管理员范围/有效期/审批控件 MUST 隐藏或不可编辑

### Requirement: Invite settings nav under system

管理端系统导航 MUST 包含「邀请设置」入口，且受权限过滤；无权限 MUST NOT 显示该入口。

#### Scenario: Nav visible with permission

- **WHEN** 当前管理员具备查看用户相关权限
- **THEN** 侧栏可进入邀请设置页

