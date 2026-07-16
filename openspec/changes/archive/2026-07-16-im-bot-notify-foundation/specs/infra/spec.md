## REMOVED Requirements

### Requirement: Notify inbox persistence

**Reason**：业务触达写真源迁移至 IM Bot（`im_message` / `bot_dm`）；开发期 `0.x` 硬切删除 `infra_notify`。  
**Migration**：不再持久化站内通知行；历史数据不迁移。产方改调 `ImBotApi`。

### Requirement: NotifyInboxApi cross-module push

**Reason**：跨域业务触达入口改为 `ImBotApi`（`im-api`）。  
**Migration**：删除 `NotifyInboxApi`；`system-biz` / `task-biz` 等改为依赖 `im-api`。

### Requirement: Workspace notify inbox APIs

**Reason**：取消独立通知收件箱 REST；用户从 `/app/messages` 阅读 Bot 消息。  
**Migration**：删除 `/app-api/infra/notify/*`；前端拆除铃铛与 notify store。

### Requirement: Notify WebSocket domain (optional V1)

**Reason**：`domain=notify` 不再作为业务触达推送通道；实时统一走 `domain=im`。  
**Migration**：移除对 `notify.new` 的业务义务；可选清理枚举或保留无业务绑定的 no-op（不得再写真源）。
