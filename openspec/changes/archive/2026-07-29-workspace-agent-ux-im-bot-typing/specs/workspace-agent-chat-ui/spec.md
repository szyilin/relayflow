## MODIFIED Requirements

### Requirement: Workspace agent chat shell and prompt

工作台智能体侧栏 MUST 常驻展示历史 Session（按最近活动时间分组），MUST NOT 用「历史任务」顶栏入口切换替换该列表。顶栏仅保留新任务 / 智能体 / 自动化；选中智能体或自动化时，管理功能 MUST 显示在右侧主区。发送 Prompt MUST 使用 `UChatPrompt`，智能体选择 MUST 置于 Prompt footer（禁止落地页独立大块 Select）。同一 Session 内 MUST 允许切换智能体且保留消息上下文。

#### Scenario: History always visible

- **WHEN** 用户打开智能体页并切换「智能体」或「自动化」
- **THEN** 侧栏下方仍列出历史对话
- **AND** 右侧主区展示对应管理 UI

#### Scenario: Switch agent mid-session

- **WHEN** 用户在已有 Session 的 Prompt footer 选择另一智能体并继续发送
- **THEN** Session 绑定的 agentId 更新，历史消息仍在该 Session
- **AND** 后续生成使用新智能体的模型与人设

### Requirement: Streaming markdown and waiting indicator

流式正文 MUST 在 delta 到达过程中以 Markdown 方式增量渲染（`Comark` streaming）。从提交到首个内容 delta 期间 MUST 保持等待指示（Nuxt UI Chat submitted/indicator），避免长时间空白。

#### Scenario: User waits for first token

- **WHEN** 用户发送消息且服务端尚未产出 text/reasoning/tool delta
- **THEN** UI 显示 submitted/streaming 等待指示

#### Scenario: Markdown during stream

- **WHEN** 助手 `text-delta` 持续到达
- **THEN** 助手气泡内 Markdown 随增量更新，不必等 `done`
