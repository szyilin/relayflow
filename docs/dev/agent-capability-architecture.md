# Agent 能力体系架构（对齐 Grok Build 设计理念）

> **状态**：目标态架构 · 指导后续 Agent / Skill / 工具扩展  
> **用途**：回答「技能变多、域变多之后，模型如何稳定找到能力」；与母稿 [`agent-architecture-draft.md`](agent-architecture-draft.md) 互补——母稿管 Channel / Runtime / AgentDef，**本文管 Capability 分层与发现**。  
> **参照**：本地 `/Users/luwenkai/study/temp/grok-build`（`search_tool` / `use_tool`、Skills 短清单 + 懒加载、内置小工具集）  
> **约束**：仍遵守 AGENTS.md——跨域只走 `*-api`；禁止 `agent-biz → *-biz`；不嵌 grok-build 源码。  
> **整理日期**：2026-07-29

---

## 0. 结论：是否要对齐 Grok？

**有必要对齐其设计理念，而不是移植其实现。**

| 对齐（理念） | 不对齐（实现） |
|--------------|----------------|
| 热路径小工具集 + 长尾检索/按需展开 | Rust crate、终端 harness、bash/文件默认工具 |
| MCP 式「先发现再调用」二级路由 | 真 · MCP 协议 / BM25 库（初期可用域目录 + 关键词即可） |
| Skill = 提示词包；Tool = 可执行能力 | Markdown 文件发现为主（我们以 DB + Catalog + UI 为主） |
| 工具列表稳定，避免每次把几百个 schema 塞进模型 | 其 KV-cache 细节、TUI slash 体系 |

企业场景下 Agent 会持续增加 **任务 / 日历 / 文档 / IM / 审批…** 能力。若始终「AgentDef.skillIds 全量扁平挂 Tool」，会出现：上下文膨胀、选错工具、权限面过大、按域维护困难。Grok 用分层发现解决的正是这个问题。

---

## 1. 问题陈述

当前（及刚落地的 IM 读技能）形态：

```text
AgentDef.skillIds  →  SkillRegistry.resolve  →  全部变成 Spring AI ToolCallback
Carrier 入站还强制合并 im.read_*
```

技能少时可行。技能到几十上百时会坏：

1. **发现**：模型在扁平列表里靠 description「蒙」；
2. **成本**：每轮 tools schema 占满上下文；
3. **安全**：不该出现在该 Channel 的工具也暴露；
4. **维护**：无法按业务域（IM / Task）做能力包演进。

目标：模型「总能找到该用的能力」——靠 **架构缩小候选集 + 检索/分级发现**，不是靠模型记全站 API。

---

## 2. 三层能力模型（RelayFlow 用语）

对齐 Grok 的三分法，映射到本仓库：

```text
┌─────────────────────────────────────────────────────────────────────────┐
│ L1  热路径工具（Hot Tools）                                              │
│     本 Channel / 本 Agent 运行时「直接」出现在 Spring AI tools 列表      │
│     例：im.read_*（Carrier）、task.search（工作台已绑）                  │
│     类比 Grok：read_file / bash 等内置小集合                             │
└─────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│ L2  域能力目录（Domain Catalog · 长尾）                                   │
│     不全部塞进 tools；先暴露「域摘要」或 meta-tool，再展开具体 Tool        │
│     例：域 im / task / calendar；具体 tool im.read_history …             │
│     类比 Grok：MCP server 摘要 + search_tool → use_tool                  │
└─────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│ L3  流程技能包（Procedure Skills）                                        │
│     产品流程说明（步骤、约定），不是 *-api 本身                          │
│     短清单（name + description + when-to-use）→ 命中后注入全文到上下文   │
│     类比 Grok：SKILL.md + Skill 工具 / slash                             │
└─────────────────────────────────────────────────────────────────────────┘
```

**硬规则**：

- **L1/L2 的可执行单元** = BuiltinSkill / Tool，**只调 `*-api`**，带租户与主体权限。
- **L3** 只改「模型怎么做事」的说明，最终仍通过 L1/L2 落地；禁止在 Skill 正文里教模型绕过 API 直连库表。
- **域（Domain）** 与 Maven 模块对齐：`im` / `task` / `calendar` / …，code 前缀 `im.*`、`task.*`。

---

## 3. 核心架构图

```text
                         ┌──────────────────────┐
                         │     AgentDef         │
                         │  modelRef            │
                         │  persona prompt      │
                         │  capabilityProfile   │  ← 目标：热路径包 + 可检索域白名单
                         │  procedureSkillIds   │  ← 目标：流程技能包 ID
                         └──────────┬───────────┘
                                    │
                         AgentBuilder（按 Channel 装配）
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          ▼                         ▼                         ▼
   Channel=agent_web         Channel=im_bot              Channel=automation
   热路径：task.* 等         热路径：im.read_*            热路径：推送相关子集
   可检索：task/calendar…    可检索：仅当前会话相关域      可检索：按自动化声明
          │                         │                         │
          └─────────────────────────┼─────────────────────────┘
                                    ▼
                    ┌───────────────────────────────┐
                    │     AgentRuntime (Spring AI)   │
                    │  tools = Hot ∪ MetaDiscovery   │
                    │  (+ 本轮已展开的 Deferred Tools)│
                    └───────────────┬───────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
        Hot ToolCallback     Meta: list/search      Procedure Skill
        → *-api              domain / tool          → 注入说明全文
                             → 再挂/再调具体 Tool         │
                                                    仍走 Hot/Deferred Tool
```

### 3.1 Channel 决定「看见什么」

| Channel | 热路径原则 | 长尾原则 |
|---------|------------|----------|
| `im_bot` | 默认只挂 IM 读类 + 明确授权的少量业务 tool | 禁止默认暴露「搜全站会话 / 跨群」；检索域也须会话绑定 |
| `agent_web` | 用户在 AgentDef 勾选的热路径包 | 可检索已授权域；仍受用户 ACL |
| `automation` | 自动化配置声明的最小集 | 默认关闭开放检索，防失控 |

**Carrier 强制合并 `im.read_*`** 属于热路径正确用法；后续应收敛为「IM Carrier 热路径包」，而不是特例 if。

---

## 4. 发现与调用协议（目标态）

### 4.1 热路径（L1）

- 进入 Runtime 的 `ToolCallback` 列表。
- 数量目标：**个位数～十余个** 量级（按 Channel）。
- 选型标准：高频、低歧义、与当前场景强绑定。

### 4.2 域目录 / 二级发现（L2 · 对齐 Grok search → use）

模型侧**稳定**暴露少量 meta 工具（名称可迭代，语义固定）：

| Meta 工具（示意） | 职责 |
|-------------------|------|
| `capability.search` | 在**已授权域**内按关键词检索工具；返回 code、description、inputSchema、domain |
| `capability.use` | 调用已检索到的 tool code + 参数（服务端再次校验白名单与 ACL） |

配套：

1. **域摘要提醒**（类 Grok「Connected MCP servers」）：系统提示或首轮 reminder 只列  
   `im（会话读）· task（待办）· calendar（日程）…` + 一句说明，不列全量 schema。
2. **索引字段**：`domain`、`code`、`name`、`description`、`tags`；初期可用内存过滤 + 简单关键词，后续可上 BM25/向量，**接口稳定**。
3. **调用门禁**：`capability.use` 必须校验  
   - code ∈ 本 Agent/Channel 授权集合；  
   - 参数中的 conversationId / 资源 ID 符合主体权限（IM 读继续用现有 `ImReadPrincipal` 模式）。
4. **原生纠偏**（类 Grok `native_tool_correction`）：热路径已有的 tool，禁止再经 `capability.use` 绕一圈；返回明确错误引导直接调用。

> **过渡期**：技能仍少时，可继续「热路径全挂」；引入 meta 工具后，长尾迁出扁平列表，**不破坏**已有 `im.*` / `task.*` code。

### 4.3 流程技能包（L3 · 对齐 Grok Skills）

| 概念 | 说明 |
|------|------|
| 存储 | 表或受控资源（优先 DB + 管理/工作台 UI；可兼容导入 Markdown） |
| 清单 | 只注入 name + description + when-to-use（有字符预算） |
| 激活 | 用户显式选择 / 模型调 `procedure.invoke` / 斜杠命令（后置） |
| 激活后 | 将正文注入本轮上下文；可选声明 `allowedTools` 进一步收窄热路径 |
| 与 Tool 关系 | Skill **不是** `*-api`；执行必须落到 L1/L2 |

写好 `description` / `when-to-use` = 发现质量的第一要素（与 Grok 文档一致）。

---

## 5. 与现有代码的映射

| 现状 | 演进 |
|------|------|
| `BuiltinSkill` + `SkillRegistry` | 保留；增加 `domain`、`tier(HOT\|DEFERRED)`、`tags` 元数据 |
| `AgentDef.skillIds` | 拆或扩展为：热路径包 ID + 可检索域白名单 + 流程技能包 ID（可用 JSON 兼容迁移） |
| `im.read_*` Carrier 强制合并 | 归入 **Channel 热路径配置文件包** `profile:im-carrier` |
| `ImCarrierToolContext` | 保留并推广：凡 Deferred/Hot 读 IM，必须绑定会话上下文 |
| `SkillDescriptor` | 扩展 module/domain；Catalog API 按域分组返回（给人看的管理 UI） |
| Spring AI ToolCalling | Hot ∪ Meta 始终注册；Deferred 可「本轮动态追加」或仅经 `capability.use` 服务端分发（推荐后者，工具列表更稳） |

**推荐默认：Deferred 不改模型 tools 列表，只经 `capability.use` 服务端分发**——与 Grok「use_tool 保持列表稳定」同构，也更贴合 Spring AI 常见用法。

---

## 6. 权限与安全（能力发现不能削弱 ACL）

发现层解决「找得到」，权限层解决「不能乱拿」：

1. **Catalog 可见 ≠ 可调用**：搜索结果只含授权集合。
2. **Tool 实现层二次鉴权**：与今 `ImConversationReadApi` + Principal 相同；**禁止**只信模型传入的 ID。
3. **Channel 最小权限**：IM Channel 默认不可检索「任意会话」类工具。
4. **流程技能包的 `allowedTools`**：激活后若声明子集，Runtime 临时收窄；未声明则沿用当前 Channel 规则。
5. **审计**：记录 `capability.search` 查询、`capability.use` 的 code 与主体，便于追责。

---

## 7. 产品与工程维护方式（对齐 AGENTS 模块理念）

```text
relayflow-module-agent-biz/.../skill/
  im/          # im.* 适配器 → im-api
  task/        # task.* → task-api
  calendar/    # …
  meta/        # capability.search / capability.use / procedure.invoke
  procedure/   # 流程技能包加载（非域 API）
```

- **按域维护**：新能力只加对应包 + Catalog 注册 + 域摘要文案。
- **管理端 / 工作台**：Catalog UI **按域分组**（给人）；模型侧用摘要 + search（给 AI）。
- **Agent 配置 UX（目标）**：勾选「热路径能力包」+「可检索业务域」+「流程技能」，而不是勾 80 个原子 tool（高级模式可展开）。

这与你说的「先 IM 大类 → 再看技能是否存在」一致：  
- **对人**：域分组 Catalog；  
- **对模型**：域摘要 + `capability.search(query="im 历史")`；  
- **对执行**：仍走 `*-api` + ACL。

---

## 8. 产品配置面与后期扩展（提示词 · 系统技能 · 市场 Skill · 知识库）

本节回答：「智能体可配提示词、可拓展技能、以后上市场 Skill、再加知识库——当前三层架构够不够往后长？」  
**结论：够作为扩展骨架；不要求一次实现，但配置对象与挂载点现在就要分清，避免后期推倒。**

### 8.1 一个智能体上用户会配什么（目标产品面）

```text
AgentDef（一个智能体）
├── ① 人设提示词 personaPrompt          ← 已有方向：systemPrompt / Builder base+persona
├── ② 热路径能力包 hotPacks[]            ← 系统内置包，如 task-basic；Channel 可强制包
├── ③ 可检索业务域 deferredDomains[]     ← 长尾 Tool 的授权范围（im/task/…）
├── ④ 流程技能包 procedureSkillIds[]     ← 说明型 Skill（自建或市场安装后绑定）
└── ⑤ 知识范围 knowledgeBindings[]       ← 后期：知识库/空间 ID + 检索策略（见 §8.5）
```

| 配置项 | 是否「技能」 | 落点 |
|--------|--------------|------|
| 提示词 | **否** | L0 提示词组装（Builder）；与 Tool/Skill 正交 |
| 系统内置能力（如 IM 读） | **是 · Tool 包** | L1 热路径或 L2 域；**不是**用户从市场装的 Markdown |
| 用户勾选的业务能力 | **是 · Tool** | L1 包或 L2 域白名单 |
| 市面/社区 Skill | **是 · 流程包（L3）**，偶发附带 Tool 包 | 安装进租户 Catalog → 再绑定到 AgentDef |
| 知识库 | **能力源**，暴露为 `kb.*` Tool 和/或 检索注入 | 见 §8.5；不塞进提示词当唯一手段 |

### 8.2 「系统默认技能」到底是什么？（含你理解的 IM）

建议产品语言统一成三类来源，避免都叫「技能」：

| 来源 | 例子 | 谁决定挂上 | 模型怎么看到 |
|------|------|------------|--------------|
| **A. Channel 强制包** | Carrier / bot_dm：`im.read_*` | **系统按通道强制**，用户不必也不应关掉（或仅高级开关） | L1 热路径 |
| **B. 平台内置可选包** | `task-basic`、`calendar-basic` | 用户在 Agent 配置里勾选 | L1 或授权进 L2 |
| **C. 扩展/市场包** | 社区「周报助手」流程、第三方 MCP 工具集 | 先安装到租户，再绑定到 Agent | L3 清单 + 可选 Deferred Tool |

因此：**IM 读能力更接近 A（通道强制系统包），不是「每个智能体默认 skillIds 里写死一串」的偶然实现。**  
当前 Carrier 强制合并 `im.read_*` = A 的雏形；P1 应收成具名包 `profile:im-carrier`。

工作台 Session（`agent_web`）**默认不必**带全量 IM 读；只有用户显式授权或场景需要时再进 B/C。

### 8.3 市面 Skill（后期）如何挂进来

不新开第四套运行时，复用 L3 + 可选 L2：

1. **安装**：市场/插件 → 写入租户 `procedure_skill`（及可选 `tool_pack` 声明）。  
2. **审核/权限**：租户管理员启用；Tool 包必须声明调用哪些 `*-api` / 外域，过允许列表。  
3. **绑定**：AgentDef 只存 ID，与自建流程技能同一字段。  
4. **运行**：短清单发现 → `procedure.invoke` 注入正文 → 仍经 `capability.use` / 热路径执行。

若市场项是「一堆 MCP 工具」而非说明文：走 **L2 域/连接器**（类 Grok MCP），不要伪装成 L3。

P2 未落地前 **禁止**开放任意市场 Tool（§9 同旨）；P3 可先做「仅流程说明类」市场 Skill。

### 8.4 提示词与技能的关系（避免混用）

- **提示词**：稳定人设、语气、禁区、输出格式。  
- **流程技能包**：可插拔的任务剧本（「如何写周报」「如何处理请假」）。  
- **Tool**：对系统真实副作用（读 IM、改任务、查知识库）。

Builder 顺序建议（目标）：

```text
平台 base 提示
  + 人设 persona
  + （可选）本轮已激活的流程技能正文
  + 域摘要 / 热路径工具说明
  + 知识库策略说明（若绑定了 KB）
```

不要把知识库全文或全部 Skill 正文塞进人设字段。

### 8.5 知识库（后期）挂载点

知识库是 **又一类能力源**，推荐双通道，都吃现有骨架：

| 模式 | 做法 | 何时用 |
|------|------|--------|
| **检索 Tool** | `kb.search` / `kb.get`：L1（强依赖 KB 的 Agent）或 L2（经 capability.search） | 模型主动决定何时查 |
| **预检索注入** | Session/Carrier 入站时按 query 检索 Top-K 片段写入上下文（类「默认短期历史」） | 强 RAG 助手、客服 Bot |

约束：

- 真源在未来 `kb` / `docs` 域 `*-api`，Agent 只适配；  
- 绑定在 AgentDef.`knowledgeBindings`（库 ID、权限范围、是否预注入）；  
- **不要**把「知识库」做成第四种与 L1/L2/L3 平行的发现协议——它是 **域 + 可选预注入**。

Grok 侧有 `memory_search` / `memory_get` 同类思路：记忆/知识是可检索后端，不是扁平 tools 爆炸。

### 8.6 扩展性总表（你列的后期是否盖得住）

| 后期能力 | 能否在本架构上长 | 主要挂载点 |
|----------|------------------|------------|
| 每智能体提示词 | ✅ 已在母稿 Builder | AgentDef persona |
| 系统默认 IM 等 | ✅ Channel 强制包 (A) | L1 热路径包 |
| 用户拓展业务技能 | ✅ 勾选包 / 域 | L1 + L2 |
| 市面 Skill | ✅ 安装后当 L3（+可选 Tool 包） | Catalog → AgentDef |
| 知识库 | ✅ `kb.*` Tool + 预注入 | 域 API + knowledge |
| 外部 MCP | ✅ 作 L2 连接器（P4） | search/use 同一协议 |

**不能满足、也不该用本架构硬撑的**：无权限的「装了就能调全公司任意 API」、把市场脚本当可信后端直连数据库——那是产品红线，与发现分层无关。

---

## 9. 分阶段落地（避免一次做完）

| 阶段 | 内容 | 说明 |
|------|------|------|
| **P0（现状巩固）** | 热路径 + `domain` 元数据 + Catalog 按域展示 | 几乎无 Runtime 大改；IM/Task 已具备前缀 |
| **P1** | Channel 热路径包（取代 Carrier 硬编码合并） | 配置化 `im-carrier` / `workspace-default`；对应 §8.2-A |
| **P2** | `capability.search` + `capability.use`；长尾迁出扁平列表 | 对齐 Grok 二级发现；索引可先做内存 |
| **P3** | 流程技能包（DB）+ 短清单 + invoke；可先支持「仅说明类」市场导入 | 对齐 Grok Skills；与 Tool 目录分离 |
| **P4** | 知识库绑定 + `kb.*`；检索升级；可选 MCP 网关 | §8.5；接口保持 P2 语义 |

**禁止**：在未做 P1/P2 前引入「对外技能市场 / 任意 MCP」导致权限与发现同时爆炸。

---

## 10. 刻意不抄 Grok 的部分

- 以编程 Agent（改文件、跑 shell）为默认热路径；
- 以本地 `SKILL.md` 文件树为唯一真源；
- 终端 slash / TUI 交互细节；
- 把 MCP 当唯一扩展模型（企业域优先一等 `*-api` Tool）；
- 嵌 grok-build 为运行时。

---

## 11. 成功标准（日后验收用）

1. 单 Channel 热路径 tools **有上限**（建议可配置，默认 ≤ 15）。  
2. 新增一个业务域工具：**默认进 Deferred**，不自动撑爆所有 Agent 的 tools 列表。  
3. 模型通过「域摘要 + search」能找到工具；search 无结果时明确失败，不编造。  
4. IM 相关工具在 Carrier 外调用仍受会话/成员校验；跨会话 ID 拒绝。  
5. 流程技能包增加时，仅清单变长（有预算），不线性增加 Tool schema。  
6. 架构演进不破坏 `*-api` 边界与现有 `im.*` / `task.*` code。  
7. Agent 配置能区分：人设提示词 / 系统通道包 / 可选业务包 / 流程技能（市场与自建同一绑定模型）。  
8. 知识库以域 Tool + 可选预注入接入，不污染人设字段、不另起一套发现协议。

---

## 12. 文档关系

| 文档 | 关系 |
|------|------|
| [`agent-architecture-draft.md`](agent-architecture-draft.md) | Agent 平台总分层、Channel、Runtime、子 change 路线 |
| **本文** | Capability / Skill / 发现协议（Grok 理念对齐） |
| [`im-messaging-architecture-draft.md`](im-messaging-architecture-draft.md) | Bot 载体与 IM 边界 |
| OpenSpec 子 change | 按 §9 阶段开单实现；本文不是 tasks 真源 |

## 附录：术语对照

| RelayFlow | Grok Build 近似 |
|-----------|-----------------|
| Hot Tools | Built-in tools（小集合直接暴露） |
| Domain Catalog + capability.search/use | MCP index + search_tool / use_tool |
| Domain 摘要 | Connected MCP servers reminder |
| Procedure Skill | SKILL.md + Skill tool |
| Channel 强制包 (A) | 模式/通道默认 toolset |
| 市场 Skill | 插件/社区 skills（安装后再绑定） |
| 知识库 `kb.*` / 预注入 | memory_search / 上下文检索类能力 |
| AgentDef capability profile | tools allowlist +（间接）MCP 连接集 |
| Channel 热路径包 | 按模式裁剪 toolset |
| BuiltinSkill → \*-api | Tool 实现（我们强制域 API） |
