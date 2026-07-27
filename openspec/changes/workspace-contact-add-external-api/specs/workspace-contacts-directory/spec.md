## MODIFIED Requirements

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

## ADDED Requirements

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
