# 智能体（Agent）平台架构草案

> **状态**：**规划中 · 真源见 OpenSpec 母 change** [`agent-platform-foundation`](../../openspec/changes/agent-platform-foundation/proposal.md)。  
> **用途**：产品边界、模块分层、Spring AI 落点、与 IM Bot / 开源参照的对照；**实现以各子 change 的 tasks 为准**。  
> **整理日期**：2026-07-28  
> **参照（本地）**：`/Users/luwenkai/study/temp/openclaw`（Channel / Runtime 分层）、`/Users/luwenkai/study/temp/grok-build`（`xai-grok-agent` Definition→Builder）  
> **相关**：[`im-messaging-architecture-draft.md`](im-messaging-architecture-draft.md) · [`frontend-first-workflow.md`](frontend-first-workflow.md)

---

## 1. 产品动机

RelayFlow 要展示 **可配置企业智能体** 能力（对齐飞书 aily 形态）：智能体 = 提示词 + 技能 + 模型引用；工作台内对话；自动化结果经 **自定义 Bot** 推送到 `/app/messages`。

亮点不在「嵌一套开源 Agent 产品」，而在：

1. **清晰的企业域边界**（租户、权限、IM 已有地基）；
2. **Spring AI 作为 Runtime 内核**，产品编排自研；
3. **可配置 AgentDef**（grok 式 Definition 思想，落库 + UI）。

---

## 2. 已拍板结论

| # | 议题 | 结论 |
|---|------|------|
| 1 | 自定义 Bot | **服务于 Agent**；IM 提供通用供给 API；Agent 在则 Bot 在，Agent 删则 Bot 退 |
| 2 | Agent 会话 vs IM | **分离**：智能体页聊天只进 Agent Session；**只有** Bot 推送 / 群内 Bot 对话进 `/app/messages` |
| 3 | Runtime 框架 | **Spring AI**（ChatClient + Tool Calling）；不整仓嵌 OpenClaw / grok-build |
| 4 | 模型连接 | **管理后台**配置 Provider / Key / 连通性；Agent 只选已启用 `modelRef` |
| 5 | Skill vs API | 产品名 **技能**；运行时 **Tool**；实现调现有 `*-api`（禁止 agent-biz→他域 biz） |
| 6 | 开源用法 | **抄架构模式**，不嵌仓库：OpenClaw→Channel；grok→Definition/Builder |

---

## 3. 与开源对照（学什么 / 不学什么）

### 3.1 grok-build（`xai-grok-agent` + tools/skills）

| 学 | 不学 |
|----|------|
| Agent = Definition → Builder → 可运行对象 | Markdown/YAML 文件发现（我们用 DB + 管理/工作台 UI） |
| tools 白名单、promptMode extend（base + persona） | 编程工具集（bash/文件）作默认 Skill |
| Agent 与 Shell（Host）分离 | Rust crate / 终端 harness |
| **热路径小工具集 + 长尾二级发现**（search → use）；Skills 短清单 + 懒加载正文 | 嵌其运行时 / 真 MCP 协议为唯一扩展；照搬 BM25 实现细节 |

> **能力发现与 Skill/Tool 分层的目标态**见专文：[`agent-capability-architecture.md`](agent-capability-architecture.md)（2026-07-29）。母稿管 Channel/Runtime；该文管「技能变多后如何找得到」。

### 3.2 OpenClaw

| 学 | 不学 |
|----|------|
| **Channel ≠ Runtime**：入站归一化 → Agent 跑一轮 → **确定性回到来源通道** | Telegram/WhatsApp 等多 Messenger 插件矩阵 |
| WebChat / 内部对话 **不是** 对外 outbound channel | 整仓 Gateway + 插件生态 |
| Provider / Model / AgentRuntime / Channel 四层分开 | 把编码 Agent 当唯一产品形态 |

RelayFlow 第一期只有两个 Channel：

| Channel id | 入口 | 出口 |
|------------|------|------|
| `agent_web` | `/app` 智能体工作台 | 写 `agent_session` 消息 |
| `im_bot` | bot_dm / 群 @ / Automation 推送 | `ImBotApi` / Bot Runtime 同会话回复 |

---

## 4. 分层蓝图

```text
┌──────────────────────────────────────────────────────────────────┐
│ L4 产品面                                                         │
│  /app 智能体：新任务 / 历史 Session / 智能体配置 / 自动化（后）     │
│  /admin：LLM Provider 连接、连通性、组织启用模型                    │
│  /app/messages：仅 IM Bot 消息（与 Agent Session 隔离）            │
└───────────────────────────────┬──────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│ L3 module-agent（新域 agent_*）                                    │
│  AgentDef │ SkillCatalog │ Session │ Automation                   │
│  AgentBuilder（grok 思路）│ AgentRuntime（Spring AI）              │
│  ChannelDispatcher（openclaw 思路：按 origin 投递）                 │
└───────┬─────────────────────────────┬────────────────────────────┘
        │ Skill → *-api               │ Bot 供给 / 推送 / 入站回调
        ▼                             ▼
┌───────────────┐            ┌─────────────────────────────────────┐
│ task/calendar │            │ module-im（已有）                    │
│ docs/system…  │            │ ImBotProvisioningApi + ImBotApi     │
│ *-api         │            │ BotIngress → AgentRuntime（绑定）   │
└───────────────┘            └─────────────────────────────────────┘
```

### 4.1 Maven 模块（目标）

```text
relayflow-module-agent/
  relayflow-module-agent-api    # 跨域契约（若他域需查询 Agent 元数据；宜薄）
  relayflow-module-agent-biz    # 定义、Session、Runtime、Skill 适配、Automation
```

- 表前缀：`agent_`（见 `database.md` 扩展约定，本母 change 锁定）
- `server` 加载 `agent-biz`；依赖 Spring AI 版本只进 **BOM**
- **禁止** `agent-biz` → 其他 `*-biz`

### 4.2 Spring AI 落点（必须体现架构，而非散落调用）

```text
agent-biz
  ├── definition/     AgentDef 持久化、校验
  ├── builder/        AgentBuilder：Def + Skills → RuntimeAgent（提示词组装 + Tool 列表）
  ├── runtime/        SpringAiAgentRuntime：ChatClient、ToolCalling、流式、多轮
  ├── skill/          SkillRegistry；BuiltinSkill → ToolCallback；内部调 *-api
  ├── session/        AgentSession 真源（≠ im_conversation）
  ├── channel/        AgentWebChannel / ImBotChannel 适配
  └── automation/     触发器 → run → ImBotApi（后置切片）
```

原则：

- Controller / 业务服务 **不**直接 `new OpenAiClient`；只依赖 `AgentRuntime` / `AgentBuilder` 接口
- 模型凭据来自 **admin 侧连接配置**（system 或 infra 子域，子 change 定表），Runtime 只读 `modelRef`

---

## 5. 核心对象

| 对象 | 职责 | 非职责 |
|------|------|--------|
| **AgentDef** | name、systemPrompt、modelRef、skillIds、carrierBotId、可见性 | 不存 API Key；不存 IM 消息 |
| **AgentSession** | 智能体页一轮对话线程与历史 | 不进 `/app/messages` |
| **Skill** | 目录项 + input schema + 实现绑定 | 不是对外 OpenAPI（后置） |
| **Automation** | 定时/触发 + 指派 Agent + 推送目标 | 不是 Session |
| **Carrier Bot** | IM `type=tenant` Bot，生命周期跟 Agent | 不含 LLM 逻辑 |

---

## 6. 子 change 路线图（大类）

> 母 change 只固化架构与开单；**禁止**在一个 OpenSpec 里实现全部能力。

| 序 | Change | 大类 | 说明 |
|----|--------|------|------|
| 0 | `agent-platform-foundation` | **母 / 规划** | 本文 + 规格骨架 + 开子 change；少量子基建任务 |
| 1 | `admin-llm-provider-v1` | **模型基础设施** | 管理后台 Provider / Key / 连通性 / 组织启用 |
| 2 | `agent-module-scaffold` | **[平台] 模块脚手架** | Maven `module-agent`、BOM 引入 Spring AI、包结构空壳 |
| 3 | `agent-runtime-core-v1` | **Runtime 核心（亮点）** | AgentDef + Builder + Spring AI Runtime + Session API |
| 4 | `workspace-agent-web` | **工作台 UI** | aily 式壳：智能体列表、新任务、历史、聊天（Mock 可） |
| 5 | `workspace-agent-integrate` | **联调** | 去 Mock，接 Session/Runtime API |
| 6 | `agent-skill-catalog-v1` | **技能** | Skill 目录 + 首批内置 Tool（task/calendar 等） |
| 7 | `im-bot-agent-carrier-v1` | **Bot 载体** | `ImBotProvisioningApi`；Agent 生命周期挂钩；入站转 Runtime |
| 8 | `agent-automation-v1` | **自动化** | 定时触发 + 推送配置；结果只走 IM |

推荐依赖顺序：`0 → 1∥2 → 3 → 4 → 5`；`6` 可与 `5` 交错；`7 → 8`。

前端优先：`4` 可与 `3` 并行起草 contract；`5` 必须在 `3` API 就绪后。

---

## 7. 刻意不做（防膨胀）

- 嵌 OpenClaw / grok-build 源码或以其为运行时进程
- 智能体小队 / 多智能体编排（P3+）
- 技能市场、MCP 外部技能（后置）
- 把 Agent Session 写入 `im_message`
- 独立 `module-bot`（Bot 仍属 IM）
- V1 Gateway / Nacos

---

## 8. 文档关系

| 文档 | 关系 |
|------|------|
| 本文 | Agent 平台目标态与分层 |
| [`agent-capability-architecture.md`](agent-capability-architecture.md) | **Capability / Skill 发现**（对齐 Grok：热路径 · 域检索 · 流程技能包） |
| `im-messaging-architecture-draft.md` | Bot / bot_dm / Runtime SPI（载体层） |
| `agent-platform-foundation` | OpenSpec 母 change |
| 各子 change | 可执行 tasks |

## 附录：术语

| 术语 | 含义 |
|------|------|
| AgentDef | 可配置智能体定义 |
| AgentBuilder | 由 Def+Skills 组装 RuntimeAgent（grok 同构） |
| AgentRuntime | Spring AI 驱动的一次/多轮执行 |
| Channel | 消息进出通道（agent_web / im_bot） |
| Skill / Tool | 产品技能 vs 运行时工具 |
| Carrier Bot | 绑定到 Agent 的 IM 机器人载体 |
| modelRef | 指向组织已启用模型连接的引用 |
