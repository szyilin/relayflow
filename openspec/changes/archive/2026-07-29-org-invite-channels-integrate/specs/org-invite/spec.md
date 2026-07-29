## MODIFIED Requirements

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
