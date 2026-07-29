## ADDED Requirements

### Requirement: Add external contact from hub and directory web

工作台 MUST 可从 Create Hub「添加外部联系人」与通讯录「外部联系人」入口打开同一添加流程。用户 MUST 能通过确切手机号或邮箱搜索账号；同一标识多账号 MUST 全部展示。点击添加 MUST 记录发起申请（`-web` 可为客户端草稿）。已是外部联系人或已有发出的 pending MUST 显示「已添加」。

#### Scenario: Search exact mobile

- **WHEN** 用户输入 11 位手机号并搜索
- **THEN** 主区列出匹配账号（可含多企业标签）或空状态

#### Scenario: Send add request draft

- **WHEN** 用户对未添加账号点击「添加」
- **THEN** 该条变为已添加/待处理态，并提示申请已发送

### Requirement: Reject contact request web

「新的联系人」对 pending 申请 MUST 提供拒绝操作。`-web` 可将状态更新为 `rejected` 并不得写入外部联系人。

#### Scenario: Reject pending

- **WHEN** 用户点击拒绝且成功
- **THEN** 该条状态为已拒绝，且无「同意」按钮
