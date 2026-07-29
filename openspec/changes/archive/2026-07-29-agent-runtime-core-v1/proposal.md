## Why

这是 Agent 产品的**展示亮点核心**：可配置 AgentDef + grok 式 Builder + **Spring AI** Runtime + 与 IM 隔离的 Session API。没有本切片，UI 与技能/自动化均无大脑可接。

## What Changes

- AgentDef 持久化与 CRUD（prompt、modelRef、skillIds 占位）
- `AgentBuilder`：组装 system prompt（平台 base + persona）+ Tool 列表（可先空）
- `AgentRuntime`：Spring AI `ChatClient` + 多轮 Session；流式（深化定）
- Session / Message API：`/app-api/agent/...`；**禁止**写入 `im_message`
- Channel：`agent_web` 出口写 Session
- 架构对照：本地 `grok-build/.../xai-grok-agent`、`openclaw` docs（Channel 分离）——design 深化时附图

## Capabilities

### New Capabilities

- `agent`：深化 Definition / Runtime / Session 场景（相对母 change 骨架）

### Modified Capabilities

- （无强制）

## Impact

- `module-agent-*`；依赖 `admin-llm-provider-v1`（modelRef 解析）与 `agent-module-scaffold`
- 前端 contract 由 `workspace-agent-web` 起草；本切片实现 API
- **BREAKING**：无（新能力）

## 非目标

- 完整 Skill 市场、Automation、Carrier Bot 供给（分属其它 change）
- 嵌开源仓库源码
