## REMOVED Requirements

### Requirement: Workspace notification entry

**Reason**：产品取消 Rail 铃铛；业务触达统一在 `/app/messages` 的 Bot 会话中呈现（对齐飞书机器人私信模型）。  
**Migration**：移除工作台通知铃铛组件、未读角标与 notify 列表 UI；保留注册页 pending invite banner（非铃铛通道）。用户通过会话列表未读进入 bot_dm。

## ADDED Requirements

### Requirement: Workspace reaches users via IM bot conversations

The workspace product surface MUST present business reachability (invites, task due, approvals, etc.) as Bot conversations / messages under `/app/messages`, not as a separate notification bell inbox.

#### Scenario: No notification bell in shell

- **WHEN** an authenticated user opens the workspace shell
- **THEN** the rail MUST NOT render a notification-bell control backed by `infra_notify` or equivalent parallel inbox
- **AND** unread business reminders are visible via IM conversation list unread state for bot_dm (once Bot foundation is integrated)
