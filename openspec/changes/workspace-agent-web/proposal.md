## Why

按前端优先，先交付 aily 式工作台壳：智能体列表、新任务（选 Agent）、历史 Session、聊天窗，用 Mock/本地数据可演示，并起草 `openspec/lanes/` contract，锁定页面即规格。

## What Changes

- 路由与导航入口（工作台 `/app/...` 智能体区，具体路径深化定）
- 页面：智能体列表/创建编辑（基础字段）、新任务、历史、对话 UI
- Pinia store + 临时数据；**起草** lane contract
- **不**把对话渲染进 `/app/messages`

## Capabilities

### New Capabilities

- （web 行为可写 `agent` 或 `web-auth` 导航 delta——深化时）

### Modified Capabilities

- `web-auth`：工作台导航增加智能体入口（深化）

## Impact

- 仅 `web/`；母路线 `agent-platform-foundation`
- 后接 `workspace-agent-integrate`

## 非目标

- 真实 Spring AI 调用、Bot 推送、自动化配置完整产品（可占位入口）
