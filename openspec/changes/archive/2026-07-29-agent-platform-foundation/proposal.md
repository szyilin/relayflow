## Why

RelayFlow 要把「可配置企业智能体」做成产品亮点（对齐飞书 aily：定义 = 提示词 + 技能 + 模型；工作台对话；自动化经 Bot 推送）。IM Bot 地基已就绪，但 **Agent 域、Spring AI Runtime、模型管理、Session 与 IM 隔离** 尚未立项。若不先用 **母 change** 拆大类、锁架构，后续极易把定义/会话/IM/自动化耦进单一 OpenSpec，导致不可维护且无法分阶段展示 AI 能力。

## What Changes

本 change 为 **母 change（规划 + 架构真源 + 规格骨架 + 开子 change）**；**不**在本 change 内实现完整智能体产品。

1. 固化架构草案 [`docs/dev/agent-architecture-draft.md`](../../../docs/dev/agent-architecture-draft.md)（拍板结论、分层、Spring AI 落点、开源参照用法）
2. 引入能力域 **`agent`** 规格骨架（对象、通道隔离、模块边界）；细节需求由子 change delta 深化
3. 锁定子 change 大类与依赖顺序（见 design / tasks § 开单）
4. 更新开发者文档索引与 `AGENTS.md`「下一优先」指向本路线
5. **最小基建（可选勾选）**：仅文档与规格；Maven/Spring AI 引入留给 `agent-module-scaffold`

## Capabilities

### New Capabilities

- `agent`：企业智能体平台——AgentDef、Session（与 IM 隔离）、Skill/Tool、Channel 投递、与 Carrier Bot / 模型引用的边界（本母 change 给骨架 Requirement；子 change 补场景）

### Modified Capabilities

- `im`：预留「Agent 载体 Bot 供给 API / 入站转 AgentRuntime」方向（**行为细节**在 `im-bot-agent-carrier-v1`）；本母 change 仅交叉引用，可不改 im 主规格条文，或加一条占位 Requirement
- `system` / `web-admin`：预留管理端 LLM Provider 配置归属（细节在 `admin-llm-provider-v1`）

## Impact

| 层 | 影响 |
|----|------|
| 文档 | 新增 `docs/dev/agent-architecture-draft.md`；更新 `docs/dev/README.md`、`AGENTS.md` |
| OpenSpec | 新能力 `agent`；多个子 change 骨架目录 |
| 代码（本母） | **默认无**业务代码；禁止在本 change 实现 Runtime/UI |
| 依赖 | Spring AI 引入 **不在**本母 change（见 `agent-module-scaffold`） |
| 回滚 | 仅文档/规格；无 DB 迁移义务 |

## 非目标

- 一次实现「整个智能体产品」
- 嵌 OpenClaw / grok-build 仓库
- 多智能体小队、技能市场、MCP
- Agent Session 写入 `im_message`
