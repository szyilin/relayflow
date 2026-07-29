## Why

工作台智能体 UI（`workspace-agent-chat-ui-web`）已按 Nuxt UI Chat + `parts[]` 与 Mock 流式跑通，并冻结了 `openspec/lanes/workspace-agent-chat/contract.md`。当前后端仍只有同步 `POST /send`，无法推送 text/reasoning/tool 增量，也无法落库 `parts`。本期实现 **真实 SSE + parts 落库**，供后续 `-integrate` 去 Mock。

## What Changes

- 新增 `POST /app-api/agent/session/send/stream`（`text/event-stream`），事件形状对齐 lane contract
- Flyway：`agent_session_message` 增加可空 `parts_json`
- Runtime：Spring AI `ChatClient.stream()` 产出 text-delta；工具调用尽量映射为 `tool-start` / `tool-result`；reasoning 在模型支持时透出，否则可省略
- `GET .../messages` 响应增加可选 `parts`；同步 `POST /send` **保留**（助手仍可只写 `content`，`parts_json` 可空或单 text part）
- 更新对接看板：本切片 `api → in_progress / done`

## Capabilities

### New Capabilities

- `workspace-agent-chat-stream`: 智能体会话流式发送（SSE）、消息 parts 落库与历史回放

### Modified Capabilities

- （无已归档 `openspec/specs/agent`；行为以本 change delta + lane contract 为准）

## Impact

- **Java**：`relayflow-module-agent`（controller / service / runtime / DO / VO）；`relayflow-server` Flyway
- **web/**：本期不改（`-integrate` 接真实 SSE）
- **deploy/**：无
- **回滚**：删除 stream 端点 + 忽略 `parts_json` 列即可；旧客户端继续用 `/send`
