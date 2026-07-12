# 设计：已读回执 UI（im-read-receipt-web）

## Goals / Non-Goals

**Goals：**

- 单聊 outgoing 气泡：最后一行小字「已读」当 `msg.seq <= peerReadSeq`
- 进入会话 / 收到 `read.updated` 更新 peerReadSeq
- 群聊 V1：不展示 per-message 已读（保持现状）

**Non-Goals：**

- 群聊「已读 3/5」
- 已读时间戳 tooltip

## UI

```text
[ 我发的消息气泡 ]
           已读   ← text-xs muted，仅 direct + seq 满足时
```

## 数据流

```text
selectConversation → GET read-status → peerReadSeq
reportConversationRead（对方）→ WS read.updated → 更新 peerReadSeq
```

## Mock

`read-status` 404 → peerReadSeq = 当前 max seq（演示全部已读）

## 验证

```bash
cd web && pnpm build
openspec validate im-read-receipt-web --strict
```
