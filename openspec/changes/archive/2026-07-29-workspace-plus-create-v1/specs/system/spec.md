## ADDED Requirements

### Requirement: Org invite settings and multi-channel invites

system 域 MUST 实现租户邀请设置的持久化与管理端读写，以及组织邀请实体（多渠道：手机号、链接、二维码、邀请码）的创建、校验、消费、撤销与过期。既有 `POST /admin-api/system/user/invite` MUST 委托到新邀请服务或提供等价行为，避免管理端邀请入口行为回退。成员邀请过程真源 MUST 为邀请实体；成员结果真源 MUST 为 `sys_tenant_user`。

#### Scenario: Admin mobile invite via legacy path

- **WHEN** 管理员调用既有 `POST /admin-api/system/user/invite`（或新等价端点）且邀请功能未关闭
- **THEN** 系统创建有效组织邀请并最终可被目标用户接受为租户成员

#### Scenario: Register activates open mobile invites

- **WHEN** 用户以某手机号完成注册/激活流程且存在指向该手机号的未过期可消费邀请
- **THEN** 系统消费这些邀请并建立对应租户的有效成员关系

### Requirement: Member invite bot card uses invite id

对已有 ACTIVE 成员身份的用户发送的组织邀请触达（org-assistant 卡片）MUST 关联邀请实体 ID；接受操作 MUST 走邀请消费逻辑。Bot 投递失败 MUST NOT 回滚邀请创建成功（best-effort，与既有策略一致）。

#### Scenario: Accept bot invite card

- **WHEN** 被邀请人点击组织助手邀请卡的接受且邀请仍有效
- **THEN** 邀请被消费且该用户在邀请租户下成为 ACTIVE 成员
