# 提案：在线状态 API（im-presence-api）

## Why

[`im-presence-web`](../im-presence-web/proposal.md) 需要 batch 在线查询。`NoOpPresenceHandler` 须替换为真实实现或 im 模块提供 REST 封装 `RealtimeTransportApi.isUserOnline`。

## What Changes

- `ImPresenceController`：`GET /app-api/im/presence/batch`
- `ImPresenceService`：租户内 userIds 过滤 + 批量 isUserOnline
- （可选）WS connect/disconnect hook fanout `presence.updated`
- 替换/补充 `NoOpPresenceHandler` 文档说明

## Capabilities

### Modified Capabilities

- `im`：presence batch REST

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-im-biz` | Controller、Service |
| `relayflow-module-infra-api` | RealtimeTransportApi |
| `web/` | **不改** |

## 前置

- 契约：[`im-presence/contract.md`](../../lanes/im-presence/contract.md)

## 下一 change

- `im-presence-integrate`
