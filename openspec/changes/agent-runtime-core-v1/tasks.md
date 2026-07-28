# Tasks：agent-runtime-core-v1（亮点 · 已深化可 apply）

> 前置：`agent-module-scaffold`、`admin-llm-provider-v1`。  
> 参照：本地 grok-agent README、OpenClaw agent-runtime docs、`docs/dev/agent-architecture-draft.md`。

## 0. 契约

- [x] 0.1 起草 `openspec/lanes/workspace-agent/contract.md`（web 未开时由本切片定 API）
- [x] 0.2 `openspec validate agent-runtime-core-v1 --strict`

## 1. Schema

- [x] 1.1 Flyway：`agent_def` / `agent_session` / `agent_session_message`（`V0.1.0.54`）
- [x] 1.2 codegen diff 合入 DO/Mapper

## 2. Builder + Runtime

- [x] 2.1 `AgentBuilder`：base + persona；tools 钩子（可空）
- [x] 2.2 `LlmConnectionResolver`：modelRef → 解密连接参数
- [x] 2.3 `SpringAiAgentRuntime`：编程式 ChatClient；多轮历史；错误映射
- [x] 2.4 单测：Builder prompt 组装；`toOpenAiBaseUrl`

## 3. App API

- [x] 3.1 AgentDef CRUD（owner 范围）
- [x] 3.2 Session create/list/get/messages/send
- [x] 3.3 `./mvnw -pl relayflow-server -am compile` + agent-biz 单测

## 4. 文档

- [x] 4.1 更新 api-integration-board：runtime-core api **done**
- [x] 4.2 验收说明（见下）

### 验收注记

1. 管理端 `/admin/infra/llm` 配置并启用一条 OpenAI 兼容连接（记下 `code`）
2. 登录工作台，调用 `POST /app-api/agent/def/create`（`modelRef`=该 code）
3. `POST /app-api/agent/session/create` → `POST /app-api/agent/session/send`
4. 确认 `/app/messages` **无**对应会话；消息仅在 agent session API 可见
5. UI：待 `workspace-agent-web` / `integrate`
