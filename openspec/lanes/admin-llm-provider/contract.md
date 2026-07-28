# Lane contract：admin-llm-provider

## Admin APIs

Base: `/admin-api/agent/llm/connection`

| Method | Path | Auth | Body / Query |
|--------|------|------|----------------|
| GET | `/list` | `agent:llm:query` | — |
| GET | `/get?id=` | `agent:llm:query` | id |
| POST | `/create` | `agent:llm:update` | SaveReq |
| PUT | `/update` | `agent:llm:update` | SaveReq（须含 id） |
| DELETE | `/delete?id=` | `agent:llm:update` | id |
| POST | `/test` | `agent:llm:test` | TestReq |

### SaveReq

```json
{
  "id": null,
  "code": "openai-gpt-4o-mini",
  "name": "GPT-4o mini",
  "providerType": "openai_compatible",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-...",
  "modelName": "gpt-4o-mini",
  "enabled": true
}
```

- `apiKey` 创建必填；更新时可省略/空串以保留原密钥
- 响应 **永不** 返回明文 key；字段 `apiKeyConfigured: boolean`

### TestReq

```json
{ "id": 1, "baseUrl": "...", "apiKey": "optional-override" }
```

或无 id、带 baseUrl+apiKey 测未保存表单。

## App API

| Method | Path | Auth |
|--------|------|------|
| GET | `/app-api/agent/llm/model/list-enabled` | 已登录成员 |

返回 `{ code, name, modelName, providerType }[]`，仅 `enabled=true`。

## UI

- `/admin/infra/llm` · 导航「模型设置」· perm `agent:llm:query`

## curl 示例

```bash
# list
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/agent/llm/connection/list

# create
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"code":"demo","name":"Demo","baseUrl":"https://api.openai.com/v1","apiKey":"sk-x","modelName":"gpt-4o-mini","enabled":true}' \
  http://localhost:8080/admin-api/agent/llm/connection/create
```
