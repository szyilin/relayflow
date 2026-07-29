## Why

工作台智能体对话仍是手写气泡 + 同步 `POST /send`，无法流式展示、Markdown、Thinking、工具调用状态。Nuxt UI 已提供完整 AI Chat 组件族（`UChatMessages` / `UChatReasoning` / `UChatTool` 等），应直接采用，避免继续维护简陋自定义 UI。本期为 **前端优先 `-web`**：先把 Chat 壳与 parts 模型、Mock 流式跑通，并冻结 SSE contract，供后续 `-api` / `-integrate`。

## What Changes

- 升级 `@nuxt/ui` 至 **≥4.10**（ChatTool actions 等能力），新增 `ai`、`@ai-sdk/vue`、`@comark/vue`（Vue 非 Nuxt 路径）
- 用 Nuxt UI AI Chat 组件重写 `/app/agents` 对话区（Messages / Message / Prompt / PromptSubmit / Reasoning / Tool / Shimmer）
- 前端消息模型改为 `parts[]`（text / reasoning / tool），历史与进行中一轮统一渲染；本期 **Mock SSE** 驱动流式
- 起草 `openspec/lanes/workspace-agent-chat/contract.md`：`POST .../send/stream` 事件形状与落库 `parts` 草案
- **不**改 IM「正在输入」、**不**做侧栏拖拽排序（另开 change）
- **不**在本期实现真实 Spring AI SSE（属后续 `workspace-agent-chat-stream-api`）

## Capabilities

### New Capabilities

- `workspace-agent-chat-ui`: 工作台智能体会话 UI 采用 Nuxt UI AI Chat；支持流式 Markdown、Reasoning、Tool 可视化；消息以 parts 建模

### Modified Capabilities

- （无已归档 main spec；agent 行为仍在 change 增量中。本 change 仅新增 UI 能力规格。）

## Impact

- **web/**：`pages/app/agents`、`stores/agent.ts`、`api/app/agent.ts`、Vite `prose`、依赖升级
- **openspec/lanes/**：新增 `workspace-agent-chat/contract.md` 草案
- **Java / Flyway**：本期不改；contract 标明后续 API 与 `parts_json` 迁移
- **回滚**：前端可回退到同步 send + 旧气泡；无 DB 迁移风险
- **后续 change**：`workspace-agent-chat-stream-api`、`workspace-agent-chat-integrate`
