## Why

地基已落地 `subject_type` 与 Bot 目录，但群聊 API 仍只加人、成员列表只返回 User。要对齐飞书「群可挂机器人」，需先完成 G1：群挂载/移除 Bot，并为后续 @Bot（G2）铺路。

## What Changes

- 群管理 API：向 `group` 会话添加 / 移除 Bot 成员（`subject_type=bot`）
- 群成员列表与会话侧栏：区分并展示 Bot（名称/头像来自 `im_bot`）
- 挂载时可选写入 `sender_type=system` 环境文案（如「某某机器人加入了群聊」）
- 权限：仅群 owner/admin（或与现有加人策略一致的授权成员）可挂载/移除
- **不**实现 @ 解析、Ingress、Runtime、可交互卡片

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `im`：群成员模型与 REST 支持 Bot 挂载/移除与列表展示；拆分地基「群 Bot 分期」中 G1 行为

## Impact

- `im-biz`：`ImGroupService` / Controller / VO；成员插入走 `subject_type=bot`
- `web/`：`/app/messages` 群成员面板展示 Bot；提供添加/移除入口（可开子 lane 或同切片）
- 依赖：`im-bot-notify-foundation`（schema G0 已就绪）
- 后续：`im-bot-group-mention`（G2）、`im-bot-runtime-platform`（G3）
