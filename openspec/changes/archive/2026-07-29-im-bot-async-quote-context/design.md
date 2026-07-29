## Context

Carrier 入站已打通（`BotRuntime` → `AgentCarrierInboundApi` → `AgentRuntime.run`），但：

1. `ImMessageServiceImpl` 在 `afterCommit` **同步**跑完整 Bot/LLM，HTTP 不返回 → 前端乐观态「发送中」卡死；typing 另路 WS，出现「发送中 + 对方正在输入」并存。
2. `AgentCarrierInboundApiImpl` 固定 `history(List.of())`，Bot 无短期记忆。
3. 表字段 `reply_to_msg_id` 已存在但未产品化；产品要求微信式单选引用（非 `@`、非多选）。
4. Agent 读 IM 须走 `*-api`，工具层强制会话范围，防模型越权。

约束：禁止 `*-biz → *-biz`；Skill 只调 `im-api`；V1 无新外部依赖、预期无 Flyway。

## Goals / Non-Goals

**Goals:**

- 发送 HTTP：落库 + 实时推送后立即返回；Bot 异步派发；等待态靠 typing
- 单选引用：写/读 `reply_to_msg_id` + UI（输入预览可取消、气泡内引用块）
- bot_dm：服务端注入最近 N 条进 carrier history（默认 N≈20，可配置上限）
- 群 @Bot：当前句 + 前后各 M=5；提示词引导更早走工具
- `ImConversationReadApi` + Agent `im.*` 技能；绑定当前会话，拒绝跨会话
- 群 typing 展示用户/Bot 显示名

**Non-Goals:**

- 多选引用、向量检索、全量历史默认注入
- Agent 直连 IM Mapper / 人端 HTTP Controller
- 流式 Bot 消息气泡（仍整条回复）
- 修改工作台 Agent Session 历史模型

## Decisions

### D1 — 异步派发方式

**选择**：`afterCommit` 提交到应用内线程池（或现有 `@Async`），不阻塞发送事务/HTTP。

**备选**：消息队列 — V1 过重；保持同步 — 用户已否决。

**细节**：失败打日志 + 可选一条 bot 错误文案；typing finally 清除；长任务可续期 typing TTL（沿用既有）。

### D2 — 读能力归属

**选择**：真能力在 `im-api`（`ImConversationReadApi`）；Agent Skill 薄适配。

**备选**：Agent 内复制查询 — 违反边界且权限易漏；复用人端 Controller — 契约与 Bot 调用方身份不匹配。

**Principal**：服务端构造 `ImReadPrincipal`（tenantId、userId 或 botId、allowedConversationId）；模型传入的 conversationId 必须等于绑定值，否则拒绝。

### D3 — 默认历史 vs 工具

**选择**：默认短期历史由 **入站管道** 调同一读 API 装配；工具留给引用展开 / 再往前翻。

**默认参数（可配置，写进 properties）**：

| 场景 | 注入 |
|------|------|
| bot_dm | 触发消息之前最近 N 条（建议 20，硬顶 50） |
| 群 @Bot | 触发 seq 前后各 M=5（不含/含触发句以实现为准，须去重） |

入站 user 文本：当前句 + 若有 `reply_to` 则附「引用了 messageId=… / 发送者 / 摘要」。

### D4 — 引用数据模型

**选择**：真源 `im_message.reply_to_msg_id`；列表/推送附带 `replyTo` 摘要 VO（senderName + preview 截断），避免前端二次请求。

**备选**：仅 content_json 内嵌 — 与列冲突、难校验同会话。

**校验**：被引消息必须同 `conversation_id`、未删；V1 单选。

### D5 — Skill 代码

仿 `TaskListDueSkill`：`im.read_message` / `im.read_nearby` / `im.read_history`；Carrier 入口设置 ThreadLocal（或等价）上下文，工具结束清理。

### D6 — 群 typing 文案

payload 已有 userId/botId；前端群聊用成员/Bot 名；bot_dm 可保留「对方正在输入…」。

## Risks / Trade-offs

- **[Risk] 异步后用户连发多条** → Mitigation：按会话串行化 Bot 入站队列，或允许并行但提示词说明「可能乱序」；V1 优先同会话串行。
- **[Risk] 模型伪造 conversationId** → Mitigation：工具忽略/拒绝与绑定 ID 不符的参数；ACL 二次校验成员关系。
- **[Risk] 默认 N/M 过大撑爆上下文** → Mitigation：条数 + 字符/token 双上限；超限截断旧消息。
- **[Risk] 引用摘要过时** → Mitigation：摘要仅展示；展开以 messageId 再读。

## Migration Plan

1. 先合异步派发（体验立刻改善），再合读 API / history / 引用 UI（可同 PR 分 commit）。
2. 无库迁移；前端旧客户端忽略 `replyTo` 字段仍可用。
3. 回滚：同步派发开关或去掉 `@Async`；禁用 `im.*` skill；UI 隐藏引用入口。

## Open Questions

- 同会话 Bot 入站串行：是否必须 V1 就做？（建议：是，避免乱序回复）
- N/M 最终默认值是否写入 `application.yaml` 可见配置？（建议：是）
