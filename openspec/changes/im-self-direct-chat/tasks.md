## 1. 后端

- [x] 1.1 `getOrCreateDirectConversation` 允许 peer==self；单成员；复用 low=high
- [x] 1.2 `loadPeerUserIds`：self-direct 将 peer 解析为本人

## 2. 前端

- [x] 2.1 `WorkspaceBusinessCard` self 显示「消息」、隐藏语音视频；emit message
- [x] 2.2 通讯录/Rail 名片对 self 接通 `openDirectChat`

## 3. 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 `cd web && pnpm build && pnpm typecheck`
- [x] 3.3 `openspec validate im-self-direct-chat --strict`
