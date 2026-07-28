# Tasks：im-bot-agent-carrier-v1

## 0. 深化

- [x] 0.1 冻结 `ImBotProvisioningApi`：provision / retire / updateProfile
- [x] 0.2 入站契约：`AgentCarrierInboundApi` + bot_dm / 群 @ → Runtime → sendBotReply（不写 Session）

## 1. IM

- [x] 1.1 Flyway `owner_agent_id` + Provisioning API（type=tenant 创建/retire）
- [x] 1.2 BotRuntime 转发 Agent；bot_dm 入站接线

## 2. Agent

- [x] 2.1 创建/删除挂钩 + `AgentCarrierInboundApi` 实现
- [x] 2.2 compile + 测试
