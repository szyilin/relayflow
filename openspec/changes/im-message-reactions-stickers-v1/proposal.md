## Why

消息交互基线（`im-message-ux-v1`）补齐右键 / 预览 / 发送手感后，仍缺飞书式表情回应、转发与表情包，协作表达与消息流转不完整。

## What Changes（规划 · 本 change 待深化后实现）

- **表情回应（reaction）**：消息级 emoji 回应聚合；右键 / 悬停快捷入口
- **转发**：单条转发至其它会话（权限与审计另定）
- **表情包系统**：系统 / 租户表情包目录、发送 sticker 消息类型、选择器 UI
- **API + 表**：reaction / sticker 持久化与实时推送

## Capabilities

### New Capabilities

- `im-message-reaction`: 消息表情回应
- `im-message-forward`: 消息转发
- `im-sticker-pack`: 表情包目录与发送

### Modified Capabilities

- `im`: 消息类型与实时事件增量（实现前补 delta）

## Impact

- `web/` 消息页与选择器；`relayflow-module-im` 表与 API；可能 `infra` 文件存贴纸资源
- **依赖**：建议先完成并 archive `im-message-ux-v1`
- 本目录目前仅 proposal 占位；实现前须补 design / specs / tasks 并 `openspec validate`
