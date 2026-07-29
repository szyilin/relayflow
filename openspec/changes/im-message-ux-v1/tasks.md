## 1. OpenSpec / 校验

- [x] 1.1 `openspec validate im-message-ux-v1 --strict`

## 2. 消息交互基线（web）

- [x] 2.1 提取 `buildMessageActions(msg)`；右键上下文菜单（指针定位）为主入口；悬停三点降为辅入口并共用 actions
- [x] 2.2 右键/菜单增加「复制」；引用块可点击跳转并短暂高亮原消息
- [x] 2.3 图片单击灯箱预览（关闭 / 下载）；PDF 应用内预览；其它文件单击下载 + toast
- [x] 2.4 Composer 改为可多行：Enter 发送、Shift+Enter 换行；发送后保持焦点

## 3. 群机器人回复 @ 策略（im）

- [x] 3.1 Flyway：`im_bot.group_reply_mention_mode`（`trigger`|`none`，默认 `trigger`）；DO diff 合入
- [x] 3.2 `sendBotReply` / `BotRuntime.persistReply`：群 + `trigger` + triggerUserId 时平台插入 user mention（非 Agent）
- [x] 3.3 单元测试覆盖 trigger / none / bot_dm 不插 @

## 4. 验证

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 `./mvnw -pl relayflow-server -am compile`
