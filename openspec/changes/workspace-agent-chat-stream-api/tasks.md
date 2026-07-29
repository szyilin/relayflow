## 1. Schema & DO

- [x] 1.1 Flyway `V0.1.0.58__agent_session_message_parts.sql`：`agent_session_message.parts_json`（TEXT NULL）
- [x] 1.2 `AgentSessionMessageDO` + Mapper.xml `Base_Column_List` 增加 `partsJson` / `parts_json`
- [x] 1.3 `AgentSessionMessageRespVO` 增加可选 `parts`；`toMessageVo` 反序列化

## 2. Runtime stream

- [x] 2.1 定义 `AgentStreamEvent`（与 contract 事件对齐的内部类型）与 `AgentRuntime.stream(...)`
- [x] 2.2 `SpringAiAgentRuntime`：`ChatClient.stream()` 映射 `text-delta`；工具/reasoning 尽力映射，不足则仅 text
- [x] 2.3 支持取消（Disposable / abort）

## 3. Session stream API

- [x] 3.1 `AgentSessionService.sendStream(...)`：落库 user → 流式 → 落库 assistant（parts_json）→ 事件回调；事务不跨流式
- [x] 3.2 `AppAgentSessionController`：`POST /send/stream` 返回 `SseEmitter`；捕获并还原租户/用户上下文
- [x] 3.3 同步 `/send` 仍可用；写入时可填简单 text part（可选）

## 4. Verify & board

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 更新 `docs/dev/api-integration-board.md`：stream-api → done；注明 curl 冒烟路径
- [x] 4.3 勾选本 tasks；说明下一步 `-integrate`
