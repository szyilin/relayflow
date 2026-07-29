## Why

消息会话缺少「正在输入」反馈；对方打字时界面无感知，对话节奏偏生硬。在线 presence 已落地（REST batch），但 typing 在 `im-presence` contract 中明确排除。本期补上会话级 ephemeral typing，走已有 WebSocket `domain=presence` 通道。

## What Changes

- 工作台消息页：私聊/群聊标题区展示「正在输入…」；输入框防抖上行 start/stop
- WebSocket：`domain=presence` 上行 `typing.start` / `typing.stop`；下行 `typing.updated`
- 后端：校验会话成员、Redis TTL 暂存、向会话其他在线成员 fanout（不含发送者）
- 起草 `openspec/lanes/im-typing-indicator/contract.md`；更新对接看板
- **不**改在线状态 REST；**不**做 bot_dm / 会话列表预览

## Capabilities

### New Capabilities

- `im-typing-indicator`: IM 会话正在输入指示（WS ephemeral + 消息页 UI）

### Modified Capabilities

- （无已归档主规格增量；以 lane contract + 本 change delta 为准）

## Impact

- **web/**：`useImWebSocket`、消息页 header/composer、可选 `stores/typing.ts`
- **Java**：`im-biz` presence handler（替换 NoOp 或新增 typing 分支）、Redis TTL、RealtimeTransport fanout
- **infra**：复用现有 Redis / WS；无 Flyway
- **回滚**：停用 handler + 隐藏 UI；无持久化副作用
