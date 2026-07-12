# 设计：单聊联调（im-direct-chat-integrate）

## 数据流

```text
/app/messages
  → im store: GET conversation/list, message/list, POST message/send
  → useImWebSocket: ws /infra/ws?token=
       ↓ message.new → store.handleMessageNew
       ↑ ping (optional keepalive)
```

发送仍走 **REST**（ACK 在 HTTP 响应）；WS 负责 **他人消息推送** 与会话列表预览更新。

## 开发代理

```typescript
// vite.config.ts
'/app-api' → localhost:8080
'/infra/ws' → ws://localhost:8080 (ws: true)
```

## Store 变更

- 删除 `mocks/im` 引用与 `usingMock`
- API 层 `normalizeConversation` / `normalizeMessage`（Long → string）
- `handleMessageNew`：更新会话 preview/unread；活跃会话 append（去重 id/clientMsgId）

## 验证

```bash
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：/app/login → /app/messages；双账号可测 message.new
```
