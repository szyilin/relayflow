## ADDED Requirements

### Requirement: Conversation flag toggle

系统 MUST 允许会话成员为本人成员行设置或取消会话旗标；旗标 MUST 仅对当前用户生效，并随成员数据多端可见。本批 MUST 提供列表右键（或等价菜单）开关；消息侧分组「标记」MUST 能按旗标筛选会话。

#### Scenario: Flag conversation from list menu

- **WHEN** 成员在会话列表对某会话执行加旗标
- **THEN** 该会话对其本人标记为已旗标，且不影响其他用户

#### Scenario: Unflag conversation

- **WHEN** 成员对已旗标会话取消旗标
- **THEN** 该会话对其本人不再显示为已旗标

#### Scenario: Flagged group includes flagged conversation

- **WHEN** 成员已旗标某会话并选中分组「标记」
- **THEN** 该会话出现在分组结果中

### Requirement: Flag icon on list title row and chat header

已旗标会话 MUST 在会话列表**标题行最右侧**展示小旗帜图标（与用户名同一行，不得仅出现在预览行）。打开该会话后，聊天面板标题区 MUST 在显示名之后同步展示旗帜。未旗标时 MUST NOT 显示已旗标旗帜。

#### Scenario: List shows flag on title row

- **WHEN** 成员查看已旗标会话的列表行
- **THEN** 标题行最右侧可见小旗帜

#### Scenario: Chat header shows flag after name

- **WHEN** 成员打开已旗标会话
- **THEN** 聊天标题显示名后可见同一语义的旗帜图标
