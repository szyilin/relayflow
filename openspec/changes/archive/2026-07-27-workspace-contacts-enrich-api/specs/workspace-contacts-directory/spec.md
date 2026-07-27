## MODIFIED Requirements

### Requirement: External contacts alphabetical list

「外部联系人」主区 MUST 以显示名排序并按首字母分组展示列表。数据 MUST 来自 `GET /app-api/system/contact/external/list`（持久化外部联系人关系）；拉丁字母归入 A–Z，非拉丁可归入 `#`。服务端 SHOULD 返回 `sortLetter`；客户端在缺失时可本地兜底。

#### Scenario: View grouped external contacts

- **WHEN** 用户打开「外部联系人」且当前用户存在外部联系人关系
- **THEN** 主区按首字母分组列出联系人名称（显示名、可选 `orgLabel`）

#### Scenario: List external contacts via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/external/list`
- **THEN** 系统返回当前租户下、当前用户为 owner 的外部联系人数组；每项含 `id`、`userId`、`displayName`，以及可选 `orgLabel`、`avatarText`、`sortLetter`

### Requirement: New contact friend requests

「新的联系人」主区 MUST 展示加好友/加联系人申请，并显示状态（至少含待处理与已通过）。数据 MUST 来自 `GET /app-api/system/contact/request/list`。同意操作 MUST 调用 `POST /app-api/system/contact/request/{id}/accept`：将该申请标为已通过，并为当前用户写入对应外部联系人关系（若不存在则创建）。

#### Scenario: List contact requests via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/request/list`
- **THEN** 系统返回指向当前用户的申请列表；每项含 `id`、`fromUserId`、`displayName`、`status`（至少 `pending` \| `accepted`），以及可选 `subtitle`、`avatarText`、`createTime`

#### Scenario: Accept pending request via API

- **WHEN** 当前用户对一条 `pending` 且 `to_user_id` 为自己的申请调用 accept
- **THEN** 该申请 `status` 变为 `accepted`，且外部联系人列表中出现该 `fromUserId`（已存在则保持一条）

#### Scenario: Accept non-owned or non-pending request

- **WHEN** 用户对不存在、非指向自己、或非 `pending` 的申请调用 accept
- **THEN** 系统拒绝该操作（业务错误，不静默成功）

### Requirement: Starred contacts list

「星标联系人」主区 MUST 展示已星标人员列表，并允许取消星标。列表 MUST 来自 `GET /app-api/system/contact/starred/list`；设/取消星标 MUST 使用 `PUT /app-api/system/contact/starred/{userId}`，body 含 `starred: true | false`。

#### Scenario: List starred contacts via API

- **WHEN** 有效组织成员请求 `GET /app-api/system/contact/starred/list`
- **THEN** 系统返回当前租户下、当前用户星标的人员列表（含 `userId` 与显示字段）

#### Scenario: Star and unstar a contact via API

- **WHEN** 用户对某 `userId` 调用 `PUT` 且 `starred=true`
- **THEN** 星标列表包含该用户；随后 `starred=false` 后该用户不再出现在星标列表

## ADDED Requirements

### Requirement: Contact enrich APIs tenant and member scope

外部联系人、申请与星标的读写 MUST 限定在当前 JWT 租户与当前用户；MUST NOT 读写其他用户的关系数据。未登录或非有效组织成员 MUST NOT 访问这些 `/app-api/system/contact/*` 端点。

#### Scenario: Reject unauthenticated contact list

- **WHEN** 无有效 JWT 请求任一 contact enrich 列表端点
- **THEN** 系统拒绝访问（与现有 `/app-api` 鉴权一致）
