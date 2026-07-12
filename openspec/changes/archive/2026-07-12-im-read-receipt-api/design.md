# 设计：已读回执 API（im-read-receipt-api）

## 实现要点

```text
GET /conversation/read-status
└── 查询 im_conversation_member where conversation_id
    └── 返回 userId + readSeq（不含敏感字段）

markConversationRead (existing)
└── 更新 read_seq / unread_count 后
    └── RealtimeTransportApi.sendToUsers(其他成员, envelope read.updated)
```

## ImRealtimeTypes

新增常量 `READ_UPDATED = "read.updated"`。

## 验证

```bash
./mvnw -pl relayflow-server -am compile
# 用户 A 发消息，用户 B mark read，A 收到 WS
openspec validate im-read-receipt-api --strict
```
