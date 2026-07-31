## ADDED Requirements

### Requirement: Per-user conversation tag library

系统 MUST 为每个租户内用户维护私有的会话标签库（标签名称属于该用户，MUST NOT 共享给其他用户）。用户 MUST 能创建新标签并纳入本人标签库。同用户下标签名在规范化（至少 trim）后 MUST 唯一。

#### Scenario: Create tag in library

- **WHEN** 成员创建名称合法的新标签
- **THEN** 该标签出现在其本人标签库中，其他用户不可见

#### Scenario: Duplicate name rejected or idempotent

- **WHEN** 成员再次创建与本人已有标签规范化后同名的标签
- **THEN** 系统拒绝重复或返回已有标签（实现二选一，但 MUST NOT 插入第二条同名库记录）

### Requirement: Assign tag to conversation via context submenu

工作台会话列表右键菜单 MUST 提供「标签」入口，并以**二级（级联）子菜单**展示：悬停或展开后列出当前用户历史标签供点选，将选中标签打到该会话（仅对该用户生效）。打标 MUST 幂等。本批 MUST NOT 要求在会话列表行或聊天标题强制展示这些自定义标签文案；消息侧分组栏 MUST 能按具体标签筛选（见 `im-conversation-groups`）。

#### Scenario: Assign existing tag from submenu

- **WHEN** 成员右键某会话，在标签二级菜单中点选已有标签
- **THEN** 该会话对该成员关联上该标签

#### Scenario: Tags not required on list row this slice

- **WHEN** 成员已为会话打上自定义标签
- **THEN** 本批会话列表行与聊天标题 MUST NOT 因此强制展示该自定义标签文案或图标

### Requirement: Add new tag action at bottom of submenu

标签二级菜单 MUST 在历史标签列表下方提供「添加新标签」；选择后 MUST 允许输入名称，成功后 MUST 将新标签写入本人标签库并打到当前会话。

#### Scenario: Create and assign in one flow

- **WHEN** 成员在标签二级菜单选择「添加新标签」并提交合法名称
- **THEN** 标签库增加该标签，且当前会话已关联该标签

### Requirement: Long tag library scrolls in submenu

当历史标签数量较多时，二级菜单中的标签列表区域 MUST 限制可视高度并支持滚动；「添加新标签」入口 MUST 仍可被发现（优先固定在列表底部可见，或不随内容完全滚出视口而无提示）。

#### Scenario: Many tags remain usable

- **WHEN** 成员标签库条目超过二级菜单一屏可展示数量
- **THEN** 可通过滚动浏览历史标签，且仍能到达「添加新标签」
