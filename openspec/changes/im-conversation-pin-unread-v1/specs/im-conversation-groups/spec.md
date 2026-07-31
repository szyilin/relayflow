## ADDED Requirements

### Requirement: Message sidebar conversation groups

工作台消息页 MUST 提供会话分组侧栏（文案可为「分组」），用于筛选中间会话列表。本版 MUST 支持下列内置分组（实现可用稳定 id：`all` / `unread` / `flagged` / `mention_me` / `tags` / `direct` / `group` / `muted` / `bot`），筛选语义如下（凡未写明「仅未读」者，MUST 不区分已读/未读）：

| 分组 | 筛选 |
|------|------|
| 消息 | 当前用户全部会话 |
| 未读 | inbox 未读数大于 0 |
| 标记 | 已旗标会话 |
| @我 | 仍有未读且包含 @我 的会话；已读清 @ 未读后 MUST 不再出现在本分组 |
| 标签 | 父级可展开；MUST NOT 在仅选中父级「标签」时用「全部打标会话」替代具体标签（本版只能点选具体标签） |
| 单聊 | 类型为单聊（`direct`） |
| 群聊 | 类型为群聊（`group`） |
| 免打扰 | 已开启消息免打扰的会话 |
| 机器人 | 机器人会话（至少含 `bot_dm`） |

本版 MUST NOT 要求实现云文档、话题、已完成等业务分组。

#### Scenario: Switch to unread group

- **WHEN** 成员选中分组「未读」
- **THEN** 会话列表仅展示 inbox 未读大于 0 的会话

#### Scenario: At-me group only unread mentions

- **WHEN** 成员选中分组「@我」且某会话曾有 @我 但已读清 mention 未读
- **THEN** 该会话不出现在「@我」分组结果中

#### Scenario: Flagged group ignores read state

- **WHEN** 成员选中分组「标记」
- **THEN** 列表包含已旗标会话，无论其是否已读

#### Scenario: Direct and group type filters

- **WHEN** 成员分别选中「单聊」「群聊」
- **THEN** 列表分别仅含 `direct`、`group` 类型会话

### Requirement: Tags group lists user tags as selectable children

分组「标签」MUST 可展开，并在其下列出当前用户标签库中的全部标签；成员 MUST 通过点选**具体标签**过滤出会话（不区分已读/未读）。父级「标签」行 MAY 提供新建标签入口（如「+」），与右键打标共用标签库。

#### Scenario: Select a concrete tag filter

- **WHEN** 成员在「标签」下点选标签「项目A」
- **THEN** 会话列表仅展示该用户已打上「项目A」的会话

#### Scenario: Parent tags is not an all-tagged filter

- **WHEN** 成员仅展开或高亮父级「标签」而未选具体标签
- **THEN** 系统 MUST NOT 将结果定义为「所有已打任意标签的会话」的正式筛选（应保持展开态或提示选择具体标签；实现不得用父级冒充全标签汇总筛选）

### Requirement: Edit groups visibility and order

分组栏 MUST 提供设置入口（如齿轮）打开「编辑分组」：可将内置分组在「展示 / 隐藏」间移动，并对展示区排序。默认 MUST 将本版九类内置分组全部置于展示。用户标签子项 MUST NOT 进入编辑列表（随标签库挂在「标签」下）。展示/隐藏与顺序 MUST 经用户偏好持久化并多端同步（C 类）。

#### Scenario: Default all groups visible

- **WHEN** 成员尚无分组栏偏好
- **THEN** 九类内置分组均在展示区且按产品默认顺序出现

#### Scenario: Hide muted group

- **WHEN** 成员在编辑分组中将「免打扰」移入隐藏并保存
- **THEN** 分组栏不再展示「免打扰」项，直到再次展示

#### Scenario: Reorder shown groups

- **WHEN** 成员调整展示区顺序并保存
- **THEN** 分组栏按新顺序渲染展示中的分组
