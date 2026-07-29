## MODIFIED Requirements

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
