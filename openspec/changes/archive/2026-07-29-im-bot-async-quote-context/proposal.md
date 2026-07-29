## Why

IM 与 Agent carrier 联调后暴露三类体验/能力缺口：发消息 HTTP 同步等待 Bot 完整生成导致「发送中」卡死；carrier 入站不带会话历史导致 Bot「无记忆」；引用（微信式 reply）与群 @Bot 附近窗口未产品化。同时 Agent 侧若读 IM 历史，必须经 `im-api` 强权限，防止模型越权读其他会话。

## What Changes

- **Bot 异步派发**：用户消息落库并实时推送后立即返回 HTTP；Bot Runtime / Agent carrier 在事务提交后异步执行；等待态仅靠 typing（已有 `ImBotApi.setTyping`）
- **微信式单选引用**：启用已有 `reply_to_msg_id`；发送/列表/气泡与输入框预览（可取消）；V1 不做多选引用；非 `@` 语法
- **bot_dm 默认短期历史**：入站管道服务端注入最近 N 条（token/条数上限），拼入 carrier `history`；非全量 dump、非向量库
- **群 @Bot 附近窗口**：除当前句外自动带前后各 M 条（建议 M=5）；系统提示引导「更早内容用工具」
- **`ImConversationReadApi`（im-api）**：按消息 ID / 附近窗口 / 向前历史读取；调用方身份 + 会话成员与 bot 成员校验；强制会话白名单
- **Agent `im.*` BuiltinSkill**：薄适配调上述 API；执行上下文绑定当前 `conversationId`/`triggerUserId`，拒绝跨会话 ID
- **群 typing 文案**：群聊展示「某某正在输入…」；Bot 用 bot 显示名；避免一律「对方」

## Capabilities

### New Capabilities

- `im-conversation-read`：跨域可读会话消息契约（`ImConversationReadApi`）与强制授权范围

### Modified Capabilities

- `im`：异步 Bot 入站派发；单选 `reply_to` 引用产品化；@Bot/bot_dm 入站上下文装配（短期历史 / 附近窗口）；引用摘要入站
- `im-typing-indicator`：群聊 typing 展示发送者/Bot 显示名
- `agent`：carrier 注入 history；`im.*` 技能；工具层会话范围绑定；引用展开走读消息 API

## Impact

- **后端**：`relayflow-module-im-api/biz`（读 API、派发异步化、reply_to 读写、入站上下文装配）；`relayflow-module-agent-api/biz`（carrier history、BuiltinSkill、执行上下文）
- **前端**：`web/` IM 消息页（引用 UI、群 typing 文案）；发送契约带 `replyToMsgId`
- **库表**：`im_message.reply_to_msg_id` 已存在，预期 **无 Flyway**（若列表需冗余 preview 再议）
- **回滚**：异步派发可回退为同步（体验回退）；技能/读 API 可禁用；引用字段可忽略不展示
- **非目标**：多选引用；向量检索；Agent 直连 IM Mapper；让模型调人端 HTTP Controller；跨会话任意读
