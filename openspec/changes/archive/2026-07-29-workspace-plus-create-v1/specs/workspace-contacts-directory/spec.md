## ADDED Requirements

### Requirement: Search account to add as external contact

系统 MUST 允许有效组织成员通过**确切**手机号或邮箱搜索可添加的账号。同一标识绑定多个账号时 MUST 全部返回（含可区分的企业/组织标签）。MUST NOT 提供仅依赖模糊关键字的全站用户枚举搜索作为本能力入口。

#### Scenario: Exact mobile returns multiple accounts

- **WHEN** 用户输入已绑定多个账号的确切手机号并搜索
- **THEN** 系统列出全部匹配账号，每条可执行「添加」或展示「已添加」

#### Scenario: Unknown mobile empty

- **WHEN** 用户搜索未注册或不存在的确切手机号
- **THEN** 系统返回空结果，MUST NOT 泄露其它用户信息

### Requirement: Create contact friend request

系统 MUST 提供发起加联系人申请的 API 与 UI（Create Hub「添加外部联系人」与通讯录入口共用）。发起成功后目标用户的「新的联系人」列表 MUST 可见该 pending 申请。已是外部联系人或已有 pending 时 MUST 幂等或明确拒绝（实现二选一并在契约写明）。

#### Scenario: Send request from search result

- **WHEN** 用户对搜索结果中某人点击「添加」且成功
- **THEN** 该条展示为已添加或待对方处理，且对方申请列表出现 pending 项

### Requirement: Reject contact friend request

「新的联系人」除同意外 MUST 支持拒绝待处理申请；拒绝后状态为 `rejected`（或等价），MUST NOT 写入外部联系人关系。

#### Scenario: Reject pending request

- **WHEN** 用户对 pending 申请点击拒绝且成功
- **THEN** 该申请不再可被同意为新的外部关系，列表状态为已拒绝
