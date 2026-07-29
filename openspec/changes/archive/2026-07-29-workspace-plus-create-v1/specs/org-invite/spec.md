## ADDED Requirements

### Requirement: Tenant invite settings

系统 MUST 为每个租户持久化邀请全局设置，并在管理端「成员与部门」相关导航下提供设置页。设置 MUST 包含：

1. **邀请功能**：`OFF`（关闭）｜`ADMIN_ONLY`（仅管理员可发送邀请）｜`ALL_MEMBERS`（全员含非管理员可发送邀请）
2. **非管理员可邀请范围**（仅 `ALL_MEMBERS`）：`INVITER_DEPT`（邀请人所在部门）｜`DESIGNATED_DEPTS`（指定部门列表）
3. **非管理员邀请有效期**（天数，到期后非管理员发出的可用邀请失效）
4. **非管理员邀请审批**：是否需要管理员审批后方可加入（或后方可生效）

默认 MUST 为 `ADMIN_ONLY`（与历史「仅管理端可邀」接近）。未登录或非管理员 MUST NOT 通过管理端 API 修改设置。

#### Scenario: Admin updates invite mode

- **WHEN** 具备权限的管理员将邀请功能保存为「开启，全员均可发送邀请」并指定部门范围
- **THEN** 系统持久化该设置，随后满足条件的非管理员在工作台可见「添加企业成员」

#### Scenario: Disable invitations revokes open channels

- **WHEN** 管理员将邀请功能保存为「关闭邀请」
- **THEN** 新的邀请创建 MUST 被拒绝，且已有开放的链接/二维码/邀请码 MUST 在校验时失效（或被标记撤销）

### Requirement: Org invite entity as process source of truth

系统 MUST 使用独立的组织邀请实体（而非仅 `sys_tenant_user.status=NOT_JOINED`）作为邀请过程真源，记录渠道、发起人、目标（手机号等）、过期时间与状态。成功加入后 MUST 以 `sys_tenant_user`（通常 `ACTIVE`）为成员结果真源。开发期允许破坏性迁移历史 `NOT_JOINED` 邀请行到新实体。

#### Scenario: Mobile invite creates invite row

- **WHEN** 授权用户通过手机号发起邀请
- **THEN** 系统创建一条 `MOBILE` 渠道邀请记录，含过期时间与目标手机号，且不把「仅有邀请、尚未接受」的过程态只存在于无元数据的 membership 行

#### Scenario: Consume invite activates membership

- **WHEN** 被邀请人通过合法未过期邀请完成接受或注册激活
- **THEN** 邀请状态变为已消费（或等价终态），且该用户在该租户下成为有效成员（`ACTIVE`）

### Requirement: Invite channels QR link code and mobile

工作台「添加企业成员」MUST 支持四种方式对齐飞书：**企业二维码**、**企业链接**、**企业邀请码**、**手机号**。共享类渠道（QR/链接/码）MUST 支持展示失效时间与重置（重置后旧 token/码失效）。手机号渠道 MUST 定向邀请指定号码。

#### Scenario: Copy enterprise invite link

- **WHEN** 有邀请能力的用户打开「企业链接」并复制链接
- **THEN** 链接包含可校验的邀请 token，未登录访问者可被引导至登录或注册后加入当前企业

#### Scenario: Reset QR code

- **WHEN** 用户重置企业二维码
- **THEN** 新二维码对应新 token，旧二维码/链接 token MUST 不再可用于加入

#### Scenario: Join with invite code

- **WHEN** 用户在加入页输入有效未过期的企业邀请码
- **THEN** 系统校验通过后将其加入当前企业（或进入待审批，若设置要求审批）

### Requirement: Non-admin invite constraints

当 `invite_mode=ALL_MEMBERS` 时，非管理员发起的邀请 MUST 遵守：目标部门落在允许范围；邀请有效期取自设置；若开启「需要管理员审批」则加入前 MUST 经管理员批准。管理员发起的邀请 MUST NOT 受非管理员范围/审批限制（仍受 `OFF` 关闭约束）。

#### Scenario: Non-admin blocked outside scope

- **WHEN** 非管理员尝试邀请成员加入不在允许范围内的部门
- **THEN** 系统拒绝该操作

#### Scenario: Approval required before join

- **WHEN** 非管理员邀请且设置要求审批，被邀请人尝试加入
- **THEN** 在管理员批准前 MUST NOT 成为 `ACTIVE` 成员

### Requirement: App invite capability projection

系统 MUST 提供工作台可读的邀请能力投影（例如是否可邀请），供「+」菜单显隐。该投影 MUST NOT 向非管理员泄露不必要的完整设置细节（如全部策略内部字段可不返回）。

#### Scenario: Capability for menu

- **WHEN** 有效成员请求邀请能力
- **THEN** 响应包含是否可发起企业邀请，与当前设置及身份一致

### Requirement: Admin invite settings navigation

管理端 MUST 在成员与部门相关导航下提供「邀请设置」（或等价文案）入口，路由以 `/admin` 开头，且仅管理身份可进入。

#### Scenario: Open invite settings page

- **WHEN** 管理员打开邀请设置页并保存
- **THEN** 设置对后续工作台邀请行为立即生效（或在约定缓存窗口内生效）
