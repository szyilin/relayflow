## Context

`getOrCreateDirectConversation` 当前拒绝 self。成员唯一约束禁止对同一 user 插两行。

## Goals / Non-Goals

**Goals:** self-DIRECT 可创建/列表/发消息；名片可进；无语音视频。

**Non-Goals:** 独立「文件传输助手」Bot；多端必达推送改造。

## Decisions

### D1. 单成员 DIRECT，不新造类型

`peer==me` 时 `low=high=me`，只 `createMember` 一次。

### D2. 列表 peer 回退为本人

`loadPeerUserIds`：无其他成员且 `low==high==me` → `peerUserId=me`。

### D3. UI

self 名片仅「消息」；语音/视频仍仅 peer。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 标题像普通私聊 | 用自己昵称即可（微信感） |
