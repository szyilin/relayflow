# 提案：已读回执 API（im-read-receipt-api）

## Why

[`im-read-receipt-web`](../im-read-receipt-web/proposal.md) 需要 peer `readSeq` 与实时更新。须在 `markConversationRead` 后 fanout `read.updated`，并新增 `GET /conversation/read-status`。

## What Changes

- `ImConversationController`：`GET /read-status`
- `ImConversationService.getReadStatus()`
- `markConversationRead` 成功后 WS fanout `im.read.updated`
- 更新 [`contract.md`](../../lanes/im-read-receipt/contract.md) 状态为 ready

## Capabilities

### Modified Capabilities

- `im`：已读水位查询与 WS 推送

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-im-biz` | ConversationService、Controller |
| `web/` | **不改**（integrate 在 read-receipt-web 联调或单独小 integrate） |

## 前置

- 契约：[`im-read-receipt/contract.md`](../../lanes/im-read-receipt/contract.md)
- `im-read-receipt-web` ui_ready

## 说明

本切片无独立 `-integrate` change；`im-read-receipt-web` 在 API ready 后去 Mock 即可，或会话内联调勾选 web tasks。
