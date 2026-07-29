# API 契约：im-typing-indicator

> **状态**：实现中（`im-typing-indicator-v1`）  
> **架构**：WS `domain=presence`；与 [im-presence](./im-presence/contract.md) 同域，但 typing 为独立事件（在线 batch REST 不变）

## 目标

私聊 / 群聊会话中展示「正在输入」；状态 ephemeral，不落库。

**不在范围**：`bot_dm`、会话列表预览、自定义 presence、在线 `presence.updated` 推送。

## 鉴权

Bearer JWT（WS 已鉴权）；上报者须为该会话成员。

## WebSocket

### 上行 `domain=presence`

| type | payload | 说明 |
|------|---------|------|
| `typing.start` | `{ "conversationId": "…" }` | 开始/续期输入中 |
| `typing.stop` | `{ "conversationId": "…" }` | 停止输入中 |

客户端：输入防抖约 300ms 发 `start`；清空 / 失焦 / 发送后发 `stop`。

### 下行 `domain=presence`

| type | payload |
|------|---------|
| `typing.updated` | `{ "conversationId": "…", "userId": "…", "typing": true\|false }` |

服务端向会话内 **其他用户** fanout（不含上报者）。非成员上报：静默忽略。

## 服务端状态

- Redis key：`im:typing:{tenantId}:{conversationId}:{userId}`
- TTL ≈ **5s**；`start` 续期；`stop` 删除并 fanout `typing=false`
- TTL 到期：删除 key 并 fanout `typing=false`（若实现到期回调；否则依赖客户端本地 TTL）

## 前端映射

| UI | 行为 |
|----|------|
| 消息页 header 副标题 | 私聊：「对方正在输入…」；群聊：最多 3 个昵称「A、B 正在输入…」 |
| Composer | 防抖上行；本地维护对端 typing map + 客户端 TTL 兜底 |

## 冒烟

1. 两浏览器登录不同用户，打开同一私聊  
2. A 输入 → B 标题出现正在输入；A 停止/发送 → 提示消失  
3. 群聊同上（多名输入时合并展示）
