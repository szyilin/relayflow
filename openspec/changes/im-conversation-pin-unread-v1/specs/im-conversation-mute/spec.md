## ADDED Requirements

### Requirement: Conversation message mute toggle

系统 MUST 允许会话成员为本人开启或取消该会话的消息免打扰；状态 MUST 仅对当前用户生效。V1 MUST 支持右键（或等价菜单）开关式免打扰；MAY 复用成员行 `mute_until`（空为未免打扰，未来时间表示免打扰中）。

#### Scenario: Mute conversation from list menu

- **WHEN** 成员对某会话开启消息免打扰
- **THEN** 该会话对其本人进入免打扰态，且不影响其他用户

#### Scenario: Unmute conversation

- **WHEN** 成员取消某会话的消息免打扰
- **THEN** 该会话对其本人恢复普通未读展示规则

### Requirement: Muted conversation shows subtle unread dot not loud badge

当会话处于免打扰且存在 inbox 未读时，会话列表 MUST NOT 展示醒目的未读数字大红点（或等价高对比数字角标），MUST 改为不显眼的小红点提示。无未读时 MUST NOT 显示该小红点。非免打扰会话 MUST 继续使用醒目未读角标（与现有一致）。

#### Scenario: Muted with unread uses quiet dot

- **WHEN** 免打扰会话 `unreadCount > 0`
- **THEN** 列表行显示弱小红点且不显示未读数字大红点

#### Scenario: Unmuted with unread uses loud badge

- **WHEN** 非免打扰会话 `unreadCount > 0`
- **THEN** 列表行显示醒目未读角标（可含数字）

### Requirement: Mute bell affordance on list row

免打扰会话在会话列表行 MUST 展示静音小铃铛（划掉铃铛或等价图标），建议位于预览行右侧、与旗标等右侧状态协调排布。未免打扰时 MUST NOT 显示该静音铃铛。

#### Scenario: Muted row shows bell

- **WHEN** 成员查看已免打扰的会话列表行
- **THEN** 可见静音小铃铛

### Requirement: Global unread aggregate excludes muted conversations

工作台消息入口等汇总未读角标 MUST NOT 将免打扰会话的未读数计入醒目汇总（仅合计非免打扰会话的未读）。免打扰会话自身的 `unreadCount` MUST 仍可正确维护，供进会话与行内小红点使用。

#### Scenario: Muted unread does not inflate nav badge

- **WHEN** 仅免打扰会话存在未读、其余会话无未读
- **THEN** 消息模块醒目汇总角标不因此显示未读（或为 0）
- **AND** 该免打扰会话行仍可显示弱小红点
