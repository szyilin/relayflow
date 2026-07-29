## Why

智能体需要可引用的大模型连接；密钥与连通性属于**组织基础设施**，不能散落在每个 AgentDef 上。本切片交付管理端「LLM / 模型 Provider」配置能力，供后续 Runtime 只读 `modelRef`。

## What Changes

- 管理后台：新增/编辑 Provider（厂商、Base URL、API Key 密文存储）、连通性测试、组织内启用模型列表
- 供 Agent 下拉的「已启用模型」查询 API（admin 或 app 只读，子设计拍板）
- **骨架**：表归属（`system` vs `agent_llm_*`）、加密方案在 design 深化

## Capabilities

### New Capabilities

- （深化时）可挂 `system` 或新建 `agent` delta：LLM Provider 连接与启用

### Modified Capabilities

- `web-admin` / `system`：管理端应用设置增加 AI 模型配置入口（深化时写 delta）

## Impact

- `web/` admin 设置页；`system-biz` 或 `agent-biz` 配置存储；密钥不得进前端明文
- 依赖：无；被 `agent-runtime-core-v1` 依赖
- 母 change：`agent-platform-foundation`

## 非目标

- Agent 对话 UI、Tool Calling、自定义 Bot
