## REMOVED Requirements

### Requirement: Approval notification producer contract

**Reason**：业务触达写真源已迁移至 IM Bot（`approval-bot` + `ImBotApi`）；`NotifyInboxApi` / `infra_notify` 类型目录已拆除，bpm 不再依赖 infra 通知目录。

**Migration**：待办触达见同 change `specs/bpm`「Approval pending bot delivery」。
