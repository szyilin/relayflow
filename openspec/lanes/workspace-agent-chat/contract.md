# Lane contract：workspace-agent-chat（流式 + parts）

> 由 `workspace-agent-chat-ui-web` 起草；`workspace-agent-chat-stream-api` 按本契约实现。  
> 既有同步契约见 [workspace-agent/contract.md](../workspace-agent/contract.md)。

## 目标

工作台智能体会话支持：

1. SSE 流式输出（text / reasoning / tool）
2. 消息 `parts[]` 落库与回放
3. 前端 Nuxt UI Chat（`UChatMessages` / `UChatReasoning` / `UChatTool` / `Comark`）

**铁律**：本路径只写 `agent_session*`，不写 `im_message`。

## 鉴权

与现有 app-api 相同：登录 JWT；会话须属当前用户。

## 流式发送

`POST /app-api/agent/session/send/stream`

Request（JSON body）:

```json
{
  "sessionId": "…",
  "content": "用户输入"
}
```

Response：`Content-Type: text/event-stream`

每条 SSE `data:` 行为 JSON 事件（UTF-8）：

| type | 字段 | 说明 |
|------|------|------|
| `message.user` | `id`, `role`, `seq`, `content`, `parts?` | 用户消息已落库（可首包） |
| `reasoning-delta` | `text` | 推理增量（模型支持时） |
| `reasoning-end` | — | 推理结束 |
| `tool-start` | `toolCallId`, `toolName`, `input?` | 工具开始 |
| `tool-result` | `toolCallId`, `toolName`, `output` | 工具结果 |
| `text-delta` | `text` | 助手正文增量 |
| `message.assistant` | `id`, `role`, `seq`, `content`, `parts` | 助手消息最终态 |
| `done` | — | 流结束 |
| `error` | `message`, `code?` | 可恢复/终态错误 |

示例：

```text
data: {"type":"message.user","id":"1","role":"user","seq":1,"content":"你好","parts":[{"type":"text","text":"你好"}]}

data: {"type":"reasoning-delta","text":"用户在打招呼…"}

data: {"type":"tool-start","toolCallId":"c1","toolName":"task.search","input":"{}"}

data: {"type":"tool-result","toolCallId":"c1","toolName":"task.search","output":"[]"}

data: {"type":"text-delta","text":"你好"}

data: {"type":"message.assistant","id":"2","role":"assistant","seq":2,"content":"你好！","parts":[…]}

data: {"type":"done"}
```

取消：客户端中断 HTTP 请求；服务端应尽量停止生成并保留已落库用户消息（助手消息策略由 `-api` design 定稿）。

## Parts 模型

```ts
type AgentMessagePart =
  | { type: 'text'; text: string }
  | { type: 'reasoning'; text: string }
  | { type: 'tool'; toolCallId?: string; toolName: string; input?: string; output?: string }
```

- `content`：助手可见正文纯文本（text parts 拼接），兼容旧客户端
- `parts`：完整轨迹；历史回放优先用 `parts`

## 历史消息（增量）

`GET /app-api/agent/session/messages?sessionId=`

每条消息在现有字段上 **增加可选** `parts`：

```json
{
  "id": "…",
  "role": "assistant",
  "content": "你好！",
  "seq": 2,
  "parts": [
    { "type": "reasoning", "text": "…" },
    { "type": "tool", "toolName": "task.search", "output": "…" },
    { "type": "text", "text": "你好！" }
  ]
}
```

无 `parts` 时前端将 `content` 映射为单一 text part。

## 落库（`-api` 实现）

建议 Flyway：`agent_session_message` 增加 `parts_json`（JSONB/TEXT，可空）。  
写入：`content` + `parts_json`；旧行 `parts_json` 为空。

## 兼容

- 保留 `POST /app-api/agent/session/send`（同步）至 integrate 后再评估废弃
- 工作台主路径切到 `/send/stream`

## curl 冒烟（`-api` 后）

```bash
curl -N -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{\"sessionId\":$SID,\"content\":\"你好\"}" \
  http://localhost:8080/app-api/agent/session/send/stream
```
