## ADDED Requirements

### Requirement: Conversation identity badges after title

工作台会话列表中，表示会话身份/类型的标签（至少包括群、助手，以及机器人、智能体、官方等同类标签若展示）MUST 放在会话**显示名之后**（与用户名同一行）。MUST NOT 将这些身份标签放在预览文案行的开头。消息态提示（如「有人@我」）MAY 继续出现在预览行。

#### Scenario: Group badge follows title

- **WHEN** 列表展示群会话
- **THEN** 「群」（或等价标签）出现在标题名之后，而非预览文案前缀位置

#### Scenario: Assistant or bot badge follows title

- **WHEN** 列表展示助手 / 机器人 / 智能体类会话且需展示身份标签
- **THEN** 该标签出现在标题名之后

### Requirement: Conversation list avatar vertically centered

会话列表行头像 MUST 相对该行「标题行 + 预览行」文本块垂直居中，MUST NOT 仅相对标题行顶对齐而显得明显偏上。

#### Scenario: Avatar centers to two-line block

- **WHEN** 列表行同时有标题与预览两行文本
- **THEN** 头像视觉中心约落在两行文本块的垂直中线附近
