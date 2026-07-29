## ADDED Requirements

### Requirement: Agent capability profile on definition

Agent 定义 MUST 能表达能力配置，至少包括：

- 热路径能力包 ID 列表（hot packs）
- 可检索业务域列表（deferred domains）

存储 MUST 兼容现有 `skillIds` / `skill_ids_json`：当尚无 profile 时，原 `skillIds` MUST 仍可解析为显式热路径技能列表。保存时未知包 ID / 未知域 / 未知技能 code MUST 被拒绝。人设提示词（system prompt）仍为独立字段，MUST NOT 与技能列表混存为唯一配置面。

#### Scenario: Save agent with hot packs and deferred domains

- **WHEN** 成员保存 Agent，指定合法 hot packs 与 deferred domains
- **THEN** 定义持久化成功
- **AND** 后续运行按该 profile 装配热路径与 search 授权范围

#### Scenario: Legacy skillIds still load

- **WHEN** 某 Agent 仅有历史 `skillIds`、无 capability profile
- **THEN** 运行时仍可将这些 code 作为热路径工具解析
- **AND** 不因缺少 profile 字段而无法启动会话

## MODIFIED Requirements

### Requirement: Bindable skill catalog

The system SHALL maintain a catalog of built-in skills that can be bound to an Agent via explicit skill codes and/or capability packs and deferred domains (see Agent capability profile). At runtime, **hot-path** bound skills SHALL be exposed as Spring AI tools to the agent loop, together with capability discovery meta tools when deferred domains are enabled. Deferred skills SHALL be invocable via the capability search/use protocol and MUST NOT all be flattened into the model tool list solely by appearing in the global catalog. Built-in skill implementations MUST call other domains only via `*-api` under the acting user's authorization (tenant + user from the current security context), and MUST honor channel/conversation binding rules where applicable (e.g. IM read).

The V1 catalog SHALL include at least:

- `task.search` — search the current user's tasks by keyword
- `task.list_due` — list the current user's tasks with due time in a range
- `im.read_message` / `im.read_nearby` / `im.read_history` — IM conversation read (primarily via `im-carrier` hot pack on `im_bot` channel)

Catalog entries MUST expose `domain` and `tier` metadata. Unknown skill codes or pack ids MUST be rejected when saving an Agent definition. Skills outside the agent's hot path and deferred authorization MUST NOT be available as directly registered tools for that Agent.

#### Scenario: Bound skill available as tool

- **WHEN** an Agent has a built-in skill on its hot path (via skillIds or hot pack)
- **AND** a session turn runs
- **THEN** the runtime may invoke that skill as a tool
- **AND** the implementation does not depend on foreign `*-biz` modules

#### Scenario: Unbound skill not available

- **WHEN** an Agent does not bind `task.search` on the hot path and does not authorize the `task` deferred domain (or equivalent)
- **AND** a session turn runs
- **THEN** the `task_search` tool is not provided directly to the model
- **AND** capability.use of `task.search` is rejected

#### Scenario: Reject unknown skill on save

- **WHEN** a client saves an Agent with a skill code not in the catalog
- **THEN** the system rejects the save

#### Scenario: Catalog exposes domain metadata

- **WHEN** a client lists the skill catalog
- **THEN** each entry includes domain and tier suitable for grouped display

### Requirement: AgentBuilder assembles prompt and tools hook

Before each run (or at session bind time), the system SHALL assemble the effective system prompt from a platform base template plus the Agent persona prompt, plus a short summary of authorized capability domains when present. The system SHALL attach the Agent's **hot-path** skill tools and, when deferred domains are configured, the capability search/use meta tools. Empty hot-path allowlist remains allowed. Channel-specific packs (e.g. `im-carrier` on IM carrier inbound) SHALL be merged according to channel rules.

#### Scenario: Persona appended to platform base

- **WHEN** an Agent runs with a persona system prompt
- **THEN** the effective prompt includes the platform base template and the persona

#### Scenario: Hot path and meta tools attached

- **WHEN** an Agent has hot-path skills and at least one deferred domain
- **AND** a run starts
- **THEN** the runtime tool list includes those hot-path tools and capability search/use meta tools
- **AND** does not flatten all deferred catalog skills into the tool list
