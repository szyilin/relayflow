## Context

- WS 路由已有 `domain=presence`，但 `NoOpPresenceHandler` 丢弃全部事件；在线状态仅 REST batch。
- 消息实时走 `domain=im`（`message.*` / `read.updated`）；typing 按平台 foundation 归 **presence**，避免污染消息域。
- 参考 fanout：`read.updated` → `listOtherMemberUserIds` + `RealtimeTransportApi.sendToUsers`。

## Goals / Non-Goals

**Goals:**

- 私聊：对端 typing 时 header 显示「对方正在输入…」
- 群聊：显示最多 2–3 个昵称「A、B 正在输入…」
- 上行防抖；空内容 / 失焦 / 发送后 stop
- Redis TTL（约 5s）自动过期；过期可下行 `typing=false` 或依赖客户端本地 TTL
- 纵向切片：UI+contract → API → 去 Mock 联调

**Non-Goals:**

- bot_dm typing、会话列表角标预览
- 忙碌/离开等自定义 presence
- 修复 deferred 的 `presence.updated` 在线推送（另开）
- DB 持久化 typing

## Decisions

### D1 — 事件形状

```text
↑ domain=presence  type=typing.start|typing.stop  payload={ conversationId }
↓ domain=presence  type=typing.updated           payload={ conversationId, userId, typing }
```

### D2 — 状态只存 Redis

Key：`im:typing:{tenantId}:{conversationId}:{userId}`，TTL ≈ 5s；start 续期；stop 删除并 fanout `typing=false`。

### D3 — 前端优先

- `-web`：Mock 本机模拟对端 typing；contract 冻结
- `-api`：真 handler + Redis + fanout
- integrate：去 Mock

### D4 — UI 挂载点

消息页 `headerSubtitle()`；composer `input` 防抖 300ms。

## Risks / Trade-offs

- [多实例 Redis 必须] → 与现有 WS Redis fanout 一致
- [群成员多 fanout 大] → V1 仅会话成员在线用户；不做全租户广播
- [客户端漏 stop] → TTL 兜底

## Migration Plan

无 schema。回滚删 handler / 关 UI。
