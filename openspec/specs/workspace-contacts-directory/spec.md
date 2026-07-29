# workspace-contacts-directory Specification

## Purpose

工作台通讯录目录覆盖飞书式分类导航，以及组织内部门钻取、外部联系人、加联系人申请、星标与我的群组。邮箱联系人与完整「发起加好友」产品流不在本能力范围。外部 / 申请 / 星标以 `/app-api/system/contact/*` 为唯一数据真源。
## Requirements
### Requirement: Contacts directory category navigation

工作台 `/app/contacts` 左栏 MUST 提供飞书式分类导航（含图标），至少包含：组织内联系人、外部联系人、新的联系人、星标联系人、我的群组。本切片 MUST NOT 提供邮箱联系人入口。

#### Scenario: Switch category from panel

- **WHEN** 用户点击左栏某一分类
- **THEN** 该分类高亮，主区切换为对应内容，且不展示部门树作为左栏主导航

#### Scenario: Category navigation remains

- **WHEN** 用户打开 `/app/contacts`
- **THEN** 左栏仍为分类导航（组织内 / 外部 / 新的联系人 / 星标 / 我的群组），且无邮箱联系人入口

### Requirement: Organization contacts by department

「组织内联系人」主区 MUST 按部门层级浏览：展示当前部门面包屑、子部门行（可进入下级）、以及当前部门直属成员列表；点成员 MUST 打开个人名片并可发起单聊。组织部门与成员数据 MUST 继续使用既有 `dept/tree` 与 `list-by-dept`。

#### Scenario: Drill into child department

- **WHEN** 用户在组织内主区点击某子部门的「下级」或等价进入操作
- **THEN** 面包屑更新到该部门，并加载其子部门与直属成员

#### Scenario: Open peer business card

- **WHEN** 用户点击某组织内成员
- **THEN** 系统展示 `WorkspaceBusinessCard`，并可通过「消息」打开单聊

### Requirement: External contacts alphabetical list

「外部联系人」主区 MUST 以显示名排序并按首字母分组展示列表。数据 MUST 来自 `GET /app-api/system/contact/external/list`（持久化外部联系人关系）；工作台客户端 MUST 以此为唯一数据真源，MUST NOT 使用 store seed / 常驻 Mock。拉丁字母归入 A–Z，非拉丁可归入 `#`。服务端 SHOULD 返回 `sortLetter`；客户端在缺失时可本地兜底。

#### Scenario: View grouped external contacts from API

- **WHEN** 用户打开「外部联系人」且 API 返回外部联系人
- **THEN** 主区按首字母分组列出联系人名称（显示名、可选 `orgLabel`）

#### Scenario: Empty external list without mock

- **WHEN** 用户打开「外部联系人」且 API 返回空数组
- **THEN** 主区展示空状态，且页面不注入演示用假联系人

#### Scenario: List external contacts via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/external/list`
- **THEN** 系统返回当前租户下、当前用户为 owner 的外部联系人数组；每项含 `id`、`userId`、`displayName`，以及可选 `orgLabel`、`avatarText`、`sortLetter`

### Requirement: New contact friend requests

「新的联系人」主区 MUST 展示加好友/加联系人申请，并显示状态（至少含待处理、已通过、已拒绝）。列表 MUST 来自 `GET /app-api/system/contact/request/list`。点击「同意」MUST 调用 `POST /app-api/system/contact/request/{id}/accept`：将该申请标为已通过，并为当前用户写入对应外部联系人关系（若不存在则创建）；成功后刷新申请与外部联系人。点击「拒绝」MUST 调用 `POST /app-api/system/contact/request/{id}/reject`：将该申请标为 `rejected`，MUST NOT 写入外部联系人。同意与拒绝 MUST NOT 仅改本地内存作为真源。

#### Scenario: List contact requests via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/request/list`
- **THEN** 系统返回指向当前用户的申请列表；每项含 `id`、`fromUserId`、`displayName`、`status`（至少 `pending` \| `accepted` \| `rejected`），以及可选 `subtitle`、`avatarText`、`createTime`

#### Scenario: Accept pending request via backend

- **WHEN** 用户对一条待处理申请点击「同意」且 API 成功
- **THEN** 该条目状态变为已通过，且外部联系人列表可出现该申请人（刷新后仍存在）

#### Scenario: Accept pending request via API

- **WHEN** 当前用户对一条 `pending` 且 `to_user_id` 为自己的申请调用 accept
- **THEN** 该申请 `status` 变为 `accepted`，且外部联系人列表中出现该 `fromUserId`（已存在则保持一条）

#### Scenario: Reject pending request via API

- **WHEN** 当前用户对一条 `pending` 且 `to_user_id` 为自己的申请调用 reject
- **THEN** 该申请 `status` 变为 `rejected`，且外部联系人列表 MUST NOT 因本次操作新增该 `fromUserId`

#### Scenario: Accept non-owned or non-pending request

- **WHEN** 用户对不存在、非指向自己、或非 `pending` 的申请调用 accept 或 reject
- **THEN** 系统拒绝该操作（业务错误，不静默成功）

### Requirement: Starred contacts list

「星标联系人」主区 MUST 展示已星标人员列表，并允许取消星标。列表与变更 MUST 走 `GET/PUT /app-api/system/contact/starred/*`（`PUT` body 含 `starred: true | false`）；MUST NOT 使用 store seed 作为真源。

#### Scenario: List starred contacts via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/starred/list`
- **THEN** 系统返回当前租户下、当前用户星标的人员列表（含 `userId` 与显示字段）

#### Scenario: Star and unstar a contact via API

- **WHEN** 用户对某 `userId` 调用 `PUT` 且 `starred=true`
- **THEN** 星标列表包含该用户；随后 `starred=false` 后该用户不再出现在星标列表

#### Scenario: Unstar a contact via backend

- **WHEN** 用户在星标列表取消某联系人星标且 API 成功
- **THEN** 该联系人从星标列表移除；刷新页面后仍不出现

### Requirement: My groups from conversations

「我的群组」主区 MUST 展示当前用户参与的群会话；MUST 使用 IM 会话列表中 `type=group` 的项作为真源，MUST NOT 使用 store 临时群项填充。点击群组 MUST 进入消息页对应会话。

#### Scenario: Open group conversation

- **WHEN** 用户在「我的群组」点击某一真实群会话
- **THEN** 系统导航到 `/app/messages` 并选中该群会话

### Requirement: Contact enrich APIs tenant and member scope

外部联系人、申请与星标的读写 MUST 限定在当前 JWT 租户与当前用户；MUST NOT 读写其他用户的关系数据。未登录或非有效组织成员 MUST NOT 访问这些 `/app-api/system/contact/*` 端点。

#### Scenario: Reject unauthenticated contact list

- **WHEN** 无有效 JWT 请求任一 contact enrich 列表端点
- **THEN** 系统拒绝访问（与现有 `/app-api` 鉴权一致）

### Requirement: Add external contact from hub and directory web

工作台 MUST 可从 Create Hub「添加外部联系人」与通讯录「外部联系人」入口打开同一添加流程。用户 MUST 能通过确切手机号或邮箱搜索账号；同一标识多账号 MUST 全部展示。点击添加 MUST 调用 `POST /app-api/system/contact/request` 发起申请。已是外部联系人或已有发出的 pending MUST 显示「已添加」。搜索与发起 MUST NOT 使用客户端假账号草稿。

#### Scenario: Search exact mobile

- **WHEN** 用户输入 11 位手机号并搜索
- **THEN** 主区列出服务端返回的匹配账号（可含多企业标签）或空状态

#### Scenario: Send add request

- **WHEN** 用户对未添加账号点击「添加」且服务端成功
- **THEN** 该条变为已添加/待处理态，并提示申请已发送；目标用户 `request/list` 可见 pending

### Requirement: Reject contact request web

「新的联系人」对 pending 申请 MUST 提供拒绝操作。拒绝 MUST 调用 `POST /app-api/system/contact/request/{id}/reject`，成功后该条 `status=rejected` 且不得写入外部联系人。

#### Scenario: Reject pending

- **WHEN** 用户点击拒绝且成功
- **THEN** 该条状态为已拒绝，且无「同意」按钮

### Requirement: Search account by mobile or email

系统 MUST 提供 `GET /app-api/system/contact/search-account`，以确切 `mobile` **或** `email`（二选一）匹配账号。同一标识多账号 MUST 全部返回；无匹配 MUST 返回空数组。每项 MUST 含 `userId`、`displayName`、`orgLabel`、`avatarText`、`alreadyAdded`（对本租户当前用户而言已是外部联系人或已有发出的 pending）。MUST NOT 支持模糊搜索。

#### Scenario: Exact mobile multi-hit

- **WHEN** 有效成员以 11 位手机号查询且系统存在多个绑定该号的用户
- **THEN** 响应包含全部匹配项，且各自带 `alreadyAdded`

#### Scenario: No match

- **WHEN** 查询的确切手机号或邮箱无对应用户
- **THEN** `data` 为空数组且 `code=0`

### Requirement: Create contact request

系统 MUST 提供 `POST /app-api/system/contact/request`，body 含 `targetUserId` 与可选 `message`。成功后目标用户在其 `request/list` 中可见 `pending` 申请。若已是外部联系人或已有 pending，MUST 返回业务错误。申请人 MUST NOT 向自己发起申请。

#### Scenario: Create pending request

- **WHEN** 有效成员对未关联用户发起申请且成功
- **THEN** 目标用户的申请列表出现对应 `pending` 项

#### Scenario: Duplicate or self request rejected

- **WHEN** 对已是外部联系人、已有 pending、或不存在/自己的 `targetUserId` 发起申请
- **THEN** 系统拒绝（业务错误）

