## Context

- IM Bot 统一触达、Runtime SPI、群 @、卡片已落地；**自定义 Bot 产品面**与 **Agent 大脑**尚未立项。
- 产品对标飞书 aily；工程约束见 `docs/dev/architecture.md`、前端优先工作流。
- 本地参照：OpenClaw（Channel/Runtime）、grok-build `xai-grok-agent`（Definition→Builder）。
- 本母 change 只定架构与拆分；实现落在子 change。

## Goals / Non-Goals

**Goals:**

- 锁死 Agent / Session / Bot / Channel / Spring AI 边界，避免单 change 吞全栈
- 给出可执行的子 change 大类与依赖序，突出 **Runtime 核心** 为展示亮点
- `agent` 规格骨架可被后续 delta 深化，而不在母 change 写死全部 API 字段

**Non-Goals:**

- 本母 change 不引入 Spring AI 依赖、不建表、不做 UI
- 不嵌开源 Agent 仓库
- 不定多智能体 / MCP / 技能市场细节

## Decisions

### D1 — 多 change 拆分（非单体 OpenSpec）

按大类开子 change（见路线图表）。母 change 只规划 + 开单。  
**备选**：一个巨大 `agent-v1` → 否决（违背小步 change 与展示分期）。

### D2 — 新域 `module-agent`，Bot 仍属 IM

Agent 配置/Session/Runtime 在 `relayflow-module-agent`；Carrier Bot 与 `ImBotApi` 仍在 `module-im`。Agent 通过 `im-api` 供给/推送。  
**备选**：Agent 塞进 im-biz → 否决（耦合爆炸）。

### D3 — Session ≠ IM Conversation

`agent_*` 会话表独立；`/app/messages` 永不展示 Agent Session 消息。Automation / 主动触达只经 Bot → `im_message`。  
对齐 OpenClaw「内部 WebChat ≠ outbound channel」。

### D4 — Spring AI 为唯一 Runtime 内核

BOM 引入 Spring AI；`AgentRuntime` 封装 `ChatClient` + Tool Calling；产品层 `AgentBuilder` 组装 Def+Tools。  
**备选**：LangChain4j / 纯自研 HTTP → 否决（与 Boot 契合度与维护成本）；嵌 OpenClaw 进程 → 否决。

### D5 — 开源参照方式

- grok：Definition / Builder / tool allowlist / base+persona prompt  
- OpenClaw：ChannelDispatcher、来源通道确定性回复、四层（Provider/Model/Runtime/Channel）  
代码不 vendoring；深化子 change design 时可引用本地路径下的文档/包结构作对照。

### D6 — 模型配置归属 admin

`admin-llm-provider-v1` 负责连接与连通性；AgentDef 只存 `modelRef`。具体表落 `system` 还是 `infra` 由该子 change 深化时拍板（倾向 system 企业设置或独立 `agent_model_*` 由 admin API 暴露）。

### D7 — 前端优先

用户可见能力：`workspace-agent-web` →（API 在 `agent-runtime-core-v1`）→ `workspace-agent-integrate`。  
`agent-module-scaffold` / `admin-llm-provider` 的纯管理或平台切片可按 tasks 标 `[平台]`。

### D8 — 表前缀

新表使用 `agent_` 前缀（与 `sys_`/`im_`/`task_` 并列）。Flyway 仍按产品 SemVer 命名。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Spring AI 版本动荡 | 版本只进 BOM；Runtime 接口隔离，便于升级 |
| Session/IM 双宇宙用户困惑 | UI 文案区分；自动化结果明确「发到消息」 |
| 子 change 过多协作成本 | 母 change 路线图表 + 严格依赖序；一次只 apply 一个 |
| Skill 越权 | Tool 以当前用户 JWT/委托执行；禁止 Bot 服务账号默认可读全租户 |
| 过早做 Automation/Carrier | 依赖序强制：先 Runtime+UI，再 Bot，再自动化 |

## Migration Plan

- 本母：仅文档/规格；无运行时迁移
- 子 change 各自 Flyway；`0.x` 允许演进

## Open Questions（留给子 change 深化，不阻塞开单）

1. LLM Provider 配置表挂 `system` 还是独立 `agent_llm_*`？
2. Agent 可见性：仅个人 / 企业共享 / 管理员下发？
3. Session 删除策略与审计保留多久？
4. 群 @ Carrier Bot 是否默认开启，还是仅 Automation 推送？

## 子 change 索引

| Change | 大类 | 依赖 |
|--------|------|------|
| `admin-llm-provider-v1` | 模型基础设施 | 母 |
| `agent-module-scaffold` | [平台] Maven + Spring AI BOM | 母 |
| `agent-runtime-core-v1` | Runtime 核心（亮点） | scaffold +（建议）llm-provider |
| `workspace-agent-web` | 工作台 UI | 可与 runtime 并行（Mock） |
| `workspace-agent-integrate` | 联调 | web + runtime-core |
| `agent-skill-catalog-v1` | 技能目录与内置 Tool | runtime-core |
| `im-bot-agent-carrier-v1` | Bot 载体供给 | runtime-core + 现有 im Bot |
| `agent-automation-v1` | 自动化推送 | carrier + skill（建议） |
