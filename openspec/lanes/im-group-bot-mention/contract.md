# API 契约：im-group-bot-mention（G2 · 群 @Bot）

> **状态**：草案（`im-bot-group-mention`）  
> **依赖**：[`im-group-bot`](../im-group-bot/contract.md)、`im-bot-runtime-platform`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

群消息 content 含结构化 `mention` 块时，落库成功后对**已挂载**的 Bot 调用 `BotIngress` → `BotRuntime`。真源为结构化块，不依赖纯文本 `@名字` 解析。

## Mention block schema

`content.blocks[]` 中可出现：

```json
{
  "type": "mention",
  "subjectType": "bot",
  "subjectId": "900003",
  "botCode": "task-bot",
  "text": "@任务助手"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `type` | ✅ | 固定 `mention` |
| `subjectType` | ✅ | V1 入站仅处理 `bot` |
| `subjectId` | 建议 | Bot id（字符串化雪花） |
| `botCode` | 建议 | 与 `subjectId` 至少其一 |
| `text` | 建议 | 展示文案，如 `@任务助手` |

文本消息仍须至少有一个 `type=text` 块（可与 mention 并存）。

### 发送示例

```json
{
  "conversationId": "301",
  "clientMsgId": "uuid",
  "type": "text",
  "content": {
    "version": 1,
    "blocks": [
      { "type": "mention", "subjectType": "bot", "botCode": "task-bot", "text": "@任务助手" },
      { "type": "text", "text": " 帮我查一下待办" }
    ]
  }
}
```

## 服务端行为

1. 用户消息先持久化、再向 **User** 成员推 `message.new`（现网路径不变；Bot subject 不进 fanout）。
2. 若会话为 `group`：解析 `mention` 且 `subjectType=bot` → 过滤本群 Bot 成员 → 对每个命中调用 `BotIngress.onInbound`（事务提交后 / best-effort）。
3. 未挂载的 Bot mention：**忽略**，不调 Runtime。
4. Ingress/Runtime 失败：**不回滚**用户消息。

## 前端

| UI | 行为 |
|----|------|
| 群输入栏 | 可从群内 Bot 成员插入 mention |
| 气泡 | 渲染 `@BotName` 高亮；其余 text 照常 |

## V1 不在范围

- 纯文本 `@名字` 兜底解析
- @User 入站
- 富文本编辑器
