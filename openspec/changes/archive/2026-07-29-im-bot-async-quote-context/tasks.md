## 1. IM 读 API 与权限

- [x] 1.1 新增 `ImConversationReadApi` + DTO（getMessage / listNearby / listHistory）与 `ImReadPrincipal`
- [x] 1.2 `im-biz` 实现：成员/Bot 成员校验、会话白名单、条数硬顶；禁止越权返回

## 2. 异步 Bot 派发

- [x] 2.1 bot_dm / 群 @Bot `afterCommit` 改为异步（线程池或 `@Async`）；同会话串行（可选但建议）
- [x] 2.2 确认发送 HTTP 落库即返回；typing 仍包裹 Bot 处理；失败不回滚用户消息

## 3. 引用 reply_to

- [x] 3.1 发送 API 接受 `replyToMsgId`，写入 `reply_to_msg_id`，校验同会话
- [x] 3.2 列表/推送返回 `replyTo` 摘要 VO；前端气泡引用块 + 输入区单选预览（可取消）

## 4. Carrier 上下文装配

- [x] 4.1 bot_dm：入站前经读 API 注入最近 N 条；群 @Bot：前后各 M=5；配置项 + 字符上限
- [x] 4.2 有 `reply_to` 时附引用摘要到入站文本/结构化字段；`AgentCarrierInbound` 映射非空 history

## 5. Agent im.* 技能

- [x] 5.1 BuiltinSkill：`im.read_message` / `im.read_nearby` / `im.read_history` → 仅调 `ImConversationReadApi`
- [x] 5.2 Carrier 执行上下文绑定 conversationId；模型传其他会话 ID 拒绝；默认技能挂到 carrier Agent（或文档说明启用方式）

## 6. 群 typing 文案

- [x] 6.1 前端群聊 typing 用成员/Bot 显示名；bot_dm 可保留「对方正在输入…」

## 7. 验证

- [x] 7.1 `./mvnw -pl relayflow-server -am compile`；`cd web && pnpm build && pnpm typecheck`；`openspec validate im-bot-async-quote-context --strict`
