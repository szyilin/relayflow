## Why

Agent 内置 Tool 正从「几个 task.*」扩展到「多域 im/task/calendar…」。当前把 `skillIds`（及 Carrier 强制合并的 `im.read_*`）全部扁平挂进 Spring AI tools，短期可用，长期会导致上下文膨胀、选错工具、难按域维护，也无法平滑接上「人设提示词 / 系统通道包 / 可选业务域 / 市场流程 Skill / 知识库」。需要按 Grok Build 理念落地 **热路径小集合 + 长尾二级发现**，且跨域仍只走 `*-api`。

## What Changes

- **能力元数据**：BuiltinSkill 增加 `domain`、`tier(HOT|DEFERRED)`（及可选 tags）；Catalog API 按域分组返回
- **Channel 热路径包**：用具名包（如 `im-carrier`、`workspace-default`）装配 L1 tools，**取代** Carrier 硬编码合并 `im.read_*`
- **Agent 能力配置面（兼容演进）**：在 AgentDef 上引入 capability profile 形状（热路径包 + 可检索域白名单）；保留现有 `skillIds` 兼容读/写迁移路径
- **二级发现 Meta 工具**：`capability.search` / `capability.use`（语义对齐 Grok `search_tool`/`use_tool`）；Deferred 工具默认不塞进模型 tools 列表，经 use 服务端分发并二次 ACL
- **域摘要**：Builder/Runtime 向模型注入已授权域的短摘要（非全量 schema）
- **文档锚定**：行为以本 change specs 为准；背景见 `docs/dev/agent-capability-architecture.md`

**本 change 不做（后续 change）**：流程技能包 L3 全文产品化、技能市场、知识库 `kb.*`、真 MCP/BM25、完整 Agent 配置 UI 大改（Catalog 分组与 profile 字段可读即可）。

## Capabilities

### New Capabilities

- `agent-capability`：能力分层（热路径包 / 域 Deferred / Meta 发现）、Catalog 域元数据、Channel 强制包、`capability.search`/`capability.use` 与授权边界

### Modified Capabilities

- `agent`：AgentDef 能力配置与 Builder/Runtime 装配方式（热路径 ∪ Meta；长尾经 search/use）；Catalog 契约扩展；Carrier 入站改走热路径包而非硬编码 skill 合并

## Impact

- **后端**：`relayflow-module-agent-api/biz`（SkillDescriptor、Registry、Builder、Runtime、Carrier inbound、AgentDef 持久化/VO）；可能触及 `web/` Catalog 与 Agent 保存契约
- **前端**：`web/` 技能目录按域展示（最小）；Agent 保存兼容新 profile 字段（可先 API 就绪、UI 渐进）
- **库表**：优先 JSON 扩展兼容现有 `skill_ids_json`；若需新列再开 Flyway（design 定案）
- **回滚**：关闭 Meta 工具、恢复扁平 resolve；Carrier 可回退硬编码合并（体验回退）
- **依赖文档**：[`docs/dev/agent-capability-architecture.md`](../../../docs/dev/agent-capability-architecture.md)（P0–P2 对应本 change）
