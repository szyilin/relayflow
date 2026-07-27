## ADDED Requirements

### Requirement: Contacts directory category navigation

工作台 `/app/contacts` 左栏 MUST 提供飞书式分类导航（含图标），至少包含：组织内联系人、外部联系人、新的联系人、星标联系人、我的群组。本切片 MUST NOT 提供邮箱联系人入口。

#### Scenario: Switch category from panel

- **WHEN** 用户点击左栏某一分类
- **THEN** 该分类高亮，主区切换为对应内容，且不展示部门树作为左栏主导航

### Requirement: Organization contacts by department

「组织内联系人」主区 MUST 按部门层级浏览：展示当前部门面包屑、子部门行（可进入下级）、以及当前部门直属成员列表；点成员 MUST 打开个人名片并可发起单聊。组织部门与成员数据 MUST 继续使用既有 `dept/tree` 与 `list-by-dept`。

#### Scenario: Drill into child department

- **WHEN** 用户在组织内主区点击某子部门的「下级」或等价进入操作
- **THEN** 面包屑更新到该部门，并加载其子部门与直属成员

#### Scenario: Open peer business card

- **WHEN** 用户点击某组织内成员
- **THEN** 系统展示 `WorkspaceBusinessCard`，并可通过「消息」打开单聊

### Requirement: External contacts alphabetical list

「外部联系人」主区 MUST 以显示名排序并按首字母分组展示列表。`-web` 阶段可用 store 临时数据；拉丁字母归入 A–Z，非拉丁可归入 `#`。

#### Scenario: View grouped external contacts

- **WHEN** 用户打开「外部联系人」且存在临时/真实外部联系人
- **THEN** 主区按首字母分组列出联系人名称

### Requirement: New contact friend requests

「新的联系人」主区 MUST 展示加好友/加联系人申请，并显示状态（至少含待处理与已通过）。`-web` 阶段可用 store 临时数据，并允许本地将待处理标记为已通过。

#### Scenario: Accept pending request locally

- **WHEN** 用户对一条待处理申请点击「同意」
- **THEN** 该条目状态变为已通过（本切片可为本地状态）

### Requirement: Starred contacts list

「星标联系人」主区 MUST 展示已星标人员列表，并允许取消星标。`-web` 阶段可用 store 临时数据。

#### Scenario: Unstar a contact

- **WHEN** 用户在星标列表取消某联系人星标
- **THEN** 该联系人从星标列表移除（本切片可为本地状态）

### Requirement: My groups from conversations

「我的群组」主区 MUST 展示当前用户参与的群会话；优先使用 IM 会话列表中 `type=group` 的项，不足时可辅以 store 临时项。点击群组 MUST 进入消息页对应会话。

#### Scenario: Open group conversation

- **WHEN** 用户在「我的群组」点击某一群
- **THEN** 系统导航到 `/app/messages` 并选中该群会话（若为临时项且无真实会话 ID，则可仅展示占位或 toast）
