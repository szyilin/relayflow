## MODIFIED Requirements

### Requirement: External contacts alphabetical list

「外部联系人」主区 MUST 以显示名排序并按首字母分组展示列表。工作台客户端 MUST 以 `GET /app-api/system/contact/external/list` 为唯一数据真源，MUST NOT 使用 store seed / 常驻 Mock。拉丁字母归入 A–Z，非拉丁可归入 `#`（可用服务端 `sortLetter` 或本地兜底）。

#### Scenario: View grouped external contacts from API

- **WHEN** 用户打开「外部联系人」且 API 返回外部联系人
- **THEN** 主区按首字母分组列出联系人名称

#### Scenario: Empty external list without mock

- **WHEN** 用户打开「外部联系人」且 API 返回空数组
- **THEN** 主区展示空状态，且页面不注入演示用假联系人

### Requirement: New contact friend requests

「新的联系人」主区 MUST 展示加好友/加联系人申请，并显示状态（至少含待处理与已通过）。列表 MUST 来自 `GET /app-api/system/contact/request/list`。点击「同意」MUST 调用 `POST /app-api/system/contact/request/{id}/accept`，成功后刷新申请与外部联系人；MUST NOT 仅改本地内存作为真源。

#### Scenario: Accept pending request via backend

- **WHEN** 用户对一条待处理申请点击「同意」且 API 成功
- **THEN** 该条目状态变为已通过，且外部联系人列表可出现该申请人（刷新后仍存在）

### Requirement: Starred contacts list

「星标联系人」主区 MUST 展示已星标人员列表，并允许取消星标。列表与变更 MUST 走 `GET/PUT /app-api/system/contact/starred/*`；MUST NOT 使用 store seed 作为真源。

#### Scenario: Unstar a contact via backend

- **WHEN** 用户在星标列表取消某联系人星标且 API 成功
- **THEN** 该联系人从星标列表移除；刷新页面后仍不出现

### Requirement: My groups from conversations

「我的群组」主区 MUST 展示当前用户参与的群会话；MUST 使用 IM 会话列表中 `type=group` 的项作为真源，MUST NOT 使用 store 临时群项填充。点击群组 MUST 进入消息页对应会话。

#### Scenario: Open group conversation

- **WHEN** 用户在「我的群组」点击某一真实群会话
- **THEN** 系统导航到 `/app/messages` 并选中该群会话

## ADDED Requirements

### Requirement: Contacts directory purpose

工作台通讯录目录 MUST 覆盖飞书式分类导航，以及组织内部门钻取、外部联系人、加联系人申请、星标与我的群组。本能力 MUST NOT 提供邮箱联系人入口，MUST NOT 把完整「发起加好友」产品流纳入本切片。外部 / 申请 / 星标在 integrate 后 MUST 以 `/app-api/system/contact/*` 为唯一数据真源。

#### Scenario: Category navigation remains

- **WHEN** 用户打开 `/app/contacts`
- **THEN** 左栏仍为分类导航（组织内 / 外部 / 新的联系人 / 星标 / 我的群组），且无邮箱联系人入口
