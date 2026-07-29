# agent-capability Specification

## Purpose
TBD - created by archiving change agent-capability-discovery-v1. Update Purpose after archive.
## Requirements
### Requirement: Skill domain and tier metadata

系统 MUST 为每个内置可执行技能（BuiltinSkill）提供稳定元数据：至少 `domain`（与业务域对齐，如 `im`、`task`）与 `tier`（`HOT` 或 `DEFERRED`）。Catalog API MUST 能按 `domain` 分组返回技能条目。未知 domain/tier MUST 在注册或校验阶段被拒绝或落到明确默认策略（实现须文档化；推荐缺省 `DEFERRED` 且 domain 必填）。

#### Scenario: Catalog groups by domain

- **WHEN** 客户端请求技能目录
- **THEN** 响应可按 domain 分组（或含 domain 字段供客户端分组）
- **AND** 每条含 code、name、description、domain、tier

#### Scenario: Deferred skills are not all bound as model tools by default

- **WHEN** 某技能 `tier=DEFERRED` 且未进入热路径包
- **THEN** Agent 运行时 MUST NOT 仅因「在全局 Catalog 中」就将其注册为 Spring AI tool
- **AND** 该技能仍可经能力发现协议被检索与调用（见 capability.search / capability.use）

### Requirement: Channel hot-path capability packs

系统 MUST 支持具名热路径能力包（capability pack），并按 Channel 装配 L1 工具集。至少定义：

- `im-carrier`：用于 `im_bot` Channel，MUST 包含 IM 会话读类技能（`im.read_message`、`im.read_nearby`、`im.read_history` 或等价 code）
- `workspace-default`：用于 `agent_web` Channel 的默认热路径起点（可为空集或平台约定的 HOT 子集）

`im_bot` 入站 MUST 通过 `im-carrier` 包装配热路径，MUST NOT 依赖对 `im.read_*` 的硬编码临时合并作为长期机制。`agent_web` MUST NOT 默认强制挂载全量 IM 读技能。

#### Scenario: Carrier inbound uses im-carrier pack

- **WHEN** Agent 经 IM Carrier（bot_dm 或群 @）入站执行
- **THEN** 运行时热路径工具集包含 `im-carrier` 包内技能
- **AND** 不再以「无文档的硬编码列表合并」作为唯一装配方式

#### Scenario: Workspace session does not force IM read pack

- **WHEN** 成员在工作台 Agent Session 发消息且未授权 IM 域/包
- **THEN** 运行时 MUST NOT 仅因存在全局 `im.read_*` 技能就强制注入全部 IM 读工具

### Requirement: Capability search and use meta tools

系统 MUST 提供面向模型的稳定 Meta 工具（名称可实现为 `capability.search` / `capability.use` 或文档等价 code）：

1. **search**：在当前 Agent/Channel **已授权** 的域与技能范围内，按关键词检索技能；返回 code、domain、description、input schema（或摘要）与条数上限。
2. **use**：按 skill code + 参数调用对应 BuiltinSkill；MUST 校验 code 在授权集合内；MUST 在实现层保留原有 ACL（如 IM 会话绑定）；MUST NOT 仅凭模型传入的任意会话/资源 ID 放行。

Deferred 技能的默认调用路径 MUST 为 use（或经 search 后再 use）。对已在热路径直接暴露的技能，use MUST 拒绝或返回明确纠偏（引导直接调用热路径工具），避免双路由。

运行时暴露给模型的 tools 列表 MUST 保持为：**热路径工具 ∪ Meta 工具**；MUST NOT 因 search 命中而把全部 Deferred schema 持久塞进该列表。

#### Scenario: Search only within authorized domains

- **WHEN** Agent 的可检索域白名单为 `task`
- **AND** 模型调用 capability.search 查询 IM 相关关键词
- **THEN** 结果 MUST NOT 返回未授权域的可调用技能（或为空）

#### Scenario: Use rejects unauthorized or cross-scope tool

- **WHEN** 模型调用 capability.use 且 code 不在授权集合
- **THEN** 调用失败且不执行业务副作用

#### Scenario: Use respects IM conversation binding

- **WHEN** 在 Carrier 上下文中经 use 调用 IM 读技能且模型传入其他 conversationId
- **THEN** 系统拒绝越权读取（与现有 IM 读权限要求一致）

### Requirement: Domain summary for the model

当 Agent 存在已授权业务域时，系统 MUST 在组装提示词时附带简短域摘要（域名 + 一句能力说明），引导模型用 search/use 获取长尾工具；MUST NOT 用全量工具 schema 代替该摘要。

#### Scenario: Authorized domains appear as short summary

- **WHEN** Agent 授权了 `task` 与 `im` 域（在适用 Channel 下）
- **AND** 一轮运行开始
- **THEN** 有效系统提示或等价上下文含这些域的短摘要

