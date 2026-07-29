## Context

Agent 能力正在从少量 `task.*` 扩展到多域（`im.*` 等）。现状：`AgentDef.skillIds` 扁平解析为 Spring AI tools；Carrier 入站硬编码合并 `im.read_*`。架构目标见 [`docs/dev/agent-capability-architecture.md`](../../../docs/dev/agent-capability-architecture.md)（对齐 Grok：热路径 + 长尾 search/use）。

约束：跨域只走 `*-api`；禁止 `agent-biz → *-biz`；不嵌 grok-build；本 change 对应架构 P0–P2。

## Goals / Non-Goals

**Goals:**

- BuiltinSkill 元数据：`domain`、`tier(HOT|DEFERRED)`
- Catalog API 按域分组
- 具名 **Channel 热路径包**（`im-carrier`、`workspace-default`）装配 L1，取代 Carrier 硬编码合并
- AgentDef **capability profile**（热路径包 + 可检索域），兼容现有 `skillIds`
- Meta 工具 `capability.search` / `capability.use`：Deferred 默认不进模型 tools 列表，经 use 服务端分发 + ACL
- Builder 注入已授权 **域摘要**

**Non-Goals:**

- L3 流程技能包产品化、技能市场、知识库 `kb.*`
- 真 MCP / BM25（索引可先内存关键词）
- Agent 配置 UI 大改（Catalog 分组与 profile 字段可读即可）
- 改 IM 读 API 权限模型（沿用 `ImConversationReadApi`）

## Decisions

### D1 — Deferred 经 `capability.use` 分发，不动态改 tools 列表

**选择**：模型侧稳定暴露 Hot ∪ `{capability.search, capability.use}`；长尾只经 use 调用。  
**备选**：命中后动态追加 ToolCallback — 列表不稳、Spring AI 集成复杂。  
**对齐**：Grok `use_tool` 保持列表稳定。

### D2 — Channel 强制包 vs Agent 勾选

| 包 | 通道 | 说明 |
|----|------|------|
| `im-carrier` | `im_bot` | 强制含 `im.read_message/nearby/history` |
| `workspace-default` | `agent_web` | 平台默认可选热路径起点（可空或含已绑 HOT） |

`agent_web` **默认不**强制全量 IM 读。

### D3 — AgentDef 存储兼容

**选择**：扩展 JSON（新字段或 `skill_ids_json` 旁并列 `capability_profile_json`）；读路径：有 profile 用 profile，否则 `skillIds` → 视为 HOT 显式列表。  
**备选**：立刻废 `skillIds` — **BREAKING**，拒。  
若需新列：Flyway `V0.1.0.x__agent_capability_profile.sql`。

### D4 — search / use 语义

- `capability.search(query, limit?)`：仅在授权域内检索 DEFERRED（+可选列出 HOT 供发现）；返回 code、domain、description、inputSchema 摘要
- `capability.use(toolCode, arguments)`：校验白名单与 ACL 后调用对应 BuiltinSkill；热路径已有的 code 应提示直接调用（native 纠偏）
- IM 类 use：继续依赖 Carrier/`ImCarrierToolContext` 会话绑定；无上下文则拒绝跨会话

### D5 — 域摘要

Builder 根据授权域生成短列表（域名 + 一句说明），写入 system 附加段；不塞全量 schema。

## Risks / Trade-offs

- **[Risk] 模型绕过 search 瞎猜 code** → use 侧白名单拒绝  
- **[Risk] profile 与 skillIds 双写不一致** → 单一读模型：profile 优先；写 API 规范化  
- **[Risk] 技能仍少时 Meta 过重** → Hot 可继续挂常用 tool；Deferred 迁移可渐进  
- **[Risk] Catalog UI 滞后** → API 先分组；前端最小展示

## Migration Plan

1. 先合元数据 + Catalog 分组（无行为破坏）  
2. 再合热路径包（Carrier 去掉硬编码合并）  
3. 再挂 Meta 工具；现有 `skillIds` Agent 仍可用  
4. 回滚：feature 开关关闭 Meta + 恢复扁平 resolve / 硬编码合并  

## Open Questions

- `capability_profile_json` 新列 vs 塞进现有 JSON：实现时选迁移成本更低者（建议新列更清晰）  
- 工作台是否在本 change 暴露「可检索域」勾选：建议 API 就绪，UI 可只读展示
