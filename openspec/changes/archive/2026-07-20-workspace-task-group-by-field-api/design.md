## Context

接 group-by-field-web contract；母 change D4 / §4.2。

## Goals / Non-Goals

**Goals:** `group-move` 写回系统字段；鉴权同 `requireEditable`；子任务拒绝；status 不可清空。

**Non-Goals:** 服务端分桶查询端点（仍由 page + 前端分区）；改 `web/`；删 `board-move`。

## Decisions

1. `value`：字符串；空分组传 `null` 或 `"__empty__"`。
2. `dueTime`：按 `YYYY-MM-DD` 解析为当天 12:00 本地偏移（或保留既有时分若同日改桶仅换日）；清空设 null。
3. `assigneeId`：非空须为本租户有效成员；清空同时 `assignerId=null`；指派他人设 `assignerId=当前用户`（与 assign 一致）。
4. `status` + 可选 `beforeId`：缺省列尾 `boardRank`；有 `beforeId` 则插到该卡之前。
5. `board-move` 内部可继续独立实现；contract 注明新客户端优先 `group-move`。

## Risks

assignee 拖拽不发指派 Bot（本切片）；integrate 可对齐 notify。
