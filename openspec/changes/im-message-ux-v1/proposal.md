## Why

工作台 IM 消息交互与常见 IM（微信 / 飞书 / Slack）习惯不一致：操作依赖悬停三点左键菜单、图片不能放大、发送后输入框失焦、Enter 无法稳定换行。同时群内 @ 机器人回复时未固定 @ 触发人，用户难以感知「这是回给我的」；群助手类广播又不能一律 @。需要先补齐交互基线与平台级 mention 策略，表情回应 / 转发 / 表情包另立 change。

## What Changes

- **消息右键上下文菜单**（主入口）：在鼠标位置弹出操作列表；悬停三点菜单保留为辅入口，共用同一 action 源
- **媒体与附件**：图片单击灯箱预览；可预览类型（V1：图片、PDF）先预览，其余单击下载并给反馈
- **composer**：Enter 发送、Shift+Enter 换行；发送成功后输入框保持焦点
- **引用**：点击引用块跳转并短暂高亮原消息；右键菜单含「复制」「引用」等
- **群机器人回复 @ 策略（平台固定，非智能体决定）**：群会话内 Bot Runtime 回复默认前置 @ 触发用户；bot 目录增加可配置模式（`trigger` / `none`），供群助手广播等关闭 @
- **明确不做（本 change）**：表情回应、转发、表情包系统 → 后续 `im-message-reactions-stickers-v1`

## Capabilities

### New Capabilities

- `im-message-ux`: 工作台消息交互基线（右键菜单、媒体预览、composer 键盘与焦点、引用跳转）
- `im-bot-group-reply-mention`: 群内机器人回复的平台级 @ 触发人策略与 bot 配置

### Modified Capabilities

- `im`: 增量补充消息交互与 Bot Runtime 群回复 mention 行为（若与主规格交叉处需对齐）

## Impact

- **web/**：`pages/app/messages/index.vue` 及新建消息菜单 / 灯箱组件；可能触及 `ImAuthenticatedImage`、file 下载 API 封装
- **relayflow-module-im**：`sendBotReply` / `BotRuntimeImpl.persistReply`；`im_bot` 表新增列（Flyway）；DO/codegen diff 合入
- **部署**：需跑 Flyway；无 Compose 变更
- **回滚**：前端可回退；DB 列可保留无用；mention 行为回退为纯文本回复
