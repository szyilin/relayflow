## Why

`workspace-agent-chat-ui-web` 已用 Nuxt UI Chat + Mock 流式演示 parts；`workspace-agent-chat-stream-api` 已提供真实 `POST /send/stream` 与 `parts_json`。本期去掉 Mock，让工作台主路径对接真实 SSE，完成流式 Chat 闭环。

## What Changes

- Store：`sendMessage` 改走 `POST /app-api/agent/session/send/stream`（fetch + ReadableStream / EventSource 风格解析）
- 删除或停用 `USE_MOCK_AGENT_STREAM` 与 `mockAgentStream.ts`（及 preamble + sync `/send` 双轨）
- 历史消息继续用 `GET .../messages`，消费可选 `parts`
- 保留同步 `POST /send` 作兼容（前端主路径不再调用）
- 更新对接看板：web/api → done / archived 说明

## Capabilities

### New Capabilities

- （无独立新能力名；行为落在已有 lane contract 的联调闭环）

### Modified Capabilities

- `workspace-agent-chat-ui`：发送路径从 Mock 改为真实 SSE（requirement 级）

## Impact

- **web/**：`stores/agent.ts`、`api/app/agent.ts`、删除 `composables/mockAgentStream.ts`
- **Java**：本期原则上不改（已由 stream-api 交付）
- **回滚**：可临时恢复 Mock 开关；后端 `/send` 仍可用
