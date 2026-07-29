# agent Specification

## Purpose
TBD - created by archiving change admin-llm-provider-v1. Update Purpose after archive.
## Requirements
### Requirement: Admin LLM connection management

Authorized administrators SHALL be able to create, update, delete, list, and connectivity-test tenant LLM connections of type `openai_compatible`. API keys MUST be stored encrypted server-side and MUST NOT be returned in API responses; responses MAY expose `apiKeyConfigured` boolean only.

#### Scenario: Create connection without echoing secret

- **WHEN** an admin with `agent:llm:update` creates a connection with an API key
- **THEN** the key is persisted encrypted
- **AND** subsequent GET/list responses omit the raw key and set `apiKeyConfigured` true when a key exists

#### Scenario: Connectivity test requires test permission

- **WHEN** an admin without `agent:llm:test` calls the test endpoint
- **THEN** the request is denied by RBAC

#### Scenario: Update keeps previous key when blank

- **WHEN** an admin updates a connection and omits or leaves API key blank
- **THEN** the previously stored encrypted key is retained

### Requirement: Enabled models for workspace agents

Authenticated workspace members SHALL be able to list enabled LLM connections for the current tenant as model references (`code`, `name`, `modelName`) without secrets, for selecting an Agent `modelRef`.

#### Scenario: Member lists enabled models

- **WHEN** a logged-in member calls the enabled-model list API
- **THEN** only `enabled=true` connections of the current tenant are returned
- **AND** no API key material is included

### Requirement: Admin LLM settings UI

The admin console SHALL expose a model settings page under infrastructure navigation for managing LLM connections, gated by `agent:llm:query` (and update/test actions by their permissions).

#### Scenario: Nav entry for model settings

- **WHEN** an admin has `agent:llm:query`
- **THEN** the admin sidebar shows a model settings entry leading to the LLM connection management page

### Requirement: Agent definition CRUD for members

Authenticated members SHALL be able to create, update, list, and soft-delete their own Agent definitions in the current tenant. Each definition MUST include name, system prompt, and a `modelRef` that references an enabled `agent_llm_connection.code`. Provider secrets MUST NOT be stored on the definition.

#### Scenario: Create agent with modelRef

- **WHEN** a member creates an Agent with prompt and an enabled modelRef
- **THEN** the definition is persisted owned by that member
- **AND** raw API keys are not stored on the definition

#### Scenario: Reject unknown modelRef

- **WHEN** a member creates an Agent with a modelRef that is missing or disabled
- **THEN** the system rejects the request with a clear error

### Requirement: Agent session chat via Spring AI runtime

Members SHALL be able to create sessions against an Agent they can access, send user messages, and receive assistant replies produced by a Spring AI-backed Agent runtime. Session turns MUST be stored only in agent session tables and MUST NOT write `im_message`.

#### Scenario: Send message stores session turns only

- **WHEN** a member sends a message in an Agent session
- **THEN** the user and assistant messages are persisted in the agent session message store
- **AND** no `im_message` row is required for that turn

#### Scenario: Runtime uses organization LLM connection

- **WHEN** a session turn runs
- **THEN** the runtime resolves the Agent's modelRef to the tenant LLM connection
- **AND** invokes Spring AI chat completion using that connection's endpoint and decrypted key

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

### Requirement: Workspace agent shell UI (skeleton)

The workspace SHALL provide an Agent product shell where members can browse Agents, start a new task session, view session history, and chat. Agent chat content MUST NOT be presented as ordinary IM conversation list items.

#### Scenario: New task opens agent chat surface

- **WHEN** a member starts a new task and selects an Agent
- **THEN** the UI opens an Agent chat session surface
- **AND** that surface is not the `/app/messages` conversation list

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

### Requirement: Scheduled agent automation with bot push

The system SHALL allow configuring automations that run an Agent on a cron schedule and push results via the Agent's carrier bot to a configured IM user target. Automation result delivery MUST use `ImBotApi` / the carrier bot channel and MUST NOT append to an Agent workspace session.

Automations SHALL store at least: title, prompt, agent id, cron expression, status, target user id, next/last run metadata. A platform XXL-JOB handler SHALL periodically execute due automations.

#### Scenario: Cron run pushes via bot

- **WHEN** a scheduled automation fires for an Agent with a carrier bot
- **THEN** the system runs the Agent runtime for the automation prompt
- **AND** delivers the result through `ImBotApi` / carrier bot
- **AND** `/app/messages` can show that delivery while Agent session history is unchanged

#### Scenario: Manual run now

- **WHEN** the owner triggers run-now for an active automation
- **THEN** the system executes the same run path as the scheduler
- **AND** still does not write `agent_session` messages

### Requirement: Workspace agent uses live APIs (skeleton)

After integrate, the workspace Agent shell SHALL load Agents and session chat through live `/app-api/agent` APIs without temporary store mock data for those flows.

#### Scenario: Chat round-trip against server

- **WHEN** a member sends a message in an Agent session with server running
- **THEN** the UI displays the assistant reply from the live runtime
- **AND** temporary mock data for that flow is removed

### Requirement: Agent domain boundary

The system SHALL treat enterprise Agents as a dedicated domain (`agent`), separate from IM message storage. Agent configuration, agent chat sessions, and agent runtime execution MUST NOT write business chat history into `im_message` solely because a user chats in the Agent workspace.

#### Scenario: Agent workspace chat stays in agent session

- **WHEN** a member sends a message in the Agent workspace UI to an Agent
- **THEN** the system persists that turn in an Agent session store
- **AND** the system MUST NOT create a corresponding `/app/messages` timeline item for that turn unless an explicit Bot delivery path is invoked

### Requirement: Agent definition composition

The system SHALL represent a configurable Agent as a definition composed of at least: display identity, system prompt (persona), a model reference to an organization-enabled model connection, and a set of bound skills. API keys for model providers MUST NOT be stored on the Agent definition.

#### Scenario: Agent references model without embedding secrets

- **WHEN** a user configures an Agent model
- **THEN** the definition stores a `modelRef` (or equivalent reference) to an admin-managed connection
- **AND** raw provider secrets are not persisted on the Agent definition row

### Requirement: Runtime uses Spring AI behind a stable port

The system SHALL execute Agent turns through an Agent runtime port whose primary LLM and tool-calling implementation is Spring AI. Application controllers and non-runtime services MUST NOT call provider SDKs directly for Agent chat.

#### Scenario: Chat goes through AgentRuntime

- **WHEN** the workspace requests an Agent completion for a session
- **THEN** the call is handled by the Agent runtime abstraction
- **AND** Spring AI ChatClient / tool-calling is used inside that runtime implementation

### Requirement: Channel delivery separation

The system SHALL distinguish delivery channels for Agent outputs. At minimum it SHALL support an Agent workspace channel (session messages) and an IM Bot channel (messages visible under `/app/messages`). Replies MUST return to the originating channel unless a feature explicitly requests cross-channel delivery (e.g. automation push).

#### Scenario: Automation push uses IM Bot channel only

- **WHEN** an automation run completes and is configured to notify a user or group
- **THEN** delivery uses the IM Bot / `ImBotApi` path
- **AND** the automation result is not required to appear as an Agent workspace session message

### Requirement: Skills as tools calling domain APIs

The system SHALL expose bound skills to the runtime as tools. Built-in skill implementations MUST invoke other domains only through `*-api` modules and MUST NOT depend on other domains' `*-biz` modules. Skill execution MUST run under the acting member's authorization context (no silent privilege escalation to a superuser bot identity).

#### Scenario: Built-in skill uses domain API only

- **WHEN** a skill implementation needs task or calendar data
- **THEN** it calls the corresponding `*-api`
- **AND** it does not access foreign-domain mappers or `*-biz` types

### Requirement: Carrier bot lifecycle follows Agent

When a product Agent requires an IM carrier bot, the IM module SHALL provide a provisioning API usable by the Agent domain. If the Agent is removed, the associated carrier bot MUST be retired according to IM rules. Custom carrier bots exist to serve Agents; they are not a parallel notification bus.

#### Scenario: Deleting agent retires carrier bot

- **WHEN** an Agent that owns a carrier bot is deleted
- **THEN** the system retires or disables that bot via the IM provisioning API
- **AND** no orphan tenant bot remains bound to the deleted Agent definition

### Requirement: Staged delivery via child changes

Platform delivery of Agent capabilities MUST be split across dedicated OpenSpec changes (module scaffold, LLM provider admin, runtime core, workspace UI, skills, carrier bot, automation). A single change MUST NOT implement the entire Agent product surface.

#### Scenario: Mother change does not ship full product

- **WHEN** `agent-platform-foundation` is completed
- **THEN** architecture docs and capability skeleton are in place and child changes are opened
- **AND** full Agent chat UI + runtime + automation need not all be implemented inside the mother change

### Requirement: Carrier inbound uses short IM history

`AgentCarrierInboundApi`（或等价入站实现）MUST 接受并使用由 IM 侧装配的短期历史与可选引用摘要；调用 `AgentRuntime` 时 MUST 将短期历史映射为 runtime `history`（或等价多轮消息），MUST NOT 在存在可读近期消息时固定传入空 history。入站 MUST 在触发用户安全上下文下运行。

#### Scenario: Non-empty history passed to runtime

- **WHEN** carrier 入站命令携带近期消息列表
- **THEN** `AgentRuntime.run` 的 history 含对应轮次
- **AND** 当前用户句为最新 user message（可含引用说明）

### Requirement: Agent im.* skills call ImConversationReadApi only

系统 MUST 提供 BuiltinSkill（建议 code：`im.read_message`、`im.read_nearby`、`im.read_history`）供 Agent 在需要时读取**当前触发会话**更多消息。技能实现 MUST 仅依赖 `ImConversationReadApi`（im-api），MUST NOT 依赖 im-biz 或直查 `im_*` 表。执行 MUST 绑定入站上下文中的 `conversationId`（及触发用户/租户）；模型传入的其他会话 ID MUST 被拒绝。

#### Scenario: Tool reads current conversation only

- **WHEN** Agent 调用 `im.read_history` 且上下文绑定会话 C
- **THEN** 仅返回 C 内消息（受上限约束）

#### Scenario: Tool rejects other conversationId

- **WHEN** 模型入参指定会话 D ≠ 绑定会话 C
- **THEN** 工具失败或拒绝，MUST NOT 返回 D 的内容

#### Scenario: Skill mirrors task.list_due layering

- **WHEN** 实现 `im.*` 技能
- **THEN** 结构为 Agent 薄适配器 → `ImConversationReadApi`，与现有 `task.list_due` → `TaskItemApi` 模式一致

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

