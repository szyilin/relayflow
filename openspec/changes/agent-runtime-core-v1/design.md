## Context

亮点核心切片。前置：`agent-module-scaffold`、`admin-llm-provider-v1`（modelRef）。

本地架构参照：

- `/Users/luwenkai/study/temp/grok-build/crates/codegen/xai-grok-agent/README.md` — Definition → Builder
- `/Users/luwenkai/study/temp/openclaw/docs/agent-runtime-architecture.md` — Runtime 边界
- `/Users/luwenkai/study/temp/openclaw/docs/concepts/agent-runtimes.md` — Provider/Model/Runtime/Channel
- `docs/dev/agent-architecture-draft.md`

## Goals / Non-Goals

**Goals:**

- 表：`agent_def`、`agent_session`、`agent_session_message`
- `AgentBuilder`：平台 base prompt + persona；可选 tools 钩子（空列表可跑）
- `SpringAiAgentRuntime`：按 modelRef 解析 `agent_llm_connection`，编程式构建 OpenAI-compatible `ChatModel`/`ChatClient`（**不**用 yml `spring.ai.openai.api-key`）
- App API：AgentDef CRUD；Session create/list/get；发消息并返回助手回复（首版同步非流式可接受）
- Channel：`agent_web` 只写 session 表；**禁止**写 `im_message`

**Non-Goals:** Skill 全量、Automation、Carrier Bot、流式 SSE（可列为后续任务）、多智能体

## Decisions

### D1 — 表设计（草案）

**agent_def**

| 列 | 说明 |
|----|------|
| id, tenant_id | 公共 |
| name, description | 展示 |
| system_prompt | persona |
| model_ref | = `agent_llm_connection.code` |
| skill_ids_json | JSON 数组，可空 `[]` |
| carrier_bot_id | 可空，carrier 切片再填 |
| status | active/disabled |
| owner_user_id | 创建者 |
| visibility | `private`（V1 仅本人可见可够用；企业共享后置） |

**agent_session**

| 列 | 说明 |
|----|------|
| id, tenant_id, agent_id, user_id | |
| title | 可空，首条消息截断 |
| status | active/archived |

**agent_session_message**

| 列 | 说明 |
|----|------|
| id, tenant_id, session_id | |
| role | user / assistant / system |
| content | text |
| seq | 会话内序号 |

### D2 — Prompt 组装（grok extend）

```
[platform base]
- 你是 RelayFlow 企业助手
- 仅使用绑定工具；无工具时只回答
- 遵守租户与用户权限语境
[persona = agent_def.system_prompt]
```

### D3 — Runtime 端口

```java
public interface AgentRuntime {
  AgentRunResult run(AgentRunCommand command);
}
```

`SpringAiAgentRuntime` 实现；Controller → SessionService → Runtime。

### D4 — 模型客户端

从 `LlmConnectionService`（或内部 Resolver）取解密 key + baseUrl + modelName → `OpenAiApi` / `OpenAiChatModel`（Spring AI 1.1 API）。按连接缓存可选（后置）。

### D5 — API（草案）

| Method | Path |
|--------|------|
| CRUD | `/app-api/agent/def/*` |
| POST | `/app-api/agent/session/create` |
| GET | `/app-api/agent/session/list` |
| GET | `/app-api/agent/session/get` |
| GET | `/app-api/agent/session/messages` |
| POST | `/app-api/agent/session/send` |

鉴权：登录成员；Def 可见性 V1=`owner_user_id == current`。

### D6 — 流式

V1 同步返回完整 assistant 文本。SSE 另开任务。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Spring AI 1.1 编程式 API 与文档漂移 | 实现前对照 1.1.8 javadoc / 示例 |
| 长对话 token | 先全量历史；后续 compaction |
| 无 Skill 体验弱 | 纯对话先闭环；skill change 叠加 |

## Open Questions

- visibility 是否一期就做企业共享？**建议 V1 private only**
- send 接口超时默认多少秒？**建议 60s**
