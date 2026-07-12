# API 契约：im-presence

> **状态**：已联调（integrate done）  
> **起草**：`im-presence-web` change  
> **架构**：[`im-platform-foundation` design](../../changes/archive/2026-07-12-im-platform-foundation/design.md) domain=presence

## 背景

消息页与通讯录右侧栏展示组织成员 **在线/离线** 状态。V1 基于 WebSocket 连接 + Redis/内存在线表（`RealtimeTransportApi.isUserOnline`），不持久化 presence 历史。

## 鉴权

Bearer JWT；有效组织成员。

## GET /app-api/im/presence/batch

批量查询用户在线状态（通讯录、消息侧栏用）。

**Query**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `userIds` | 是 | 逗号分隔用户 ID，最多 50 个 |

**Response `data`**：

```json
{
  "items": [
    { "userId": "100", "online": true },
    { "userId": "102", "online": false }
  ]
}
```

**规则**：

- 仅返回 **同租户** 内用户
- 忽略非本租户 ID（不报错）

## WebSocket

### 上行（可选 V1）

客户端连接后无需额外 heartbeat（框架已有 WS 会话）。

### 下行：`presence.updated`

用户上线/下线时，向 **同租户订阅者** fanout（V1 MVP：仅推给曾查询过 batch 的在线用户，或简化为全租户在线用户广播 — 见 design）。

```json
{
  "domain": "presence",
  "type": "updated",
  "payload": {
    "userId": "102",
    "online": true
  }
}
```

V1 MVP 可 **仅 REST batch 轮询**（30s），WS 推送在 `-api` 可选。

## 前端映射

| UI | API |
|----|-----|
| 消息页 Aside「活跃状态」 | batch + 对端 direct peer |
| 通讯录 Aside | batch 当前列表成员 |

## V1 不在范围

- 忙碌/离开/custom status
- 输入中 typing
- 最后活跃时间精确展示
