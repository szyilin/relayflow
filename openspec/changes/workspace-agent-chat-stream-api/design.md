## Context

`workspace-agent-chat-ui-web` 已交付 Chat UI + Mock stream + lane contract。后端 `AgentSessionServiceImpl.send` 同步调用 `SpringAiAgentRuntime.run()` → `ChatClient.call().content()`，消息仅 `content`。Spring AI **1.1.8** 支持 `ChatClient.stream()` → `Flux`。仓库内尚无 `SseEmitter` 先例。

契约真源：[openspec/lanes/workspace-agent-chat/contract.md](../../lanes/workspace-agent-chat/contract.md)

## Goals / Non-Goals

**Goals:**

- `POST /app-api/agent/session/send/stream` SSE，事件类型对齐 contract
- `parts_json` 落库；历史 `GET /messages` 返回可选 `parts`
- Runtime 流式 text；工具事件尽量可见；取消时保留已落库用户消息
- 保留同步 `/send`

**Non-Goals:**

- 前端去 Mock / 接 SSE（→ `workspace-agent-chat-integrate`）
- Tool approval、ChatPalette
- IM Carrier / Automation 改流式
- 强制所有模型输出 reasoning（无则跳过相关事件）

## Decisions

### D1 — MVC `SseEmitter`，不用 WebFlux 应用

- **选择**：Controller 返回 `SseEmitter`；服务在请求线程拿好 security/tenant 上下文后，在独立线程或 Reactor subscribe 中推事件
- **替代**：WebFlux 全栈 —— 与现有 Boot MVC 单体摩擦大

### D2 — 事务边界：流式期间不长持事务

1. 短事务：校验会话 + 插入 user 消息 → 发 `message.user`
2. 流式生成（无 DB 事务）
3. 短事务：插入 assistant（`content` + `parts_json`）→ 发 `message.assistant` + `done`
4. 失败：发 `error`；若已有部分文本，可选仍落库 partial assistant（建议：**失败不落库助手行**，仅保留用户消息）

### D3 — Runtime 流式 API

- 新增 `AgentRuntime.stream(AgentRunCommand, AgentStreamListener)` 或返回 `Flux<AgentStreamEvent>`
- `SpringAiAgentRuntime`：`spec.stream().content()` 映射 `text-delta`
- Tool：优先用 Spring AI 流式响应中的 tool 相关回调/消息；若版本能力不足，可在本 turn **退化为仅 text-delta**（仍满足最小流式），并在 design 备注；目标态尽量发 `tool-start`/`tool-result`
- Reasoning：OpenAI-compatible thinking 字段若可得则 `reasoning-delta`；否则不发

### D4 — `parts_json` 存储

- Flyway `V0.1.0.58__agent_session_message_parts.sql`：`parts_json TEXT NULL`（PG 存 JSON 文本即可，避免 JSONB 与 MyBatis 额外类型麻烦；或 JSONB + String 读写）
- DO 增加 `partsJson`；VO `parts` 为 `List`/`Object` 反序列化
- `content` = 所有 text parts 拼接（兼容旧客户端）

### D5 — 取消

- 客户端断开 → `SseEmitter` completion/timeout/error → `Disposable.dispose()` / 取消 Flux
- 用户消息已提交保留；助手未完成则不插入（或插入带 `parts` 的 partial —— **选不插入**）

### D6 — Security

- 与 `/send` 相同 JWT；在启动异步前从 `SecurityFrameworkUtils` / TenantContext 捕获 userId/tenantId，并在异步线程还原（参考项目既有 async 模式；若无则手动 set/clear）

## Risks / Trade-offs

- [工具事件不完整] → 至少 text 流式可用；后续加强 ChatResponse 解析
- [代理/超时掐断 SSE] → 文档注明 nginx 需关闭 buffering；Tomcat 默认 async 超时可配置
- [codegen DO] → 按约定：Flyway 后可用 CLI 临时对照，最小手改 `partsJson` 字段合入（本切片允许小 diff）

## Migration Plan

1. 部署 Flyway + 新端点
2. 旧客户端忽略 `parts`；继续用 `/send`
3. integrate 切前端到 stream
4. 回滚：停用 stream 路由；列可留

## Open Questions

- 代理层 SSE buffering（运维文档后续补）
- 是否在 stream 超时后写 partial assistant（本期：否）
