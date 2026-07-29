## ADDED Requirements

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
