## Context

组织级大模型连接供 Agent `modelRef` 引用。**依赖** `agent-module-scaffold` 已合入。密钥对齐存储设置：`AesGcmEncryptor`；响应仅 `apiKeyConfigured`。

## Goals / Non-Goals

**Goals:** 表 `agent_llm_connection`；管理端 CRUD + 连通性测试；权限种子；工作台只读已启用模型列表；admin 页 `/admin/infra/llm`。

**Non-Goals:** AgentDef/Chat；非 OpenAI 兼容协议。

## Decisions

### D1 — 表在 agent 域

`agent_llm_connection`；列 `code` 租户内唯一，即 **modelRef**。

### D2 — `openai_compatible`

字段：`base_url`、`api_key_enc`、`model_name`、`name`、`enabled`。连通性：HTTP GET `{normalizedBaseUrl}/models`，Header `Authorization: Bearer …`。

### D3 — 权限

`agent:llm:query|update|test` → `super_admin`。

### D4 — API 见下表

| Method | Path | Perm |
|--------|------|------|
| GET | `/admin-api/agent/llm/connection/list` | query |
| GET | `/admin-api/agent/llm/connection/get?id=` | query |
| POST | `/admin-api/agent/llm/connection/create` | update |
| PUT | `/admin-api/agent/llm/connection/update` | update |
| DELETE | `/admin-api/agent/llm/connection/delete?id=` | update |
| POST | `/admin-api/agent/llm/connection/test` | test |
| GET | `/app-api/agent/llm/model/list-enabled` | 已登录成员 |

### D5 — DO 经 codegen

Flyway → 扩展 codegen 支持 `agent` → `./scripts/codegen.sh --module agent --tables agent_llm_connection` → diff 合入。

### D6 — UI

对照 `/admin/infra/storage`；导航「模型设置」；`apiKey` 只写不回显。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 各厂商 `/models` 路径差异 | 规范化 baseUrl（去尾 `/`，若无 `/v1` 则补全尝试一次） |
| 无真实 Key 的 CI | 单测 mock HTTP；手工测连通性 |

## Open Questions

- （无）
