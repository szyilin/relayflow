## Context

工作台消息页（`web/src/pages/app/messages/index.vue`）已有悬停三点菜单（引用 / 撤回 / 编辑）、引用发送、`ImAuthenticatedImage` 内联图、文件点击下载。群内 @ 机器人走 `GroupBotMentionDispatcher` → `BotRuntime` → `ImMessageService.sendBotReply`，回复目前仅为纯 text block，不含 @ 用户。

约束：前端 Nuxt UI；管理面 RBAC 不作用于 `/app`；Bot Runtime 回复不得交给智能体决定是否 @ 人。

## Goals / Non-Goals

**Goals:**

- 右键主入口 + 三点辅入口的统一消息操作模型
- 图片 / PDF 预览，其它文件下载反馈
- Enter 发送、Shift+Enter 换行，发送后保持焦点
- 引用块跳转高亮
- 群 Bot Runtime 回复默认 @ 触发人；bot 级可关

**Non-Goals:**

- 表情回应、转发、表情包（另 change）
- 线程回复、消息多选
- Office 在线预览、视频播放器
- 改 `ImBotApi.send` 的 bot_dm 投递语义（本 change 只管会话内 Runtime 回复；若将来群广播 send API，复用同一 mention 模式字段）

## Decisions

### D1 · 右键菜单实现

- 使用 `@contextmenu.prevent` 在气泡 / 消息行打开 Nuxt UI `UDropdownMenu` 或自绘定位菜单（`fixed` + 鼠标坐标），与悬停三点共用 `buildMessageActions(msg)`。
- 触控：本 change 不做长按；桌面优先。
- **保留**三点，视觉可略弱（更小 / 仍 hover 显），不删除。

### D2 · 媒体预览

- 图片：单击 → 全屏灯箱（遮罩 + ESC 关闭 + 下载按钮）。
- PDF：单击 → 模态内 `<iframe>` / `object` 拉 authenticated blob URL；失败则降级下载。
- 其它 MIME：单击下载 + toast。
- 右键对媒体另给「下载 / 另存为」「引用」。

### D3 · Composer 键盘与焦点

- `UTextarea`：`Enter` 无 Shift → `preventDefault` + send；`Shift+Enter` 插入换行。
- send 成功后 `nextTick` 对 textarea `focus()`；失败同样保留焦点与草稿。

### D4 · 群回复 @ 策略（平台层）

```text
BotRuntime.persistReply
  → 若 conversation.type=group
     且 bot.group_reply_mention_mode = 'trigger'（默认）
     且 triggerUserId 非空
  → sendBotReply 内容 blocks 前插入 mention(user) + 可选空格 text
  → 否则保持纯文本
```

- **新列** `im_bot.group_reply_mention_mode`：`trigger` | `none`，默认 `trigger`。
- 种子：现有系统广播倾向 bot（若仅 bot_dm 可仍默认 trigger，无害）；文档约定「群助手 / 定时整理」类创建时设 `none`。
- **禁止** Agent / LLM 决定是否 @；Agent 只返回正文，平台包装 mention。
- 单聊 `bot_dm`：不插 @（对方即会话对端）。

### D5 · 引用跳转

- 点击引用块 → `scrollIntoView` 目标 `data-msg-id`；加短暂高亮 class；消息不在当前窗口则先拉取历史（若已有分页能力则用；否则 toast「原消息不在当前列表」）。

## Risks / Trade-offs

- [右键与浏览器原生菜单冲突] → `preventDefault` 仅在消息气泡区域
- [PDF iframe 鉴权] → 与图片相同走 blob URL，勿直接塞需 Cookie 的 URL
- [mention 重复] → 若正文已含同一 user mention，平台仍固定前置一块（可接受）；后续可去重
- [Flyway 改表] → 仅加列 + 默认值，低风险

## Migration Plan

1. 合入前端 + 后端；跑 Flyway
2. 回滚：回退代码；列可留

## Open Questions

- 无（产品拍板已收入 Decisions）
