# Lane contract：workspace-agent（runtime-core API）

> 由 `agent-runtime-core-v1` 定稿；`workspace-agent-web` 按本契约接 UI。

## Agent 定义

Base: `/app-api/agent/def`（需登录；V1 仅本人 `owner_user_id`）

| Method | Path | Body / Query |
|--------|------|----------------|
| GET | `/list` | — |
| GET | `/get?id=` | id |
| POST | `/create` | SaveReq |
| PUT | `/update` | SaveReq（含 id） |
| DELETE | `/delete?id=` | id |

### SaveReq

```json
{
  "name": "数据分析师",
  "description": "可选",
  "systemPrompt": "你是…",
  "modelRef": "openai-gpt-4o-mini",
  "status": "active"
}
```

- `modelRef` 必须对应已启用的 `/app-api/agent/llm/model/list-enabled` 中的 `code`
- 不存 API Key

### Resp

```json
{
  "id": "…",
  "name": "…",
  "description": "…",
  "systemPrompt": "…",
  "modelRef": "…",
  "status": "active",
  "ownerUserId": "…",
  "visibility": "private"
}
```

## Session

Base: `/app-api/agent/session`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/create` | `{ "agentId": …, "title": optional }` → sessionId |
| GET | `/list` | 我的会话 |
| GET | `/get?id=` | 会话详情 |
| GET | `/messages?sessionId=` | 按 seq 升序 |
| POST | `/send` | `{ "sessionId", "content" }` → user + assistant 两条 |

### SendResp

```json
{
  "userMessage": { "id": "…", "role": "user", "content": "…", "seq": 1 },
  "assistantMessage": { "id": "…", "role": "assistant", "content": "…", "seq": 2 }
}
```

**铁律**：本路径只写 `agent_session*`，不写 `im_message`。

## Enabled models（已有）

`GET /app-api/agent/llm/model/list-enabled` → `{ code, name, modelName, providerType }[]`

## curl 冒烟

```bash
# 1) 先在管理端创建 modelRef 连接
# 2) 创建智能体
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"助手","systemPrompt":"简洁回答","modelRef":"demo","status":"active"}' \
  http://localhost:8080/app-api/agent/def/create

# 3) 开会话并发送
SID=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"agentId":AGENT_ID}' http://localhost:8080/app-api/agent/session/create | jq -r .data)
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"sessionId\":$SID,\"content\":\"你好\"}" \
  http://localhost:8080/app-api/agent/session/send
```

## 浏览器（待 web 切片）

工作台智能体页接上述 API；对话内容不出现在 `/app/messages`。
