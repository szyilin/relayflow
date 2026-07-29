## ADDED Requirements

### Requirement: Workspace modal body layout stability

工作台用户可见的模态框 MUST 在内容增减（loading、空态、展开列表、切换表单项）时保持外壳在视口中的位置与高度稳定。模态框 MUST 通过内容区最大高度限制与区内滚动展示完整内容，MUST NOT 因内容高度骤变导致整窗明显上下跳动。关键切换态（loading / empty / 主列表）SHOULD 使用一致的最小高度以避免塌缩。

#### Scenario: Content expands without shell jump

- **WHEN** 用户在已打开的工作台模态框内触发导致内容显著增多的操作（如展开成员来源、切换群类型显示额外字段）
- **THEN** 模态框外壳高度与垂直位置保持稳定（变化由内容区内滚动或预留 min-height 吸收）
- **AND** 用户仍可滚动访问全部新增内容

#### Scenario: Loading and empty states reserve height

- **WHEN** 模态框在 loading 骨架、空态与数据列表之间切换
- **THEN** 内容区最小高度保持一致或接近，避免外壳明显塌缩或拉长

#### Scenario: Create group and add-bot modals apply the pattern

- **WHEN** 用户打开「创建群组」或「添加机器人」模态框并切换其内部状态
- **THEN** 上述稳定布局要求适用
