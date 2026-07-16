## Why

Inbound 路径（群 @ / 未来 bot_dm 对话）需要统一的 Bot Runtime：按 `handler_kind` 分发。地基与架构草案已定 `platform` / `noop` 必做、`webhook` 占位；本切片把 SPI 与内置 handler 落地，避免 Ingress 悬空。

## What Changes

- 在 `im-api` / `im-biz` 定义 Bot Runtime SPI：`resolve(bot)` → 按 `handler_kind` 分发
- 实现 `noop`：消费入站、不回复；Outbound `ImBotApi.send` 不受影响
- 实现 `platform`：可注册内置 handler（首批可为 echo / 空实现占位，至少一个可验证通路）
- `webhook`：仅契约/枚举占位，**不**做 HTTP 回调实装
- Runtime 回复写入**同一会话**（群或 bot_dm），`sender_type=bot`，经既有消息落库 + WS（仅 User 成员）
- **不**与 Card Action Ingress 混用（卡片走独立 SPI，见 `im-bot-interactive-card`）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `im`：Bot Runtime SPI + `platform`/`noop`；明确 `webhook` 占位；Inbound 与 Outbound / Card Action 分离

## Impact

- `im-api`：Runtime / Handler 接口
- `im-biz`：Dispatcher、noop/platform 实现；与 Ingress 接线
- 依赖：地基 Bot 目录 `handler_kind`；建议在 `im-bot-group-mention` 前或同窗口合入
- 业务域：**不**在 task/bpm 实现「收 @」；厚逻辑听领域事件再 `ImBotApi.send`
