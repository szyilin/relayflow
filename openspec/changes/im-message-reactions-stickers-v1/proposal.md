## Why

消息交互基线（`im-message-ux-v1`）补齐右键 / 预览 / 发送手感后，仍缺飞书式表情回应、转发与表情包，协作表达与消息流转不完整。

## What Changes

- **表情回应（reaction）**：消息级 Unicode emoji 增删与聚合；右键 / 快捷栏入口；WS `reaction.updated`
- **转发**：单条转发至本租户其它可写会话（拷贝内容 + `forwardedFrom` 元数据；禁止转发已撤回与 card）
- **表情包**：系统表情包目录、`type=sticker` 消息、选择器发送（租户自定义上传另开 change）
- **API + 表**：`im_message_reaction` / `im_sticker_pack` / `im_sticker`；消息列表扩展字段
- **交付顺序**：同一 change 内 `-web` → `-api` → `-integrate`

## Capabilities

### New Capabilities

- `im-message-reaction`: 消息表情回应
- `im-message-forward`: 消息转发
- `im-sticker-pack`: 表情包目录与发送

### Modified Capabilities

- `im`: 消息类型 `sticker`、列表字段与实时事件增量

## Impact

- `web/`：消息页 reaction 条、转发选择器、sticker 面板；复用右键菜单 action 源
- `relayflow-module-im`：表、app-api、WS；`infra` 文件引用贴纸资源
- **依赖**：`im-message-ux-v1` 已实现（右键菜单可挂新 action）
- 部署：需跑 Flyway；系统 sticker 种子可选
