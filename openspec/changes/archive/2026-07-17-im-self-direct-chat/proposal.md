## Why

通讯录点自己无法发消息；后端拒绝 `peer == self`，名片 self 模式也隐藏消息。需要微信式「自己对自己」备忘会话：可发消息、无语音视频、不双泡。

## What Changes

- 允许创建单成员 DIRECT：`direct_peer_low = direct_peer_high = userId`
- 会话列表将 self-direct 的 peer 解析为本人
- 名片 self：显示「消息」，不显示语音/视频；接通 `openDirectChat(me)`
- 发消息/扇出/未读逻辑原则上不改（已跳过发送者）

## Capabilities

### New Capabilities

- `im-self-direct`: 自己对自己的单聊（备忘）行为

### Modified Capabilities

- （无主规格强制改名；名片行为随实现微调）

## Impact

- **im-biz**：`ImConversationServiceImpl`
- **web/**：`WorkspaceBusinessCard`、通讯录/Rail 名片入口、可选 im store 标题
- **无 Flyway**（现有唯一索引已支持 `low=high`）
