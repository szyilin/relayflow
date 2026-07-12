# 设计：在线状态 API（im-presence-api）

## 实现要点

```text
ImPresenceService.batchStatus(tenantId, requesterId, userIds)
├── 过滤：仅同租户有效成员（UserApi 或 tenant_user）
├── 逐个 RealtimeTransportApi.isUserOnline(tenantId, userId)
└── 返回 items[]

ImPresenceController
└── GET /app-api/im/presence/batch?userIds=1,2,3
```

## WS 推送（可选 MVP）

V1 可先 **仅 REST**；integrate 阶段加 connect/disconnect listener fanout `presence.updated` 至租户在线用户。

若实现：在 `WebSocketSessionListener` 已有 hook 处调用 `ImPresenceService.broadcastStatusChange`。

## 验证

```bash
./mvnw -pl relayflow-server -am compile
openspec validate im-presence-api --strict
```
