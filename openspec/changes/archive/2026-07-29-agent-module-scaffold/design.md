## Context

新建 `relayflow-module-agent`（api + biz），在 BOM 引入 **Spring AI 1.1.x**（兼容 Boot 3.4，**不用**要求 Boot 4 的 Spring AI 2.x）。本切片为 `[平台]`：可编译、server 可加载、包结构就位；**无**业务表与 REST。

参照：现有 `relayflow-module-task` / `calendar` 模块形态；架构见 `docs/dev/agent-architecture-draft.md`。

## Goals / Non-Goals

**Goals:**

- Maven 反应堆登记 `module-agent`；server 依赖 `agent-biz`
- BOM：`spring-ai-bom` **1.1.8** + 内部 `agent-api`/`agent-biz` 版本
- biz 引入 `spring-ai-openai` + `spring-ai-client-chat`（**不用** `spring-ai-starter-model-openai`，避免启动强制 `spring.ai.openai.api-key`）
- 包结构空壳：`definition` / `builder` / `runtime` / `skill` / `session` / `channel` / `llm`
- 错误码域号 **009**（`1_009_*`）；`database.md` 登记 `agent_` 前缀

**Non-Goals:** AgentDef CRUD、Session、LLM 管理 UI、表、自动配置 ChatClient bean

## Decisions

### D1 — Spring AI 1.1.8，非 2.x

Boot 3.4.3 → Spring AI **1.1.x**。2.0 硬依赖 Boot 4，V1 禁止。

### D2 — 库依赖而非 starter autoconfig

密钥来自租户 DB（后续 `admin-llm-provider-v1`），Runtime 编程式构建客户端；scaffold 阶段不注册默认 `ChatModel`。

### D3 — 包结构对齐 grok/OpenClaw 分层

| 包 | 职责（后续切片填充） |
|----|----------------------|
| `definition` | AgentDef |
| `builder` | AgentBuilder |
| `runtime` | SpringAiAgentRuntime |
| `skill` | SkillRegistry / Tool |
| `session` | AgentSession |
| `channel` | agent_web / im_bot 投递 |
| `llm` | 连接配置解析（本后续 llm-provider） |

### D4 — scanBasePackages 已覆盖

`RelayflowServerApplication` 已 `scanBasePackages = "com.relayflow"`，无需改启动类。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Spring AI 传递依赖冲突 | 仅 openai + client-chat；compile 验证 |
| 误加 starter 导致启动失败 | 明确禁止 starter；design 写死 |

## Open Questions

- （无，阻塞已清）

## 深化说明

本 design 已可 `/opsx:apply`。
