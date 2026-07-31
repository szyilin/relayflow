## ADDED Requirements

### Requirement: Conversation pin and unpin

系统 MUST 允许会话成员将本人成员行上的会话置顶或取消置顶；置顶状态 MUST 仅对当前用户生效。成员表 MUST 使用 `pinned`（及排序用的 `pinned_at` 或等价字段）。会话列表 MUST 将置顶会话排在非置顶之前，置顶之间按最近置顶时间（或明确约定的稳定序）排序。

#### Scenario: Pin conversation from list menu

- **WHEN** 成员在会话列表对某会话执行置顶
- **THEN** 该会话在其列表中进入置顶区且对其他用户的列表无影响

#### Scenario: Unpin conversation

- **WHEN** 成员对已置顶会话取消置顶
- **THEN** 该会话离开置顶区并回到普通列表排序

### Requirement: Pinned section visual distinction

工作台会话列表中的置顶会话行 MUST 在视觉上与普通会话区分（例如相对降低对比度或使用略深的行底），且 MUST 使用工作台既有视觉 token，MUST NOT 引入无关色相主题。

#### Scenario: Pinned rows look distinct

- **WHEN** 列表同时存在置顶与非置顶会话
- **THEN** 用户可仅凭行样式区分置顶区与普通区

### Requirement: Collapsible pinned section with aggregated unread

工作台 MUST 提供可操作的置顶区折叠横条：折叠后隐藏置顶会话行，横条上 MUST 聚合展示被折叠会话的未读提示（至少包括未读总数；若任一会话有 @ 我等既有强提示，MUST 在横条上有可见表示或保留等价提示）。展开后恢复各置顶行及其独立未读展示。

#### Scenario: Collapse hides pinned rows but keeps badge

- **WHEN** 用户折叠置顶区且区内存在未读
- **THEN** 置顶行不可见，折叠横条仍显示聚合未读（或等价红点）

#### Scenario: Expand shows pinned rows again

- **WHEN** 用户展开置顶区
- **THEN** 置顶会话行重新可见且各自未读展示正确

### Requirement: Pinned section collapse preference syncs across devices

置顶区折叠态 MUST 经用户偏好持久化并多端一致（例如 `settings.im.pinnedSectionCollapsed`，默认展开/`false`）。实现 MUST 遵循既有用户偏好 C 类惯例（读合并、写 upsert；无行不预插）。localStorage MUST NOT 作为跨设备写权威。

#### Scenario: Collapse state restored on another device

- **WHEN** 成员在一端折叠置顶区并成功保存偏好
- **THEN** 同一租户账号在另一端水合偏好后置顶区同为折叠

#### Scenario: Default is expanded

- **WHEN** 成员尚无 IM 折叠偏好键
- **THEN** 置顶区默认展开
